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

import io.dropwizard.hibernate.UnitOfWork;
import io.dropwizard.lifecycle.Managed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * A task executor that repeatedly polls a task source for new work items at a fixed interval and executes them using a provided task factory. This class is designed to manage the lifecycle of a
 * polling process that retrieves tasks and executes them in a controlled manner.
 *
 * @param <R> the type of the task records returned by the task source
 */
@Slf4j
@RequiredArgsConstructor
public class PollingTaskExecutor<R> implements Managed {
    private final String name;
    private final ScheduledExecutorService scheduledExecutorService;
    private final Duration pollingInterval;
    private final TaskSource<R> taskSource;
    private final TaskFactory<R> taskFactory;
    private final TaskScheduler taskScheduler;

    private ScheduledFuture<?> future;

    public PollingTaskExecutor(String name, ScheduledExecutorService scheduledExecutorService, Duration pollingInterval, TaskSource<R> taskSource, TaskFactory<R> taskFactory) {
        this(name, scheduledExecutorService, pollingInterval, taskSource, taskFactory, new ImmediateTaskScheduler());
    }

    /**
     * Copy constructor. The source executor must not be running. The purpose of this constructor is only to be able to wrap a PollingTaskExecutor in a UnitOfWorkAwareProxy. In general, no copies
     * should be created of a PollingTaskExecutor, and in particular should the scheduler not be shared among PollingTaskExecutors.
     *
     * @param other the source executor
     */
    @SuppressWarnings("CopyConstructorMissesField") // future is not copied on purpose
    public PollingTaskExecutor(PollingTaskExecutor<R> other) {
        if (other.future != null) {
            throw new IllegalArgumentException("Cannot copy a running executor");
        }
        this.name = other.name;
        this.scheduledExecutorService = other.scheduledExecutorService;
        this.pollingInterval = other.pollingInterval;
        this.taskSource = other.taskSource;
        this.taskFactory = other.taskFactory;
        this.taskScheduler = other.taskScheduler;
    }

    @Override
    public void start() {
        long delayMs = Math.max(1L, pollingInterval.toMillis());
        future = scheduledExecutorService.scheduleWithFixedDelay(this::tick, 0, delayMs, TimeUnit.MILLISECONDS);
        log.info("{} started; polling every {}", name, pollingInterval);
    }

    @Override
    public void stop() {
        if (future != null) {
            future.cancel(false);
        }
        scheduledExecutorService.shutdown();
        log.info("{} stopped", name);
    }

    public void tick() {
        try {
            Optional<R> input = getNextInput();
            if (input.isEmpty()) {
                return;
            }
            log.debug("{}: found next task input: {}", name, input.get());
            Runnable task = taskFactory.create(input.get());
            taskScheduler.schedule(task);
        }
        catch (Exception e) {
            log.error("{}: error while polling or running task", name, e);
        }
    }

    // Must be protected for UnitOfWork to function.
    @UnitOfWork
    protected Optional<R> getNextInput() {
        return taskSource.nextInput();
    }
}
