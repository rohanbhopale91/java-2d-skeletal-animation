package com.animstudio.logging;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * JSON formatter for log events - useful for structured logging and log analysis tools.
 */
public class JsonLogFormatter implements LogFormatter {
    
    private static final DateTimeFormatter ISO_FORMAT = 
        DateTimeFormatter.ISO_INSTANT.withZone(ZoneId.systemDefault());
    
    private final boolean prettyPrint;
    private final boolean includeStackTrace;
    
    public JsonLogFormatter() {
        this(false, true);
    }
    
    public JsonLogFormatter(boolean prettyPrint, boolean includeStackTrace) {
        this.prettyPrint = prettyPrint;
        this.includeStackTrace = includeStackTrace;
    }
    
    @Override
    public String format(LogEvent event) {
        StringBuilder sb = new StringBuilder();
        String indent = prettyPrint ? "  " : "";
        String newline = prettyPrint ? "\n" : "";
        
        sb.append("{").append(newline);
        
        // Core fields
        appendField(sb, indent, "id", event.id(), true);
        appendField(sb, indent, "timestamp", ISO_FORMAT.format(event.timestamp()), true);
        appendField(sb, indent, "level", event.level().name(), true);
        appendField(sb, indent, "category", event.category().name(), true);
        appendField(sb, indent, "source", event.source(), true);
        appendField(sb, indent, "message", escapeJson(event.message()), true);
        appendField(sb, indent, "thread", event.threadName(), true);
        
        // Optional fields
        if (event.correlationId() != null) {
            appendField(sb, indent, "correlationId", event.correlationId(), true);
        }
        
        // Context
        if (event.hasContext()) {
            sb.append(indent).append("\"context\": {").append(newline);
            boolean first = true;
            for (Map.Entry<String, Object> entry : event.context().entrySet()) {
                if (!first) sb.append(",").append(newline);
                String contextIndent = prettyPrint ? "    " : "";
                sb.append(contextIndent).append("\"").append(escapeJson(entry.getKey())).append("\": ");
                appendValue(sb, entry.getValue());
                first = false;
            }
            sb.append(newline).append(indent).append("},").append(newline);
        }
        
        // Exception
        if (includeStackTrace && event.hasThrowable()) {
            appendField(sb, indent, "exception", formatException(event.throwable()), true);
        }
        
        // Remove trailing comma
        int lastComma = sb.lastIndexOf(",");
        if (lastComma > 0 && lastComma > sb.lastIndexOf("}")) {
            sb.deleteCharAt(lastComma);
        }
        
        sb.append("}");
        
        return sb.toString();
    }
    
    @Override
    public boolean includesStackTrace() {
        return includeStackTrace;
    }
    
    private void appendField(StringBuilder sb, String indent, String key, String value, boolean hasMore) {
        sb.append(indent)
          .append("\"").append(key).append("\": ")
          .append("\"").append(value != null ? escapeJson(value) : "").append("\"");
        if (hasMore) sb.append(",");
        if (prettyPrint) sb.append("\n");
    }
    
    private void appendValue(StringBuilder sb, Object value) {
        if (value == null) {
            sb.append("null");
        } else if (value instanceof Number) {
            sb.append(value);
        } else if (value instanceof Boolean) {
            sb.append(value);
        } else {
            sb.append("\"").append(escapeJson(value.toString())).append("\"");
        }
    }
    
    private String escapeJson(String text) {
        if (text == null) return "";
        
        StringBuilder sb = new StringBuilder();
        for (char c : text.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < ' ') {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.toString();
    }
    
    private String formatException(Throwable throwable) {
        StringBuilder sb = new StringBuilder();
        sb.append(throwable.getClass().getName());
        if (throwable.getMessage() != null) {
            sb.append(": ").append(throwable.getMessage());
        }
        for (StackTraceElement element : throwable.getStackTrace()) {
            sb.append("\\n\\tat ").append(element.toString());
        }
        if (throwable.getCause() != null) {
            sb.append("\\nCaused by: ").append(formatException(throwable.getCause()));
        }
        return sb.toString();
    }
    
    /**
     * Create a compact JSON formatter (single line, no stack traces).
     */
    public static JsonLogFormatter compact() {
        return new JsonLogFormatter(false, false);
    }
    
    /**
     * Create a pretty-printed JSON formatter.
     */
    public static JsonLogFormatter pretty() {
        return new JsonLogFormatter(true, true);
    }
}
