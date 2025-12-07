package com.animstudio.logging;

/**
 * Categories for log messages to enable filtering by subsystem.
 */
public enum LogCategory {
    GENERAL("General"),
    CORE("Core"),
    EDITOR("Editor"),
    ANIMATION("Animation"),
    SKELETON("Skeleton"),
    IO("IO"),
    EXPORT("Export"),
    IMPORT("Import"),
    AUTOMATION("Automation"),
    TOOLS("Tools"),
    UI("UI"),
    EVENT("Event"),
    COMMAND("Command"),
    PREFERENCES("Preferences"),
    DIAGNOSTICS("Diagnostics"),
    PERFORMANCE("Performance"),
    MEMORY("Memory"),
    STARTUP("Startup"),
    SHUTDOWN("Shutdown");
    
    private final String displayName;
    
    LogCategory(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Parse category from string, case-insensitive.
     */
    public static LogCategory fromString(String category) {
        if (category == null || category.isEmpty()) {
            return GENERAL;
        }
        try {
            return LogCategory.valueOf(category.toUpperCase());
        } catch (IllegalArgumentException e) {
            return GENERAL;
        }
    }
}
