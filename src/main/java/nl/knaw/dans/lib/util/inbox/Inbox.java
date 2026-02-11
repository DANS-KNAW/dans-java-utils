/*
 * Copyright (C) 2021 DANS - Data Archiving and Networked Services (info@dans.knaw.nl)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.knaw.dans.lib.util.inbox;

import io.dropwizard.lifecycle.Managed;
import lombok.Builder;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import nl.knaw.dans.lib.util.CustomFileFilters;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.apache.commons.io.monitor.FileEntry;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * <p>
 * A managed inbox that monitors a directory for new files and directories, processes them using a provided task factory, and allows for custom file filtering. A managed inbox that monitors a
 * directory for new files and/or directories, processes them using a provided task factory, and allows for custom file filtering. Note, however, that the inbox does not support recursive monitoring
 * of subdirectories; it only processes files and directories directly within the specified inbox directory.
 * </p>
 * <p>
 * The inbox can be started and stopped, and it supports initial processing of existing items in the inbox.
 * </p>
 */
@Slf4j
public class Inbox extends FileAlterationListenerAdaptor implements Managed {
    @NonNull
    private final FileEntry inboxFileEntry;
    private final IOFileFilter fileFilter;
    @NonNull
    private final InboxTaskFactory taskFactory;
    private final Runnable onPollingHandler;
    @NonNull
    private final ExecutorService executorService;
    @NonNull
    private final Comparator<Path> inboxItemComparator;

    private final FileAlterationMonitor monitor;
    private final CountDownLatch awaitLatch;
    private final List<Path> createdFilesAndDirectories = new LinkedList<>();
    private boolean initialItemsProcessed = false;
    private final int startupGracePeriodMillis;

    @Builder
    private Inbox(Path inbox, IOFileFilter fileFilter, @NonNull InboxTaskFactory taskFactory, Runnable onPollingHandler, int interval, ExecutorService executorService, Comparator<Path> inboxItemComparator,
        CountDownLatch awaitLatch, Integer startupGracePeriodMillis) {
        this.inboxFileEntry = new FileEntry(inbox.toFile());
        this.fileFilter = fileFilter == null ? CustomFileFilters.subDirectoryOf(inbox) : FileFilterUtils.and(fileFilter, CustomFileFilters.childOf(inbox));
        this.taskFactory = taskFactory;
        this.onPollingHandler = onPollingHandler == null ? () -> {
        } : onPollingHandler;
        this.executorService = executorService == null ? Executors.newSingleThreadExecutor() : executorService;
        this.inboxItemComparator = inboxItemComparator == null ? Comparator.comparing(Path::getFileName) : inboxItemComparator;
        this.monitor = new FileAlterationMonitor(interval == 0 ? 1000 : interval);
        this.awaitLatch = awaitLatch;
        this.startupGracePeriodMillis = startupGracePeriodMillis == null || startupGracePeriodMillis <= 0 ? 10_000 : startupGracePeriodMillis;
    }

    @Override
    public void start() throws Exception {
        if (awaitLatch != null) {
            log.info("Waiting for latch to be released before starting inbox");
            awaitLatch.await();
        }
        log.info("Starting Inbox at '{}'", this.inboxFileEntry.getFile());

        // Start asynchronously and retry until inbox becomes available
        executorService.submit(this::awaitInboxAndStartMonitor);
    }

    private void awaitInboxAndStartMonitor() {
        var inboxPath = inboxFileEntry.getFile().toPath();
        while (true) {
            if (!Files.exists(inboxPath)) {
                log.warn("Inbox directory does not exist: {}. Retrying in {} ms", inboxPath, startupGracePeriodMillis);
            }
            else if (!Files.isDirectory(inboxPath)) {
                log.warn("Inbox path is not a directory: {}. Retrying in {} ms", inboxPath, startupGracePeriodMillis);
            }
            else if (!Files.isReadable(inboxPath)) {
                log.warn("Inbox directory is not readable: {}. Retrying in {} ms", inboxPath, startupGracePeriodMillis);
            }
            else {
                try {
                    log.debug("Starting file alteration monitor for path '{}'", this.inboxFileEntry.getFile());
                    startFileAlterationMonitor();
                    return;
                }
                catch (IOException e) {
                    log.error("Failed to start file alteration monitor: {}. Retrying in {} ms", e.getMessage(), startupGracePeriodMillis);
                }
                catch (Exception e) {
                    log.error("Unexpected error while starting inbox monitor", e);
                }
            }
            try {
                Thread.sleep(startupGracePeriodMillis);
            }
            catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                log.warn("Inbox startup retry interrupted");
                return;
            }
        }
    }

    @Override
    public void stop() throws Exception {
        log.info("Stopping Inbox at '{}'", this.inboxFileEntry.getFile());
        monitor.stop();
    }

    @Override
    public void onFileCreate(File file) {
        log.debug("New inbox item detected at: {}", file);
        createdFilesAndDirectories.add(file.toPath());
    }

    @Override
    public void onDirectoryCreate(File directory) {
        log.debug("New inbox item detected at: {}", directory);
        createdFilesAndDirectories.add(directory.toPath());
    }

    @Override
    public void onStart(FileAlterationObserver observer) {
        log.debug("Start polling round for inbox at: {}", inboxFileEntry.getFile());
        processInitialItems();
        onPollingHandler.run();
    }

    @Override
    public void onStop(FileAlterationObserver observer) {
        log.debug("Processing {} created files and directories", createdFilesAndDirectories.size());
        createdFilesAndDirectories.sort(inboxItemComparator);
        for (Path file : createdFilesAndDirectories) {
            log.debug("Processing created file: {}", file);
            executorService.submit(taskFactory.createInboxTask(file));
        }
        createdFilesAndDirectories.clear();
    }

    private void processInitialItems() {
        if (initialItemsProcessed) {
            return;
        }
        try {
            List<Path> filesToProcess = new ArrayList<>();
            Path inboxPath = inboxFileEntry.getFile().toPath();

            try (var stream = Files.list(inboxPath)) {
                stream.filter(path -> fileFilter.accept(path.toFile()))
                    .forEach(filesToProcess::add);
            }

            filesToProcess.sort(inboxItemComparator);

            for (Path file : filesToProcess) {
                log.debug("Initial inbox item detected at: {}", file);
                executorService.submit(taskFactory.createInboxTask(file));
            }
        }
        catch (IOException e) {
            throw new RuntimeException("Error processing initial items in inbox", e);
        }
        finally {
            initialItemsProcessed = true;
            log.debug("Initial items processed");
        }
    }

    private void startFileAlterationMonitor() throws Exception {
        FileAlterationObserver observer = FileAlterationObserver.builder()
            .setRootEntry(inboxFileEntry)
            .setFileFilter(fileFilter)
            .get();

        observer.addListener(this);
        monitor.addObserver(observer);
        monitor.start();
    }
}