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

import java.util.Optional;

/**
 * Represents a source of tasks from which tasks can be fetched one at a time. Implementations of this interface are responsible for providing the logic to retrieve the next available task or
 * returning an empty result if no tasks are available.
 *
 * @param <R> the type of the tasks managed by this source
 */
public interface TaskSource<R> {
    Optional<R> nextTask();
}
