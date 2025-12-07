package com.animstudio.logging;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Log appender that writes to files with rotation support.
 */
public class FileLogAppender implements LogAppender {
    
    private static final DateTimeFormatter ROTATION_FORMAT = 
        DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    
    private final String name;
    private final Path logDirectory;
    private final String baseFileName;
    private final LogFormatter formatter;
    private final long maxFileSize;
    private final int maxBackupCount;
    
    private LogLevel minLevel;
    private PrintWriter writer;
    private Path currentFile;
    private long currentSize;
    private boolean enabled;
    
    public FileLogAppender(Path logDirectory, String baseFileName) {
        this(logDirectory, baseFileName, new TextLogFormatter(), 
             LogLevel.DEBUG, 10 * 1024 * 1024, 5); // 10MB, 5 backups
    }
    
    public FileLogAppender(Path logDirectory, String baseFileName, LogFormatter formatter,
                          LogLevel minLevel, long maxFileSize, int maxBackupCount) {
        this.name = "File:" + baseFileName;
        this.logDirectory = logDirectory;
        this.baseFileName = baseFileName;
        this.formatter = formatter;
        this.minLevel = minLevel;
        this.maxFileSize = maxFileSize;
        this.maxBackupCount = maxBackupCount;
        this.enabled = true;
        
        initializeFile();
    }
    
    private void initializeFile() {
        try {
            Files.createDirectories(logDirectory);
            this.currentFile = logDirectory.resolve(baseFileName + ".log");
            this.currentSize = Files.exists(currentFile) ? Files.size(currentFile) : 0;
            this.writer = new PrintWriter(new BufferedWriter(
                new FileWriter(currentFile.toFile(), true)), true);
        } catch (IOException e) {
            System.err.println("Failed to initialize file appender: " + e.getMessage());
            this.enabled = false;
        }
    }
    
    @Override
    public synchronized void append(LogEvent event) {
        if (!enabled || !shouldAppend(event) || writer == null) {
            return;
        }
        
        String formatted = formatter.format(event);
        writer.println(formatted);
        currentSize += formatted.length() + System.lineSeparator().length();
        
        // Check for rotation
        if (currentSize >= maxFileSize) {
            rotate();
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
    
    @Override
    public synchronized void flush() {
        if (writer != null) {
            writer.flush();
        }
    }
    
    @Override
    public synchronized void close() {
        if (writer != null) {
            writer.flush();
            writer.close();
            writer = null;
        }
    }
    
    /**
     * Rotate the log file.
     */
    public synchronized void rotate() {
        if (writer == null) return;
        
        try {
            writer.close();
            
            // Create backup name with timestamp
            String timestamp = LocalDateTime.now().format(ROTATION_FORMAT);
            Path backupFile = logDirectory.resolve(baseFileName + "_" + timestamp + ".log");
            
            // Move current file to backup
            Files.move(currentFile, backupFile, StandardCopyOption.REPLACE_EXISTING);
            
            // Clean up old backups
            cleanupOldBackups();
            
            // Create new file
            initializeFile();
            
        } catch (IOException e) {
            System.err.println("Failed to rotate log file: " + e.getMessage());
            // Try to reinitialize
            initializeFile();
        }
    }
    
    private void cleanupOldBackups() {
        try {
            var backups = Files.list(logDirectory)
                .filter(p -> p.getFileName().toString().startsWith(baseFileName + "_"))
                .filter(p -> p.getFileName().toString().endsWith(".log"))
                .sorted((a, b) -> {
                    try {
                        return Files.getLastModifiedTime(b).compareTo(Files.getLastModifiedTime(a));
                    } catch (IOException e) {
                        return 0;
                    }
                })
                .toList();
            
            // Delete old backups beyond maxBackupCount
            for (int i = maxBackupCount; i < backups.size(); i++) {
                try {
                    Files.delete(backups.get(i));
                } catch (IOException e) {
                    System.err.println("Failed to delete old backup: " + backups.get(i));
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to cleanup old backups: " + e.getMessage());
        }
    }
    
    /**
     * Get the current log file path.
     */
    public Path getCurrentFile() {
        return currentFile;
    }
    
    /**
     * Get the current file size.
     */
    public long getCurrentSize() {
        return currentSize;
    }
    
    /**
     * Get the log directory.
     */
    public Path getLogDirectory() {
        return logDirectory;
    }
    
    /**
     * Create a file appender with default settings in user's app data directory.
     */
    public static FileLogAppender createDefault() {
        Path userHome = Path.of(System.getProperty("user.home"));
        Path logDir = userHome.resolve(".animstudio").resolve("logs");
        return new FileLogAppender(logDir, "animstudio");
    }
    
    /**
     * Create a file appender for a specific session.
     */
    public static FileLogAppender createSession(Path logDirectory, String sessionId) {
        return new FileLogAppender(
            logDirectory,
            "session_" + sessionId,
            TextLogFormatter.verbose(),
            LogLevel.TRACE,
            50 * 1024 * 1024, // 50MB
            3
        );
    }
}
