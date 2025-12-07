package com.animstudio.editor.preferences;

import java.util.prefs.Preferences;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Manages application preferences with persistent storage.
 */
public class EditorPreferences {
    
    private static EditorPreferences instance;
    private final Preferences prefs;
    private final List<Consumer<String>> listeners;
    
    // Preference keys
    public static final String KEY_THEME = "theme";
    public static final String KEY_FRAME_RATE = "frameRate";
    public static final String KEY_CANVAS_WIDTH = "canvasWidth";
    public static final String KEY_CANVAS_HEIGHT = "canvasHeight";
    public static final String KEY_AUTOSAVE_ENABLED = "autosaveEnabled";
    public static final String KEY_AUTOSAVE_INTERVAL = "autosaveInterval";
    public static final String KEY_UNDO_LEVELS = "undoLevels";
    public static final String KEY_GRID_VISIBLE = "gridVisible";
    public static final String KEY_GRID_SIZE = "gridSize";
    public static final String KEY_SNAP_TO_GRID = "snapToGrid";
    public static final String KEY_BONE_COLOR = "boneColor";
    public static final String KEY_SELECTED_BONE_COLOR = "selectedBoneColor";
    public static final String KEY_IK_TARGET_COLOR = "ikTargetColor";
    public static final String KEY_SHOW_BONE_NAMES = "showBoneNames";
    public static final String KEY_SHOW_BONE_LENGTHS = "showBoneLengths";
    public static final String KEY_ANTIALIASING = "antialiasing";
    public static final String KEY_LANGUAGE = "language";
    public static final String KEY_RECENT_FILES_COUNT = "recentFilesCount";
    public static final String KEY_LAST_DIRECTORY = "lastDirectory";
    public static final String KEY_WINDOW_X = "windowX";
    public static final String KEY_WINDOW_Y = "windowY";
    public static final String KEY_WINDOW_WIDTH = "windowWidth";
    public static final String KEY_WINDOW_HEIGHT = "windowHeight";
    public static final String KEY_WINDOW_MAXIMIZED = "windowMaximized";
    
    // Default values
    public static final String DEFAULT_THEME = "dark";
    public static final int DEFAULT_FRAME_RATE = 30;
    public static final int DEFAULT_CANVAS_WIDTH = 1280;
    public static final int DEFAULT_CANVAS_HEIGHT = 720;
    public static final boolean DEFAULT_AUTOSAVE_ENABLED = true;
    public static final int DEFAULT_AUTOSAVE_INTERVAL = 5; // minutes
    public static final int DEFAULT_UNDO_LEVELS = 100;
    public static final boolean DEFAULT_GRID_VISIBLE = true;
    public static final int DEFAULT_GRID_SIZE = 20;
    public static final boolean DEFAULT_SNAP_TO_GRID = false;
    public static final String DEFAULT_BONE_COLOR = "#4A90D9";
    public static final String DEFAULT_SELECTED_BONE_COLOR = "#FF6B35";
    public static final String DEFAULT_IK_TARGET_COLOR = "#00FF00";
    public static final boolean DEFAULT_SHOW_BONE_NAMES = true;
    public static final boolean DEFAULT_SHOW_BONE_LENGTHS = false;
    public static final boolean DEFAULT_ANTIALIASING = true;
    public static final String DEFAULT_LANGUAGE = "en";
    public static final int DEFAULT_RECENT_FILES_COUNT = 10;
    
    public static EditorPreferences getInstance() {
        if (instance == null) {
            instance = new EditorPreferences();
        }
        return instance;
    }
    
    private EditorPreferences() {
        prefs = Preferences.userNodeForPackage(EditorPreferences.class);
        listeners = new ArrayList<>();
    }
    
    // Theme
    public String getTheme() {
        return prefs.get(KEY_THEME, DEFAULT_THEME);
    }
    
    public void setTheme(String theme) {
        prefs.put(KEY_THEME, theme);
        notifyListeners(KEY_THEME);
    }
    
    // Frame Rate
    public int getFrameRate() {
        return prefs.getInt(KEY_FRAME_RATE, DEFAULT_FRAME_RATE);
    }
    
    public void setFrameRate(int frameRate) {
        prefs.putInt(KEY_FRAME_RATE, frameRate);
        notifyListeners(KEY_FRAME_RATE);
    }
    
    // Canvas Dimensions
    public int getCanvasWidth() {
        return prefs.getInt(KEY_CANVAS_WIDTH, DEFAULT_CANVAS_WIDTH);
    }
    
    public void setCanvasWidth(int width) {
        prefs.putInt(KEY_CANVAS_WIDTH, width);
        notifyListeners(KEY_CANVAS_WIDTH);
    }
    
    public int getCanvasHeight() {
        return prefs.getInt(KEY_CANVAS_HEIGHT, DEFAULT_CANVAS_HEIGHT);
    }
    
