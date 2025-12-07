package com.animstudio.logging;

import com.animstudio.logging.diagnostics.DiagnosticsManager;
import com.animstudio.logging.performance.PerformanceMonitor;

/**
 * Initializes and configures the logging system.
 * Call LoggingSystem.initialize() at application startup.
 */
public final class LoggingSystem {
    
    private static boolean initialized = false;
    private static LoggingConfig config;
    private static FileLogAppender fileAppender;
    private static UILogAppender uiAppender;
    
    private LoggingSystem() {
        // Static utility class
    }
    
    /**
     * Initialize the logging system with default configuration.
     */
    public static void initialize() {
        initialize(LoggingConfig.load());
    }
    
    /**
     * Initialize the logging system with custom configuration.
     */
    public static synchronized void initialize(LoggingConfig configuration) {
        if (initialized) {
            Log.warn("LoggingSystem", "Logging system already initialized");
            return;
        }
        
        config = configuration;
        LogManager manager = LogManager.getInstance();
        
        // Configure global level
        manager.setGlobalMinLevel(config.getGlobalLevel());
        manager.setAsyncMode(config.isAsyncMode());
        
        // Configure console appender
        ConsoleLogAppender consoleAppender = (ConsoleLogAppender) manager.getAppender("Console");
        if (consoleAppender != null) {
            consoleAppender.setMinLevel(config.getConsoleLevel());
            consoleAppender.setEnabled(config.isConsoleLoggingEnabled());
        }
        
        // Add file appender
        if (config.isFileLoggingEnabled()) {
            fileAppender = new FileLogAppender(
                config.getLogDirectory(),
                config.getLogFileName(),
                new TextLogFormatter(true, false, false, true),
                config.getFileLevel(),
                config.getMaxFileSize(),
                config.getMaxBackupFiles()
            );
            manager.addAppender(fileAppender);
        }
        
        // Add UI appender
        if (config.isUiLoggingEnabled()) {
            uiAppender = UILogAppender.createForLogPanel();
            uiAppender.setMinLevel(config.getUiLevel());
            manager.addAppender(uiAppender);
        }
        
        initialized = true;
        
        // Log startup
        Log.info(LogCategory.STARTUP, "LoggingSystem", "Logging system initialized");
        Log.info(LogCategory.STARTUP, "LoggingSystem", 
            "Global level: " + config.getGlobalLevel() + 
            ", File logging: " + config.isFileLoggingEnabled() +
            ", Log directory: " + config.getLogDirectory());
        
        // Run startup diagnostics
        if (config.isDiagnosticsEnabled()) {
            DiagnosticsManager.getInstance().initialize();
        }
        
        // Start performance monitoring
        if (config.isPerformanceMonitoringEnabled()) {
            PerformanceMonitor.getInstance().start();
        }
    }
    
    /**
     * Shutdown the logging system.
     */
    public static synchronized void shutdown() {
        if (!initialized) return;
        
        Log.info(LogCategory.SHUTDOWN, "LoggingSystem", "Shutting down logging system...");
        
        // Stop performance monitor
        PerformanceMonitor.getInstance().stop();
        
        // Stop diagnostics
        DiagnosticsManager.getInstance().shutdown();
        
        // Flush and close
        Log.flush();
        Log.shutdown();
        
        initialized = false;
    }
    
    /**
     * Get the current configuration.
     */
    public static LoggingConfig getConfig() {
        return config;
    }
    
    /**
     * Check if the logging system is initialized.
     */
    public static boolean isInitialized() {
        return initialized;
    }
    
    /**
     * Get the file appender (for accessing log file path).
     */
    public static FileLogAppender getFileAppender() {
        return fileAppender;
    }
    
    /**
     * Get the UI appender (for connecting to LogPanel).
     */
    public static UILogAppender getUIAppender() {
        return uiAppender;
    }
    
    /**
     * Reconfigure the logging system (updates appender levels).
     */
    public static void reconfigure(LoggingConfig newConfig) {
        if (!initialized) {
            initialize(newConfig);
            return;
        }
        
        config = newConfig;
        LogManager manager = LogManager.getInstance();
        
        manager.setGlobalMinLevel(config.getGlobalLevel());
        
        ConsoleLogAppender consoleAppender = (ConsoleLogAppender) manager.getAppender("Console");
        if (consoleAppender != null) {
            consoleAppender.setMinLevel(config.getConsoleLevel());
            consoleAppender.setEnabled(config.isConsoleLoggingEnabled());
        }
        
        if (fileAppender != null) {
            fileAppender.setMinLevel(config.getFileLevel());
            fileAppender.setEnabled(config.isFileLoggingEnabled());
        }
        
        if (uiAppender != null) {
            uiAppender.setMinLevel(config.getUiLevel());
            uiAppender.setEnabled(config.isUiLoggingEnabled());
        }
        
        Log.info("LoggingSystem", "Logging configuration updated");
    }
}
