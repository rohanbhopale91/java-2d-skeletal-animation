package com.animstudio.logging.notifications;

import javafx.application.Platform;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Central manager for notifications.
 */
public class NotificationManager {
    
    private static final NotificationManager INSTANCE = new NotificationManager();
    
    private final Queue<Notification> notificationQueue;
    private final CopyOnWriteArrayList<Consumer<Notification>> listeners;
    private final CopyOnWriteArrayList<Consumer<String>> dismissListeners;
    private int maxQueueSize = 10;
    private boolean enabled = true;
    
    private NotificationManager() {
        this.notificationQueue = new LinkedList<>();
        this.listeners = new CopyOnWriteArrayList<>();
        this.dismissListeners = new CopyOnWriteArrayList<>();
    }
    
    public static NotificationManager getInstance() {
        return INSTANCE;
    }
    
    /**
     * Show a notification.
     */
    public void show(Notification notification) {
        if (!enabled) return;
        
        // Add to queue, removing oldest if full
        synchronized (notificationQueue) {
            while (notificationQueue.size() >= maxQueueSize) {
                notificationQueue.poll();
            }
            notificationQueue.offer(notification);
        }
        
        // Notify listeners on FX thread
        if (Platform.isFxApplicationThread()) {
            notifyListeners(notification);
        } else {
            Platform.runLater(() -> notifyListeners(notification));
        }
    }
    
    /**
     * Dismiss a notification by ID.
     */
    public void dismiss(String notificationId) {
        synchronized (notificationQueue) {
            notificationQueue.removeIf(n -> n.id().equals(notificationId));
        }
        
        // Notify dismiss listeners
        if (Platform.isFxApplicationThread()) {
            notifyDismissListeners(notificationId);
        } else {
            Platform.runLater(() -> notifyDismissListeners(notificationId));
        }
    }
    
    /**
     * Dismiss all notifications.
     */
    public void dismissAll() {
        synchronized (notificationQueue) {
            notificationQueue.clear();
        }
    }
    
    /**
     * Add a listener for new notifications.
     */
    public void addListener(Consumer<Notification> listener) {
        listeners.add(listener);
    }
    
    /**
     * Remove a notification listener.
     */
    public void removeListener(Consumer<Notification> listener) {
        listeners.remove(listener);
    }
    
    /**
     * Add a listener for dismiss events.
     */
    public void addDismissListener(Consumer<String> listener) {
        dismissListeners.add(listener);
    }
    
    /**
     * Remove a dismiss listener.
     */
    public void removeDismissListener(Consumer<String> listener) {
        dismissListeners.remove(listener);
    }
    
    /**
     * Enable or disable notifications.
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    /**
     * Check if notifications are enabled.
     */
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * Set the max queue size.
     */
    public void setMaxQueueSize(int size) {
        this.maxQueueSize = size;
    }
    
    /**
     * Get the max queue size.
     */
    public int getMaxQueueSize() {
        return maxQueueSize;
    }
    
    private void notifyListeners(Notification notification) {
        for (Consumer<Notification> listener : listeners) {
            try {
                listener.accept(notification);
            } catch (Exception e) {
                System.err.println("Error in notification listener: " + e.getMessage());
            }
        }
    }
    
    private void notifyDismissListeners(String notificationId) {
        for (Consumer<String> listener : dismissListeners) {
            try {
                listener.accept(notificationId);
            } catch (Exception e) {
                System.err.println("Error in dismiss listener: " + e.getMessage());
            }
        }
    }
    
    // ========== Convenience Methods ==========
    
    public static void info(String message) {
        getInstance().show(Notification.info(message));
    }
    
    public static void info(String title, String message) {
        getInstance().show(Notification.info(title, message));
    }
    
    public static void success(String message) {
        getInstance().show(Notification.success(message));
    }
    
    public static void success(String title, String message) {
        getInstance().show(Notification.success(title, message));
    }
    
    public static void warning(String message) {
        getInstance().show(Notification.warning(message));
    }
    
    public static void warning(String title, String message) {
        getInstance().show(Notification.warning(title, message));
    }
    
    public static void error(String message) {
        getInstance().show(Notification.error(message));
    }
    
    public static void error(String title, String message) {
        getInstance().show(Notification.error(title, message));
    }
}
