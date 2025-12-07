package com.animstudio.logging;

import java.nio.file.Path;
import java.util.prefs.Preferences;

/**
 * Configuration for the logging system.
 */
public class LoggingConfig {
    
    private static final String PREFS_NODE = "com.animstudio.logging";
    
    private LogLevel globalLevel = LogLevel.INFO;
    private LogLevel consoleLevel = LogLevel.DEBUG;
    private LogLevel fileLevel = LogLevel.DEBUG;
    private LogLevel uiLevel = LogLevel.INFO;
    
    private boolean fileLoggingEnabled = true;
    private boolean consoleLoggingEnabled = true;
    private boolean uiLoggingEnabled = true;
    private boolean asyncMode = true;
    
    private Path logDirectory;
    private String logFileName = "animstudio";
    private long maxFileSize = 10 * 1024 * 1024; // 10MB
    private int maxBackupFiles = 5;
    
    private boolean performanceMonitoringEnabled = false;
    private boolean diagnosticsEnabled = true;
    private int diagnosticsIntervalSeconds = 60;
    
    public LoggingConfig() {
        // Set default log directory
        Path userHome = Path.of(System.getProperty("user.home"));
        this.logDirectory = userHome.resolve(".animstudio").resolve("logs");
    }
    
    // ========== Getters and Setters ==========
    
    public LogLevel getGlobalLevel() {
        return globalLevel;
    }
    
    public LoggingConfig setGlobalLevel(LogLevel level) {
        this.globalLevel = level;
        return this;
    }
    
    public LogLevel getConsoleLevel() {
        return consoleLevel;
    }
    
    public LoggingConfig setConsoleLevel(LogLevel level) {
        this.consoleLevel = level;
        return this;
    }
    
    public LogLevel getFileLevel() {
        return fileLevel;
    }
    
    public LoggingConfig setFileLevel(LogLevel level) {
        this.fileLevel = level;
        return this;
    }
    
    public LogLevel getUiLevel() {
        return uiLevel;
    }
    
    public LoggingConfig setUiLevel(LogLevel level) {
        this.uiLevel = level;
        return this;
    }
    
    public boolean isFileLoggingEnabled() {
        return fileLoggingEnabled;
    }
    
    public LoggingConfig setFileLoggingEnabled(boolean enabled) {
        this.fileLoggingEnabled = enabled;
        return this;
    }
    
    public boolean isConsoleLoggingEnabled() {
        return consoleLoggingEnabled;
    }
    
    public LoggingConfig setConsoleLoggingEnabled(boolean enabled) {
        this.consoleLoggingEnabled = enabled;
        return this;
    }
    
    public boolean isUiLoggingEnabled() {
        return uiLoggingEnabled;
    }
    
    public LoggingConfig setUiLoggingEnabled(boolean enabled) {
        this.uiLoggingEnabled = enabled;
        return this;
    }
    
    public boolean isAsyncMode() {
        return asyncMode;
    }
    
    public LoggingConfig setAsyncMode(boolean async) {
        this.asyncMode = async;
        return this;
    }
    
    public Path getLogDirectory() {
        return logDirectory;
    }
    
    public LoggingConfig setLogDirectory(Path directory) {
        this.logDirectory = directory;
        return this;
    }
    
    public String getLogFileName() {
        return logFileName;
    }
    
    public LoggingConfig setLogFileName(String fileName) {
        this.logFileName = fileName;
        return this;
    }
    
    public long getMaxFileSize() {
        return maxFileSize;
    }
    
    public LoggingConfig setMaxFileSize(long size) {
        this.maxFileSize = size;
        return this;
    }
    
    public int getMaxBackupFiles() {
        return maxBackupFiles;
    }
    
    public LoggingConfig setMaxBackupFiles(int count) {
        this.maxBackupFiles = count;
        return this;
    }
    
    public boolean isPerformanceMonitoringEnabled() {
        return performanceMonitoringEnabled;
    }
    
    public LoggingConfig setPerformanceMonitoringEnabled(boolean enabled) {
        this.performanceMonitoringEnabled = enabled;
        return this;
    }
    
    public boolean isDiagnosticsEnabled() {
        return diagnosticsEnabled;
    }
    
