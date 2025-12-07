package com.animstudio.logging.notifications;

/**
 * Types of notifications for the toast system.
 */
public enum NotificationType {
    INFO("Info", "#3498db", "ℹ️"),
    SUCCESS("Success", "#27ae60", "✅"),
    WARNING("Warning", "#f39c12", "⚠️"),
    ERROR("Error", "#e74c3c", "❌");
    
    private final String label;
    private final String color;
    private final String icon;
    
    NotificationType(String label, String color, String icon) {
        this.label = label;
        this.color = color;
        this.icon = icon;
    }
    
    public String getLabel() {
        return label;
    }
    
    public String getColor() {
        return color;
    }
    
    public String getIcon() {
        return icon;
    }
}
