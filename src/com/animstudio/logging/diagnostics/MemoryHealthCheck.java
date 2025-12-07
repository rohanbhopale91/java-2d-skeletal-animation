package com.animstudio.logging.diagnostics;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Health check for memory usage.
 */
public class MemoryHealthCheck implements HealthCheck {
    
    private static final double WARNING_THRESHOLD = 0.75; // 75%
    private static final double ERROR_THRESHOLD = 0.90;   // 90%
    
    @Override
    public String getName() {
        return "Memory Usage";
    }
    
    @Override
    public String getCategory() {
        return "System";
    }
    
    @Override
    public List<DiagnosticIssue> check() {
        List<DiagnosticIssue> issues = new ArrayList<>();
        
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        
        double usagePercent = (double) usedMemory / maxMemory;
        
        Map<String, Object> details = Map.of(
            "usedMemory", usedMemory,
            "maxMemory", maxMemory,
            "usagePercent", usagePercent
        );
        
        if (usagePercent >= ERROR_THRESHOLD) {
            issues.add(DiagnosticIssue.builder()
                .category("Memory")
                .severity(DiagnosticSeverity.ERROR)
                .title("Critical Memory Usage")
                .description(String.format("Memory usage is at %.1f%% (%s / %s)", 
                    usagePercent * 100, formatBytes(usedMemory), formatBytes(maxMemory)))
                .recommendation("Close unused projects or increase heap size with -Xmx")
                .details(details)
                .build());
        } else if (usagePercent >= WARNING_THRESHOLD) {
            issues.add(DiagnosticIssue.builder()
                .category("Memory")
                .severity(DiagnosticSeverity.WARNING)
                .title("High Memory Usage")
                .description(String.format("Memory usage is at %.1f%% (%s / %s)", 
                    usagePercent * 100, formatBytes(usedMemory), formatBytes(maxMemory)))
                .recommendation("Consider saving work and restarting the application")
                .details(details)
                .build());
        } else {
            issues.add(DiagnosticIssue.builder()
                .category("Memory")
                .severity(DiagnosticSeverity.INFO)
                .title("Memory Status OK")
                .description(String.format("Memory usage: %.1f%% (%s / %s)", 
                    usagePercent * 100, formatBytes(usedMemory), formatBytes(maxMemory)))
                .details(details)
                .build());
        }
        
        return issues;
    }
    
    @Override
    public boolean runsPeriodically() {
        return true;
    }
    
    @Override
    public int getIntervalSeconds() {
        return 30;
    }
    
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }
}
