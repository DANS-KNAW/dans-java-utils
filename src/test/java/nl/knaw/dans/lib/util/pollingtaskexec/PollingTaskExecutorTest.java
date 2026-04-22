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
package nl.knaw.dans.lib.util.pollingtaskexec;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PollingTaskExecutorTest {

    private ScheduledExecutorService scheduler;
    private TaskSource<String> taskSource;
    private TaskFactory<String> taskFactory;
    private PollingTaskExecutor<String> executor;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        scheduler = mock(ScheduledExecutorService.class);
        taskSource = mock(TaskSource.class);
        taskFactory = mock(TaskFactory.class);
        executor = new PollingTaskExecutor<>("test-executor", scheduler, Duration.ofMillis(100), taskSource, taskFactory);
    }

    @Test
    @SuppressWarnings("unchecked")
    void start_should_schedule_tick_with_fixed_delay() {
        executor.start();

        verify(scheduler).scheduleWithFixedDelay(any(Runnable.class), eq(0L), eq(100L), eq(TimeUnit.MILLISECONDS));
    }

    @Test
    @SuppressWarnings("unchecked")
    void stop_should_cancel_future_and_shutdown_scheduler() {
        ScheduledFuture future = mock(ScheduledFuture.class);
        when(scheduler.scheduleWithFixedDelay(any(Runnable.class), anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(future);

        executor.start();
        executor.stop();

        verify(future).cancel(false);
        verify(scheduler).shutdown();
    }

    @Test
    void stop_without_start_should_only_shutdown_scheduler() {
        executor.stop();

        verify(scheduler).shutdown();
    }

    @Test
    void tick_should_poll_source_create_task_and_run_it() {
        String record = "test-record";
        Runnable task = mock(Runnable.class);

        when(taskSource.nextInput()).thenReturn(Optional.of(record));
        when(taskFactory.create(record)).thenReturn(task);

        executor.tick();

        verify(taskSource).nextInput();
        verify(taskFactory).create(record);
        verify(task).run();
    }

    @Test
    void tick_should_do_nothing_if_source_is_empty() {
        when(taskSource.nextInput()).thenReturn(Optional.empty());

        executor.tick();

        verify(taskSource).nextInput();
        verify(taskFactory, never()).create(any());
    }

    @Test
    void tick_should_catch_and_log_exceptions() {
        when(taskSource.nextInput()).thenThrow(new RuntimeException("test exception"));

        // Should not throw exception
        executor.tick();

        verify(taskSource).nextInput();
    }

    @Test
    void copy_constructor_should_copy_fields() {
        PollingTaskExecutor<String> copy = new PollingTaskExecutor<>(executor);

        // Verify it works as expected by running a tick on the copy
        String record = "test-record";
        Runnable task = mock(Runnable.class);
        when(taskSource.nextInput()).thenReturn(Optional.of(record));
        when(taskFactory.create(record)).thenReturn(task);

        copy.tick();

        verify(taskSource).nextInput();
        verify(taskFactory).create(record);
        verify(task).run();
    }

    @Test
    @SuppressWarnings("unchecked")
    void copy_constructor_should_throw_if_source_is_running() {
        ScheduledFuture future = mock(ScheduledFuture.class);
        when(scheduler.scheduleWithFixedDelay(any(Runnable.class), anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(future);

        executor.start();

        assertThatThrownBy(() -> new PollingTaskExecutor<>(executor))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Cannot copy a running executor");
    }
}
