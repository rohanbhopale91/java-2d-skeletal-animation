package com.animstudio.logging.performance;

import com.animstudio.logging.Log;
import com.animstudio.logging.LogCategory;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Performance monitor that periodically reports performance stats.
 */
public class PerformanceMonitor {
    
    private static final PerformanceMonitor INSTANCE = new PerformanceMonitor();
    
    private final ScheduledExecutorService scheduler;
    private boolean running;
    private int reportIntervalSeconds = 60;
    
    private PerformanceMonitor() {
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "PerformanceMonitor");
            t.setDaemon(true);
            return t;
        });
        this.running = false;
    }
    
    public static PerformanceMonitor getInstance() {
        return INSTANCE;
    }
    
    /**
     * Start the performance monitor.
     */
    public void start() {
        if (running) return;
        running = true;
        
        scheduler.scheduleAtFixedRate(
            this::reportStats,
            reportIntervalSeconds,
            reportIntervalSeconds,
            TimeUnit.SECONDS
        );
        
        Log.info(LogCategory.PERFORMANCE, "PerformanceMonitor", "Started performance monitoring");
    }
    
    /**
     * Stop the performance monitor.
     */
    public void stop() {
        running = false;
        scheduler.shutdown();
    }
    
    /**
     * Set the report interval.
     */
    public void setReportIntervalSeconds(int seconds) {
        this.reportIntervalSeconds = seconds;
    }
    
    /**
     * Report current stats.
     */
    public void reportStats() {
        Map<String, PerfTimer.PerfStats> stats = PerfTimer.getAllStats();
        
        if (stats.isEmpty()) {
            return;
        }
        
        StringBuilder sb = new StringBuilder("Performance Stats:\n");
        
        for (Map.Entry<String, PerfTimer.PerfStats> entry : stats.entrySet()) {
            sb.append("  ").append(entry.getKey()).append(": ")
              .append(entry.getValue()).append("\n");
        }
        
        Log.debug(LogCategory.PERFORMANCE, "PerformanceMonitor", sb.toString().trim());
    }
    
    /**
     * Get a summary of all performance stats.
     */
    public String getSummary() {
        Map<String, PerfTimer.PerfStats> stats = PerfTimer.getAllStats();
        
        if (stats.isEmpty()) {
            return "No performance data recorded.";
        }
        
        StringBuilder sb = new StringBuilder("=== Performance Summary ===\n\n");
        
        // Sort by total time (descending)
        stats.entrySet().stream()
            .sorted((a, b) -> Double.compare(b.getValue().getTotalMs(), a.getValue().getTotalMs()))
            .forEach(entry -> {
                sb.append(String.format("%-40s %s\n", entry.getKey(), entry.getValue()));
            });
        
        return sb.toString();
    }
    
    /**
     * Check if monitoring is running.
     */
    public boolean isRunning() {
        return running;
    }
}
