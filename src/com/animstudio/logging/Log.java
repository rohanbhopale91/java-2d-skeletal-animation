package com.animstudio.logging;

import java.util.Map;

/**
 * Static facade for convenient logging.
 * Usage: Log.info("MyClass", "Something happened");
 *        Log.error("MyClass", "Failed to load", exception);
 */
public final class Log {
    
    private Log() {
        // Static utility class
    }
    
    // ========== TRACE ==========
    
    public static void trace(String source, String message) {
        log(LogLevel.TRACE, LogCategory.GENERAL, source, message, null, null);
    }
    
    public static void trace(LogCategory category, String source, String message) {
        log(LogLevel.TRACE, category, source, message, null, null);
    }
    
    // ========== DEBUG ==========
    
    public static void debug(String source, String message) {
        log(LogLevel.DEBUG, LogCategory.GENERAL, source, message, null, null);
    }
    
    public static void debug(LogCategory category, String source, String message) {
        log(LogLevel.DEBUG, category, source, message, null, null);
    }
    
    public static void debug(String source, String message, Map<String, Object> context) {
        log(LogLevel.DEBUG, LogCategory.GENERAL, source, message, null, context);
    }
    
    // ========== INFO ==========
    
    public static void info(String source, String message) {
        log(LogLevel.INFO, LogCategory.GENERAL, source, message, null, null);
    }
    
    public static void info(LogCategory category, String source, String message) {
        log(LogLevel.INFO, category, source, message, null, null);
    }
    
    public static void info(String source, String message, Map<String, Object> context) {
        log(LogLevel.INFO, LogCategory.GENERAL, source, message, null, context);
    }
    
    // ========== WARN ==========
    
    public static void warn(String source, String message) {
        log(LogLevel.WARN, LogCategory.GENERAL, source, message, null, null);
    }
    
    public static void warn(LogCategory category, String source, String message) {
        log(LogLevel.WARN, category, source, message, null, null);
    }
    
    public static void warn(String source, String message, Throwable throwable) {
        log(LogLevel.WARN, LogCategory.GENERAL, source, message, throwable, null);
    }
    
    public static void warn(LogCategory category, String source, String message, Throwable throwable) {
        log(LogLevel.WARN, category, source, message, throwable, null);
    }
    
    // ========== ERROR ==========
    
    public static void error(String source, String message) {
        log(LogLevel.ERROR, LogCategory.GENERAL, source, message, null, null);
    }
    
    public static void error(LogCategory category, String source, String message) {
        log(LogLevel.ERROR, category, source, message, null, null);
    }
    
    public static void error(String source, String message, Throwable throwable) {
        log(LogLevel.ERROR, LogCategory.GENERAL, source, message, throwable, null);
    }
    
    public static void error(LogCategory category, String source, String message, Throwable throwable) {
        log(LogLevel.ERROR, category, source, message, throwable, null);
    }
    
    public static void error(String source, String message, Throwable throwable, Map<String, Object> context) {
        log(LogLevel.ERROR, LogCategory.GENERAL, source, message, throwable, context);
    }
    
    // ========== FATAL ==========
    
    public static void fatal(String source, String message) {
        log(LogLevel.FATAL, LogCategory.GENERAL, source, message, null, null);
    }
    
    public static void fatal(LogCategory category, String source, String message) {
        log(LogLevel.FATAL, category, source, message, null, null);
    }
    
    public static void fatal(String source, String message, Throwable throwable) {
        log(LogLevel.FATAL, LogCategory.GENERAL, source, message, throwable, null);
    }
    
    public static void fatal(LogCategory category, String source, String message, Throwable throwable) {
        log(LogLevel.FATAL, category, source, message, throwable, null);
    }
    
    // ========== Generic ==========
    
    public static void log(LogLevel level, String source, String message) {
        log(level, LogCategory.GENERAL, source, message, null, null);
    }
    
    public static void log(LogLevel level, LogCategory category, String source, String message) {
        log(level, category, source, message, null, null);
    }
    
    public static void log(LogLevel level, LogCategory category, String source, String message,
                          Throwable throwable, Map<String, Object> context) {
        LogEvent event = LogEvent.builder()
            .level(level)
            .category(category)
            .source(source)
            .message(message)
            .throwable(throwable)
            .context(context != null ? context : Map.of())
            .build();
        
        LogManager.getInstance().log(event);
    }
    
    /**
     * Log an event directly.
     */
    public static void log(LogEvent event) {
        LogManager.getInstance().log(event);
    }
    
    // ========== Utility Methods ==========
    
    /**
     * Check if a level is enabled globally.
     */
    public static boolean isEnabled(LogLevel level) {
        return level.isEnabled(LogManager.getInstance().getGlobalMinLevel());
    }
    
    /**
     * Set the global minimum log level.
     */
    public static void setLevel(LogLevel level) {
        LogManager.getInstance().setGlobalMinLevel(level);
    }
    
    /**
     * Get the log manager instance.
     */
    public static LogManager getManager() {
        return LogManager.getInstance();
    }
    
    /**
     * Flush all log outputs.
     */
    public static void flush() {
        LogManager.getInstance().flush();
    }
    
    /**
     * Shutdown the logging system.
     */
    public static void shutdown() {
        LogManager.getInstance().shutdown();
    }
}