    public void setCanvasHeight(int height) {
        prefs.putInt(KEY_CANVAS_HEIGHT, height);
        notifyListeners(KEY_CANVAS_HEIGHT);
    }
    
    // Autosave
    public boolean isAutosaveEnabled() {
        return prefs.getBoolean(KEY_AUTOSAVE_ENABLED, DEFAULT_AUTOSAVE_ENABLED);
    }
    
    public void setAutosaveEnabled(boolean enabled) {
        prefs.putBoolean(KEY_AUTOSAVE_ENABLED, enabled);
        notifyListeners(KEY_AUTOSAVE_ENABLED);
    }
    
    public int getAutosaveInterval() {
        return prefs.getInt(KEY_AUTOSAVE_INTERVAL, DEFAULT_AUTOSAVE_INTERVAL);
    }
    
    public void setAutosaveInterval(int minutes) {
        prefs.putInt(KEY_AUTOSAVE_INTERVAL, minutes);
        notifyListeners(KEY_AUTOSAVE_INTERVAL);
    }
    
    // Undo Levels
    public int getUndoLevels() {
        return prefs.getInt(KEY_UNDO_LEVELS, DEFAULT_UNDO_LEVELS);
    }
    
    public void setUndoLevels(int levels) {
        prefs.putInt(KEY_UNDO_LEVELS, levels);
        notifyListeners(KEY_UNDO_LEVELS);
    }
    
    // Grid
    public boolean isGridVisible() {
        return prefs.getBoolean(KEY_GRID_VISIBLE, DEFAULT_GRID_VISIBLE);
    }
    
    public void setGridVisible(boolean visible) {
        prefs.putBoolean(KEY_GRID_VISIBLE, visible);
        notifyListeners(KEY_GRID_VISIBLE);
    }
    
    public int getGridSize() {
        return prefs.getInt(KEY_GRID_SIZE, DEFAULT_GRID_SIZE);
    }
    
    public void setGridSize(int size) {
        prefs.putInt(KEY_GRID_SIZE, size);
        notifyListeners(KEY_GRID_SIZE);
    }
    
    public boolean isSnapToGrid() {
        return prefs.getBoolean(KEY_SNAP_TO_GRID, DEFAULT_SNAP_TO_GRID);
    }
    
    public void setSnapToGrid(boolean snap) {
        prefs.putBoolean(KEY_SNAP_TO_GRID, snap);
        notifyListeners(KEY_SNAP_TO_GRID);
    }
    
    // Colors
    public String getBoneColor() {
        return prefs.get(KEY_BONE_COLOR, DEFAULT_BONE_COLOR);
    }
    
    public void setBoneColor(String color) {
        prefs.put(KEY_BONE_COLOR, color);
        notifyListeners(KEY_BONE_COLOR);
    }
    
    public String getSelectedBoneColor() {
        return prefs.get(KEY_SELECTED_BONE_COLOR, DEFAULT_SELECTED_BONE_COLOR);
    }
    
    public void setSelectedBoneColor(String color) {
        prefs.put(KEY_SELECTED_BONE_COLOR, color);
        notifyListeners(KEY_SELECTED_BONE_COLOR);
    }
    
    public String getIkTargetColor() {
        return prefs.get(KEY_IK_TARGET_COLOR, DEFAULT_IK_TARGET_COLOR);
    }
    
    public void setIkTargetColor(String color) {
        prefs.put(KEY_IK_TARGET_COLOR, color);
        notifyListeners(KEY_IK_TARGET_COLOR);
    }
    
    // Display Options
    public boolean isShowBoneNames() {
        return prefs.getBoolean(KEY_SHOW_BONE_NAMES, DEFAULT_SHOW_BONE_NAMES);
    }
    
    public void setShowBoneNames(boolean show) {
        prefs.putBoolean(KEY_SHOW_BONE_NAMES, show);
        notifyListeners(KEY_SHOW_BONE_NAMES);
    }
    
    public boolean isShowBoneLengths() {
        return prefs.getBoolean(KEY_SHOW_BONE_LENGTHS, DEFAULT_SHOW_BONE_LENGTHS);
    }
    
    public void setShowBoneLengths(boolean show) {
        prefs.putBoolean(KEY_SHOW_BONE_LENGTHS, show);
        notifyListeners(KEY_SHOW_BONE_LENGTHS);
    }
    
    public boolean isAntialiasing() {
        return prefs.getBoolean(KEY_ANTIALIASING, DEFAULT_ANTIALIASING);
    }
    
    public void setAntialiasing(boolean enabled) {
        prefs.putBoolean(KEY_ANTIALIASING, enabled);
        notifyListeners(KEY_ANTIALIASING);
    }
    
    // Language
    public String getLanguage() {
        return prefs.get(KEY_LANGUAGE, DEFAULT_LANGUAGE);
    }
    
