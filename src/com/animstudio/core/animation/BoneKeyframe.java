package com.animstudio.core.animation;

import com.animstudio.core.util.TimeUtils;

/**
 * A simple data class holding all bone transform values at a specific time.
 * Used for convenient keyframe manipulation in the editor.
 * 
 * TIME CONVENTION: The 'time' field is in SECONDS (not frames).
 * Use TimeUtils.framesToSeconds() and TimeUtils.secondsToFrames() for conversion.
 */
public class BoneKeyframe {
    
    private double time;  // Time in SECONDS
    private Double x;
    private Double y;
    private Double rotation;
    private Double scaleX;
    private Double scaleY;
    
    /**
     * Create a keyframe at the specified time in seconds.
     */
    public BoneKeyframe(double timeSeconds) {
        this.time = timeSeconds;
    }
    
    /**
     * Create a keyframe with all properties set.
     * @param timeSeconds Time in seconds
     */
    public BoneKeyframe(double timeSeconds, double x, double y, double rotation, double scaleX, double scaleY) {
        this.time = timeSeconds;
        this.x = x;
        this.y = y;
        this.rotation = rotation;
        this.scaleX = scaleX;
        this.scaleY = scaleY;
    }
    
    /**
     * Get time in seconds.
     */
    public double getTime() { return time; }
    
    /**
     * Set time in seconds.
     */
    public void setTime(double timeSeconds) { this.time = timeSeconds; }
    
    /**
     * Get time as frame number (using default frame rate).
     * @deprecated Use getTime() which returns seconds. Convert with TimeUtils if frames needed.
     */
    @Deprecated
    public double getFrame() { return TimeUtils.secondsToFrames(time); }
    
    /**
     * Set time from frame number (using default frame rate).
     * @deprecated Use setTime() which takes seconds. Convert with TimeUtils if needed.
     */
    @Deprecated
    public void setFrame(double frame) { this.time = TimeUtils.framesToSeconds(frame); }
    
    public Double getX() { return x; }
    public void setX(Double x) { this.x = x; }
    
    public Double getY() { return y; }
    public void setY(Double y) { this.y = y; }
    
    public Double getRotation() { return rotation; }
    public void setRotation(Double rotation) { this.rotation = rotation; }
    
    public Double getScaleX() { return scaleX; }
    public void setScaleX(Double scaleX) { this.scaleX = scaleX; }
    
    public Double getScaleY() { return scaleY; }
    public void setScaleY(Double scaleY) { this.scaleY = scaleY; }
    
    @Override
    public String toString() {
        return String.format("BoneKeyframe[time=%.3fs, x=%s, y=%s, rot=%s, sx=%s, sy=%s]",
            time, x, y, rotation, scaleX, scaleY);
    }
}
