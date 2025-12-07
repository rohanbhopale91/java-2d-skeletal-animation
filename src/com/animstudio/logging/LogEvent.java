package com.animstudio.logging;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Immutable log event capturing all relevant information about a log entry.
 */
public record LogEvent(
    String id,
    Instant timestamp,
    LogLevel level,
    LogCategory category,
    String source,
    String message,
    Throwable throwable,
    String threadName,
    String correlationId,
    Map<String, Object> context
) {
    
    /**
     * Builder for creating LogEvent instances.
     */
    public static class Builder {
        private String id;
        private Instant timestamp;
        private LogLevel level = LogLevel.INFO;
        private LogCategory category = LogCategory.GENERAL;
        private String source = "";
        private String message = "";
        private Throwable throwable;
        private String threadName;
        private String correlationId;
        private Map<String, Object> context = Map.of();
        
        public Builder() {
            this.id = UUID.randomUUID().toString();
            this.timestamp = Instant.now();
            this.threadName = Thread.currentThread().getName();
        }
        
        public Builder id(String id) {
            this.id = id;
            return this;
        }
        
        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }
        
        public Builder level(LogLevel level) {
            this.level = level;
            return this;
        }
        
        public Builder category(LogCategory category) {
            this.category = category;
            return this;
        }
        
        public Builder source(String source) {
            this.source = source;
            return this;
        }
        
        public Builder message(String message) {
            this.message = message;
            return this;
        }
        
        public Builder throwable(Throwable throwable) {
            this.throwable = throwable;
            return this;
        }
        
        public Builder threadName(String threadName) {
            this.threadName = threadName;
            return this;
        }
        
        public Builder correlationId(String correlationId) {
            this.correlationId = correlationId;
            return this;
        }
        
        public Builder context(Map<String, Object> context) {
            this.context = context != null ? Map.copyOf(context) : Map.of();
            return this;
        }
        
        public LogEvent build() {
            return new LogEvent(
                id, timestamp, level, category, source, message,
                throwable, threadName, correlationId, context
            );
        }
    }
    
    /**
     * Create a new builder.
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Convenience method to create a simple log event.
     */
    public static LogEvent of(LogLevel level, String source, String message) {
        return builder()
            .level(level)
            .source(source)
            .message(message)
            .build();
    }
    
    /**
     * Convenience method to create a log event with exception.
     */
    public static LogEvent of(LogLevel level, String source, String message, Throwable throwable) {
        return builder()
            .level(level)
            .source(source)
            .message(message)
            .throwable(throwable)
            .build();
    }
    
    /**
     * Check if this event has an associated exception.
     */
    public boolean hasThrowable() {
        return throwable != null;
    }
    
    /**
     * Check if this event has context data.
     */
    public boolean hasContext() {
        return context != null && !context.isEmpty();
    }
    
    /**
     * Get a context value by key.
     */
    @SuppressWarnings("unchecked")
    public <T> T getContextValue(String key) {
        return context != null ? (T) context.get(key) : null;
    }
    
    /**
     * Create a copy with a different level.
     */
    public LogEvent withLevel(LogLevel newLevel) {
        return new LogEvent(id, timestamp, newLevel, category, source, message,
            throwable, threadName, correlationId, context);
    }
    
    /**
     * Create a copy with additional context.
     */
    public LogEvent withContext(Map<String, Object> additionalContext) {
        var newContext = new java.util.HashMap<>(this.context);
        newContext.putAll(additionalContext);
        return new LogEvent(id, timestamp, level, category, source, message,
            throwable, threadName, correlationId, Map.copyOf(newContext));
    }
}
