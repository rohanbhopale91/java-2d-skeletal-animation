package com.animstudio.core.animation;

import com.animstudio.core.interpolation.Interpolator;
import com.animstudio.core.interpolation.LinearInterpolator;

/**
 * Represents a single keyframe - a value at a specific time with interpolation settings.
 * 
 * TIME CONVENTION: All time values are in SECONDS (not frames).
 * The editor UI may display frames, but converts to seconds when calling the core.
 * 
 * @param <T> The type of value stored (Double, Vector2, etc.)
 */
public class Keyframe<T> implements Comparable<Keyframe<T>> {
    
    private double time;        // Time in SECONDS
    private T value;
    private Interpolator interpolator;
    
    public Keyframe(double time, T value) {
        this(time, value, LinearInterpolator.INSTANCE);
    }
    
    public Keyframe(double time, T value, Interpolator interpolator) {
        this.time = time;
        this.value = value;
        this.interpolator = interpolator;
    }
    
    /**
     * Copy constructor.
     */
    public Keyframe(Keyframe<T> other) {
        this.time = other.time;
        this.value = other.value;
        this.interpolator = other.interpolator;
    }
    
    /**
     * Get the time in seconds.
     */
    public double getTime() { return time; }
    
    /**
     * Set the time in seconds.
     */
    public void setTime(double time) { this.time = time; }
    
    public T getValue() { return value; }
    public void setValue(T value) { this.value = value; }
    
    public Interpolator getInterpolator() { return interpolator; }
    public void setInterpolator(Interpolator interpolator) { this.interpolator = interpolator; }
    
    @Override
    public int compareTo(Keyframe<T> other) {
        return Double.compare(this.time, other.time);
    }
    
    @Override
    public String toString() {
        return String.format("Keyframe[t=%.3fs, value=%s, interp=%s]", 
            time, value, interpolator.getType());
    }
}
