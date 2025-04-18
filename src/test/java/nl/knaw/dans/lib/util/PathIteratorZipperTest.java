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
package nl.knaw.dans.lib.util;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class PathIteratorZipperTest extends AbstractTestWithTestDir {

    @Test
    public void zip_should_zip_single_file() throws Exception {
        // Given
        Path inputDir = testDir.resolve("onefile");
        Files.createDirectories(inputDir);
        FileUtils.writeStringToFile(inputDir.resolve("file.txt").toFile(), "Hello, world!", StandardCharsets.UTF_8);

        // When
        Iterator<Path> pathIterator = new PathIterator(FileUtils.iterateFiles(inputDir.toFile(), null, true));
        PathIteratorZipper.builder()
            .rootDir(inputDir)
            .sourceIterator(pathIterator)
            .targetZipFile(testDir.resolve("onefile.zip"))
            .build()
            .zip();

        // Then
        assertThat(testDir.resolve("onefile.zip")).exists();
        try (ZipFile zipFile = new ZipFile(testDir.resolve("onefile.zip").toFile())) {
            assertThat(zipFile.stream().map(ZipEntry::getName)).containsExactly("file.txt");
        }
    }

    @Test
    public void zip_should_zip_multiple_files() throws Exception {
        // Given
        Path inputDir = testDir.resolve("multiplefiles");
        Files.createDirectories(inputDir);
        FileUtils.writeStringToFile(inputDir.resolve("file1.txt").toFile(), "Hello, world!", StandardCharsets.UTF_8);
        FileUtils.writeStringToFile(inputDir.resolve("file2.txt").toFile(), "Hello, world!", StandardCharsets.UTF_8);

        // When
        Iterator<Path> pathIterator = new PathIterator(FileUtils.iterateFiles(inputDir.toFile(), null, true));
        PathIteratorZipper.builder()
            .rootDir(inputDir)
            .sourceIterator(pathIterator)
            .targetZipFile(testDir.resolve("multiplefiles.zip"))
            .build()
            .zip();

        // Then
        assertThat(testDir.resolve("multiplefiles.zip")).exists();
        try (ZipFile zipFile = new ZipFile(testDir.resolve("multiplefiles.zip").toFile())) {
            assertThat(zipFile.stream().map(ZipEntry::getName)).containsExactlyInAnyOrder("file1.txt", "file2.txt");
        }
    }

    @Test
    public void zip_should_zip_only_files() throws Exception {
        // Given
        Path inputDir = testDir.resolve("onlyfiles");
        Files.createDirectories(inputDir);
        FileUtils.writeStringToFile(inputDir.resolve("file.txt").toFile(), "Hello, world!", StandardCharsets.UTF_8);
        Files.createDirectory(inputDir.resolve("dir"));

        // When
        Iterator<Path> pathIterator = new PathIterator(FileUtils.iterateFiles(inputDir.toFile(), null, true));
        PathIteratorZipper.builder()
            .rootDir(inputDir)
            .sourceIterator(pathIterator)
            .targetZipFile(testDir.resolve("onlyfiles.zip"))
            .build()
            .zip();

        // Then
        assertThat(testDir.resolve("onlyfiles.zip")).exists();
        try (ZipFile zipFile = new ZipFile(testDir.resolve("onlyfiles.zip").toFile())) {
            assertThat(zipFile.stream().map(ZipEntry::getName)).containsExactly("file.txt");
        }
    }

    @Test
    public void zip_should_zip_also_files_in_subdirectories() throws Exception {
        // Given
        Path inputDir = testDir.resolve("subdirectories");
        Files.createDirectories(inputDir);
        FileUtils.writeStringToFile(inputDir.resolve("file1.txt").toFile(), "Hello, world!", StandardCharsets.UTF_8);
        FileUtils.writeStringToFile(inputDir.resolve("file2.txt").toFile(), "Hola, mundo!", StandardCharsets.UTF_8);
        Files.createDirectories(inputDir.resolve("dir1"));
        FileUtils.writeStringToFile(inputDir.resolve("dir1/file3.txt").toFile(), "Hallo, wereld!", StandardCharsets.UTF_8);
        Files.createDirectories(inputDir.resolve("dir2"));
        FileUtils.writeStringToFile(inputDir.resolve("dir2/file4.txt").toFile(), "Bonjour, monde!", StandardCharsets.UTF_8);

        // When
        Iterator<Path> pathIterator = new PathIterator(FileUtils.iterateFilesAndDirs(inputDir.toFile(), TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE));
        PathIteratorZipper.builder()
            .rootDir(inputDir)
            .sourceIterator(pathIterator)
            .targetZipFile(testDir.resolve("subdirectories.zip"))
            .build()
            .zip();

        // Then
        assertThat(testDir.resolve("subdirectories.zip")).exists();
        try (ZipFile zipFile = new ZipFile(testDir.resolve("subdirectories.zip").toFile())) {
            assertThat(zipFile.stream().map(ZipEntry::getName)).containsExactlyInAnyOrder("file1.txt", "file2.txt", "dir1/file3.txt", "dir2/file4.txt");
        }
    }

    @Test
    public void zip_should_zip_only_files_up_to_maxNumberOfFiles() throws Exception {
        // Given
        Path inputDir = testDir.resolve("maxnumberoffiles");
        Files.createDirectories(inputDir);
        FileUtils.writeStringToFile(inputDir.resolve("file1.txt").toFile(), "Hello, world!", StandardCharsets.UTF_8);
        FileUtils.writeStringToFile(inputDir.resolve("file2.txt").toFile(), "Hola, mundo!", StandardCharsets.UTF_8);
        FileUtils.writeStringToFile(inputDir.resolve("file3.txt").toFile(), "Hallo, wereld!", StandardCharsets.UTF_8);
        FileUtils.writeStringToFile(inputDir.resolve("file4.txt").toFile(), "Bonjour, monde!", StandardCharsets.UTF_8);

        // When
        try (Stream<Path> stream = Files.list(inputDir)) {
            List<File> files = stream.map(Path::toFile).sorted().collect(Collectors.toList()); // Sort the files to ensure consistent order
            Iterator<Path> pathIterator = new PathIterator(files.iterator());
            PathIteratorZipper.builder()
                .rootDir(inputDir)
                .sourceIterator(pathIterator)
                .targetZipFile(testDir.resolve("part1.zip"))
                .maxNumberOfFiles(2)
                .build()
                .zip();

            PathIteratorZipper.builder()
                .rootDir(inputDir)
                .sourceIterator(pathIterator)
                .targetZipFile(testDir.resolve("part2.zip"))
                .maxNumberOfFiles(2)
                .build()
                .zip();

            // Then
            assertThat(testDir.resolve("part1.zip")).exists();
            try (ZipFile zipFile = new ZipFile(testDir.resolve("part1.zip").toFile())) {
                assertThat(zipFile.stream().map(ZipEntry::getName)).containsExactlyInAnyOrder("file1.txt", "file2.txt");
            }
            assertThat(testDir.resolve("part2.zip")).exists();
            try (ZipFile zipFile = new ZipFile(testDir.resolve("part2.zip").toFile())) {
                assertThat(zipFile.stream().map(ZipEntry::getName)).containsExactlyInAnyOrder("file3.txt", "file4.txt");
            }
        }
    }

    @Test
    public void zip_should_throw_IllegalArgumentException_if_non_existent_file_included_by_iterator() throws Exception {
        // Given
        Iterator<File> mockIterator = new Iterator<File>() {

            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public File next() {
                return new File("non-existent-file");
            }
        };

        Path inputDir = testDir.resolve("input");
        Files.createDirectories(inputDir);

        // When
        Iterator<Path> pathIterator = new PathIterator(mockIterator);
        PathIteratorZipper zipper = PathIteratorZipper.builder()
            .rootDir(inputDir)
            .sourceIterator(pathIterator)
            .targetZipFile(testDir.resolve("output.zip"))
            .build();

        // Then
        assertThatThrownBy(zipper::zip)
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("File to zip does not exist: non-existent-file");
    }

    @Test
    public void zip_should_throw_IllegalArgumentException_if_non_existent_file_included_by_iterator_after_several_valid_files() throws Exception {
        // Given
        File inputDir = testDir.resolve("input").toFile();
        List<File> files = Arrays.asList(
            new File(inputDir, "file1.txt"),
            new File(inputDir, "file2.txt"),
            new File(inputDir, "non-existent-file"),
            new File(inputDir, "file3.txt")
        );

        Iterator<File> iterator = files.iterator();

        Files.createDirectories(inputDir.toPath());
        FileUtils.writeStringToFile(files.get(0), "Hello, world!", StandardCharsets.UTF_8);
        FileUtils.writeStringToFile(files.get(1), "Hola, mundo!", StandardCharsets.UTF_8);
        FileUtils.writeStringToFile(files.get(3), "Hallo, wereld!", StandardCharsets.UTF_8);

        // When
        Iterator<Path> pathIterator = new PathIterator(iterator);
        PathIteratorZipper zipper = PathIteratorZipper.builder()
            .rootDir(inputDir.toPath())
            .sourceIterator(pathIterator)
            .targetZipFile(testDir.resolve("output.zip"))
            .build();

        // Then
        assertThatThrownBy(zipper::zip)
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("File to zip does not exist: target/test/PathIteratorZipperTest/input/non-existent-file");

    }

}