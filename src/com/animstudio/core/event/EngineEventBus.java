package com.animstudio.core.event;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Publish/subscribe event bus for decoupling components.
 * Thread-safe implementation for use across editor and engine.
 */
public class EngineEventBus {
    
    private static EngineEventBus instance;
    
    private final Map<Class<?>, List<Consumer<?>>> listeners;
    private final Map<Class<?>, List<Consumer<?>>> onceListeners;
    private final List<Consumer<EngineEvent>> globalListeners;
    private boolean logging;
    
    public EngineEventBus() {
        this.listeners = new ConcurrentHashMap<>();
        this.onceListeners = new ConcurrentHashMap<>();
        this.globalListeners = new CopyOnWriteArrayList<>();
        this.logging = false;
    }
    
    /**
     * Get the global singleton instance.
     */
    public static synchronized EngineEventBus getInstance() {
        if (instance == null) {
            instance = new EngineEventBus();
        }
        return instance;
    }
    
    /**
     * Subscribe to events of a specific type.
     */
    public <T extends EngineEvent> void subscribe(Class<T> eventType, Consumer<T> handler) {
        listeners.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>()).add(handler);
    }
    
    /**
     * Subscribe to an event type, but only receive one event then auto-unsubscribe.
     */
    public <T extends EngineEvent> void subscribeOnce(Class<T> eventType, Consumer<T> handler) {
        onceListeners.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>()).add(handler);
    }
    
    /**
     * Subscribe to ALL events (for logging, debugging, etc.).
     */
    public void subscribeAll(Consumer<EngineEvent> handler) {
        globalListeners.add(handler);
    }
    
    /**
     * Unsubscribe from events of a specific type.
     */
    public <T extends EngineEvent> void unsubscribe(Class<T> eventType, Consumer<T> handler) {
        List<Consumer<?>> list = listeners.get(eventType);
        if (list != null) {
            list.remove(handler);
        }
    }
    
    /**
     * Unsubscribe from all events.
     */
    public void unsubscribeAll(Consumer<EngineEvent> handler) {
        globalListeners.remove(handler);
    }
    
    /**
     * Publish an event to all subscribers.
     */
    @SuppressWarnings("unchecked")
    public <T extends EngineEvent> void publish(T event) {
        if (logging) {
            System.out.println("[EventBus] " + event.getEventType() + " from " + event.getSource());
        }
        
        // Notify global listeners
        for (Consumer<EngineEvent> handler : globalListeners) {
            try {
                handler.accept(event);
            } catch (Exception e) {
                System.err.println("[EventBus] Error in global handler: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        Class<?> eventClass = event.getClass();
        
        // Notify type-specific listeners
        List<Consumer<?>> handlers = listeners.get(eventClass);
        if (handlers != null) {
            for (Consumer<?> handler : handlers) {
                if (event.isConsumed()) break;
                try {
                    ((Consumer<T>) handler).accept(event);
                } catch (Exception e) {
                    System.err.println("[EventBus] Error in handler: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
        
        // Notify one-time listeners
        List<Consumer<?>> onceHandlers = onceListeners.remove(eventClass);
        if (onceHandlers != null) {
            for (Consumer<?> handler : onceHandlers) {
                if (event.isConsumed()) break;
                try {
                    ((Consumer<T>) handler).accept(event);
                } catch (Exception e) {
                    System.err.println("[EventBus] Error in once-handler: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
        
        // Also notify handlers of parent classes
        Class<?> superClass = eventClass.getSuperclass();
        while (superClass != null && EngineEvent.class.isAssignableFrom(superClass)) {
            List<Consumer<?>> superHandlers = listeners.get(superClass);
            if (superHandlers != null) {
                for (Consumer<?> handler : superHandlers) {
                    if (event.isConsumed()) break;
                    try {
                        ((Consumer<T>) handler).accept(event);
                    } catch (Exception e) {
                        System.err.println("[EventBus] Error in super handler: " + e.getMessage());
                    }
                }
            }
            superClass = superClass.getSuperclass();
        }
    }
    
    /**
     * Clear all subscribers.
     */
    public void clear() {
        listeners.clear();
        onceListeners.clear();
        globalListeners.clear();
    }
    
    /**
     * Enable/disable event logging.
     */
    public void setLogging(boolean enabled) {
        this.logging = enabled;
    }
    
    /**
     * Get subscriber count for a specific event type.
     */
    public int getSubscriberCount(Class<? extends EngineEvent> eventType) {
        List<Consumer<?>> list = listeners.get(eventType);
        return list != null ? list.size() : 0;
    }
}
