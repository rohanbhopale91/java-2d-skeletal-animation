package com.animstudio.automation;

import com.animstudio.core.model.Skeleton;

/**
 * Abstract base class for animation rules with common functionality.
 */
public abstract class AbstractRule implements AnimationRule {
    
    protected String name;
    protected String description;
    protected boolean enabled = true;
    protected String targetBone;
    protected double intensity = 1.0;
    
    protected AbstractRule(String name, String description) {
        this.name = name;
        this.description = description;
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public String getDescription() {
        return description;
    }
    
    @Override
    public boolean isEnabled() {
        return enabled;
    }
    
    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    @Override
    public String getTargetBone() {
        return targetBone;
    }
    
    @Override
    public void setTargetBone(String boneName) {
        this.targetBone = boneName;
    }
    
    public double getIntensity() {
        return intensity;
    }
    
    public void setIntensity(double intensity) {
        this.intensity = Math.max(0, Math.min(1, intensity));
    }
    
    @Override
    public void reset() {
        // Override in subclasses if needed
    }
    
    @Override
    public void apply(Skeleton skeleton, double time, double deltaTime) {
        if (!enabled || skeleton == null) return;
        doApply(skeleton, time, deltaTime);
    }
    
    /**
     * Subclasses implement actual rule logic here.
     */
    protected abstract void doApply(Skeleton skeleton, double time, double deltaTime);
}
