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
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FileSystemFreeSpaceHealthCheckTest {

    @Test
    public void healthy_when_free_space_is_above_required_margin() throws Exception {
        // Given
        Path path = Path.of("/any/path");
        DataSize margin = DataSize.megabytes(100); // 100 MB
        FileStore fileStore = mock(FileStore.class);
        when(fileStore.getUsableSpace()).thenReturn(DataSize.gigabytes(1).toBytes()); // 1 GB free

        try (MockedStatic<Files> filesMock = Mockito.mockStatic(Files.class)) {
            filesMock.when(() -> Files.getFileStore(path)).thenReturn(fileStore);

            FileSystemFreeSpaceHealthCheck healthCheck = new FileSystemFreeSpaceHealthCheck(path, margin);

            // When
            HealthCheck.Result result = healthCheck.check();

            // Then
            assertThat(result.isHealthy()).isTrue();
            assertThat(result.getMessage())
                .contains("Free space on file system containing")
                .contains("is ")
                .contains("which is above the required margin");
        }
    }

    @Test
    public void unhealthy_when_free_space_is_below_required_margin() throws Exception {
        // Given
        Path path = Path.of("/any/path");
        DataSize margin = DataSize.gigabytes(2); // 2 GB required
        FileStore fileStore = mock(FileStore.class);
        when(fileStore.getUsableSpace()).thenReturn(DataSize.megabytes(500).toBytes()); // 500 MB free

        try (MockedStatic<Files> filesMock = Mockito.mockStatic(Files.class)) {
            filesMock.when(() -> Files.getFileStore(path)).thenReturn(fileStore);

            FileSystemFreeSpaceHealthCheck healthCheck = new FileSystemFreeSpaceHealthCheck(path, margin);

            // When
            HealthCheck.Result result = healthCheck.check();

            // Then
            assertThat(result.isHealthy()).isFalse();
            assertThat(result.getMessage())
                .contains("Free space on file system containing")
                .contains("is ")
                .contains("which is less than the required margin");
        }
    }
}
