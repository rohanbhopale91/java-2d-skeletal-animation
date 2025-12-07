package com.animstudio.logging;

/**
 * Log severity levels ordered by priority.
 * Higher ordinal = higher severity.
 */
public enum LogLevel {
    TRACE(0, "TRACE"),
    DEBUG(1, "DEBUG"),
    INFO(2, "INFO"),
    WARN(3, "WARN"),
    ERROR(4, "ERROR"),
    FATAL(5, "FATAL");
    
    private final int priority;
    private final String label;
    
    LogLevel(int priority, String label) {
        this.priority = priority;
        this.label = label;
    }
    
    public int getPriority() {
        return priority;
    }
    
    public String getLabel() {
        return label;
    }
    
    /**
     * Check if this level should be logged given a minimum level.
     */
    public boolean isEnabled(LogLevel minimumLevel) {
        return this.priority >= minimumLevel.priority;
    }
    
    /**
     * Parse a log level from string, case-insensitive.
     */
    public static LogLevel fromString(String level) {
        if (level == null || level.isEmpty()) {
            return INFO;
        }
        try {
            return LogLevel.valueOf(level.toUpperCase());
        } catch (IllegalArgumentException e) {
            return INFO;
        }
    }
}
