package com.animstudio.logging;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Human-readable text formatter for log events.
 * Format: [timestamp] [LEVEL] [category] source - message
 */
public class TextLogFormatter implements LogFormatter {
    
    private static final DateTimeFormatter TIMESTAMP_FORMAT = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
            .withZone(ZoneId.systemDefault());
    
    private final boolean includeThread;
    private final boolean includeCorrelationId;
    private final boolean includeContext;
    private final boolean includeStackTrace;
    
    public TextLogFormatter() {
        this(true, false, false, true);
    }
    
    public TextLogFormatter(boolean includeThread, boolean includeCorrelationId, 
                           boolean includeContext, boolean includeStackTrace) {
        this.includeThread = includeThread;
        this.includeCorrelationId = includeCorrelationId;
        this.includeContext = includeContext;
        this.includeStackTrace = includeStackTrace;
    }
    
    @Override
    public String format(LogEvent event) {
        StringBuilder sb = new StringBuilder();
        
        // Timestamp
        sb.append("[").append(TIMESTAMP_FORMAT.format(event.timestamp())).append("] ");
        
        // Level - padded to 5 chars for alignment
        sb.append("[").append(String.format("%-5s", event.level().getLabel())).append("] ");
        
        // Category
        sb.append("[").append(event.category().getDisplayName()).append("] ");
        
        // Thread (optional)
        if (includeThread && event.threadName() != null) {
            sb.append("[").append(event.threadName()).append("] ");
        }
        
        // Source
        if (event.source() != null && !event.source().isEmpty()) {
            sb.append(event.source()).append(" - ");
        }
        
        // Message
        sb.append(event.message());
        
        // Correlation ID (optional)
        if (includeCorrelationId && event.correlationId() != null) {
            sb.append(" [corr:").append(event.correlationId()).append("]");
        }
        
        // Context (optional)
        if (includeContext && event.hasContext()) {
            sb.append(" {");
            boolean first = true;
            for (var entry : event.context().entrySet()) {
                if (!first) sb.append(", ");
                sb.append(entry.getKey()).append("=").append(entry.getValue());
                first = false;
            }
            sb.append("}");
        }
        
        // Stack trace (optional)
        if (includeStackTrace && event.hasThrowable()) {
            sb.append("\n").append(formatThrowable(event.throwable()));
        }
        
        return sb.toString();
    }
    
    @Override
    public boolean includesStackTrace() {
        return includeStackTrace;
    }
    
    private String formatThrowable(Throwable throwable) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        return sw.toString().trim();
    }
    
    /**
     * Create a compact formatter (no thread, no correlation, no context).
     */
    public static TextLogFormatter compact() {
        return new TextLogFormatter(false, false, false, true);
    }
    
    /**
     * Create a verbose formatter (all fields included).
     */
    public static TextLogFormatter verbose() {
        return new TextLogFormatter(true, true, true, true);
    }
    
    /**
     * Create a formatter without stack traces (for UI display).
     */
    public static TextLogFormatter noStackTrace() {
        return new TextLogFormatter(false, false, false, false);
    }
}
