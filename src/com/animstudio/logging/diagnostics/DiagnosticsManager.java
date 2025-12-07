package com.animstudio.logging.diagnostics;

import com.animstudio.logging.Log;
import com.animstudio.logging.LogCategory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Central manager for running diagnostics and health checks.
 */
public class DiagnosticsManager {
    
    private static final DiagnosticsManager INSTANCE = new DiagnosticsManager();
    
    private final List<HealthCheck> healthChecks;
    private final List<DiagnosticIssue> currentIssues;
    private final List<Consumer<List<DiagnosticIssue>>> listeners;
    private final ScheduledExecutorService scheduler;
    
    private Instant lastCheckTime;
    private boolean initialized;
    
    private DiagnosticsManager() {
        this.healthChecks = new CopyOnWriteArrayList<>();
        this.currentIssues = new CopyOnWriteArrayList<>();
        this.listeners = new CopyOnWriteArrayList<>();
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "DiagnosticsManager-Scheduler");
            t.setDaemon(true);
            return t;
        });
        this.initialized = false;
        
        // Register default health checks
        registerDefaultChecks();
    }
    
    public static DiagnosticsManager getInstance() {
        return INSTANCE;
    }
    
    private void registerDefaultChecks() {
        addHealthCheck(new MemoryHealthCheck());
        addHealthCheck(new FileSystemHealthCheck());
        addHealthCheck(new JavaRuntimeHealthCheck());
    }
    
    /**
     * Initialize and run startup diagnostics.
     */
    public void initialize() {
        if (initialized) return;
        
        Log.info(LogCategory.DIAGNOSTICS, "DiagnosticsManager", "Running startup diagnostics...");
        
        // Run all startup checks
        runAllChecks();
        
        // Schedule periodic checks
        schedulePeriodicChecks();
        
        initialized = true;
        Log.info(LogCategory.DIAGNOSTICS, "DiagnosticsManager", 
            "Diagnostics initialized. Found " + currentIssues.size() + " items.");
    }
    
    /**
     * Add a health check.
     */
    public void addHealthCheck(HealthCheck check) {
        healthChecks.add(check);
    }
    
    /**
     * Remove a health check by name.
     */
    public void removeHealthCheck(String name) {
        healthChecks.removeIf(c -> c.getName().equals(name));
    }
    
    /**
     * Run all health checks.
     */
    public List<DiagnosticIssue> runAllChecks() {
        currentIssues.clear();
        
        for (HealthCheck check : healthChecks) {
            try {
                List<DiagnosticIssue> issues = check.check();
                currentIssues.addAll(issues);
                
                // Log any errors or warnings
                for (DiagnosticIssue issue : issues) {
                    if (issue.severity() == DiagnosticSeverity.ERROR || 
                        issue.severity() == DiagnosticSeverity.CRITICAL) {
                        Log.warn(LogCategory.DIAGNOSTICS, check.getName(), issue.toDisplayString());
                    }
                }
            } catch (Exception e) {
                Log.error(LogCategory.DIAGNOSTICS, check.getName(), 
                    "Health check failed: " + e.getMessage(), e);
                currentIssues.add(DiagnosticIssue.error(
                    "Diagnostics",
                    "Health Check Failed",
                    "Check '" + check.getName() + "' threw exception: " + e.getMessage(),
                    "Report this issue to developers"
                ));
            }
        }
        
        lastCheckTime = Instant.now();
        notifyListeners();
        
        return List.copyOf(currentIssues);
    }
    
    /**
     * Run a specific check by name.
     */
    public List<DiagnosticIssue> runCheck(String name) {
        return healthChecks.stream()
            .filter(c -> c.getName().equals(name))
            .findFirst()
            .map(HealthCheck::check)
            .orElse(Collections.emptyList());
    }
    
    /**
     * Get all current issues.
     */
    public List<DiagnosticIssue> getCurrentIssues() {
        return List.copyOf(currentIssues);
    }
    
    /**
     * Get issues by severity.
     */
    public List<DiagnosticIssue> getIssuesBySeverity(DiagnosticSeverity severity) {
        return currentIssues.stream()
            .filter(i -> i.severity() == severity)
            .toList();
    }
    
    /**
     * Get issues by category.
     */
    public List<DiagnosticIssue> getIssuesByCategory(String category) {
        return currentIssues.stream()
            .filter(i -> i.category().equals(category))
            .toList();
    }
    
    /**
     * Check if there are critical issues.
     */
    public boolean hasCriticalIssues() {
        return currentIssues.stream()
            .anyMatch(i -> i.severity() == DiagnosticSeverity.CRITICAL);
    }
    
    /**
     * Check if there are errors.
     */
    public boolean hasErrors() {
        return currentIssues.stream()
            .anyMatch(i -> i.severity() == DiagnosticSeverity.ERROR || 
                          i.severity() == DiagnosticSeverity.CRITICAL);
    }
    
    /**
     * Add a listener for diagnostic changes.
     */
    public void addListener(Consumer<List<DiagnosticIssue>> listener) {
        listeners.add(listener);
    }
    
    /**
     * Remove a listener.
     */
    public void removeListener(Consumer<List<DiagnosticIssue>> listener) {
        listeners.remove(listener);
    }
    
    /**
     * Get the last check time.
     */
    public Instant getLastCheckTime() {
        return lastCheckTime;
    }
    
    /**
     * Get all registered health checks.
     */
    public List<HealthCheck> getHealthChecks() {
        return List.copyOf(healthChecks);
    }
    
    /**
     * Shutdown the diagnostics manager.
     */
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    private void schedulePeriodicChecks() {
        for (HealthCheck check : healthChecks) {
            if (check.runsPeriodically()) {
                scheduler.scheduleAtFixedRate(
                    () -> {
                        try {
                            List<DiagnosticIssue> issues = check.check();
                            updateIssuesForCheck(check.getCategory(), issues);
                        } catch (Exception e) {
                            Log.error(LogCategory.DIAGNOSTICS, check.getName(),
                                "Periodic check failed: " + e.getMessage(), e);
                        }
                    },
                    check.getIntervalSeconds(),
                    check.getIntervalSeconds(),
                    TimeUnit.SECONDS
                );
            }
        }
    }
    
    private void updateIssuesForCheck(String category, List<DiagnosticIssue> newIssues) {
        // Remove old issues for this category
        currentIssues.removeIf(i -> i.category().equals(category));
        // Add new issues
        currentIssues.addAll(newIssues);
        notifyListeners();
    }
    
    private void notifyListeners() {
        List<DiagnosticIssue> snapshot = List.copyOf(currentIssues);
        for (Consumer<List<DiagnosticIssue>> listener : listeners) {
            try {
                listener.accept(snapshot);
            } catch (Exception e) {
                Log.error(LogCategory.DIAGNOSTICS, "DiagnosticsManager",
                    "Listener notification failed: " + e.getMessage(), e);
            }
        }
    }
    
    /**
     * Generate a summary report.
     */
    public String generateReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Diagnostics Report ===\n");
        sb.append("Time: ").append(lastCheckTime).append("\n\n");
        
        int criticalCount = (int) currentIssues.stream().filter(i -> i.severity() == DiagnosticSeverity.CRITICAL).count();
        int errorCount = (int) currentIssues.stream().filter(i -> i.severity() == DiagnosticSeverity.ERROR).count();
        int warningCount = (int) currentIssues.stream().filter(i -> i.severity() == DiagnosticSeverity.WARNING).count();
        int infoCount = (int) currentIssues.stream().filter(i -> i.severity() == DiagnosticSeverity.INFO).count();
        
        sb.append("Summary: ")
          .append(criticalCount).append(" critical, ")
          .append(errorCount).append(" errors, ")
          .append(warningCount).append(" warnings, ")
          .append(infoCount).append(" info\n\n");
        
        // Group by severity
        for (DiagnosticSeverity severity : DiagnosticSeverity.values()) {
            var issues = getIssuesBySeverity(severity);
            if (!issues.isEmpty()) {
                sb.append("--- ").append(severity.getLabel()).append(" ---\n");
                for (DiagnosticIssue issue : issues) {
                    sb.append(issue.toDisplayString()).append("\n\n");
                }
            }
        }
        
        return sb.toString();
    }
}
