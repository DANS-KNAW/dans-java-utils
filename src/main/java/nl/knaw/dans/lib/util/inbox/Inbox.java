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
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * An inbox is a directory that is monitored for new files. When a new file is detected, a task is created to process the file.
 */
@Slf4j
public class Inbox extends FileAlterationListenerAdaptor implements Managed {
    @NonNull
    private final Path inbox;
    private final IOFileFilter fileFilter;
    @NonNull
    private final InboxTaskFactory taskFactory;
    private final Runnable onPollingHandler;
    @NonNull
    private final ExecutorService executorService;
    @NonNull
    private final Comparator<Path> inboxItemComparator;

    private final FileAlterationMonitor monitor;

    @Builder
    private Inbox(Path inbox, IOFileFilter fileFilter, InboxTaskFactory taskFactory, Runnable onPollingHandler, int interval, ExecutorService executorService, Comparator<Path> inboxItemComparator) {
        this.inbox = inbox;
        this.fileFilter = fileFilter == null ? CustomFileFilters.subDirectoryOf(inbox) : fileFilter;
        this.taskFactory = taskFactory;
        this.onPollingHandler = onPollingHandler == null ?
            () -> {
            } : onPollingHandler;
        this.executorService = executorService == null ? Executors.newSingleThreadExecutor() : executorService;
        this.inboxItemComparator = inboxItemComparator == null ? Comparator.comparing(Path::getFileName) : inboxItemComparator;
        this.monitor = new FileAlterationMonitor(interval == 0 ? 1000 : interval);
    }

    @Override
    public void start() throws Exception {
        log.info("Starting Inbox at '{}'", this.inbox);

        try {
            log.debug("Starting file alteration monitor for path '{}'", this.inbox);
            processFilesBeforeStart();
            startFileAlterationMonitor();
        }
        catch (IOException e) {
            log.error(e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public void stop() throws Exception {
        log.info("Stopping Inbox at '{}'", this.inbox);
        monitor.stop();
    }

    @Override
    public void onFileCreate(File file) {
        log.debug("New inbox item detected at: {}", file);
        executorService.submit(taskFactory.createInboxTask(file.toPath()));
    }

    @Override
    public void onDirectoryCreate(File directory) {
        log.debug("New inbox item detected at: {}", directory);
        executorService.submit(taskFactory.createInboxTask(directory.toPath()));
    }

    @Override
    public void onStart(FileAlterationObserver observer) {
        onPollingHandler.run();
    }

    private void processFilesBeforeStart() throws IOException {
        try (Stream<Path> files = Files.list(inbox)) {
            for (Path path : files.sorted(inboxItemComparator).collect(Collectors.toList())) {
                try {
                    executorService.submit(taskFactory.createInboxTask(path));
                }
                catch (Exception e) {
                    log.error("Error processing inbox item: {}", path, e);
                }
            }
        }
    }

    private void startFileAlterationMonitor() throws Exception {
        FileAlterationObserver observer = new FileAlterationObserver(inbox.toFile(), fileFilter);
        observer.addListener(this);

        monitor.addObserver(observer);
        monitor.start();
    }
}
