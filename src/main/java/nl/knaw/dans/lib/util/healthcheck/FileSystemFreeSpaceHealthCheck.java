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
package nl.knaw.dans.lib.util.healthcheck;

import com.codahale.metrics.health.HealthCheck;
import io.dropwizard.util.DataSize;
import lombok.AllArgsConstructor;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * A health check that verifies that the file system containing the given path has at least the given margin of free space.
 */
@AllArgsConstructor
public class FileSystemFreeSpaceHealthCheck extends HealthCheck {
    private final Path path;
    private final DataSize margin;

    @Override
    protected Result check() throws Exception {
        long freeSpace = Files.getFileStore(path).getUsableSpace();
        long requiredSpace = margin.toBytes();

        if (freeSpace < requiredSpace) {
            return Result.unhealthy(String.format(
                "Free space on file system containing %s is %d bytes, which is less than the required margin of %d bytes.",
                path, freeSpace, requiredSpace
            ));
        }
        return Result.healthy(String.format(
            "Free space on file system containing %s is %d bytes, which is above the required margin of %d bytes.",
            path, freeSpace, requiredSpace
        ));
    }

    @Override
    public String toString() {
        return "FileSystemFreeSpaceHealthCheck{" +
            "path=" + path +
            ", margin=" + margin +
            '}';
    }
}
