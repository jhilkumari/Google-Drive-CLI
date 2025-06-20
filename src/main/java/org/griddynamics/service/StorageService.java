package org.griddynamics.service;

import org.griddynamics.domain.Directory;
import org.griddynamics.domain.File;
import org.springframework.stereotype.Service;
// NOTE: The following imports require Spring Boot dependencies in your build file (pom.xml or build.gradle):
// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

import static org.griddynamics.app.MenuBar.listFilesInDirectory;

/**
 * Service class responsible for handling file storage operations on disk.
 * Supports saving, downloading, and deleting files in a configured storage folder.
 */
// NOTE: The following annotation requires Spring Boot dependencies in your build file (pom.xml or build.gradle):
//@Service
public class StorageService {

    private final Path storageDir;
    private final String storageFolder;

    /**
     * Initializes the storage service and ensures the storage directory exists.
     */
    // NOTE: The following annotation requires Spring Boot dependencies in your build file (pom.xml or build.gradle):
    // @Value
    public StorageService(String storageFolder) {
        this.storageFolder = storageFolder;
        this.storageDir = Path.of(storageFolder);
        try {
            Files.createDirectories(storageDir);
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize storage folder", e);
        }
    }

    public String getStorageFolder() {
        return storageFolder;
    }

    /**
     * Saves a file to disk using its file ID and extension.
     *
     * @param fileId        The unique file ID used as the filename.
     * @param inputStream   Input stream of the file's contents.
     * @param fileExtension The file's extension (e.g., "txt", "jpg").
     * @throws Exception If an error occurs during writing to disk.
     */
    public void saveFileToDisk(int fileId, InputStream inputStream, String fileExtension) throws Exception {
        if (!Files.exists(storageDir)) Files.createDirectories(storageDir);
        String filename = fileId + "." + fileExtension;
        Path filePath = storageDir.resolve(filename);

        try (FileOutputStream fos = new FileOutputStream(filePath.toFile())) {
            inputStream.transferTo(fos);
        }
    }

    /**
     * Downloads a file from storage to a user-specified destination.
     * Prompts the user to choose a file from the current directory.
     *
     * @param dbService        DatabaseService instance to fetch file metadata.
     * @param currentDirectory The directory to list files from.
     * @param scanner          Scanner object for user input.
     */
    public void downloadFile(DatabaseService dbService, Directory currentDirectory, Scanner scanner) {
        listFilesInDirectory(dbService, currentDirectory);
        System.out.print("Enter file ID to download (or -1 to go back): ");
        int id = scanner.nextInt();
        scanner.nextLine();
        if (id == -1) return;

        File file = dbService.getFile(id);
        if (file == null) {
            System.out.println("File not found.");
            return;
        }

        java.io.File storageFile = storageDir.resolve(file.getId() + "." + file.getFileType()).toFile();
        if (!storageFile.exists()) {
            System.out.println("Stored file not found.");
            return;
        }

        System.out.print("Enter download destination path: ");
        String destPath = scanner.nextLine();

        try {
            String baseName = file.getName();
            String extension = file.getFileType();
            if (!baseName.toLowerCase().endsWith("." + extension.toLowerCase())) {
                baseName += "." + extension;
            }

            Path targetPath = Paths.get(destPath, baseName);
            int counter = 1;
            while (Files.exists(targetPath)) {
                String nameWithoutExt = baseName.substring(0, baseName.lastIndexOf('.'));
                targetPath = Paths.get(destPath, nameWithoutExt + "_" + counter + "." + extension);
                counter++;
            }

            Files.copy(storageFile.toPath(), targetPath);
            System.out.println("Downloaded to " + targetPath);

        } catch (Exception e) {
            System.out.println("Download failed: " + e.getMessage());
        }
    }

    /**
     * Deletes a file from disk based on its file ID and type.
     *
     * @param fileId   The ID of the file to delete.
     * @param fileType The file extension (used to locate the file).
     */
    public void deleteFileFromDisk(int fileId, String fileType) {
        String filename = fileId + "." + fileType;
        Path filePath = storageDir.resolve(filename);
        try {
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete file from storage folder", e);
        }
    }
}
