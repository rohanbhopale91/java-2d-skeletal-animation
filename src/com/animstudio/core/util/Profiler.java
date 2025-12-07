package com.animstudio.core.util;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Simple profiler for performance monitoring.
 */
public class Profiler {
    
    private static final Logger LOGGER = Logger.getLogger(Profiler.class.getName());
    private static final Profiler instance = new Profiler();
    
    private final Map<String, TimingInfo> timings;
    private final ThreadLocal<Map<String, Long>> startTimes;
    private boolean enabled;
    
    public static Profiler getInstance() {
        return instance;
    }
    
    private Profiler() {
        timings = new HashMap<>();
        startTimes = ThreadLocal.withInitial(HashMap::new);
        enabled = false;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * Start timing a section.
     */
    public void begin(String name) {
        if (!enabled) return;
        startTimes.get().put(name, System.nanoTime());
    }
    
    /**
     * End timing a section and record results.
     */
    public void end(String name) {
        if (!enabled) return;
        
        Long startTime = startTimes.get().remove(name);
        if (startTime == null) return;
        
        long elapsed = System.nanoTime() - startTime;
        
        synchronized (timings) {
            TimingInfo info = timings.computeIfAbsent(name, k -> new TimingInfo(name));
            info.record(elapsed);
        }
    }
    
    /**
     * Time a runnable and return elapsed time in ms.
     */
    public double time(String name, Runnable task) {
        long start = System.nanoTime();
        task.run();
        long elapsed = System.nanoTime() - start;
        
        if (enabled) {
            synchronized (timings) {
                TimingInfo info = timings.computeIfAbsent(name, k -> new TimingInfo(name));
                info.record(elapsed);
            }
        }
        
        return elapsed / 1_000_000.0;
    }
    
    /**
     * Get timing info for a section.
     */
    public TimingInfo getTiming(String name) {
        synchronized (timings) {
            return timings.get(name);
        }
    }
    
    /**
     * Get all timings.
     */
    public Map<String, TimingInfo> getAllTimings() {
        synchronized (timings) {
            return new HashMap<>(timings);
        }
    }
    
    /**
     * Reset all timings.
     */
    public void reset() {
        synchronized (timings) {
            timings.clear();
        }
    }
    
    /**
     * Print profiler report to logger.
     */
    public void report() {
        StringBuilder sb = new StringBuilder("\n=== Profiler Report ===\n");
        sb.append(String.format("%-30s %10s %10s %10s %10s%n", 
            "Section", "Calls", "Total(ms)", "Avg(ms)", "Max(ms)"));
        sb.append("-".repeat(72)).append("\n");
        
        synchronized (timings) {
            timings.values().stream()
                .sorted((a, b) -> Long.compare(b.totalNanos, a.totalNanos))
                .forEach(info -> {
                    sb.append(String.format("%-30s %10d %10.2f %10.2f %10.2f%n",
                        info.name,
                        info.calls,
                        info.totalNanos / 1_000_000.0,
                        info.averageMs(),
                        info.maxNanos / 1_000_000.0));
                });
        }
        
        LOGGER.info(sb.toString());
    }
    
    /**
     * Timing information for a profiled section.
     */
    public static class TimingInfo {
        public final String name;
        public long calls;
        public long totalNanos;
        public long minNanos = Long.MAX_VALUE;
        public long maxNanos;
        
        public TimingInfo(String name) {
            this.name = name;
        }
        
        void record(long nanos) {
            calls++;
            totalNanos += nanos;
            minNanos = Math.min(minNanos, nanos);
            maxNanos = Math.max(maxNanos, nanos);
        }
        
        public double averageMs() {
            return calls > 0 ? (totalNanos / (double) calls) / 1_000_000.0 : 0;
        }
        
        public double totalMs() {
            return totalNanos / 1_000_000.0;
        }
    }
}