    public LoggingConfig setDiagnosticsEnabled(boolean enabled) {
        this.diagnosticsEnabled = enabled;
        return this;
    }
    
    public int getDiagnosticsIntervalSeconds() {
        return diagnosticsIntervalSeconds;
    }
    
    public LoggingConfig setDiagnosticsIntervalSeconds(int seconds) {
        this.diagnosticsIntervalSeconds = seconds;
        return this;
    }
    
    // ========== Persistence ==========
    
    /**
     * Save configuration to preferences.
     */
    public void save() {
        Preferences prefs = Preferences.userRoot().node(PREFS_NODE);
        
        prefs.put("globalLevel", globalLevel.name());
        prefs.put("consoleLevel", consoleLevel.name());
        prefs.put("fileLevel", fileLevel.name());
        prefs.put("uiLevel", uiLevel.name());
        
        prefs.putBoolean("fileLoggingEnabled", fileLoggingEnabled);
        prefs.putBoolean("consoleLoggingEnabled", consoleLoggingEnabled);
        prefs.putBoolean("uiLoggingEnabled", uiLoggingEnabled);
        prefs.putBoolean("asyncMode", asyncMode);
        
        prefs.put("logDirectory", logDirectory.toString());
        prefs.put("logFileName", logFileName);
        prefs.putLong("maxFileSize", maxFileSize);
        prefs.putInt("maxBackupFiles", maxBackupFiles);
        
        prefs.putBoolean("performanceMonitoringEnabled", performanceMonitoringEnabled);
        prefs.putBoolean("diagnosticsEnabled", diagnosticsEnabled);
        prefs.putInt("diagnosticsIntervalSeconds", diagnosticsIntervalSeconds);
    }
    
    /**
     * Load configuration from preferences.
     */
    public static LoggingConfig load() {
        LoggingConfig config = new LoggingConfig();
        Preferences prefs = Preferences.userRoot().node(PREFS_NODE);
        
        config.globalLevel = LogLevel.fromString(prefs.get("globalLevel", "INFO"));
        config.consoleLevel = LogLevel.fromString(prefs.get("consoleLevel", "DEBUG"));
        config.fileLevel = LogLevel.fromString(prefs.get("fileLevel", "DEBUG"));
        config.uiLevel = LogLevel.fromString(prefs.get("uiLevel", "INFO"));
        
        config.fileLoggingEnabled = prefs.getBoolean("fileLoggingEnabled", true);
        config.consoleLoggingEnabled = prefs.getBoolean("consoleLoggingEnabled", true);
        config.uiLoggingEnabled = prefs.getBoolean("uiLoggingEnabled", true);
        config.asyncMode = prefs.getBoolean("asyncMode", true);
        
        String logDir = prefs.get("logDirectory", null);
        if (logDir != null) {
            config.logDirectory = Path.of(logDir);
        }
        config.logFileName = prefs.get("logFileName", "animstudio");
        config.maxFileSize = prefs.getLong("maxFileSize", 10 * 1024 * 1024);
        config.maxBackupFiles = prefs.getInt("maxBackupFiles", 5);
        
        config.performanceMonitoringEnabled = prefs.getBoolean("performanceMonitoringEnabled", false);
        config.diagnosticsEnabled = prefs.getBoolean("diagnosticsEnabled", true);
        config.diagnosticsIntervalSeconds = prefs.getInt("diagnosticsIntervalSeconds", 60);
        
        return config;
    }
    
    /**
     * Create a debug configuration.
     */
    public static LoggingConfig debug() {
        return new LoggingConfig()
            .setGlobalLevel(LogLevel.TRACE)
            .setConsoleLevel(LogLevel.TRACE)
            .setFileLevel(LogLevel.TRACE)
            .setUiLevel(LogLevel.DEBUG)
            .setPerformanceMonitoringEnabled(true);
    }
    
    /**
     * Create a production configuration.
     */
    public static LoggingConfig production() {
        return new LoggingConfig()
            .setGlobalLevel(LogLevel.INFO)
            .setConsoleLevel(LogLevel.WARN)
            .setFileLevel(LogLevel.INFO)
            .setUiLevel(LogLevel.INFO)
            .setPerformanceMonitoringEnabled(false);
    }
}
