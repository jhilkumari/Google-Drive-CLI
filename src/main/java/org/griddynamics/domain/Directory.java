package org.griddynamics.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a directory in the file storage system.
 * <p>
 * A directory can contain both {@code File} and {@code Directory} instances.
 * It extends {@code StorageEntity} and overrides the {@code getDetails()} method
 * to provide directory-specific metadata.
 */
public class Directory extends StorageEntity {

    /** List of files contained in this directory (used for display or caching purposes). */
    final List<File> files = new ArrayList<>();

    /** List of subdirectories contained within this directory. */
    final List<Directory> subDirectories = new ArrayList<>();

    /**
     * Generates a formatted string containing detailed information about the directory.
     *
     * @param totalContent The total number of items (files and subdirectories) within the directory.
     * @return A formatted string displaying the directory's name, path, creation and last update timestamps,
     *         and the total number of items it contains.
     */
    public String getDetails(int totalContent) {
        return String.format("""
                Directory Information:
                    Name: %s
                    Path: %s
                    Created: %s
                    Modified: %s
                    Contents: %d item%s
                """,
                name,
                path,
                createdAt,
                updatedAt,
                totalContent,
                totalContent <= 1 ? "" : "s"
        );
    }
}
