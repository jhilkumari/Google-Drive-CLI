package org.griddynamics.db;

/**
 * This class holds all the SQL queries used throughout the application to interact with the PostgreSQL database.
 * Queries support CRUD operations and recursive traversal for both files and directories in the storage_entities table.
 */
public class SqlQueries {

    /**
     * Inserts a new directory into the database.
     */
    public static final String INSERT_DIRECTORY = """
    INSERT INTO storage_entities (name, parent_id, is_directory, path, created_at, updated_at)
    VALUES (?, ?, TRUE, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
    RETURNING id, name, parent_id, is_directory, path, created_at, updated_at
""";


    /**
     * Retrieves an item (file or directory) by its ID and type.
     */
    public static final String GET_ITEM = """
        SELECT * FROM storage_entities WHERE id = ? AND is_directory = ?
    """;

    /**
     * Inserts a new file into the database along with metadata and logical/storage paths.
     */
    public static final String INSERT_FILE = """
        INSERT INTO storage_entities (
            name, parent_id, is_directory, file_type, file_size, storage_path, path, created_at, updated_at
        )
        VALUES (?, ?, FALSE, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        RETURNING id
    """;

    /**
     * Deletes an item (file or directory) by ID.
     */
    public static final String DELETE_ITEM = """
        DELETE FROM storage_entities WHERE id = ?
    """;

    /**
     * Selects the root directory (with no parent and named 'root').
     */
    public static final String SELECT_ROOT_DIRECTORY = """
        SELECT * FROM storage_entities
        WHERE is_directory = TRUE AND parent_id IS NULL AND path = 'root'
        LIMIT 1
    """;

    /**
     * Renames an item and updates its logical path.
     */
    public static final String RENAME_ITEM = """
        UPDATE storage_entities
        SET name = ?, path = ?, updated_at = CURRENT_TIMESTAMP
        WHERE id = ?
    """;

    /**
     * Moves an item to a new parent directory and updates its logical path.
     */
    public static final String MOVE_ITEM = """
        UPDATE storage_entities SET parent_id = ?, path = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?
    """;

    /**
     * Retrieves all items (files and directories) directly under a parent directory.
     */
    public static final String GET_CONTENTS_BY_PARENT_ID = """
        SELECT * FROM storage_entities WHERE parent_id = ?
    """;

    /**
     * Performs a case-insensitive search for items whose names match a pattern.
     */
    public static final String SEARCH_ITEMS = """
        SELECT * FROM storage_entities
        WHERE LOWER(name) LIKE LOWER(?) ORDER BY is_directory DESC, name ASC
    """;

    /**
     * Updates the storage path of a file on disk.
     */
    public static final String UPDATE_FILE_PATHS = """
        UPDATE storage_entities
        SET storage_path = ?, updated_at = CURRENT_TIMESTAMP
        WHERE id = ?
    """;

    /**
     * Retrieves the logical path of an item by its ID.
     */
    public static final String GET_PATH_BY_ID = """
        SELECT path FROM storage_entities 
        WHERE id = ?
    """;

    /**
     * Retrieves all descendant items whose path begins with a given prefix (used for recursive updates).
     */
    public static final String SELECT_DESCENDANTS = """
        SELECT id, path FROM storage_entities 
        WHERE path LIKE ?
    """;

    /**
     * Updates the logical path of an item by ID.
     */
    public static final String UPDATE_PATH = """
        UPDATE storage_entities 
        SET path = ? 
        WHERE id = ?
    """;

    /**
     * Retrieves direct children of a given directory.
     */
    public static final String GET_CHILDREN = """
        SELECT * FROM storage_entities 
        WHERE parent_id = ?
    """;

    /**
     * Retrieves all directories (used for move/selection logic).
     */
    public static final String GET_ALL_DIRECTORIES = """
        SELECT * FROM storage_entities 
        WHERE is_directory = TRUE
    """;

    /**
     * Retrieves all immediate subdirectory IDs of a given directory (used to compute invalid destinations).
     */
    public static final String GET_DESCENDANT_IDS = """
        SELECT id FROM storage_entities 
        WHERE parent_id = ? AND is_directory = TRUE
    """;
}
