package org.griddynamics.domain;

import java.time.LocalDateTime;

/**
 * Abstract base class for both files and directories in the file storage system.
 * Contains common attributes and behavior shared by all storage entities.
 */
public abstract class StorageEntity {
    protected int id;
    protected String name;
    protected int parentId;
    protected LocalDateTime createdAt;
    protected LocalDateTime updatedAt;
    protected String path;


    // Getters
    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getParentId() {
        return parentId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public String getPath() {
        return path;
    }

    // Setters
    public void setId(int id) {
        this.id = id;
    }

    public void setParentId(int parentId) {
        this.parentId = parentId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
