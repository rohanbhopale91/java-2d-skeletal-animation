package com.animstudio.logging;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Central log manager that processes log events and dispatches to appenders.
 * Supports async logging via a background thread.
 */
public class LogManager {
    
    private static final LogManager INSTANCE = new LogManager();
    
    private final List<LogAppender> appenders;
    private final List<Consumer<LogEvent>> listeners;
    private final LinkedBlockingQueue<LogEvent> eventQueue;
    private final ExecutorService executor;
    private final AtomicBoolean running;
    private LogLevel globalMinLevel;
    private boolean asyncMode;
    
    private LogManager() {
        this.appenders = new CopyOnWriteArrayList<>();
        this.listeners = new CopyOnWriteArrayList<>();
        this.eventQueue = new LinkedBlockingQueue<>();
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "LogManager-Async");
            t.setDaemon(true);
            return t;
        });
        this.running = new AtomicBoolean(false);
        this.globalMinLevel = LogLevel.DEBUG;
        this.asyncMode = true;
        
        // Add default console appender
        addAppender(ConsoleLogAppender.createDefault());
        
        // Start async processor
        startAsyncProcessor();
    }
    
    public static LogManager getInstance() {
        return INSTANCE;
    }
    
    /**
     * Log an event.
     */
    public void log(LogEvent event) {
        if (!event.level().isEnabled(globalMinLevel)) {
            return;
        }
        
        if (asyncMode) {
            eventQueue.offer(event);
        } else {
            processEvent(event);
        }
    }
    
    /**
     * Add a log appender.
     */
    public void addAppender(LogAppender appender) {
        appenders.add(appender);
    }
    
    /**
     * Remove a log appender by name.
     */
    public void removeAppender(String name) {
        appenders.removeIf(a -> a.getName().equals(name));
    }
    
    /**
     * Get an appender by name.
     */
    public LogAppender getAppender(String name) {
        return appenders.stream()
            .filter(a -> a.getName().equals(name))
            .findFirst()
            .orElse(null);
    }
    
    /**
     * Get all appenders.
     */
    public List<LogAppender> getAppenders() {
        return List.copyOf(appenders);
    }
    
    /**
     * Add a listener for log events (for UI updates).
     */
    public void addListener(Consumer<LogEvent> listener) {
        listeners.add(listener);
    }
    
    /**
     * Remove a listener.
     */
    public void removeListener(Consumer<LogEvent> listener) {
        listeners.remove(listener);
    }
    
    /**
     * Get the global minimum log level.
     */
    public LogLevel getGlobalMinLevel() {
        return globalMinLevel;
    }
    
    /**
     * Set the global minimum log level.
     */
    public void setGlobalMinLevel(LogLevel level) {
        this.globalMinLevel = level;
    }
    
    /**
     * Check if async mode is enabled.
     */
    public boolean isAsyncMode() {
        return asyncMode;
    }
    
    /**
     * Set async mode.
     */
    public void setAsyncMode(boolean async) {
        this.asyncMode = async;
    }
    
    /**
     * Flush all appenders.
     */
    public void flush() {
        // Process remaining events
        processRemainingEvents();
        
        for (LogAppender appender : appenders) {
            try {
                appender.flush();
            } catch (Exception e) {
                System.err.println("Error flushing appender " + appender.getName() + ": " + e.getMessage());
            }
        }
    }
    
    /**
     * Shutdown the log manager.
     */
    public void shutdown() {
        running.set(false);
        flush();
        
        for (LogAppender appender : appenders) {
            try {
                appender.close();
            } catch (Exception e) {
                System.err.println("Error closing appender " + appender.getName() + ": " + e.getMessage());
            }
        }
        
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    private void startAsyncProcessor() {
        running.set(true);
        executor.submit(() -> {
            while (running.get() || !eventQueue.isEmpty()) {
                try {
                    LogEvent event = eventQueue.poll(100, TimeUnit.MILLISECONDS);
                    if (event != null) {
                        processEvent(event);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
    }
    
    private void processEvent(LogEvent event) {
        // Dispatch to appenders
        for (LogAppender appender : appenders) {
            try {
                if (appender.shouldAppend(event)) {
                    appender.append(event);
                }
            } catch (Exception e) {
                System.err.println("Error in appender " + appender.getName() + ": " + e.getMessage());
            }
        }
        
        // Notify listeners (for UI)
        for (Consumer<LogEvent> listener : listeners) {
            try {
                listener.accept(event);
            } catch (Exception e) {
                System.err.println("Error in log listener: " + e.getMessage());
            }
        }
    }
    
    private void processRemainingEvents() {
        LogEvent event;
        while ((event = eventQueue.poll()) != null) {
            processEvent(event);
        }
    }
    
    /**
     * Get the pending event count (for monitoring).
     */
    public int getPendingEventCount() {
        return eventQueue.size();
    }
    
    /**
     * Clear all appenders (except console).
     */
    public void clearAppenders() {
        appenders.clear();
        addAppender(ConsoleLogAppender.createDefault());
    }
}
