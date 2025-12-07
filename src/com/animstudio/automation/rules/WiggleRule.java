package com.animstudio.automation.rules;

import com.animstudio.automation.AbstractRule;
import com.animstudio.core.model.Bone;
import com.animstudio.core.model.Skeleton;

import java.util.Random;

/**
 * Procedural wiggle/jitter rule.
 * Adds random small movements to bones for natural motion.
 */
public class WiggleRule extends AbstractRule {
    
    private double frequency = 5.0; // oscillations per second
    private double rotationAmount = 3.0; // max rotation in degrees
    private double translationAmount = 0; // max translation in pixels
    private boolean smoothNoise = true;
    
    private final Random random = new Random();
    private double noiseOffset;
    
    public WiggleRule() {
        super("wiggle", "Adds random jitter/shake to a bone");
        noiseOffset = random.nextDouble() * 1000;
    }
    
    public WiggleRule(String targetBone) {
        this();
        setTargetBone(targetBone);
    }
    
    public void setFrequency(double freq) {
        this.frequency = Math.max(0.1, Math.min(30, freq));
    }
    
    public double getFrequency() {
        return frequency;
    }
    
    public void setRotationAmount(double degrees) {
        this.rotationAmount = Math.max(0, Math.min(45, degrees));
    }
    
    public double getRotationAmount() {
        return rotationAmount;
    }
    
    public void setTranslationAmount(double pixels) {
        this.translationAmount = Math.max(0, Math.min(50, pixels));
    }
    
    public double getTranslationAmount() {
        return translationAmount;
    }
    
    public void setSmoothNoise(boolean smooth) {
        this.smoothNoise = smooth;
    }
    
    @Override
    protected void doApply(Skeleton skeleton, double time, double deltaTime) {
        Bone bone = targetBone != null ? skeleton.getBone(targetBone) : null;
        if (bone == null) return;
        
        double t = time * frequency + noiseOffset;
        
        if (smoothNoise) {
            // Smooth Perlin-like noise using multiple sine waves
            double noiseRot = (Math.sin(t * 1.0) * 0.5 + 
                              Math.sin(t * 2.3) * 0.3 + 
                              Math.sin(t * 4.1) * 0.2);
            double noiseX = (Math.sin(t * 1.3 + 100) * 0.5 + 
                            Math.sin(t * 2.7 + 100) * 0.3 + 
                            Math.sin(t * 3.9 + 100) * 0.2);
            double noiseY = (Math.sin(t * 1.1 + 200) * 0.5 + 
                            Math.sin(t * 2.5 + 200) * 0.3 + 
                            Math.sin(t * 4.3 + 200) * 0.2);
            
            if (rotationAmount > 0) {
                double currentRot = bone.getRotation();
                bone.setRotation(currentRot + noiseRot * rotationAmount * intensity);
            }
            
            if (translationAmount > 0) {
                double currentX = bone.getX();
                double currentY = bone.getY();
                bone.setX(currentX + noiseX * translationAmount * intensity);
                bone.setY(currentY + noiseY * translationAmount * intensity);
            }
        } else {
            // Pure random jitter
            if (rotationAmount > 0) {
                double jitterRot = (random.nextDouble() * 2 - 1) * rotationAmount * intensity;
                bone.setRotation(bone.getRotation() + jitterRot);
            }
            
            if (translationAmount > 0) {
                double jitterX = (random.nextDouble() * 2 - 1) * translationAmount * intensity;
                double jitterY = (random.nextDouble() * 2 - 1) * translationAmount * intensity;
                bone.setX(bone.getX() + jitterX);
                bone.setY(bone.getY() + jitterY);
            }
        }
    }
}
