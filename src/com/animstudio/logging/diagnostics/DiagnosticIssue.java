package com.animstudio.logging.diagnostics;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/**
 * Represents a diagnostic issue found during system checks.
 */
public record DiagnosticIssue(
    String id,
    String category,
    DiagnosticSeverity severity,
    String title,
    String description,
    String recommendation,
    Instant timestamp,
    Map<String, Object> details
) {
    
    /**
     * Builder for DiagnosticIssue.
     */
    public static class Builder {
        private String id;
        private String category = "General";
        private DiagnosticSeverity severity = DiagnosticSeverity.INFO;
        private String title = "";
        private String description = "";
        private String recommendation = "";
        private Instant timestamp = Instant.now();
        private Map<String, Object> details = Map.of();
        
        public Builder id(String id) {
            this.id = id;
            return this;
        }
        
        public Builder category(String category) {
            this.category = category;
            return this;
        }
        
        public Builder severity(DiagnosticSeverity severity) {
            this.severity = severity;
            return this;
        }
        
        public Builder title(String title) {
            this.title = title;
            return this;
        }
        
        public Builder description(String description) {
            this.description = description;
            return this;
        }
        
        public Builder recommendation(String recommendation) {
            this.recommendation = recommendation;
            return this;
        }
        
        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }
        
        public Builder details(Map<String, Object> details) {
            this.details = details != null ? Map.copyOf(details) : Map.of();
            return this;
        }
        
        public DiagnosticIssue build() {
            if (id == null || id.isEmpty()) {
                id = category + "_" + System.currentTimeMillis();
            }
            return new DiagnosticIssue(id, category, severity, title, 
                description, recommendation, timestamp, details);
        }
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Create a simple info issue.
     */
    public static DiagnosticIssue info(String category, String title, String description) {
        return builder()
            .category(category)
            .severity(DiagnosticSeverity.INFO)
            .title(title)
            .description(description)
            .build();
    }
    
    /**
     * Create a warning issue.
     */
    public static DiagnosticIssue warning(String category, String title, String description, String recommendation) {
        return builder()
            .category(category)
            .severity(DiagnosticSeverity.WARNING)
            .title(title)
            .description(description)
            .recommendation(recommendation)
            .build();
    }
    
    /**
     * Create an error issue.
     */
    public static DiagnosticIssue error(String category, String title, String description, String recommendation) {
        return builder()
            .category(category)
            .severity(DiagnosticSeverity.ERROR)
            .title(title)
            .description(description)
            .recommendation(recommendation)
            .build();
    }
    
    /**
     * Create a critical issue.
     */
    public static DiagnosticIssue critical(String category, String title, String description, String recommendation) {
        return builder()
            .category(category)
            .severity(DiagnosticSeverity.CRITICAL)
            .title(title)
            .description(description)
            .recommendation(recommendation)
            .build();
    }
    
    /**
     * Get a detail value.
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getDetail(String key) {
        return Optional.ofNullable((T) details.get(key));
    }
    
    /**
     * Check if this issue has details.
     */
    public boolean hasDetails() {
        return details != null && !details.isEmpty();
    }
    
    /**
     * Format for display.
     */
    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();
        sb.append(severity.getIcon()).append(" ");
        sb.append("[").append(category).append("] ");
        sb.append(title);
        if (!description.isEmpty()) {
            sb.append("\n  ").append(description);
        }
        if (!recommendation.isEmpty()) {
            sb.append("\n  → ").append(recommendation);
        }
        return sb.toString();
    }
}
