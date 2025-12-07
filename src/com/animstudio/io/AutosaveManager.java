package com.animstudio.io;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Autosave manager for automatic project backup.
 */
public class AutosaveManager {
    
    private static final Logger LOG = Logger.getLogger(AutosaveManager.class.getName());
    
    private static final String AUTOSAVE_DIR = "autosave";
    private static final String AUTOSAVE_PREFIX = "autosave_";
    private static final int DEFAULT_INTERVAL_SECONDS = 300; // 5 minutes
    private static final int MAX_AUTOSAVES = 5;
    
    private final File autosaveDirectory;
    private final JsonSerializer serializer;
    private Timer timer;
    private ProjectFile currentProject;
    private int intervalSeconds;
    private boolean enabled;
    private final AtomicBoolean saving;
    private Consumer<String> statusCallback;
    
    private static AutosaveManager instance;
    
    public static AutosaveManager getInstance() {
        if (instance == null) {
            instance = new AutosaveManager();
        }
        return instance;
    }
    
    private AutosaveManager() {
        // Create autosave directory in user home
        String userHome = System.getProperty("user.home");
        autosaveDirectory = new File(userHome, ".animstudio" + File.separator + AUTOSAVE_DIR);
        
        if (!autosaveDirectory.exists()) {
            autosaveDirectory.mkdirs();
        }
        
        serializer = new JsonSerializer();
        intervalSeconds = DEFAULT_INTERVAL_SECONDS;
        enabled = true;
        saving = new AtomicBoolean(false);
    }
    
    /**
     * Set the current project to autosave.
     */
    public void setCurrentProject(ProjectFile project) {
        this.currentProject = project;
    }
    
    /**
     * Set autosave interval in seconds.
     */
    public void setInterval(int seconds) {
        this.intervalSeconds = Math.max(30, seconds); // Minimum 30 seconds
        if (timer != null) {
            stop();
            start();
        }
    }
    
    /**
     * Enable or disable autosave.
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (enabled) {
            start();
        } else {
            stop();
        }
    }
    
    /**
     * Set status callback for UI updates.
     */
    public void setStatusCallback(Consumer<String> callback) {
        this.statusCallback = callback;
    }
    
    /**
     * Start autosave timer.
     */
    public void start() {
        stop(); // Stop any existing timer
        
        if (!enabled) return;
        
        timer = new Timer("AutosaveTimer", true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                performAutosave();
            }
        }, intervalSeconds * 1000L, intervalSeconds * 1000L);
        
        LOG.info("Autosave started with interval: " + intervalSeconds + " seconds");
    }
    
    /**
     * Stop autosave timer.
     */
    public void stop() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }
    
    /**
     * Perform an autosave now.
     */
    public void performAutosave() {
        if (!enabled || currentProject == null || !currentProject.isModified()) {
            return;
        }
        
        if (!saving.compareAndSet(false, true)) {
            return; // Already saving
        }
        
        try {
            // Generate autosave filename
            String timestamp = LocalDateTime.now().toString().replace(':', '-').replace('.', '-');
            String filename = AUTOSAVE_PREFIX + timestamp + ProjectFile.FILE_EXTENSION;
            File autosaveFile = new File(autosaveDirectory, filename);
            
            // Save project
            serializer.serializeToFile(currentProject, autosaveFile);
            
            // Cleanup old autosaves
            cleanupOldAutosaves();
            
            LOG.info("Autosave completed: " + autosaveFile.getName());
            
            if (statusCallback != null) {
                statusCallback.accept("Autosaved at " + LocalDateTime.now().toLocalTime());
            }
            
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Autosave failed", e);
        } finally {
            saving.set(false);
        }
    }
    
    /**
     * Get list of available autosave files.
     */
    public File[] getAutosaveFiles() {
        File[] files = autosaveDirectory.listFiles((dir, name) -> 
            name.startsWith(AUTOSAVE_PREFIX) && name.endsWith(ProjectFile.FILE_EXTENSION)
        );
        
        if (files != null) {
            // Sort by modification time (newest first)
            java.util.Arrays.sort(files, (a, b) -> 
                Long.compare(b.lastModified(), a.lastModified()));
        }
        
        return files != null ? files : new File[0];
    }
    
    /**
     * Recover project from most recent autosave.
     */
    public ProjectFile recoverLatest() throws IOException {
        File[] autosaves = getAutosaveFiles();
        if (autosaves.length == 0) {
            return null;
        }
        
        JsonDeserializer deserializer = new JsonDeserializer();
        return deserializer.deserializeFromFile(autosaves[0]);
    }
    
    /**
     * Recover project from specific autosave file.
     */
    public ProjectFile recover(File autosaveFile) throws IOException {
        JsonDeserializer deserializer = new JsonDeserializer();
        return deserializer.deserializeFromFile(autosaveFile);
    }
    
    /**
     * Delete all autosave files for the current project.
     */
    public void clearAutosaves() {
        File[] autosaves = getAutosaveFiles();
        for (File file : autosaves) {
            file.delete();
        }
    }
    
    /**
     * Copy autosave to a new location.
     */
    public void exportAutosave(File autosaveFile, File destination) throws IOException {
        Files.copy(autosaveFile.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }
    
    /**
     * Cleanup old autosaves, keeping only the most recent ones.
     */
    private void cleanupOldAutosaves() {
        File[] autosaves = getAutosaveFiles();
        
        if (autosaves.length > MAX_AUTOSAVES) {
            for (int i = MAX_AUTOSAVES; i < autosaves.length; i++) {
                if (!autosaves[i].delete()) {
                    LOG.warning("Failed to delete old autosave: " + autosaves[i].getName());
                }
            }
        }
    }
    
    /**
     * Check if there are autosaves available for recovery.
     */
    public boolean hasAutosaves() {
        return getAutosaveFiles().length > 0;
    }
    
    /**
     * Get the autosave directory.
     */
    public File getAutosaveDirectory() {
        return autosaveDirectory;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public int getIntervalSeconds() {
        return intervalSeconds;
    }
}
