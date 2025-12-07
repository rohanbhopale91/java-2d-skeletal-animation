package com.animstudio.core.animation;

import java.util.HashMap;
import java.util.Map;

/**
 * An event that can be triggered at a specific time during animation playback.
 * Used for sound cues, particle effects, callbacks, etc.
 */
public class AnimationEvent {
    
    private String name;
    private double time;
    private String stringValue;
    private int intValue;
    private float floatValue;
    private final Map<String, Object> customData;
    
    public AnimationEvent(String name, double time) {
        this.name = name;
        this.time = time;
        this.customData = new HashMap<>();
    }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public double getTime() { return time; }
    public void setTime(double time) { this.time = time; }
    
    public String getStringValue() { return stringValue; }
    public void setStringValue(String value) { this.stringValue = value; }
    
    public int getIntValue() { return intValue; }
    public void setIntValue(int value) { this.intValue = value; }
    
    public float getFloatValue() { return floatValue; }
    public void setFloatValue(float value) { this.floatValue = value; }
    
    public void setCustomData(String key, Object value) {
        customData.put(key, value);
    }
    
    public Object getCustomData(String key) {
        return customData.get(key);
    }
    
    public Map<String, Object> getAllCustomData() {
        return new HashMap<>(customData);
    }
    
    @Override
    public String toString() {
        return String.format("AnimationEvent[%s, time=%.2f]", name, time);
    }
}
