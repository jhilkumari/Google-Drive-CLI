package org.griddynamics.domain;

/**
 * Represents a file in the file storage system.
 * <p>
 * This class extends {@code StorageEntity} and adds specific attributes for files,
 * such as file size and file type (extension).
 */
public class File extends StorageEntity {
    private String fileType;
    private long fileSize;

    /**
     * Returns a formatted string with details specific to a file.
     *
     * @return A string showing the file's name, path, timestamps, size, and type.
     */
    public String getDetails() {
        return String.format("""
            File Information:
                Name: %s
                Path: %s
                Created: %s
                Modified: %s
                Size: %d bytes
                Extension: %s
            """,
                name,
                path,
                createdAt,
                updatedAt,
                fileSize,
                fileType
        );
    }

    /**
     * Gets the file size in bytes.
     *
     * @return The file size.
     */
    public long getFileSize() {
        return fileSize;
    }

    /**
     * Gets the file extension/type (e.g., "txt", "jpg").
     *
     * @return The file type.
     */
    public String getFileType() {
        return fileType;
    }

    /**
     * Sets the file size in bytes.
     *
     * @param fileSize The size to set.
     */
    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    /**
     * Sets the file extension/type.
     *
     * @param fileType The file type to set (e.g., "pdf", "mp4").
     */
    public void setFileType(String fileType) {
        this.fileType = fileType;
    }
}
