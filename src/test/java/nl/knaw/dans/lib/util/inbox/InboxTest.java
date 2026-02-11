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

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import nl.knaw.dans.lib.util.AbstractTestWithTestDir;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@Slf4j
public class InboxTest extends AbstractTestWithTestDir {
    private final InboxTaskFactory inboxTaskFactoryMock = Mockito.mock(InboxTaskFactory.class);

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        Mockito.reset(inboxTaskFactoryMock);
    }

    @Data
    @Slf4j
    private static class BooleanTask implements Runnable {
        private boolean done = false;

        @Override
        public void run() {
            log.debug("Running BooleanTask");
            done = true;
        }
    }

    @Test
    public void inbox_picks_up_files() throws Exception {
        // Given
        Path inboxDir = testDir.resolve("inbox");
        Files.createDirectory(inboxDir);
        Inbox inbox = Inbox.builder()
            .inbox(inboxDir)
            .fileFilter(FileFilterUtils.fileFileFilter())
            .taskFactory(inboxTaskFactoryMock)
            .build();
        BooleanTask t = new BooleanTask();
        when(inboxTaskFactoryMock.createInboxTask(any())).thenReturn(t);
        inbox.start();

        // Give the inbox some time to start
        Thread.sleep(1000);

        // When
        Files.createFile(inboxDir.resolve("file1.txt"));

        // Give the inbox some time to pick up the file
        Thread.sleep(1000);

        // Then
        assertThat(t.done).isTrue();
    }

    @Test
    public void inbox_picks_up_files_already_in_inbox_before_starting() throws Exception {
        // Given
        Path inboxDir = testDir.resolve("inbox");
        Files.createDirectory(inboxDir);
        Files.createFile(inboxDir.resolve("file1.txt"));
        Inbox inbox = Inbox.builder()
            .inbox(inboxDir)
            .fileFilter(FileFilterUtils.fileFileFilter())
            .taskFactory(inboxTaskFactoryMock)
            .build();
        BooleanTask t = new BooleanTask();
        when(inboxTaskFactoryMock.createInboxTask(any())).thenReturn(t);
        inbox.start();

        // Give the inbox some time to pick up the file
        Thread.sleep(1000);

        // Then
        assertThat(t.done).isTrue();
    }

    @Test
    public void onPollingHandler_is_called_before_each_polling_run() throws Exception {
        // Given
        Path inboxDir = testDir.resolve("inbox");
        Files.createDirectory(inboxDir);
        AtomicInteger pollingHandlerCallCount = new AtomicInteger(0);
        Inbox inbox = Inbox.builder()
            .inbox(inboxDir)
            .fileFilter(FileFilterUtils.fileFileFilter())
            .taskFactory(inboxTaskFactoryMock)
            .interval(10)
            .onPollingHandler(() -> {
                log.debug("onPollingHandler called {} times", pollingHandlerCallCount.get());
                pollingHandlerCallCount.incrementAndGet();
            })
            .build();
        BooleanTask t = new BooleanTask();
        when(inboxTaskFactoryMock.createInboxTask(any())).thenReturn(t);
        inbox.start();

        // Give the inbox some time to start and poll multiple times
        Thread.sleep(1000);

        // Then
        assertThat(t.done).isFalse(); // Ensure the task was not executed
        // Ensure it was called multiple times. It is not possible to predict the exact number of calls, but more than 10 times seems reasonable.
        assertThat(pollingHandlerCallCount.get()).isGreaterThan(10);
    }

    @Test
    public void inbox_retries_start_when_directory_is_missing_then_becomes_available() throws Exception {
        // Given
        Path inboxDir = testDir.resolve("inbox-missing"); // do not create it yet
        BooleanTask t = new BooleanTask();
        when(inboxTaskFactoryMock.createInboxTask(any())).thenReturn(t);
        Inbox inbox = Inbox.builder()
            .inbox(inboxDir)
            .fileFilter(FileFilterUtils.fileFileFilter())
            .taskFactory(inboxTaskFactoryMock)
            .interval(50)
            .startupGracePeriodMillis(100)
            .build();

        // When: start while inbox dir does not exist
        inbox.start();
        // Wait a bit longer than grace period to ensure at least one retry
        Thread.sleep(150);

        // Now create the inbox directory and a file to be picked up
        Files.createDirectory(inboxDir);
        Files.createFile(inboxDir.resolve("file1.txt"));

        // Give the inbox some time to detect the file after becoming available
        Thread.sleep(500);

        // Then
        assertThat(t.isDone()).isTrue();
    }

    @Test
    public void inbox_retries_start_when_directory_is_unreadable_then_becomes_readable() throws Exception {
        // Given
        Path inboxDir = testDir.resolve("inbox-unreadable");
        Files.createDirectory(inboxDir);
        BooleanTask t = new BooleanTask();
        when(inboxTaskFactoryMock.createInboxTask(any())).thenReturn(t);
        Inbox inbox = Inbox.builder()
            .inbox(inboxDir)
            .fileFilter(FileFilterUtils.fileFileFilter())
            .taskFactory(inboxTaskFactoryMock)
            .interval(50)
            .startupGracePeriodMillis(100)
            .build();

        // Make directory unreadable for the current process (POSIX systems)
        try {
            Files.setPosixFilePermissions(inboxDir, java.util.Set.of());
        }
        catch (UnsupportedOperationException e) {
            // Non-POSIX file system; skip this test
            log.warn("POSIX file permissions not supported; skipping unreadable directory test");
            return;
        }

        // When: start while inbox dir is unreadable
        inbox.start();
        Thread.sleep(150);

        // Restore read permission and create a file
        Files.setPosixFilePermissions(inboxDir, java.util.Set.of(java.nio.file.attribute.PosixFilePermission.OWNER_READ,
            java.nio.file.attribute.PosixFilePermission.OWNER_WRITE,
            java.nio.file.attribute.PosixFilePermission.OWNER_EXECUTE));
        Files.createFile(inboxDir.resolve("file2.txt"));

        // Give the inbox some time to detect the file after permissions fixed
        Thread.sleep(500);

        // Then
        assertThat(t.isDone()).isTrue();
    }
}
