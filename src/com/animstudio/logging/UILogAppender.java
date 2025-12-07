package com.animstudio.logging;

import javafx.application.Platform;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Log appender that dispatches log events to UI components.
 * Ensures events are delivered on the JavaFX Application Thread.
 */
public class UILogAppender implements LogAppender {
    
    private final String name;
    private final CopyOnWriteArrayList<Consumer<LogEvent>> uiListeners;
    private final LogFormatter formatter;
    private LogLevel minLevel;
    private boolean enabled;
    private int maxBufferSize;
    
    public UILogAppender() {
        this("UI", TextLogFormatter.noStackTrace(), LogLevel.INFO, 1000);
    }
    
    public UILogAppender(String name, LogFormatter formatter, LogLevel minLevel, int maxBufferSize) {
        this.name = name;
        this.formatter = formatter;
        this.minLevel = minLevel;
        this.maxBufferSize = maxBufferSize;
        this.uiListeners = new CopyOnWriteArrayList<>();
        this.enabled = true;
    }
    
    @Override
    public void append(LogEvent event) {
        if (!enabled || !shouldAppend(event)) {
            return;
        }
        
        // Dispatch to UI listeners on JavaFX thread
        if (Platform.isFxApplicationThread()) {
            notifyListeners(event);
        } else {
            Platform.runLater(() -> notifyListeners(event));
        }
    }
    
    private void notifyListeners(LogEvent event) {
        for (Consumer<LogEvent> listener : uiListeners) {
            try {
                listener.accept(event);
            } catch (Exception e) {
                System.err.println("Error in UI log listener: " + e.getMessage());
            }
        }
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public LogLevel getMinLevel() {
        return minLevel;
    }
    
    @Override
    public void setMinLevel(LogLevel level) {
        this.minLevel = level;
    }
    
    @Override
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    /**
     * Add a UI listener for log events.
     */
    public void addUIListener(Consumer<LogEvent> listener) {
        uiListeners.add(listener);
    }
    
    /**
     * Remove a UI listener.
     */
    public void removeUIListener(Consumer<LogEvent> listener) {
        uiListeners.remove(listener);
    }
    
    /**
     * Clear all UI listeners.
     */
    public void clearUIListeners() {
        uiListeners.clear();
    }
    
    /**
     * Get the formatter for UI display.
     */
    public LogFormatter getFormatter() {
        return formatter;
    }
    
    /**
     * Get the max buffer size for UI components.
     */
    public int getMaxBufferSize() {
        return maxBufferSize;
    }
    
    /**
     * Set the max buffer size for UI components.
     */
    public void setMaxBufferSize(int size) {
        this.maxBufferSize = size;
    }
    
    /**
     * Format an event for display.
     */
    public String formatForDisplay(LogEvent event) {
        return formatter.format(event);
    }
    
    /**
     * Create a default UI appender for the log panel.
     */
    public static UILogAppender createForLogPanel() {
        return new UILogAppender("LogPanel", TextLogFormatter.noStackTrace(), LogLevel.INFO, 500);
    }
    
    /**
     * Create a verbose UI appender for debugging.
     */
    public static UILogAppender createDebug() {
        return new UILogAppender("DebugUI", new TextLogFormatter(true, false, true, false), LogLevel.TRACE, 2000);
    }
}
