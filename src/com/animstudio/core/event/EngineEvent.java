package com.animstudio.core.event;

/**
 * Base class for all engine events.
 */
public abstract class EngineEvent {
    
    private final Object source;
    private final long timestamp;
    private boolean consumed;
    
    public EngineEvent(Object source) {
        this.source = source;
        this.timestamp = System.currentTimeMillis();
        this.consumed = false;
    }
    
    public Object getSource() { return source; }
    public long getTimestamp() { return timestamp; }
    
    public boolean isConsumed() { return consumed; }
    public void consume() { this.consumed = true; }
    
    /**
     * Get the event type name for logging/debugging.
     */
    public String getEventType() {
        return getClass().getSimpleName();
    }
}
