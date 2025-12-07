package com.animstudio.editor.help;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import java.util.*;

/**
 * Manages keyboard shortcuts for the application.
 */
public class KeyboardShortcuts {
    
    private static KeyboardShortcuts instance;
    private final Map<String, ShortcutInfo> shortcuts;
    private final Map<KeyCombination, String> keyToAction;
    
    public static KeyboardShortcuts getInstance() {
        if (instance == null) {
            instance = new KeyboardShortcuts();
        }
        return instance;
    }
    
    private KeyboardShortcuts() {
        shortcuts = new LinkedHashMap<>();
        keyToAction = new HashMap<>();
        initializeDefaultShortcuts();
    }
    
    private void initializeDefaultShortcuts() {
        // File operations
        addShortcut("file.new", "New Project", KeyCode.N, KeyCombination.CONTROL_DOWN);
        addShortcut("file.open", "Open Project", KeyCode.O, KeyCombination.CONTROL_DOWN);
        addShortcut("file.save", "Save Project", KeyCode.S, KeyCombination.CONTROL_DOWN);
        addShortcut("file.saveAs", "Save As...", KeyCode.S, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN);
        addShortcut("file.export", "Export Animation", KeyCode.E, KeyCombination.CONTROL_DOWN);
        addShortcut("file.preferences", "Preferences", KeyCode.COMMA, KeyCombination.CONTROL_DOWN);
        addShortcut("file.quit", "Quit", KeyCode.Q, KeyCombination.CONTROL_DOWN);
        
        // Edit operations
        addShortcut("edit.undo", "Undo", KeyCode.Z, KeyCombination.CONTROL_DOWN);
        addShortcut("edit.redo", "Redo", KeyCode.Y, KeyCombination.CONTROL_DOWN);
        addShortcut("edit.cut", "Cut", KeyCode.X, KeyCombination.CONTROL_DOWN);
        addShortcut("edit.copy", "Copy", KeyCode.C, KeyCombination.CONTROL_DOWN);
        addShortcut("edit.paste", "Paste", KeyCode.V, KeyCombination.CONTROL_DOWN);
        addShortcut("edit.delete", "Delete", KeyCode.DELETE);
        addShortcut("edit.selectAll", "Select All", KeyCode.A, KeyCombination.CONTROL_DOWN);
        addShortcut("edit.deselectAll", "Deselect All", KeyCode.D, KeyCombination.CONTROL_DOWN);
        addShortcut("edit.duplicate", "Duplicate", KeyCode.D, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN);
        
        // View operations
        addShortcut("view.zoomIn", "Zoom In", KeyCode.EQUALS, KeyCombination.CONTROL_DOWN);
        addShortcut("view.zoomOut", "Zoom Out", KeyCode.MINUS, KeyCombination.CONTROL_DOWN);
        addShortcut("view.zoomReset", "Reset Zoom", KeyCode.DIGIT0, KeyCombination.CONTROL_DOWN);
        addShortcut("view.fitToWindow", "Fit to Window", KeyCode.F, KeyCombination.CONTROL_DOWN);
        addShortcut("view.toggleGrid", "Toggle Grid", KeyCode.G, KeyCombination.CONTROL_DOWN);
        addShortcut("view.toggleBoneNames", "Toggle Bone Names", KeyCode.B, KeyCombination.CONTROL_DOWN);
        addShortcut("view.fullscreen", "Toggle Fullscreen", KeyCode.F11);
        
        // Tool selection
        addShortcut("tool.select", "Select Tool", KeyCode.V);
        addShortcut("tool.move", "Move Tool", KeyCode.M);
        addShortcut("tool.rotate", "Rotate Tool", KeyCode.R);
        addShortcut("tool.scale", "Scale Tool", KeyCode.S);
        addShortcut("tool.bone", "Bone Tool", KeyCode.B);
        addShortcut("tool.ik", "IK Tool", KeyCode.I);
        addShortcut("tool.mesh", "Mesh Tool", KeyCode.E);
        
        // Animation controls
        addShortcut("animation.play", "Play/Pause", KeyCode.SPACE);
        addShortcut("animation.stop", "Stop", KeyCode.PERIOD);
        addShortcut("animation.prevFrame", "Previous Frame", KeyCode.COMMA);
        addShortcut("animation.nextFrame", "Next Frame", KeyCode.PERIOD);
        addShortcut("animation.firstFrame", "First Frame", KeyCode.HOME);
        addShortcut("animation.lastFrame", "Last Frame", KeyCode.END);
        addShortcut("animation.addKeyframe", "Add Keyframe", KeyCode.K);
        addShortcut("animation.deleteKeyframe", "Delete Keyframe", KeyCode.K, KeyCombination.SHIFT_DOWN);
        
        // Skeleton operations
        addShortcut("skeleton.addBone", "Add Bone", KeyCode.INSERT);
        addShortcut("skeleton.deleteBone", "Delete Bone", KeyCode.DELETE);
        addShortcut("skeleton.parentBone", "Parent Bone", KeyCode.P, KeyCombination.CONTROL_DOWN);
        addShortcut("skeleton.unparentBone", "Unparent Bone", KeyCode.P, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN);
        addShortcut("skeleton.mirrorBone", "Mirror Bone", KeyCode.M, KeyCombination.CONTROL_DOWN);
        
        // Help
        addShortcut("help.help", "Help", KeyCode.F1);
        addShortcut("help.shortcuts", "Keyboard Shortcuts", KeyCode.SLASH, KeyCombination.CONTROL_DOWN);
    }
    
