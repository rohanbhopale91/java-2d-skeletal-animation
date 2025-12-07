package com.animstudio.logging.diagnostics;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Health check for file system (log directory, temp space).
 */
public class FileSystemHealthCheck implements HealthCheck {
    
    private static final long MIN_FREE_SPACE_MB = 100; // 100 MB minimum
    private static final long WARNING_FREE_SPACE_MB = 500; // 500 MB warning
    
    private final Path logDirectory;
    private final Path tempDirectory;
    
    public FileSystemHealthCheck() {
        Path userHome = Path.of(System.getProperty("user.home"));
        this.logDirectory = userHome.resolve(".animstudio").resolve("logs");
        this.tempDirectory = Path.of(System.getProperty("java.io.tmpdir"));
    }
    
    public FileSystemHealthCheck(Path logDirectory, Path tempDirectory) {
        this.logDirectory = logDirectory;
        this.tempDirectory = tempDirectory;
    }
    
    @Override
    public String getName() {
        return "File System";
    }
    
    @Override
    public String getCategory() {
        return "System";
    }
    
    @Override
    public List<DiagnosticIssue> check() {
        List<DiagnosticIssue> issues = new ArrayList<>();
        
        // Check log directory
        checkDirectory(issues, logDirectory, "Log Directory");
        
        // Check temp directory
        checkDirectory(issues, tempDirectory, "Temp Directory");
        
        // Check disk space on log directory root
        checkDiskSpace(issues, logDirectory);
        
        return issues;
    }
    
    private void checkDirectory(List<DiagnosticIssue> issues, Path directory, String name) {
        if (!Files.exists(directory)) {
            try {
                Files.createDirectories(directory);
                issues.add(DiagnosticIssue.info(
                    "FileSystem",
                    name + " Created",
                    "Directory created: " + directory
                ));
            } catch (Exception e) {
                issues.add(DiagnosticIssue.error(
                    "FileSystem",
                    "Cannot Create " + name,
                    "Failed to create directory: " + directory + " - " + e.getMessage(),
                    "Check file permissions and disk space"
                ));
            }
        } else if (!Files.isWritable(directory)) {
            issues.add(DiagnosticIssue.error(
                "FileSystem",
                name + " Not Writable",
                "Directory is not writable: " + directory,
                "Check file permissions"
            ));
        }
    }
    
    private void checkDiskSpace(List<DiagnosticIssue> issues, Path directory) {
        try {
            var store = Files.getFileStore(directory.getRoot() != null ? directory.getRoot() : directory);
            long freeSpace = store.getUsableSpace();
            long freeSpaceMB = freeSpace / (1024 * 1024);
            
            Map<String, Object> details = Map.of(
                "freeSpace", freeSpace,
                "freeSpaceMB", freeSpaceMB,
                "path", directory.toString()
            );
            
            if (freeSpaceMB < MIN_FREE_SPACE_MB) {
                issues.add(DiagnosticIssue.builder()
                    .category("FileSystem")
                    .severity(DiagnosticSeverity.CRITICAL)
                    .title("Critical Disk Space")
                    .description(String.format("Only %d MB free on disk", freeSpaceMB))
                    .recommendation("Free up disk space immediately")
                    .details(details)
                    .build());
            } else if (freeSpaceMB < WARNING_FREE_SPACE_MB) {
                issues.add(DiagnosticIssue.builder()
                    .category("FileSystem")
                    .severity(DiagnosticSeverity.WARNING)
                    .title("Low Disk Space")
                    .description(String.format("Only %d MB free on disk", freeSpaceMB))
                    .recommendation("Consider freeing up disk space")
                    .details(details)
                    .build());
            }
        } catch (Exception e) {
            issues.add(DiagnosticIssue.warning(
                "FileSystem",
                "Cannot Check Disk Space",
                "Failed to check disk space: " + e.getMessage(),
                "Manually verify disk space"
            ));
        }
    }
    
    @Override
    public boolean runsPeriodically() {
        return true;
    }
    
    @Override
    public int getIntervalSeconds() {
        return 300; // 5 minutes
    }
}
