package com.animstudio.logging.diagnostics;

/**
 * Severity levels for diagnostic issues.
 */
public enum DiagnosticSeverity {
    INFO("Info", "ℹ️"),
    WARNING("Warning", "⚠️"),
    ERROR("Error", "❌"),
    CRITICAL("Critical", "🔴");
    
    private final String label;
    private final String icon;
    
    DiagnosticSeverity(String label, String icon) {
        this.label = label;
        this.icon = icon;
    }
    
    public String getLabel() {
        return label;
    }
    
    public String getIcon() {
        return icon;
    }
}
