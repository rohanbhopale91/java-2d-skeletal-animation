package com.animstudio.automation.rules;

import com.animstudio.automation.AbstractRule;
import com.animstudio.core.math.Vector2;
import com.animstudio.core.model.Bone;
import com.animstudio.core.model.Skeleton;

/**
 * Procedural look-at rule.
 * Rotates a bone to face a target position.
 */
public class LookAtRule extends AbstractRule {
    
    private double targetX = 0;
    private double targetY = 0;
    private double maxRotation = 45; // max rotation from rest in degrees
    private double smoothing = 5.0; // how fast to follow target
    private boolean clampRotation = true;
    
    private double currentRotation = 0;
    
    public LookAtRule() {
        super("lookAt", "Rotates a bone to face a target position");
    }
    
    public LookAtRule(String targetBone) {
        this();
        setTargetBone(targetBone);
    }
    
    public void setTarget(double x, double y) {
        this.targetX = x;
        this.targetY = y;
    }
    
    public double getTargetX() {
        return targetX;
    }
    
    public double getTargetY() {
        return targetY;
    }
    
    public void setMaxRotation(double degrees) {
        this.maxRotation = Math.max(0, Math.min(180, degrees));
    }
    
    public double getMaxRotation() {
        return maxRotation;
    }
    
    public void setSmoothing(double value) {
        this.smoothing = Math.max(0.1, Math.min(20, value));
    }
    
    public double getSmoothing() {
        return smoothing;
    }
    
    public void setClampRotation(boolean clamp) {
        this.clampRotation = clamp;
    }
    
    @Override
    protected void doApply(Skeleton skeleton, double time, double deltaTime) {
        Bone bone = targetBone != null ? skeleton.getBone(targetBone) : null;
        if (bone == null) return;
        
        // Get bone world position
        Vector2 worldPos = bone.getWorldPosition();
        double boneWorldX = worldPos.x;
        double boneWorldY = worldPos.y;
        
        // Calculate angle to target
        double dx = targetX - boneWorldX;
        double dy = targetY - boneWorldY;
        double targetAngle = Math.toDegrees(Math.atan2(dy, dx));
        
        // Convert to bone's local space (considering parent rotation)
        double worldRotation = bone.getWorldRotation();
        double localTargetAngle = targetAngle - worldRotation + bone.getRotation();
        
        // Clamp if needed
        if (clampRotation) {
            localTargetAngle = Math.max(-maxRotation, Math.min(maxRotation, localTargetAngle));
        }
        
        // Smooth interpolation
        double diff = localTargetAngle - currentRotation;
        
        // Normalize angle difference
        while (diff > 180) diff -= 360;
        while (diff < -180) diff += 360;
        
        currentRotation += diff * smoothing * deltaTime * intensity;
        
        // Apply rotation
        bone.setRotation(currentRotation);
    }
    
    @Override
    public void reset() {
        currentRotation = 0;
    }
}
