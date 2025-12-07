package com.animstudio.logging;

/**
 * Interface for log output destinations.
 */
public interface LogAppender {
    
    /**
     * Append a log event.
     *
     * @param event The event to append
     */
    void append(LogEvent event);
    
    /**
     * Get the name of this appender.
     */
    String getName();
    
    /**
     * Get the minimum log level for this appender.
     */
    LogLevel getMinLevel();
    
    /**
     * Set the minimum log level for this appender.
     */
    void setMinLevel(LogLevel level);
    
    /**
     * Check if this appender should process the given event.
     */
    default boolean shouldAppend(LogEvent event) {
        return event.level().isEnabled(getMinLevel());
    }
    
    /**
     * Flush any buffered output.
     */
    default void flush() {
        // Default: no-op
    }
    
    /**
     * Close this appender and release resources.
     */
    default void close() {
        flush();
    }
    
    /**
     * Check if this appender is enabled.
     */
    default boolean isEnabled() {
        return true;
    }
}
