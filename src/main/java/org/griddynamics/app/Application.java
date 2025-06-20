package org.griddynamics.app;

import org.griddynamics.domain.Directory;
import org.griddynamics.domain.File;
import org.griddynamics.domain.StorageEntity;
import org.griddynamics.service.DatabaseService;
import org.griddynamics.service.StorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

import java.io.FileInputStream;
import java.util.List;
import java.util.Scanner;

import static org.griddynamics.app.MenuBar.*;

/**
 * The {@code Application} class serves as the entry point for the CLI-based file storage system.
 * It allows users to manage files and directories via a PostgreSQL-backed system, performing operations
 * such as creating directories, uploading files, downloading, renaming, moving, deleting, and searching.
 */
@SpringBootApplication
@ComponentScan(basePackages = "org.griddynamics")
public class Application implements CommandLineRunner {
    private final Scanner scanner;
    private final DatabaseService dbService;
    Directory currentDirectory;
    final StorageService storageService;

    @Autowired
    public Application(DatabaseService dbService, StorageService storageService) {
        this.scanner = new Scanner(System.in);
        this.dbService = dbService;
        this.storageService = storageService;
        this.currentDirectory = dbService.getRootDirectory();
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Override
    public void run(String... args) {
        start();
    }

    /**
     * Main application loop.
     */
    void start() {
        while (true) {
            System.out.println("\n==============Current Directory: " + currentDirectory.getPath() + "==============");
            System.out.println(getMainMenu());

            System.out.print("Enter choice: ");
            String choice = scanner.nextLine().trim();
            System.out.println();

            switch (choice) {
                case "1" -> listContents(currentDirectory);
                case "2" -> createDirectory(currentDirectory);
                case "3" -> uploadFile(currentDirectory);
                case "4" -> storageService.downloadFile(dbService, currentDirectory, scanner);
                case "5" -> renameItem();
                case "6" -> moveItem();
                case "7" -> deleteItem();
                case "8" -> viewItemDetails();
                case "9" -> searchItems();
                case "10" -> changeDirectory();
                case "0" -> {
                    System.out.println("Exiting...");
                    scanner.close();
                    return;
                }
                default -> System.out.println("Invalid choice. Try again.");
            }
        }
    }

    /**
     * Changes the current working directory.
     */
    public void changeDirectory() {
        listAllDirectories(dbService, currentDirectory);
        System.out.print("Enter directory ID to navigate into (or -1 to go up): ");
        int id = scanner.nextInt();
        scanner.nextLine();
        if (id == -1) return;

        try {
            Directory dir = dbService.getDirectory(id);
            if (dir != null && dir.getParentId() == currentDirectory.getId()) {
                currentDirectory = dir;
            } else {
                System.out.println("Invalid directory.");
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid input.");
        }
    }

    /**
     * Lists all files and directories in the current working directory.
     * @param currentDir The current directory to list contents from.
     */
    public void listContents(Directory currentDir) {
        List<StorageEntity> contents = dbService.getDirectoryContents(currentDir.getId());
        if (contents.isEmpty()) {
            System.out.println("Directory is empty.");
        } else {
            System.out.println("Contents of " + currentDir.getName() + ":");
            contents.forEach(item ->
                    System.out.println("   Id: " + item.getId() +
                            (item instanceof Directory ? " [Directory] " : " [File] ") + item.getName())
            );
        }
        System.out.println();
    }

    /**
     * Creates a new directory inside the current working directory.
     * @param currentDir The directory where the new subdirectory will be created.
     */
    public void createDirectory(Directory currentDir) {
        System.out.print("Enter directory name: ");
        String name = scanner.nextLine();
        Directory newDir = new Directory();
        newDir.setName(name);
        newDir.setParentId(currentDir.getId());
        newDir.setPath(currentDir.getPath() + "/" + name);
        dbService.saveDirectory(newDir);
        System.out.println("Directory created.");
    }

    /**
     * Uploads a local file into the current directory.
     * @param currentDir The directory to upload the file into.
     */
    public void uploadFile(Directory currentDir) {
        try {
            System.out.print("Enter path to file to upload: ");
            String localPath = scanner.nextLine().trim();
            java.io.File f = new java.io.File(localPath);
            if (!f.exists()) {
                System.out.println("File does not exist.");
                return;
            }

            File file = new File();
            file.setName(f.getName());
            file.setParentId(currentDir.getId());
            file.setFileType(getExtension(f.getName()));
            file.setFileSize(f.length());
            file.setPath(currentDir.getPath() + "/" + f.getName());

            try (FileInputStream fis = new FileInputStream(f)) {
                dbService.saveFile(file, fis, storageService);
                System.out.println("File uploaded.");
            }
        } catch (Exception e) {
            System.out.println("Upload failed: " + e.getMessage());
        }
    }

    /**
     * Renames a file or directory in the current directory.
     */
    public void renameItem() {
        listItemsInDirectory(dbService, currentDirectory, true, false);
        System.out.print("Enter item ID to rename(or -1 to go back): ");
        int id = Integer.parseInt(scanner.nextLine());
        if (id == -1) return;
        StorageEntity item = dbService.getFile(id);
        if (item == null) item = dbService.getDirectory(id);

        if (item == null) {
            System.out.println("Item not found.");
            return;
        }
        System.out.print("Enter new name: ");
        String newName = scanner.nextLine().trim();

        if (item instanceof File file) {
            String extension = file.getFileType();
            if (!newName.toLowerCase().endsWith("." + extension.toLowerCase())) {
                newName += "." + extension;
            }
            dbService.rename(id, newName,false);
            System.out.println("File renamed to " + newName);
        } else {
            dbService.rename(id, newName,true);
            System.out.println("Folder renamed to " + newName);
        }
    }

    /**
     * Moves a file or directory to another directory.
     */
    public void moveItem() {
        listItemsInDirectory(dbService, currentDirectory, true, false);
        System.out.print("Enter item ID to move(or -1 to go back): ");
        int id = Integer.parseInt(scanner.nextLine());
        if (id == -1) return;
        StorageEntity item = dbService.getFile(id);
        if (item == null) item = dbService.getDirectory(id);
        if (item == null) {
            System.out.println("Item not found.");
            return;
        }
        if (item instanceof File) {
            listAllDirectories(dbService, currentDirectory);
        } else {
            listValidDestinationDirectories(dbService, (Directory) item);
        }
        System.out.print("Enter new parent directory ID: ");
        int newParentId = Integer.parseInt(scanner.nextLine());
        Directory newParent = dbService.getDirectory(newParentId);
        if (newParent == null) {
            System.out.println("Invalid target directory.");
            return;
        }

        String newPath = newParent.getPath() + "/" + item.getName();
        dbService.move(id, newParentId, newPath);
        System.out.println("Item moved.");
    }

    /**
     * Deletes a file or directory.
     */
    public void deleteItem() {
        listItemsInDirectory(dbService, currentDirectory, true, false);
        System.out.print("Enter item ID to delete(or -1 to go back): ");
        int id = Integer.parseInt(scanner.nextLine());
        if (id == -1) return;
        StorageEntity item = dbService.getFile(id);
        if (item instanceof File file) {
            dbService.deleteFile(file, storageService);
            System.out.println("File deleted.");
        } else {
            item = dbService.getDirectory(id);
            if (item != null) {
                dbService.deleteDirectory(id);
                System.out.println("Directory deleted.");
            } else {
                System.out.println("Item not found.");
            }
        }
    }

    /**
     * Views detailed metadata for a file or directory.
     */
    public void viewItemDetails() {
        listItemsInDirectory(dbService, currentDirectory, true, true);
        System.out.print("Enter item ID to view (or -1 to go back): ");
        int id = scanner.nextInt();
        scanner.nextLine();

        if (id == -1) return;

        // Check if the item is a File
        File item = dbService.getFile(id);

        if (item == null) {
            // Check if the item is a Directory
            Directory directoryItem = dbService.getDirectory(id);
            if (directoryItem != null) {
                int totalContent = dbService.getDirectoryContents(id).size();
                System.out.println(directoryItem.getDetails(totalContent));  // Call getDetails with totalContent
            } else {
                System.out.println("Invalid ID");
            }
        } else {
            // For File, just call the normal getDetails() method
            System.out.println(item.getDetails());
        }
    }


    /**
     * Searches for files and directories by name.
     */
    public void searchItems() {
        System.out.print("Enter search query: ");
        String query = scanner.nextLine();
        List<StorageEntity> results = dbService.searchFilesAndDirectories(query);
        if (results.isEmpty()) {
            System.out.println("No results found.");
        } else {
            for (StorageEntity item : results) {
                System.out.printf("Id: %d [%s] %s (Path: %s)\n",
                        item.getId(),
                        item instanceof Directory ? "Directory" : "File",
                        item.getName(),
                        item.getPath());
            }
        }
    }

    /**
     * Utility method to extract file extension.
     * @param filename The name of the file
     * @return The file extension (without dot)
     */
    static String getExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return (dot == -1) ? "" : filename.substring(dot + 1);
    }
}