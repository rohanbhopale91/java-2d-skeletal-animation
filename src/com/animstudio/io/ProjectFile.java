package com.animstudio.io;

import com.animstudio.core.animation.AnimationClip;
import com.animstudio.core.model.Skeleton;
import com.animstudio.core.mesh.DeformableMesh;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a complete animation project file.
 * Contains skeleton, animations, meshes, textures, and metadata.
 */
public class ProjectFile {
    
    public static final String FILE_EXTENSION = ".animproj";
    public static final int FORMAT_VERSION = 1;
    
    // Project metadata
    private String name;
    private String author;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime modifiedAt;
    private int formatVersion;
    
    // Project content
    private Skeleton skeleton;
    private final List<AnimationClip> animations;
    private final List<DeformableMesh> meshes;
    private final Map<String, String> texturePaths; // name -> path
    private final Map<String, Object> customProperties;
    
    // Project settings
    private int frameRate = 30;
    private double defaultDuration = 1.0;
    private int canvasWidth = 800;
    private int canvasHeight = 600;
    
    // File reference
    private File sourceFile;
    private boolean modified;
    
    public ProjectFile() {
        this.name = "Untitled";
        this.author = System.getProperty("user.name");
        this.description = "";
        this.createdAt = LocalDateTime.now();
        this.modifiedAt = LocalDateTime.now();
        this.formatVersion = FORMAT_VERSION;
        
        this.animations = new ArrayList<>();
        this.meshes = new ArrayList<>();
        this.texturePaths = new HashMap<>();
        this.customProperties = new HashMap<>();
        this.modified = false;
    }
    
    public ProjectFile(String name) {
        this();
        this.name = name;
    }
    
    // Metadata accessors
    public String getName() { return name; }
    public void setName(String name) { 
        this.name = name; 
        markModified();
    }
    
    public String getAuthor() { return author; }
    public void setAuthor(String author) { 
        this.author = author; 
        markModified();
    }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { 
        this.description = description; 
        markModified();
    }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getModifiedAt() { return modifiedAt; }
    public void setModifiedAt(LocalDateTime modifiedAt) { this.modifiedAt = modifiedAt; }
    
    public int getFormatVersion() { return formatVersion; }
    public void setFormatVersion(int formatVersion) { this.formatVersion = formatVersion; }
    
    // Content accessors
    public Skeleton getSkeleton() { return skeleton; }
    public void setSkeleton(Skeleton skeleton) { 
        this.skeleton = skeleton; 
        markModified();
    }
    
    public List<AnimationClip> getAnimations() { return animations; }
    
    public void addAnimation(AnimationClip clip) {
        animations.add(clip);
        markModified();
    }
    
    public void removeAnimation(AnimationClip clip) {
        animations.remove(clip);
        markModified();
    }
    
    public AnimationClip getAnimationByName(String name) {
        return animations.stream()
            .filter(a -> a.getName().equals(name))
            .findFirst()
            .orElse(null);
    }
    
    public List<DeformableMesh> getMeshes() { return meshes; }
    
    public void addMesh(DeformableMesh mesh) {
        meshes.add(mesh);
        markModified();
    }
    
    public void removeMesh(DeformableMesh mesh) {
        meshes.remove(mesh);
        markModified();
    }
    
    // Texture management
    public Map<String, String> getTexturePaths() { return texturePaths; }
    
    public void addTexture(String name, String path) {
        texturePaths.put(name, path);
        markModified();
    }
    
    public void removeTexture(String name) {
        texturePaths.remove(name);
        markModified();
    }
    
    public String getTexturePath(String name) {
        return texturePaths.get(name);
    }
    
    // Custom properties
    public Map<String, Object> getCustomProperties() { return customProperties; }
    
    public void setCustomProperty(String key, Object value) {
        customProperties.put(key, value);
        markModified();
    }
    
    public Object getCustomProperty(String key) {
        return customProperties.get(key);
    }
    
    @SuppressWarnings("unchecked")
    public <T> T getCustomProperty(String key, Class<T> type) {
        Object value = customProperties.get(key);
        if (type.isInstance(value)) {
            return (T) value;
        }
        return null;
    }
    
    // Settings
    public int getFrameRate() { return frameRate; }
    public void setFrameRate(int frameRate) { 
        this.frameRate = frameRate; 
        markModified();
    }
    
    public double getDefaultDuration() { return defaultDuration; }
    public void setDefaultDuration(double defaultDuration) { 
        this.defaultDuration = defaultDuration; 
        markModified();
    }
    
    public int getCanvasWidth() { return canvasWidth; }
    public void setCanvasWidth(int canvasWidth) { 
        this.canvasWidth = canvasWidth; 
        markModified();
    }
    
    public int getCanvasHeight() { return canvasHeight; }
    public void setCanvasHeight(int canvasHeight) { 
        this.canvasHeight = canvasHeight; 
        markModified();
    }
    
    // File management
    public File getSourceFile() { return sourceFile; }
    public void setSourceFile(File sourceFile) { this.sourceFile = sourceFile; }
    
    public boolean isModified() { return modified; }
    
    public void markModified() {
        this.modified = true;
        this.modifiedAt = LocalDateTime.now();
    }
    
    public void clearModified() {
        this.modified = false;
    }
    
    public boolean hasSourceFile() {
        return sourceFile != null && sourceFile.exists();
    }
    
    /**
     * Get display name for the project (with modification indicator).
     */
    public String getDisplayName() {
        String displayName = name;
        if (sourceFile != null) {
            displayName = sourceFile.getName();
            if (displayName.endsWith(FILE_EXTENSION)) {
                displayName = displayName.substring(0, displayName.length() - FILE_EXTENSION.length());
            }
        }
        return modified ? displayName + " *" : displayName;
    }
    
    @Override
    public String toString() {
        return "ProjectFile{" +
               "name='" + name + '\'' +
               ", animations=" + animations.size() +
               ", meshes=" + meshes.size() +
               ", modified=" + modified +
               '}';
    }
}