    private void addShortcut(String actionId, String description, KeyCode keyCode, KeyCombination.Modifier... modifiers) {
        KeyCombination combination = new KeyCodeCombination(keyCode, modifiers);
        ShortcutInfo info = new ShortcutInfo(actionId, description, combination);
        shortcuts.put(actionId, info);
        keyToAction.put(combination, actionId);
    }
    
    /**
     * Get the shortcut for a specific action.
     */
    public KeyCombination getShortcut(String actionId) {
        ShortcutInfo info = shortcuts.get(actionId);
        return info != null ? info.combination : null;
    }
    
    /**
     * Get the action ID for a key combination.
     */
    public String getAction(KeyCombination combination) {
        return keyToAction.get(combination);
    }
    
    /**
     * Get all shortcuts.
     */
    public Collection<ShortcutInfo> getAllShortcuts() {
        return shortcuts.values();
    }
    
    /**
     * Get shortcuts by category.
     */
    public Map<String, List<ShortcutInfo>> getShortcutsByCategory() {
        Map<String, List<ShortcutInfo>> byCategory = new LinkedHashMap<>();
        
        for (ShortcutInfo info : shortcuts.values()) {
            String category = getCategoryName(info.actionId);
            byCategory.computeIfAbsent(category, k -> new ArrayList<>()).add(info);
        }
        
        return byCategory;
    }
    
    private String getCategoryName(String actionId) {
        int dotIndex = actionId.indexOf('.');
        if (dotIndex > 0) {
            String prefix = actionId.substring(0, dotIndex);
            return switch (prefix) {
                case "file" -> "File";
                case "edit" -> "Edit";
                case "view" -> "View";
                case "tool" -> "Tools";
                case "animation" -> "Animation";
                case "skeleton" -> "Skeleton";
                case "help" -> "Help";
                default -> "Other";
            };
        }
        return "Other";
    }
    
    /**
     * Get human-readable display string for a key combination.
     */
    public static String getDisplayString(KeyCombination combination) {
        if (combination == null) return "";
        return combination.getDisplayText();
    }
    
    /**
     * Information about a keyboard shortcut.
     */
    public static class ShortcutInfo {
        public final String actionId;
        public final String description;
        public final KeyCombination combination;
        
        public ShortcutInfo(String actionId, String description, KeyCombination combination) {
            this.actionId = actionId;
            this.description = description;
            this.combination = combination;
        }
        
        public String getDisplayKey() {
            return combination != null ? combination.getDisplayText() : "";
        }
    }
}
