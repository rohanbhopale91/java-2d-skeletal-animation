package com.animstudio.logging.diagnostics;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Health check for Java runtime environment.
 */
public class JavaRuntimeHealthCheck implements HealthCheck {
    
    private static final int MIN_JAVA_VERSION = 17;
    private static final int RECOMMENDED_JAVA_VERSION = 21;
    
    @Override
    public String getName() {
        return "Java Runtime";
    }
    
    @Override
    public String getCategory() {
        return "Runtime";
    }
    
    @Override
    public List<DiagnosticIssue> check() {
        List<DiagnosticIssue> issues = new ArrayList<>();
        
        // Check Java version
        String javaVersion = System.getProperty("java.version");
        String javaVendor = System.getProperty("java.vendor");
        String javaHome = System.getProperty("java.home");
        int majorVersion = getMajorVersion(javaVersion);
        
        Map<String, Object> details = Map.of(
            "javaVersion", javaVersion,
            "javaVendor", javaVendor,
            "javaHome", javaHome,
            "majorVersion", majorVersion
        );
        
        if (majorVersion < MIN_JAVA_VERSION) {
            issues.add(DiagnosticIssue.builder()
                .category("Runtime")
                .severity(DiagnosticSeverity.ERROR)
                .title("Unsupported Java Version")
                .description("Java " + majorVersion + " is not supported. Minimum: Java " + MIN_JAVA_VERSION)
                .recommendation("Upgrade to Java " + RECOMMENDED_JAVA_VERSION + " or later")
                .details(details)
                .build());
        } else if (majorVersion < RECOMMENDED_JAVA_VERSION) {
            issues.add(DiagnosticIssue.builder()
                .category("Runtime")
                .severity(DiagnosticSeverity.INFO)
                .title("Java Version Info")
                .description("Running Java " + majorVersion + ". Recommended: Java " + RECOMMENDED_JAVA_VERSION)
                .details(details)
                .build());
        } else {
            issues.add(DiagnosticIssue.builder()
                .category("Runtime")
                .severity(DiagnosticSeverity.INFO)
                .title("Java Runtime OK")
                .description("Java " + javaVersion + " (" + javaVendor + ")")
                .details(details)
                .build());
        }
        
        // Check available processors
        int processors = Runtime.getRuntime().availableProcessors();
        issues.add(DiagnosticIssue.builder()
            .category("Runtime")
            .severity(DiagnosticSeverity.INFO)
            .title("CPU Cores")
            .description("Available processors: " + processors)
            .details(Map.of("processors", processors))
            .build());
        
        return issues;
    }
    
    @Override
    public boolean runsPeriodically() {
        return false; // Only at startup
    }
    
    private int getMajorVersion(String version) {
        try {
            // Handle versions like "17.0.1", "21", "1.8.0_291"
            String[] parts = version.split("[._-]");
            int first = Integer.parseInt(parts[0]);
            if (first == 1 && parts.length > 1) {
                // Old format: 1.8.0 -> 8
                return Integer.parseInt(parts[1]);
            }
            return first;
        } catch (Exception e) {
            return 0;
        }
    }
}
