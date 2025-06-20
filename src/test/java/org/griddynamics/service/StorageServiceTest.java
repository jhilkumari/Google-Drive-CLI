package org.griddynamics.service;

import org.griddynamics.domain.Directory;
import org.griddynamics.domain.File;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StorageServiceTest {

    @TempDir
    Path tempDir;
    private StorageService storageService;
    private DatabaseService mockDbService;

    @BeforeEach
    void setUp() {
        storageService = new StorageService(tempDir.toString());
        mockDbService = mock(DatabaseService.class);
    }

    @Test
    void constructor_shouldCreateStorageDirectory() {
        assertTrue(Files.exists(tempDir));
    }

    @Test
    void constructor_shouldThrowWhenCannotCreateStorageFolder() {
        try (MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
            mockedFiles.when(() -> Files.createDirectories(any(Path.class)))
                    .thenThrow(new IOException("Simulated IO error"));

            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> new StorageService("/invalid/path"));
            assertEquals("Failed to initialize storage folder", exception.getMessage());
            assertTrue(exception.getCause() instanceof IOException);
        }
    }

    @Test
    void saveFileToDisk_shouldCreateFileWithCorrectContent() throws Exception {
        int fileId = 1;
        String content = "test content";
        String extension = "txt";
        InputStream inputStream = new ByteArrayInputStream(content.getBytes());

        storageService.saveFileToDisk(fileId, inputStream, extension);

        Path expectedFile = tempDir.resolve("1.txt");
        assertTrue(Files.exists(expectedFile));
        assertEquals(content, Files.readString(expectedFile));
    }

    @Test
    void saveFileToDisk_shouldThrowWhenInputStreamFails() throws IOException {
        InputStream badStream = mock(InputStream.class);
        when(badStream.transferTo(any())).thenThrow(new IOException("test error"));

        assertThrows(Exception.class, () ->
                storageService.saveFileToDisk(1, badStream, "txt"));
    }

    @Test
    void downloadFile_shouldCopyFileToDestination() throws IOException {
        File testFile = new File();
        testFile.setId(1);
        testFile.setName("test.txt");
        testFile.setFileType("txt");

        String expectedContent = "content";
        Files.write(tempDir.resolve("1.txt"), expectedContent.getBytes());
        when(mockDbService.getFile(1)).thenReturn(testFile);

        Path destDir = tempDir.resolve("downloads");
        Files.createDirectory(destDir);
        Scanner scanner = new Scanner("1\n" + destDir.toString());

        storageService.downloadFile(mockDbService, new Directory(), scanner);

        Path downloadedFile = destDir.resolve("test.txt");
        assertTrue(Files.exists(downloadedFile));
        assertEquals(expectedContent, Files.readString(downloadedFile));
    }

    @Test
    void downloadFile_shouldHandleFileNotFoundInDatabase() {
        when(mockDbService.getFile(1)).thenReturn(null);
        Scanner scanner = new Scanner("1\n/tmp");

        storageService.downloadFile(mockDbService, new Directory(), scanner);
        // Verify no exception is thrown for graceful handling
    }

    @Test
    void downloadFile_shouldHandleFileNotFoundInStorage() {
        File testFile = new File();
        testFile.setId(1);
        testFile.setName("test.txt");
        testFile.setFileType("txt");

        when(mockDbService.getFile(1)).thenReturn(testFile);
        Scanner scanner = new Scanner("1\n/tmp");

        storageService.downloadFile(mockDbService, new Directory(), scanner);
        // Verify no exception is thrown for graceful handling
    }

    @Test
    void downloadFile_shouldHandleDownloadFailure() throws IOException {
        File testFile = new File();
        testFile.setId(1);
        testFile.setName("test.txt");
        testFile.setFileType("txt");

        Files.write(tempDir.resolve("1.txt"), "content".getBytes());
        when(mockDbService.getFile(1)).thenReturn(testFile);
        Scanner scanner = new Scanner("1\n/invalid/path");

        storageService.downloadFile(mockDbService, new Directory(), scanner);
        // Verify no exception is thrown for graceful handling
    }

    @Test
    void downloadFile_shouldHandleDuplicateFiles() throws IOException {
        File testFile = new File();
        testFile.setId(1);
        testFile.setName("test.txt");
        testFile.setFileType("txt");

        Files.write(tempDir.resolve("1.txt"), "content".getBytes());
        when(mockDbService.getFile(1)).thenReturn(testFile);

        Path destDir = tempDir.resolve("downloads");
        Files.createDirectory(destDir);
        Files.write(destDir.resolve("test.txt"), "existing".getBytes());

        Scanner scanner = new Scanner("1\n" + destDir.toString());

        storageService.downloadFile(mockDbService, new Directory(), scanner);

        Path downloadedFile = destDir.resolve("test_1.txt");
        assertTrue(Files.exists(downloadedFile));
        assertEquals("content", Files.readString(downloadedFile));
    }

    @Test
    void downloadFile_shouldHandleMissingExtension() throws IOException {
        File testFile = new File();
        testFile.setId(1);
        testFile.setName("test");
        testFile.setFileType("txt");

        Files.write(tempDir.resolve("1.txt"), "content".getBytes());
        when(mockDbService.getFile(1)).thenReturn(testFile);

        Path destDir = tempDir.resolve("downloads");
        Files.createDirectory(destDir);
        Scanner scanner = new Scanner("1\n" + destDir.toString());

        storageService.downloadFile(mockDbService, new Directory(), scanner);

        Path downloadedFile = destDir.resolve("test.txt");
        assertTrue(Files.exists(downloadedFile));
    }

    @Test
    void deleteFileFromDisk_shouldRemoveExistingFile() throws IOException {
        Path testFile = tempDir.resolve("1.txt");
        Files.write(testFile, "test content".getBytes());
        assertTrue(Files.exists(testFile));

        storageService.deleteFileFromDisk(1, "txt");
        assertFalse(Files.exists(testFile));
        assertThrows(NoSuchFileException.class, () -> Files.newInputStream(testFile));
    }

    @Test
    void deleteFileFromDisk_shouldNotThrowForMissingFile() {
        assertDoesNotThrow(() -> storageService.deleteFileFromDisk(999, "txt"));
    }

    @Test
    void deleteFileFromDisk_shouldThrowWhenDeletionFails() {
        try (MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
            // Create a real file that will be deleted
            Path testFile = tempDir.resolve("1.txt");
            try {
                Files.write(testFile, "content".getBytes());
            } catch (IOException e) {
                fail("Failed to create test file");
            }

            // Mock the Files.deleteIfExists to throw an exception
            mockedFiles.when(() -> Files.deleteIfExists(testFile))
                    .thenThrow(new IOException("Simulated deletion error"));

            // Act and Assert
            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> storageService.deleteFileFromDisk(1, "txt"));
            assertEquals("Failed to delete file from storage folder", exception.getMessage());
            assertTrue(exception.getCause() instanceof IOException);
        }
    }
}