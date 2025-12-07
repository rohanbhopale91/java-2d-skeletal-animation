package com.animstudio.core.model;

import java.io.File;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents an imported asset (image/sprite) in the project.
 * Assets can be attached to slots/bones for rendering.
 */
public class AssetEntry {
    
    private final String id;
    private String name;
    private String relativePath;
    private String absolutePath;
    private int width;
    private int height;
    private double pivotX = 0.5; // Normalized 0-1, default center
    private double pivotY = 0.5;
    
    /**
     * Creates a new asset entry with auto-generated ID.
     */
    public AssetEntry(String name, String relativePath) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.relativePath = relativePath;
    }
    
    /**
     * Creates an asset entry with a specific ID (for deserialization).
     */
    public AssetEntry(String id, String name, String relativePath) {
        this.id = id;
        this.name = name;
        this.relativePath = relativePath;
    }
    
    /**
     * Creates an asset entry from a file.
     */
    public static AssetEntry fromFile(File file, String projectBasePath) {
        String name = file.getName();
        // Remove extension for display name
        int dotIndex = name.lastIndexOf('.');
        if (dotIndex > 0) {
            name = name.substring(0, dotIndex);
        }
        
        String absolutePath = file.getAbsolutePath();
        String relativePath = absolutePath;
        
        // Make path relative to project if possible
        if (projectBasePath != null && absolutePath.startsWith(projectBasePath)) {
            relativePath = absolutePath.substring(projectBasePath.length());
            if (relativePath.startsWith(File.separator)) {
                relativePath = relativePath.substring(1);
            }
        }
        
        AssetEntry entry = new AssetEntry(name, relativePath);
        entry.setAbsolutePath(absolutePath);
        return entry;
    }
    
    // === Getters and Setters ===
    
    public String getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getRelativePath() {
        return relativePath;
    }
    
    public void setRelativePath(String relativePath) {
        this.relativePath = relativePath;
    }
    
    public String getAbsolutePath() {
        return absolutePath;
    }
    
    public void setAbsolutePath(String absolutePath) {
        this.absolutePath = absolutePath;
    }
    
    public int getWidth() {
        return width;
    }
    
    public void setWidth(int width) {
        this.width = width;
    }
    
    public int getHeight() {
        return height;
    }
    
    public void setHeight(int height) {
        this.height = height;
    }
    
    public double getPivotX() {
        return pivotX;
    }
    
    public void setPivotX(double pivotX) {
        this.pivotX = pivotX;
    }
    
    public double getPivotY() {
        return pivotY;
    }
    
    public void setPivotY(double pivotY) {
        this.pivotY = pivotY;
    }
    
    /**
     * Gets the file extension (lowercase, without dot).
     */
    public String getExtension() {
        if (relativePath == null) return "";
        int dotIndex = relativePath.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < relativePath.length() - 1) {
            return relativePath.substring(dotIndex + 1).toLowerCase();
        }
        return "";
    }
    
    /**
     * Checks if this asset is an image file.
     */
    public boolean isImage() {
        String ext = getExtension();
        return ext.equals("png") || ext.equals("jpg") || ext.equals("jpeg") || 
               ext.equals("gif") || ext.equals("bmp");
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AssetEntry that = (AssetEntry) o;
        return Objects.equals(id, that.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return name;
    }
}
