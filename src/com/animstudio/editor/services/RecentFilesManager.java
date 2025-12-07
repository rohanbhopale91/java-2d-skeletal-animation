package com.animstudio.editor.services;

import com.animstudio.editor.preferences.EditorPreferences;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * Manages the list of recently opened files.
 */
public class RecentFilesManager {
    
    private static final String PREFS_KEY = "recentFiles";
    private static final String SEPARATOR = "\n";
    
    private static RecentFilesManager instance;
    
    private final Preferences prefs;
    private final List<File> recentFiles;
    private final List<Runnable> listeners;
    
    public static RecentFilesManager getInstance() {
        if (instance == null) {
            instance = new RecentFilesManager();
        }
        return instance;
    }
    
    private RecentFilesManager() {
        prefs = Preferences.userNodeForPackage(RecentFilesManager.class);
        recentFiles = new ArrayList<>();
        listeners = new ArrayList<>();
        loadFromPrefs();
    }
    
    /**
     * Add a file to recent files list.
     */
    public void addFile(File file) {
        if (file == null || !file.exists()) return;
        
        // Remove if already exists
        recentFiles.removeIf(f -> f.getAbsolutePath().equals(file.getAbsolutePath()));
        
        // Add to front
        recentFiles.add(0, file);
        
        // Trim to max size
        int maxFiles = EditorPreferences.getInstance().getRecentFilesCount();
        while (recentFiles.size() > maxFiles) {
            recentFiles.remove(recentFiles.size() - 1);
        }
        
        saveToPrefs();
        notifyListeners();
    }
    
    /**
     * Remove a file from recent files list.
     */
    public void removeFile(File file) {
        if (file == null) return;
        recentFiles.removeIf(f -> f.getAbsolutePath().equals(file.getAbsolutePath()));
        saveToPrefs();
        notifyListeners();
    }
    
    /**
     * Get all recent files.
     */
    public List<File> getRecentFiles() {
        // Filter out non-existent files
        recentFiles.removeIf(f -> !f.exists());
        return new ArrayList<>(recentFiles);
    }
    
    /**
     * Clear all recent files.
     */
    public void clear() {
        recentFiles.clear();
        saveToPrefs();
        notifyListeners();
    }
    
    /**
     * Get the most recently opened file.
     */
    public File getMostRecent() {
        return recentFiles.isEmpty() ? null : recentFiles.get(0);
    }
    
    /**
     * Add a listener for changes.
     */
    public void addListener(Runnable listener) {
        listeners.add(listener);
    }
    
    /**
     * Remove a listener.
     */
    public void removeListener(Runnable listener) {
        listeners.remove(listener);
    }
    
    private void notifyListeners() {
        for (Runnable listener : listeners) {
            listener.run();
        }
    }
    
    private void loadFromPrefs() {
        String saved = prefs.get(PREFS_KEY, "");
        if (!saved.isEmpty()) {
            String[] paths = saved.split(SEPARATOR);
            for (String path : paths) {
                if (!path.isEmpty()) {
                    File file = new File(path);
                    if (file.exists()) {
                        recentFiles.add(file);
                    }
                }
            }
        }
    }
    
    private void saveToPrefs() {
        StringBuilder sb = new StringBuilder();
        for (File file : recentFiles) {
            if (sb.length() > 0) sb.append(SEPARATOR);
            sb.append(file.getAbsolutePath());
        }
        prefs.put(PREFS_KEY, sb.toString());
    }
}
