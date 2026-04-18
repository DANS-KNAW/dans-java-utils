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

import io.dropwizard.hibernate.UnitOfWorkAwareProxyFactory;
import lombok.RequiredArgsConstructor;

import java.util.concurrent.ExecutorService;

/**
 * A task scheduler implementation that schedules tasks for execution using an {@link ExecutorService}. This class allows for tasks to be executed asynchronously using the provided executor service.
 * If a {@link UnitOfWorkAwareProxyFactory} is provided, it will create a proxy for the task to ensure that it is executed within a unit of work context, which is useful for tasks that interact with a
 * database using Hibernate. If no proxy factory is provided, tasks will be executed directly without any additional context management.
 *
 */
@RequiredArgsConstructor
public class ExecutorServiceTaskScheduler implements TaskScheduler {
    private final ExecutorService executorService;
    private final UnitOfWorkAwareProxyFactory unitOfWorkAwareProxyFactory;

    public ExecutorServiceTaskScheduler(ExecutorService executorService) {
        this(executorService, null);
    }

    @Override
    public void schedule(Runnable task) {
        if (unitOfWorkAwareProxyFactory != null) {
            task = unitOfWorkAwareProxyFactory.create(Runnable.class, new Class[] { Runnable.class }, new Object[] { task });
        }
        executorService.submit(task);
    }
}
