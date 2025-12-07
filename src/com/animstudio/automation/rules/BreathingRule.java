package com.animstudio.automation.rules;

import com.animstudio.automation.AbstractRule;
import com.animstudio.core.model.Bone;
import com.animstudio.core.model.Skeleton;

/**
 * Procedural breathing animation rule.
 * Creates subtle scale oscillation to simulate breathing.
 */
public class BreathingRule extends AbstractRule {
    
    private double breathRate = 0.25; // breaths per second
    private double scaleAmount = 0.03; // max scale change
    private boolean affectX = false;
    private boolean affectY = true;
    
    public BreathingRule() {
        super("breathing", "Adds subtle breathing motion to a bone");
    }
    
    public BreathingRule(String targetBone) {
        this();
        setTargetBone(targetBone);
    }
    
    public void setBreathRate(double rate) {
        this.breathRate = Math.max(0.1, Math.min(2.0, rate));
    }
    
    public double getBreathRate() {
        return breathRate;
    }
    
    public void setScaleAmount(double amount) {
        this.scaleAmount = Math.max(0, Math.min(0.2, amount));
    }
    
    public double getScaleAmount() {
        return scaleAmount;
    }
    
    public void setAffectX(boolean affect) {
        this.affectX = affect;
    }
    
    public void setAffectY(boolean affect) {
        this.affectY = affect;
    }
    
    @Override
    protected void doApply(Skeleton skeleton, double time, double deltaTime) {
        Bone bone = targetBone != null ? skeleton.getBone(targetBone) : skeleton.getRootBone();
        if (bone == null) return;
        
        // Sine wave for smooth breathing
        double phase = time * breathRate * Math.PI * 2;
        double breathFactor = Math.sin(phase) * scaleAmount * intensity;
        
        if (affectX) {
            bone.setScaleX(1.0 + breathFactor);
        }
        if (affectY) {
            bone.setScaleY(1.0 + breathFactor);
        }
    }
}
