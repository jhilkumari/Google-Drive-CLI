package org.griddynamics.service;

import org.griddynamics.domain.Directory;
import org.griddynamics.domain.File;
import org.griddynamics.domain.StorageEntity;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.util.*;

import static org.griddynamics.db.SqlQueries.*;

/**
 * Service class that handles all database operations related to storing and managing
 * files and directories in the file storage system. Uses JDBC to communicate with
 * a PostgreSQL database, and works together with {@link StorageService} to persist file content.
 */
//@Service
public class DatabaseService {

    /** JDBC connection to the PostgreSQL database. */
    private final Connection connection;


    /**
     * Constructs a new {@code DatabaseService} with the provided JDBC {@link Connection}.
     *
     * @param connection the JDBC connection to the database, used for executing SQL operations
     */
    @Autowired
    public DatabaseService(Connection connection) {
        this.connection = connection;
    }

    /**
     * Retrieves the root directory. If it does not exist, it is created and returned.
     *
     * @return the root {@link Directory}
     */
    public Directory getRootDirectory() {
        try (PreparedStatement stmt = connection.prepareStatement(SELECT_ROOT_DIRECTORY)) {
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapDirectory(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        try (PreparedStatement insertStmt = connection.prepareStatement(INSERT_DIRECTORY)) {
            insertStmt.setString(1, "root");
            insertStmt.setNull(2, java.sql.Types.INTEGER);
            insertStmt.setString(3, "root");
            ResultSet rs = insertStmt.executeQuery();
            if (rs.next()) {
                return mapDirectory(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    /**
     * Retrieves a directory by its ID.
     *
     * @param directoryId the ID of the directory
     * @return the {@link Directory} object, or {@code null} if not found
     */
    public Directory getDirectory(int directoryId) {
        try (PreparedStatement stmt = connection.prepareStatement(GET_ITEM)) {
            stmt.setInt(1, directoryId);
            stmt.setBoolean(2,true);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapDirectory(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get directory", e);
        }
        return null;
    }

    /**
     * Saves a new directory to the database and sets the generated ID back to the object.
     *
     * @param directory the {@link Directory} to save
     */
    public void saveDirectory(Directory directory) {
        try (PreparedStatement stmt = connection.prepareStatement(INSERT_DIRECTORY)) {
            stmt.setString(1, directory.getName());
            stmt.setInt(2, directory.getParentId());
            stmt.setString(3, directory.getPath());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                directory.setId(rs.getInt("id"));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save directory", e);
        }
    }


    /**
     * Saves a new file both to the database and disk, generating its storage path.
     *
     * @param file           the {@link File} metadata
     * @param fileStream     the input stream of the file content
     * @param storageService the storage service to handle physical file saving
     */
    public void saveFile(File file, InputStream fileStream, StorageService storageService) {
        try {
            String parentPath = getPathById(file.getParentId());
            if (parentPath == null) {
                throw new RuntimeException("Parent directory not found");
            }

            String logicalPath = parentPath + "/" + file.getName();
            file.setPath(logicalPath);

            try (PreparedStatement stmt = connection.prepareStatement(INSERT_FILE)) {
                stmt.setString(1, file.getName());
                stmt.setInt(2, file.getParentId());
                stmt.setString(3, file.getFileType());
                stmt.setLong(4, file.getFileSize());
                stmt.setString(5, "TEMP");
                stmt.setString(6, file.getPath());

                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    int fileId = rs.getInt("id");
                    file.setId(fileId);
                    String storagePath = storageService.getStorageFolder() + "/" + fileId + "." + file.getFileType();
                    updateFilePaths(fileId, storagePath);
                    storageService.saveFileToDisk(fileId, fileStream, file.getFileType());
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to save file", e);
        }
    }

    /**
     * Retrieves a file by its ID.
     *
     * @param fileId the ID of the file
     * @return the {@link File}, or {@code null} if not found
     */
    public File getFile(int fileId) {
        try (PreparedStatement stmt = connection.prepareStatement(GET_ITEM)) {
            stmt.setInt(1, fileId);
            stmt.setBoolean(2,false);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapFile(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get file", e);
        }
        return null;
    }


    /**
     * Deletes a file from the database and disk.
     *
     * @param file           the file to delete
     * @param storageService the storage service to handle physical file deletion
     */
    public void deleteFile(File file, StorageService storageService) {
        try (PreparedStatement stmt = connection.prepareStatement(DELETE_ITEM)) {
            stmt.setInt(1, file.getId());
            stmt.executeUpdate();
            String storageFilePath = storageService.getStorageFolder() + "/" + file.getId() + "." + file.getFileType();
            Files.deleteIfExists(Paths.get(storageFilePath));
            storageService.deleteFileFromDisk(file.getId(), file.getFileType());
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete file", e);
        }
    }

    /**
     * Deletes a directory and all of its contents recursively.
     *
     * @param directoryId the ID of the directory to delete
     */
    public void deleteDirectory(int directoryId) {
        try (PreparedStatement stmt = connection.prepareStatement(DELETE_ITEM)) {
            stmt.setInt(1, directoryId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete directory", e);
        }
    }

    /**
     * Renames a file or directory and updates all descendant paths if applicable.
     *
     * @param id      the ID of the file or directory
     * @param newName the new name
     */
    public void rename(int id, String newName,boolean isDirectory) {
        try {
            String currentPath = null;
            int parentId = 0;

            try (PreparedStatement stmt = connection.prepareStatement(GET_ITEM)) {
                stmt.setInt(1, id);
                stmt.setBoolean(2,isDirectory);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    currentPath = rs.getString("path");
                    parentId = rs.getInt("parent_id") ;
                }
            }

            String parentPath = getPathById(parentId);
            String newPath = (parentPath != null ? parentPath : "") + "/" + newName;

            try (PreparedStatement stmt = connection.prepareStatement(RENAME_ITEM)) {
                stmt.setString(1, newName);
                stmt.setString(2, newPath);
                stmt.setInt(3, id);
                stmt.executeUpdate();
            }

            updateDescendantPaths(currentPath, newPath);

        } catch (SQLException e) {
            throw new RuntimeException("Failed to rename item", e);
        }
    }

    /**
     * Moves a file or directory to a new parent directory and updates its path.
     *
     * @param id          the ID of the item to move
     * @param newParentId the ID of the new parent directory
     * @param newPath     the updated logical path
     */
    public void move(int id, int newParentId, String newPath) {
        try (PreparedStatement stmt = connection.prepareStatement(MOVE_ITEM)) {
            stmt.setInt(1, newParentId);
            stmt.setString(2, newPath);
            stmt.setInt(3, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to move item", e);
        }
    }


    /**
     * Retrieves all contents (files and directories) of a given directory.
     *
     * @param directoryId the parent directory ID
     * @return a list of {@link StorageEntity} objects
     */
    public List<StorageEntity> getDirectoryContents(int directoryId) {
        List<StorageEntity> contents = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(GET_CONTENTS_BY_PARENT_ID)) {
            stmt.setInt(1, directoryId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                if (rs.getBoolean("is_directory")) {
                    contents.add(mapDirectory(rs));
                } else {
                    contents.add(mapFile(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get directory contents", e);
        }
        return contents;
    }

    /**
     * Searches for files and directories whose names match a query.
     *
     * @param query the search keyword
     * @return a list of matched {@link StorageEntity} results
     */
    public List<StorageEntity> searchFilesAndDirectories(String query) {
        List<StorageEntity> results = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(SEARCH_ITEMS)) {
            stmt.setString(1, "%" + query + "%");
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                if (rs.getBoolean("is_directory")) {
                    results.add(mapDirectory(rs));
                } else {
                    results.add(mapFile(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to search items", e);
        }
        return results;
    }

    /**
     * Retrieves the immediate children of a given parent directory.
     *
     * @param parentId the parent directory ID
     * @return a list of {@link StorageEntity} objects
     */
    public List<StorageEntity> getChildren(int parentId) {
        List<StorageEntity> children = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(GET_CHILDREN)) {
            stmt.setInt(1, parentId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                if (rs.getBoolean("is_directory")) {
                    children.add(mapDirectory(rs));
                } else {
                    children.add(mapFile(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch children", e);
        }
        return children;
    }

    /**
     * Retrieves all directories stored in the database.
     *
     * @return a list of {@link Directory} objects
     */
    public List<Directory> getAllDirectories() {
        List<Directory> directories = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(GET_ALL_DIRECTORIES)) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                directories.add(mapDirectory(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch directories", e);
        }
        return directories;
    }

    /**
     * Retrieves all descendant directory IDs of a given directory recursively.
     *
     * @param dirId the starting directory ID
     * @return a set of all descendant directory IDs
     */
    public Set<Integer> getAllDescendantDirectoryIds(int dirId) {
        Set<Integer> descendants = new HashSet<>();
        Queue<Integer> queue = new LinkedList<>();
        queue.add(Integer.valueOf(dirId));

        try {
            while (!queue.isEmpty()) {
                int currentId = queue.poll();
                try (PreparedStatement stmt = connection.prepareStatement(GET_DESCENDANT_IDS)) {
                    stmt.setInt(1, currentId);
                    ResultSet rs = stmt.executeQuery();
                    while (rs.next()) {
                        int childId = rs.getInt("id");
                        if (descendants.add(Integer.valueOf(childId))) {
                            queue.add(Integer.valueOf(childId));
                        }
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get descendant directory IDs", e);
        }
        return descendants;
    }

    /**
     * Updates the physical and logical storage paths for a file.
     *
     * @param fileId      the ID of the file
     * @param storagePath the new physical storage path
     * @throws SQLException if a database error occurs
     */
    void updateFilePaths(int fileId, String storagePath) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(UPDATE_FILE_PATHS)) {
            stmt.setString(1, storagePath);
            stmt.setInt(2, fileId);
            stmt.executeUpdate();
        }
    }

    /**
     * Retrieves the logical path of a file or directory by its ID.
     *
     * @param parentId the ID of the parent item
     * @return the logical path, or {@code null} if not found
     * @throws SQLException if a database error occurs
     */
    String getPathById(int parentId) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(GET_PATH_BY_ID)) {
            stmt.setInt(1, parentId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("path");
            }
        }
        return null;
    }

    /**
     * Recursively updates paths for all descendants when a directory is renamed or moved.
     *
     * @param oldBasePath the old base path
     * @param newBasePath the new base path
     * @throws SQLException if a database error occurs
     */
    void updateDescendantPaths(String oldBasePath, String newBasePath) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(SELECT_DESCENDANTS)) {
            stmt.setString(1, oldBasePath + "/%");
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                int childId = rs.getInt("id");
                String oldChildPath = rs.getString("path");
                String newChildPath = newBasePath + oldChildPath.substring(oldBasePath.length());

                try (PreparedStatement updateStmt = connection.prepareStatement(UPDATE_PATH)) {
                    updateStmt.setString(1, newChildPath);
                    updateStmt.setInt(2, childId);
                    updateStmt.executeUpdate();
                }
            }
        }
    }
    /**
     * Maps a {@link ResultSet} row to a {@link Directory} object.
     *
     * @param rs the result set positioned at a directory row
     * @return the {@link Directory}
     * @throws SQLException if an error occurs while reading from the result set
     */
    private Directory mapDirectory(ResultSet rs) throws SQLException {
        Directory dir = new Directory();
        dir.setId(rs.getInt("id"));
        dir.setName(rs.getString("name"));
        dir.setPath(rs.getString("path"));
        dir.setParentId(rs.getInt("parent_id"));
        dir.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        dir.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        return dir;
    }


    /**
     * Maps a {@link ResultSet} row to a {@link File} object.
     *
     * @param rs the result set positioned at a file row
     * @return the {@link File}
     * @throws SQLException if an error occurs while reading from the result set
     */
    private File mapFile(ResultSet rs) throws SQLException {
        File file = new File();
        file.setId(rs.getInt("id"));
        file.setName(rs.getString("name"));
        file.setParentId(rs.getInt("parent_id"));
        file.setFileType(rs.getString("file_type"));
        file.setFileSize(rs.getLong("file_size"));
        file.setPath(rs.getString("path"));
        file.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        file.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        return file;
    }

}
