package com.animstudio.logging.notifications;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents a notification message.
 */
public record Notification(
    String id,
    NotificationType type,
    String title,
    String message,
    Instant timestamp,
    int durationMs,
    boolean dismissible,
    Runnable action,
    String actionLabel
) {
    
    public static class Builder {
        private String id;
        private NotificationType type = NotificationType.INFO;
        private String title = "";
        private String message = "";
        private Instant timestamp = Instant.now();
        private int durationMs = 5000; // 5 seconds default
        private boolean dismissible = true;
        private Runnable action;
        private String actionLabel;
        
        public Builder() {
            this.id = UUID.randomUUID().toString();
        }
        
        public Builder id(String id) {
            this.id = id;
            return this;
        }
        
        public Builder type(NotificationType type) {
            this.type = type;
            return this;
        }
        
        public Builder title(String title) {
            this.title = title;
            return this;
        }
        
        public Builder message(String message) {
            this.message = message;
            return this;
        }
        
        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }
        
        public Builder duration(int durationMs) {
            this.durationMs = durationMs;
            return this;
        }
        
        public Builder persistent() {
            this.durationMs = -1; // Never auto-dismiss
            return this;
        }
        
        public Builder dismissible(boolean dismissible) {
            this.dismissible = dismissible;
            return this;
        }
        
        public Builder action(String label, Runnable action) {
            this.actionLabel = label;
            this.action = action;
            return this;
        }
        
        public Notification build() {
            return new Notification(id, type, title, message, timestamp, 
                durationMs, dismissible, action, actionLabel);
        }
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Create a simple info notification.
     */
    public static Notification info(String message) {
        return builder().type(NotificationType.INFO).message(message).build();
    }
    
    /**
     * Create an info notification with title.
     */
    public static Notification info(String title, String message) {
        return builder().type(NotificationType.INFO).title(title).message(message).build();
    }
    
    /**
     * Create a success notification.
     */
    public static Notification success(String message) {
        return builder().type(NotificationType.SUCCESS).message(message).build();
    }
    
    /**
     * Create a success notification with title.
     */
    public static Notification success(String title, String message) {
        return builder().type(NotificationType.SUCCESS).title(title).message(message).build();
    }
    
    /**
     * Create a warning notification.
     */
    public static Notification warning(String message) {
        return builder().type(NotificationType.WARNING).message(message).duration(7000).build();
    }
    
    /**
     * Create a warning notification with title.
     */
    public static Notification warning(String title, String message) {
        return builder().type(NotificationType.WARNING).title(title).message(message).duration(7000).build();
    }
    
    /**
     * Create an error notification.
     */
    public static Notification error(String message) {
        return builder().type(NotificationType.ERROR).message(message).duration(10000).build();
    }
    
    /**
     * Create an error notification with title.
     */
    public static Notification error(String title, String message) {
        return builder().type(NotificationType.ERROR).title(title).message(message).duration(10000).build();
    }
    
    /**
     * Check if this notification has an action.
     */
    public boolean hasAction() {
        return action != null && actionLabel != null;
    }
    
    /**
     * Check if this notification should auto-dismiss.
     */
    public boolean shouldAutoDismiss() {
        return durationMs > 0;
    }
}
