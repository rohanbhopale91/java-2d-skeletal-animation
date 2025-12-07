package com.animstudio.core.animation;

import com.animstudio.core.interpolation.Interpolator;
import com.animstudio.core.interpolation.LinearInterpolator;
import com.animstudio.core.interpolation.SteppedInterpolator;
import com.animstudio.core.math.MathUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Functional interface for three-argument lerp function.
 */
@FunctionalInterface
interface TriFunction<A, B, C, R> {
    R apply(A a, B b, C c);
}

/**
 * A timeline track containing keyframes for a single property.
 * Examples: "spine.rotation", "arm.x", "slot1.alpha"
 * 
 * TIME CONVENTION: All time values are in SECONDS (not frames).
 * 
 * @param <T> The type of value (Double for most properties)
 */
public class KeyframeTrack<T> {
    
    /**
     * Property types for determining interpolation behavior.
     */
    public enum PropertyType {
        TRANSLATION,    // x, y position
        ROTATION,       // angle (needs shortest-path interpolation)
        SCALE,          // scaleX, scaleY
        COLOR,          // r, g, b, a
        GENERIC         // other numeric values
    }
    
    private final String targetPath;    // e.g., "spine.rotation"
    private final String boneName;      // Extracted bone name
    private final String propertyName;  // Extracted property name
    private final PropertyType propertyType;
    private final List<Keyframe<T>> keyframes;
    private final TriFunction<T, T, Double, T> lerpFunction;
    
    /**
     * Create a track for double values (most common).
     */
    public static KeyframeTrack<Double> createDoubleTrack(String targetPath, PropertyType type) {
        TriFunction<Double, Double, Double, Double> lerp;
        
        if (type == PropertyType.ROTATION) {
            lerp = (a, b, t) -> MathUtil.lerpAngleDeg(a, b, t);
        } else {
            lerp = (a, b, t) -> MathUtil.lerp(a, b, t);
        }
        
        return new KeyframeTrack<>(targetPath, type, lerp);
    }
    
    public KeyframeTrack(String targetPath, PropertyType propertyType, 
                         TriFunction<T, T, Double, T> lerpFunction) {
        this.targetPath = targetPath;
        this.propertyType = propertyType;
        this.keyframes = new ArrayList<>();
        this.lerpFunction = lerpFunction;
        
        // Parse target path
        int dotIndex = targetPath.lastIndexOf('.');
        if (dotIndex > 0) {
            this.boneName = targetPath.substring(0, dotIndex);
            this.propertyName = targetPath.substring(dotIndex + 1);
        } else {
            this.boneName = "";
            this.propertyName = targetPath;
        }
    }
    
    /**
     * Add or replace a keyframe at the given time.
     */
    public void setKeyframe(double time, T value, Interpolator interpolator) {
        // Check if keyframe already exists at this time
        for (int i = 0; i < keyframes.size(); i++) {
            Keyframe<T> kf = keyframes.get(i);
            if (MathUtil.approximately(kf.getTime(), time, 0.001)) {
                kf.setValue(value);
                kf.setInterpolator(interpolator);
                return;
            }
        }
        
        // Insert new keyframe in sorted order
        Keyframe<T> newKf = new Keyframe<>(time, value, interpolator);
        int insertIndex = Collections.binarySearch(keyframes, newKf);
        if (insertIndex < 0) {
            insertIndex = -(insertIndex + 1);
        }
        keyframes.add(insertIndex, newKf);
    }
    
    /**
     * Add or replace a keyframe with default linear interpolation.
     */
    public void setKeyframe(double time, T value) {
        setKeyframe(time, value, LinearInterpolator.INSTANCE);
    }
    
    /**
     * Remove keyframe at the given time.
     */
    public boolean removeKeyframe(double time) {
        return keyframes.removeIf(kf -> MathUtil.approximately(kf.getTime(), time, 0.001));
    }
    
    /**
     * Get keyframe at exact time, or null if none exists.
     */
    public Keyframe<T> getKeyframeAt(double time) {
        for (Keyframe<T> kf : keyframes) {
            if (MathUtil.approximately(kf.getTime(), time, 0.001)) {
                return kf;
            }
        }
        return null;
    }
    
    /**
     * Evaluate the track value at the given time.
     */
    public T evaluate(double time) {
        if (keyframes.isEmpty()) {
            return null;
        }
        
        // Before first keyframe
        if (time <= keyframes.get(0).getTime()) {
            return keyframes.get(0).getValue();
        }
        
        // After last keyframe
        int lastIndex = keyframes.size() - 1;
        if (time >= keyframes.get(lastIndex).getTime()) {
            return keyframes.get(lastIndex).getValue();
        }
        
        // Find surrounding keyframes
        int nextIndex = 0;
        for (int i = 0; i < keyframes.size(); i++) {
            if (keyframes.get(i).getTime() > time) {
                nextIndex = i;
                break;
            }
        }
        
        Keyframe<T> prev = keyframes.get(nextIndex - 1);
        Keyframe<T> next = keyframes.get(nextIndex);
        
        // Calculate local t (0-1 between keyframes)
        double duration = next.getTime() - prev.getTime();
        double localT = (time - prev.getTime()) / duration;
        
        // Apply easing from the previous keyframe's interpolator
        Interpolator interp = prev.getInterpolator();
        
        // Stepped interpolation - return previous value
        if (interp instanceof SteppedInterpolator) {
            return prev.getValue();
        }
        
        double easedT = interp.evaluate(localT);
        
        // Interpolate values
        return lerpFunction.apply(prev.getValue(), next.getValue(), easedT);
    }
    
    /**
     * Get all keyframes.
     */
    public List<Keyframe<T>> getKeyframes() {
        return Collections.unmodifiableList(keyframes);
    }
    
    /**
     * Get the number of keyframes.
     */
    public int getKeyframeCount() {
        return keyframes.size();
    }
    
    /**
     * Check if track has any keyframes.
     */
    public boolean isEmpty() {
        return keyframes.isEmpty();
    }
    
    /**
     * Get the time of the first keyframe.
     */
    public double getStartTime() {
        return keyframes.isEmpty() ? 0 : keyframes.get(0).getTime();
    }
    
    /**
     * Get the time of the last keyframe.
     */
    public double getEndTime() {
        return keyframes.isEmpty() ? 0 : keyframes.get(keyframes.size() - 1).getTime();
    }
    
    /**
     * Shift all keyframes by the given offset.
     */
    public void shiftKeyframes(double offset) {
        for (Keyframe<T> kf : keyframes) {
            kf.setTime(kf.getTime() + offset);
        }
    }
    
    /**
     * Scale all keyframe times by a factor (for speed changes).
     */
    public void scaleKeyframes(double factor) {
        for (Keyframe<T> kf : keyframes) {
            kf.setTime(kf.getTime() * factor);
        }
    }
    
    public String getTargetPath() { return targetPath; }
    public String getBoneName() { return boneName; }
    public String getPropertyName() { return propertyName; }
    public PropertyType getPropertyType() { return propertyType; }
    
    @Override
    public String toString() {
        return String.format("KeyframeTrack[%s, %d keyframes]", targetPath, keyframes.size());
    }
}
