package com.animstudio.logging;

/**
 * Interface for formatting log events into string representations.
 */
public interface LogFormatter {
    
    /**
     * Format a log event into a string.
     *
     * @param event The log event to format
     * @return The formatted string representation
     */
    String format(LogEvent event);
    
    /**
     * Get the name of this formatter.
     */
    default String getName() {
        return getClass().getSimpleName();
    }
    
    /**
     * Whether this formatter includes exception stack traces.
     */
    default boolean includesStackTrace() {
        return true;
    }
}
