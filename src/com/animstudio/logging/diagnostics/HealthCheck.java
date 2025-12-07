package com.animstudio.logging.diagnostics;

import java.util.List;

/**
 * Interface for diagnostic health checks.
 */
public interface HealthCheck {
    
    /**
     * Get the name of this health check.
     */
    String getName();
    
    /**
     * Get the category of this health check.
     */
    String getCategory();
    
    /**
     * Run the health check and return any issues found.
     */
    List<DiagnosticIssue> check();
    
    /**
     * Whether this check should run at startup.
     */
    default boolean runsAtStartup() {
        return true;
    }
    
    /**
     * Whether this check should run periodically.
     */
    default boolean runsPeriodically() {
        return false;
    }
    
    /**
     * Get the interval in seconds for periodic checks.
     */
    default int getIntervalSeconds() {
        return 60;
    }
}
