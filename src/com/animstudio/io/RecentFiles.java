package com.animstudio.io;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * Manages recent files list and persistence.
 */
public class RecentFiles {
    
    private static final String PREFS_KEY = "recentFiles";
    private static final int MAX_RECENT_FILES = 10;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    
    private final Preferences prefs;
    private final List<RecentFileEntry> entries;
    private final List<RecentFilesListener> listeners;
    
    private static RecentFiles instance;
    
    public static RecentFiles getInstance() {
        if (instance == null) {
            instance = new RecentFiles();
        }
        return instance;
    }
    
    private RecentFiles() {
        prefs = Preferences.userNodeForPackage(RecentFiles.class);
        entries = new ArrayList<>();
        listeners = new ArrayList<>();
        load();
    }
    
    /**
     * Add a file to the recent files list.
     */
    public void addFile(File file) {
        if (file == null || !file.exists()) return;
        
        String path = file.getAbsolutePath();
        
        // Remove existing entry for this file
        entries.removeIf(e -> e.path.equals(path));
        
        // Add to front
        entries.add(0, new RecentFileEntry(path, LocalDateTime.now()));
        
        // Trim to max size
        while (entries.size() > MAX_RECENT_FILES) {
            entries.remove(entries.size() - 1);
        }
        
        save();
        notifyListeners();
    }
    
    /**
     * Remove a file from the recent files list.
     */
    public void removeFile(File file) {
        String path = file.getAbsolutePath();
        entries.removeIf(e -> e.path.equals(path));
        save();
        notifyListeners();
    }
    
    /**
     * Clear all recent files.
     */
    public void clear() {
        entries.clear();
        save();
        notifyListeners();
    }
    
    /**
     * Get list of recent files.
     */
    public List<RecentFileEntry> getRecentFiles() {
        // Remove non-existent files
        entries.removeIf(e -> !new File(e.path).exists());
        return new ArrayList<>(entries);
    }
    
    /**
     * Get the most recently opened file.
     */
    public File getMostRecentFile() {
        for (RecentFileEntry entry : entries) {
            File file = new File(entry.path);
            if (file.exists()) {
                return file;
            }
        }
        return null;
    }
    
    /**
     * Add a listener for recent files changes.
     */
    public void addListener(RecentFilesListener listener) {
        listeners.add(listener);
    }
    
    /**
     * Remove a listener.
     */
    public void removeListener(RecentFilesListener listener) {
        listeners.remove(listener);
    }
    
    private void notifyListeners() {
        for (RecentFilesListener listener : listeners) {
            listener.onRecentFilesChanged(getRecentFiles());
        }
    }
    
    private void load() {
        entries.clear();
        String data = prefs.get(PREFS_KEY, "");
        
        if (!data.isEmpty()) {
            String[] lines = data.split("\n");
            for (String line : lines) {
                String[] parts = line.split("\\|", 2);
                if (parts.length == 2) {
                    try {
                        LocalDateTime time = LocalDateTime.parse(parts[0], DATE_FORMAT);
                        String path = parts[1];
                        entries.add(new RecentFileEntry(path, time));
                    } catch (Exception e) {
                        // Skip invalid entries
                    }
                }
            }
        }
    }
    
    private void save() {
        StringBuilder sb = new StringBuilder();
        for (RecentFileEntry entry : entries) {
            sb.append(entry.timestamp.format(DATE_FORMAT));
            sb.append("|");
            sb.append(entry.path);
            sb.append("\n");
        }
        prefs.put(PREFS_KEY, sb.toString());
    }
    
    /**
     * Recent file entry with path and timestamp.
     */
    public static class RecentFileEntry {
        public final String path;
        public final LocalDateTime timestamp;
        
        public RecentFileEntry(String path, LocalDateTime timestamp) {
            this.path = path;
            this.timestamp = timestamp;
        }
        
        public File getFile() {
            return new File(path);
        }
        
        public String getFileName() {
            return new File(path).getName();
        }
        
        public boolean exists() {
            return new File(path).exists();
        }
    }
    
    /**
     * Listener interface for recent files changes.
     */
    public interface RecentFilesListener {
        void onRecentFilesChanged(List<RecentFileEntry> recentFiles);
    }
}
