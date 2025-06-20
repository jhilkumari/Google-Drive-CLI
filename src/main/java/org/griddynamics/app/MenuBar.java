package org.griddynamics.app;

import org.griddynamics.domain.Directory;
import org.griddynamics.domain.File;
import org.griddynamics.domain.StorageEntity;
import org.griddynamics.service.DatabaseService;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * The {@code MenuBar} class provides static utility methods to support
 * user interaction in the CLI interface of the file storage system.
 *
 * <p>It contains logic to display the main menu, list directory contents,
 * and show valid items for operations like move, rename, or delete.
 */
public class MenuBar {

    /**
     * Returns the main menu string that will be displayed in the CLI.
     *
     * @return A multi-line string representing the main menu options.
     */
    public static String getMainMenu() {
        return """
                Choose an option:
                    1. List contents
                    2. Create directory
                    3. Upload file
                    4. Download file
                    5. Rename item
                    6. Move item
                    7. Delete item
                    8. View item details
                    9. Search
                    10.Change directory
                    0. Exit
                """;
    }

    /**
     * Lists all files within a specific directory.
     *
     * @param dbService The database service used to retrieve file metadata.
     * @param dir       The directory whose files should be listed.
     */
    public static void listFilesInDirectory(DatabaseService dbService, Directory dir) {
        List<StorageEntity> items = dbService.getChildren(dir.getId());
        if(items==null){
            System.out.println("No files found in the directory.");
            return;
        }
        System.out.println("Available Files:");
        items.stream()
                .filter(item -> item instanceof File)
                .forEach(item -> System.out.println("   Id: " + item.getId() + "  [File] " + item.getName()));
        System.out.println();
    }

    /**
     * Lists both files and directories inside a given directory.
     * Optionally includes the current directory as a selectable option.
     *
     * @param dbService    The database service to query directory contents.
     * @param dir          The directory to list contents for.
     * @param includeSelf  Whether to include the current directory itself in the list.
     * @param includeRoot  Whether the root directory should be included even if it's the current one.
     */
    public static void listItemsInDirectory(DatabaseService dbService, Directory dir, boolean includeSelf, boolean includeRoot) {
        System.out.println("Available Items:\n");
        if (includeSelf && (includeRoot || dir.getParentId() != 0)) {
            System.out.println("    Id: " + dir.getId() + " (current directory) " + dir.getName());
        }
        List<StorageEntity> children = dbService.getChildren(dir.getId());
        for (StorageEntity item : children) {
            System.out.println("    Id: " + item.getId() + (item instanceof Directory ? " [Directory] " : " [File] ") + item.getName());
        }
        System.out.println();
    }

    /**
     * Lists all directories available in the system, excluding the current directory.
     * Used to display potential destinations for move operations.
     *
     * @param dbService        The database service to retrieve all directories.
     * @param currentDirectory The current directory in context.
     */
    public static void listAllDirectories(DatabaseService dbService, Directory currentDirectory) {
        List<Directory> allDirs = dbService.getAllDirectories();
        System.out.println("Destination Directories:\n");
        for (Directory d : allDirs) {
            if (d != currentDirectory)
                System.out.println("    Id: " + d.getId() + " [Directory] " + d.getName());
        }
        System.out.println();
    }

    /**
     * Lists only valid directories where a given directory can be moved to.
     * Prevents moving into self or descendants to avoid cycles.
     *
     * @param dbService    The database service used to retrieve directory hierarchy.
     * @param folderToMove The directory the user wants to move.
     */
    public static void listValidDestinationDirectories(DatabaseService dbService, Directory folderToMove) {
        Set<Integer> excludedIds = dbService.getAllDescendantDirectoryIds(folderToMove.getId());
        excludedIds.add(Integer.valueOf(folderToMove.getId()));
        excludedIds.add(Integer.valueOf(folderToMove.getParentId()));

        List<Directory> allDirs = dbService.getAllDirectories();
        System.out.println("Destination Directories:\n");
        for (Directory d : allDirs) {
            if (!excludedIds.contains(Optional.of(d.getId()))) {
                System.out.println("    Id: " + d.getId() + " [Directory] " + d.getName());
            }
        }
        System.out.println();
    }
}
