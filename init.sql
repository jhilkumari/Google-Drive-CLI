CREATE TABLE IF NOT EXISTS storage_entities (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    parent_id INTEGER REFERENCES storage_entities(id) ON DELETE CASCADE,
    is_directory BOOLEAN NOT NULL,
    file_type VARCHAR(50),
    file_size BIGINT,
    storage_path VARCHAR(512),
    path VARCHAR(1024),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- Metadata constraint: directories have no file metadata, files must have all
    CHECK (
        (is_directory AND file_type IS NULL AND file_size IS NULL AND storage_path IS NULL) OR
        (NOT is_directory AND file_type IS NOT NULL AND file_size IS NOT NULL AND storage_path IS NOT NULL)
    ),

    -- Parent ID constraint: root directories have null parent_id, others must have non-null
    CHECK (
        (is_directory AND parent_id IS NULL) OR
        (parent_id IS NOT NULL)
    )
);
