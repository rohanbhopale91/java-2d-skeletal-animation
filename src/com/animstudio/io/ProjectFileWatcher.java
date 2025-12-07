package com.animstudio.io;

import javafx.application.Platform;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Watches for external modifications to the current project file.
 * Notifies the user when the file has been modified by another program.
 */
public class ProjectFileWatcher {
    
    private static final Logger LOG = Logger.getLogger(ProjectFileWatcher.class.getName());
    
    private WatchService watchService;
    private Thread watchThread;
    private File watchedFile;
    private long lastModifiedTime;
    private final AtomicBoolean running = new AtomicBoolean(false);
    
    private Consumer<File> onFileModified;
    private Consumer<File> onFileDeleted;
    
    private static ProjectFileWatcher instance;
    
    public static ProjectFileWatcher getInstance() {
        if (instance == null) {
            instance = new ProjectFileWatcher();
        }
        return instance;
    }
    
    private ProjectFileWatcher() {
    }
    
    /**
     * Set callback for when the file is externally modified.
     */
    public void setOnFileModified(Consumer<File> callback) {
        this.onFileModified = callback;
    }
    
    /**
     * Set callback for when the file is deleted.
     */
    public void setOnFileDeleted(Consumer<File> callback) {
        this.onFileDeleted = callback;
    }
    
    /**
     * Start watching a file for changes.
     */
    public void watch(File file) {
        stop();
        
        if (file == null || !file.exists()) {
            return;
        }
        
        this.watchedFile = file;
        this.lastModifiedTime = file.lastModified();
        
        try {
            watchService = FileSystems.getDefault().newWatchService();
            Path dir = file.getParentFile().toPath();
            dir.register(watchService, 
                StandardWatchEventKinds.ENTRY_MODIFY,
                StandardWatchEventKinds.ENTRY_DELETE);
            
            running.set(true);
            watchThread = new Thread(this::watchLoop, "ProjectFileWatcher");
            watchThread.setDaemon(true);
            watchThread.start();
            
            LOG.info("Started watching: " + file.getAbsolutePath());
            
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Failed to start file watcher", e);
        }
    }
    
    /**
     * Stop watching the current file.
     */
    public void stop() {
        running.set(false);
        
        if (watchThread != null) {
            watchThread.interrupt();
            try {
                watchThread.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            watchThread = null;
        }
        
        if (watchService != null) {
            try {
                watchService.close();
            } catch (IOException e) {
                LOG.log(Level.WARNING, "Failed to close watch service", e);
            }
            watchService = null;
        }
        
        watchedFile = null;
    }
    
    private void watchLoop() {
        while (running.get()) {
            try {
                WatchKey key = watchService.take();
                
                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();
                    
                    if (kind == StandardWatchEventKinds.OVERFLOW) {
                        continue;
                    }
                    
                    @SuppressWarnings("unchecked")
                    WatchEvent<Path> pathEvent = (WatchEvent<Path>) event;
                    Path changedPath = pathEvent.context();
                    
                    if (watchedFile != null && 
                        changedPath.toString().equals(watchedFile.getName())) {
                        
                        if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
                            notifyDeleted();
                        } else if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                            // Check if actually modified (avoid duplicate events)
                            long newModTime = watchedFile.lastModified();
                            if (newModTime > lastModifiedTime) {
                                lastModifiedTime = newModTime;
                                notifyModified();
                            }
                        }
                    }
                }
                
                if (!key.reset()) {
                    break;
                }
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (ClosedWatchServiceException e) {
                break;
            }
        }
    }
    
    private void notifyModified() {
        if (onFileModified != null && watchedFile != null) {
            File file = watchedFile;
            Platform.runLater(() -> onFileModified.accept(file));
        }
    }
    
    private void notifyDeleted() {
        if (onFileDeleted != null && watchedFile != null) {
            File file = watchedFile;
            Platform.runLater(() -> onFileDeleted.accept(file));
        }
    }
    
    /**
     * Update the last modified time to avoid duplicate notifications
     * after saving the file.
     */
    public void updateLastModifiedTime() {
        if (watchedFile != null && watchedFile.exists()) {
            lastModifiedTime = watchedFile.lastModified();
        }
    }
    
    /**
     * Check if currently watching a file.
     */
    public boolean isWatching() {
        return running.get() && watchedFile != null;
    }
    
    /**
     * Get the currently watched file.
     */
    public File getWatchedFile() {
        return watchedFile;
    }
}
