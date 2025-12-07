package com.animstudio.editor.services;

import com.animstudio.editor.preferences.EditorPreferences;
import javafx.application.Platform;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.*;
import java.util.logging.Logger;

/**
 * Manages automatic saving of projects.
 */
public class AutosaveManager {
    
    private static final Logger LOGGER = Logger.getLogger(AutosaveManager.class.getName());
    private static AutosaveManager instance;
    
    private final ScheduledExecutorService scheduler;
    private final EditorPreferences prefs;
    
    private ScheduledFuture<?> autosaveTask;
    private File autosaveDirectory;
    private Runnable onAutosave;
    private boolean hasUnsavedChanges;
    
    public static AutosaveManager getInstance() {
        if (instance == null) {
            instance = new AutosaveManager();
        }
        return instance;
    }
    
    private AutosaveManager() {
        prefs = EditorPreferences.getInstance();
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "Autosave-Thread");
            t.setDaemon(true);
            return t;
        });
        
        // Set up autosave directory
        String userHome = System.getProperty("user.home");
        autosaveDirectory = new File(userHome, ".animstudio/autosave");
        if (!autosaveDirectory.exists()) {
            autosaveDirectory.mkdirs();
        }
        
        // Listen for preference changes
        prefs.addListener(key -> {
            if (EditorPreferences.KEY_AUTOSAVE_ENABLED.equals(key) ||
                EditorPreferences.KEY_AUTOSAVE_INTERVAL.equals(key)) {
                updateAutosaveSchedule();
            }
        });
    }
    
    /**
     * Start the autosave service.
     */
    public void start() {
        updateAutosaveSchedule();
        LOGGER.info("Autosave manager started");
    }
    
    /**
     * Stop the autosave service.
     */
    public void stop() {
        if (autosaveTask != null) {
            autosaveTask.cancel(false);
            autosaveTask = null;
        }
        LOGGER.info("Autosave manager stopped");
    }
    
    /**
     * Mark that there are unsaved changes.
     */
    public void markDirty() {
        hasUnsavedChanges = true;
    }
    
    /**
     * Mark that changes have been saved.
     */
    public void markClean() {
        hasUnsavedChanges = false;
    }
    
    /**
     * Check if there are unsaved changes.
     */
    public boolean hasUnsavedChanges() {
        return hasUnsavedChanges;
    }
    
    /**
     * Set callback for autosave events.
     */
    public void setOnAutosave(Runnable onAutosave) {
        this.onAutosave = onAutosave;
    }
    
    /**
     * Perform an autosave now.
     */
    public void autosaveNow() {
        if (!hasUnsavedChanges) {
            LOGGER.fine("No unsaved changes, skipping autosave");
            return;
        }
        
        Platform.runLater(() -> {
            try {
                if (onAutosave != null) {
                    onAutosave.run();
                    hasUnsavedChanges = false;
                    LOGGER.info("Autosave completed at " + 
                        LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
                }
            } catch (Exception e) {
                LOGGER.warning("Autosave failed: " + e.getMessage());
            }
        });
    }
    
    /**
     * Get the autosave directory.
     */
    public File getAutosaveDirectory() {
        return autosaveDirectory;
    }
    
    /**
     * Generate autosave filename.
     */
    public String generateAutosaveFilename(String baseName) {
        String timestamp = LocalDateTime.now().format(
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        return baseName + "_autosave_" + timestamp + ".anim";
    }
    
    /**
     * Clean up old autosave files (keep only last N).
     */
    public void cleanupOldAutosaves(int keepCount) {
        File[] files = autosaveDirectory.listFiles((dir, name) -> 
            name.endsWith("_autosave_") && name.endsWith(".anim"));
        
        if (files != null && files.length > keepCount) {
            // Sort by last modified, oldest first
            java.util.Arrays.sort(files, (a, b) -> 
                Long.compare(a.lastModified(), b.lastModified()));
            
            // Delete oldest files
            for (int i = 0; i < files.length - keepCount; i++) {
                if (files[i].delete()) {
                    LOGGER.info("Deleted old autosave: " + files[i].getName());
                }
            }
        }
    }
    
    private void updateAutosaveSchedule() {
        // Cancel existing task
        if (autosaveTask != null) {
            autosaveTask.cancel(false);
            autosaveTask = null;
        }
        
        // Start new task if enabled
        if (prefs.isAutosaveEnabled()) {
            int intervalMinutes = prefs.getAutosaveInterval();
            autosaveTask = scheduler.scheduleAtFixedRate(
                this::autosaveNow,
                intervalMinutes,
                intervalMinutes,
                TimeUnit.MINUTES
            );
            LOGGER.info("Autosave scheduled every " + intervalMinutes + " minutes");
        }
    }
    
    /**
     * Shutdown the manager.
     */
    public void shutdown() {
        stop();
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