    public void setLanguage(String language) {
        prefs.put(KEY_LANGUAGE, language);
        notifyListeners(KEY_LANGUAGE);
    }
    
    // Recent Files
    public int getRecentFilesCount() {
        return prefs.getInt(KEY_RECENT_FILES_COUNT, DEFAULT_RECENT_FILES_COUNT);
    }
    
    public void setRecentFilesCount(int count) {
        prefs.putInt(KEY_RECENT_FILES_COUNT, count);
        notifyListeners(KEY_RECENT_FILES_COUNT);
    }
    
    // Last Directory
    public String getLastDirectory() {
        return prefs.get(KEY_LAST_DIRECTORY, System.getProperty("user.home"));
    }
    
    public void setLastDirectory(String directory) {
        prefs.put(KEY_LAST_DIRECTORY, directory);
    }
    
    // Window Position and Size
    public double getWindowX() {
        return prefs.getDouble(KEY_WINDOW_X, 100);
    }
    
    public void setWindowX(double x) {
        prefs.putDouble(KEY_WINDOW_X, x);
    }
    
    public double getWindowY() {
        return prefs.getDouble(KEY_WINDOW_Y, 100);
    }
    
    public void setWindowY(double y) {
        prefs.putDouble(KEY_WINDOW_Y, y);
    }
    
    public double getWindowWidth() {
        return prefs.getDouble(KEY_WINDOW_WIDTH, 1400);
    }
    
    public void setWindowWidth(double width) {
        prefs.putDouble(KEY_WINDOW_WIDTH, width);
    }
    
    public double getWindowHeight() {
        return prefs.getDouble(KEY_WINDOW_HEIGHT, 900);
    }
    
    public void setWindowHeight(double height) {
        prefs.putDouble(KEY_WINDOW_HEIGHT, height);
    }
    
    public boolean isWindowMaximized() {
        return prefs.getBoolean(KEY_WINDOW_MAXIMIZED, false);
    }
    
    public void setWindowMaximized(boolean maximized) {
        prefs.putBoolean(KEY_WINDOW_MAXIMIZED, maximized);
    }
    
    // Generic getters/setters for extension
    public String getString(String key, String defaultValue) {
        return prefs.get(key, defaultValue);
    }
    
    public void setString(String key, String value) {
        prefs.put(key, value);
        notifyListeners(key);
    }
    
    public int getInt(String key, int defaultValue) {
        return prefs.getInt(key, defaultValue);
    }
    
    public void setInt(String key, int value) {
        prefs.putInt(key, value);
        notifyListeners(key);
    }
    
    public boolean getBoolean(String key, boolean defaultValue) {
        return prefs.getBoolean(key, defaultValue);
    }
    
    public void setBoolean(String key, boolean value) {
        prefs.putBoolean(key, value);
        notifyListeners(key);
    }
    
    public double getDouble(String key, double defaultValue) {
        return prefs.getDouble(key, defaultValue);
    }
    
    public void setDouble(String key, double value) {
        prefs.putDouble(key, value);
        notifyListeners(key);
    }
    
    // Listeners
    public void addListener(Consumer<String> listener) {
        listeners.add(listener);
    }
    
    public void removeListener(Consumer<String> listener) {
        listeners.remove(listener);
    }
    
    private void notifyListeners(String key) {
        for (Consumer<String> listener : listeners) {
            listener.accept(key);
        }
    }
    
    /**
     * Reset all preferences to defaults.
     */
    public void resetToDefaults() {
        setTheme(DEFAULT_THEME);
        setFrameRate(DEFAULT_FRAME_RATE);
        setCanvasWidth(DEFAULT_CANVAS_WIDTH);
        setCanvasHeight(DEFAULT_CANVAS_HEIGHT);
        setAutosaveEnabled(DEFAULT_AUTOSAVE_ENABLED);
        setAutosaveInterval(DEFAULT_AUTOSAVE_INTERVAL);
        setUndoLevels(DEFAULT_UNDO_LEVELS);
        setGridVisible(DEFAULT_GRID_VISIBLE);
        setGridSize(DEFAULT_GRID_SIZE);
        setSnapToGrid(DEFAULT_SNAP_TO_GRID);
        setBoneColor(DEFAULT_BONE_COLOR);
        setSelectedBoneColor(DEFAULT_SELECTED_BONE_COLOR);
        setIkTargetColor(DEFAULT_IK_TARGET_COLOR);
        setShowBoneNames(DEFAULT_SHOW_BONE_NAMES);
        setShowBoneLengths(DEFAULT_SHOW_BONE_LENGTHS);
        setAntialiasing(DEFAULT_ANTIALIASING);
        setLanguage(DEFAULT_LANGUAGE);
        setRecentFilesCount(DEFAULT_RECENT_FILES_COUNT);
    }
    
    /**
     * Force save preferences to storage.
     */
    public void flush() {
        try {
            prefs.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
