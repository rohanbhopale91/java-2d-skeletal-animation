package com.animstudio.io;

import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;

/**
 * Facade for all file I/O operations.
 * Provides a unified interface for saving, loading, exporting, and importing.
 */
public class FileManager {
    
    private final JsonSerializer serializer;
    private final JsonDeserializer deserializer;
    private final RecentFiles recentFiles;
    private final AutosaveManager autosaveManager;
    
    private ProjectFile currentProject;
    private Consumer<ProjectFile> onProjectChanged;
    private Consumer<String> onStatusMessage;
    
    private static FileManager instance;
    
    public static FileManager getInstance() {
        if (instance == null) {
            instance = new FileManager();
        }
        return instance;
    }
    
    private FileManager() {
        serializer = new JsonSerializer();
        deserializer = new JsonDeserializer();
        recentFiles = RecentFiles.getInstance();
        autosaveManager = AutosaveManager.getInstance();
    }
    
    /**
     * Create a new project.
     */
    public ProjectFile newProject(String name) {
        currentProject = new ProjectFile(name);
        autosaveManager.setCurrentProject(currentProject);
        notifyProjectChanged();
        return currentProject;
    }
    
    /**
     * Open a project file.
     */
    public ProjectFile openProject(File file) throws IOException {
        currentProject = deserializer.deserializeFromFile(file);
        recentFiles.addFile(file);
        autosaveManager.setCurrentProject(currentProject);
        notifyProjectChanged();
        notifyStatus("Opened: " + file.getName());
        return currentProject;
    }
    
    /**
     * Save current project to its source file.
     */
    public void saveProject() throws IOException {
        if (currentProject == null) {
            throw new IOException("No project to save");
        }
        
        if (!currentProject.hasSourceFile()) {
            throw new IOException("Project has no source file. Use saveProjectAs()");
        }
        
        serializer.serializeToFile(currentProject, currentProject.getSourceFile());
        recentFiles.addFile(currentProject.getSourceFile());
        notifyStatus("Saved: " + currentProject.getSourceFile().getName());
    }
    
    /**
     * Save current project to a new file.
     */
    public void saveProjectAs(File file) throws IOException {
        if (currentProject == null) {
            throw new IOException("No project to save");
        }
        
        // Ensure correct extension
        if (!file.getName().endsWith(ProjectFile.FILE_EXTENSION)) {
            file = new File(file.getAbsolutePath() + ProjectFile.FILE_EXTENSION);
        }
        
        serializer.serializeToFile(currentProject, file);
        recentFiles.addFile(file);
        notifyStatus("Saved as: " + file.getName());
    }
    
    /**
     * Close current project.
     */
    public void closeProject() {
        currentProject = null;
        autosaveManager.setCurrentProject(null);
        notifyProjectChanged();
    }
    
    /**
     * Get current project.
     */
    public ProjectFile getCurrentProject() {
        return currentProject;
    }
    
    /**
     * Check if current project has unsaved changes.
     */
    public boolean hasUnsavedChanges() {
        return currentProject != null && currentProject.isModified();
    }
    
    /**
     * Set callback for project changes.
     */
    public void setOnProjectChanged(Consumer<ProjectFile> callback) {
        this.onProjectChanged = callback;
    }
    
    /**
     * Set callback for status messages.
     */
    public void setOnStatusMessage(Consumer<String> callback) {
        this.onStatusMessage = callback;
        autosaveManager.setStatusCallback(callback);
    }
    
    /**
     * Get recent files manager.
     */
    public RecentFiles getRecentFiles() {
        return recentFiles;
    }
    
    /**
     * Get autosave manager.
     */
    public AutosaveManager getAutosaveManager() {
        return autosaveManager;
    }
    
    /**
     * Get serializer for custom use.
     */
    public JsonSerializer getSerializer() {
        return serializer;
    }
    
    /**
     * Get deserializer for custom use.
     */
    public JsonDeserializer getDeserializer() {
        return deserializer;
    }
    
    private void notifyProjectChanged() {
        if (onProjectChanged != null) {
            onProjectChanged.accept(currentProject);
        }
    }
    
    private void notifyStatus(String message) {
        if (onStatusMessage != null) {
            onStatusMessage.accept(message);
        }
    }
    
    /**
     * Check if we can recover from autosave.
     */
    public boolean canRecoverAutosave() {
        return autosaveManager.hasAutosaves();
    }
    
    /**
     * Recover from latest autosave.
     */
    public ProjectFile recoverFromAutosave() throws IOException {
        currentProject = autosaveManager.recoverLatest();
        if (currentProject != null) {
            autosaveManager.setCurrentProject(currentProject);
            notifyProjectChanged();
            notifyStatus("Recovered from autosave");
        }
        return currentProject;
    }
}
