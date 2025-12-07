package com.animstudio.logging.performance;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple performance timer for measuring code execution time.
 */
public class PerfTimer implements AutoCloseable {
    
    private static final Map<String, PerfStats> globalStats = new ConcurrentHashMap<>();
    
    private final String name;
    private final Instant startTime;
    private final boolean recordGlobal;
    private Duration elapsed;
    
    private PerfTimer(String name, boolean recordGlobal) {
        this.name = name;
        this.startTime = Instant.now();
        this.recordGlobal = recordGlobal;
    }
    
    /**
     * Start a new timer.
     */
    public static PerfTimer start(String name) {
        return new PerfTimer(name, true);
    }
    
    /**
     * Start a timer that doesn't record to global stats.
     */
    public static PerfTimer startLocal(String name) {
        return new PerfTimer(name, false);
    }
    
    /**
     * Stop the timer and return the elapsed time.
     */
    public Duration stop() {
        if (elapsed == null) {
            elapsed = Duration.between(startTime, Instant.now());
            if (recordGlobal) {
                globalStats.computeIfAbsent(name, k -> new PerfStats()).record(elapsed);
            }
        }
        return elapsed;
    }
    
    /**
     * Get elapsed time in milliseconds.
     */
    public long getElapsedMs() {
        return stop().toMillis();
    }
    
    /**
     * Get elapsed time in nanoseconds.
     */
    public long getElapsedNanos() {
        return stop().toNanos();
    }
    
    @Override
    public void close() {
        stop();
    }
    
    /**
     * Get the timer name.
     */
    public String getName() {
        return name;
    }
    
    /**
     * Get the start time.
     */
    public Instant getStartTime() {
        return startTime;
    }
    
    // ========== Global Stats ==========
    
    /**
     * Get stats for a named timer.
     */
    public static PerfStats getStats(String name) {
        return globalStats.get(name);
    }
    
    /**
     * Get all recorded stats.
     */
    public static Map<String, PerfStats> getAllStats() {
        return Map.copyOf(globalStats);
    }
    
    /**
     * Clear all recorded stats.
     */
    public static void clearStats() {
        globalStats.clear();
    }
    
    /**
     * Clear stats for a specific name.
     */
    public static void clearStats(String name) {
        globalStats.remove(name);
    }
    
    /**
     * Time a runnable and record stats.
     */
    public static Duration time(String name, Runnable runnable) {
        try (PerfTimer timer = start(name)) {
            runnable.run();
            return timer.stop();
        }
    }
    
    /**
     * Time a supplier and record stats.
     */
    public static <T> T time(String name, java.util.function.Supplier<T> supplier) {
        try (PerfTimer timer = start(name)) {
            return supplier.get();
        }
    }
    
    /**
     * Performance statistics for a timer.
     */
    public static class PerfStats {
        private long count;
        private long totalNanos;
        private long minNanos = Long.MAX_VALUE;
        private long maxNanos = Long.MIN_VALUE;
        
        synchronized void record(Duration duration) {
            long nanos = duration.toNanos();
            count++;
            totalNanos += nanos;
            minNanos = Math.min(minNanos, nanos);
            maxNanos = Math.max(maxNanos, nanos);
        }
        
        public synchronized long getCount() {
            return count;
        }
        
        public synchronized double getAverageMs() {
            return count > 0 ? (totalNanos / (double) count) / 1_000_000.0 : 0;
        }
        
        public synchronized double getMinMs() {
            return minNanos == Long.MAX_VALUE ? 0 : minNanos / 1_000_000.0;
        }
        
        public synchronized double getMaxMs() {
            return maxNanos == Long.MIN_VALUE ? 0 : maxNanos / 1_000_000.0;
        }
        
        public synchronized double getTotalMs() {
            return totalNanos / 1_000_000.0;
        }
        
        @Override
        public synchronized String toString() {
            return String.format("count=%d, avg=%.2fms, min=%.2fms, max=%.2fms, total=%.2fms",
                count, getAverageMs(), getMinMs(), getMaxMs(), getTotalMs());
        }
    }
}
