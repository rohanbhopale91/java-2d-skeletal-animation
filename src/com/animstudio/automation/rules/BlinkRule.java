package com.animstudio.automation.rules;

import com.animstudio.automation.AbstractRule;
import com.animstudio.core.model.Bone;
import com.animstudio.core.model.Skeleton;

import java.util.Random;

/**
 * Procedural blink animation rule.
 * Creates periodic blinking by scaling eye bones.
 */
public class BlinkRule extends AbstractRule {
    
    private double minInterval = 2.0; // minimum seconds between blinks
    private double maxInterval = 6.0; // maximum seconds between blinks
    private double blinkDuration = 0.15; // seconds for one blink
    
    private final Random random = new Random();
    private double nextBlinkTime = 0;
    private double blinkStartTime = -1;
    private boolean isBlinking = false;
    
    public BlinkRule() {
        super("blink", "Adds periodic blinking to eye bones");
        scheduleNextBlink(0);
    }
    
    public BlinkRule(String targetBone) {
        this();
        setTargetBone(targetBone);
    }
    
    public void setBlinkInterval(double min, double max) {
        this.minInterval = Math.max(0.5, min);
        this.maxInterval = Math.max(minInterval, max);
    }
    
    public void setBlinkDuration(double duration) {
        this.blinkDuration = Math.max(0.05, Math.min(0.5, duration));
    }
    
    private void scheduleNextBlink(double currentTime) {
        double interval = minInterval + random.nextDouble() * (maxInterval - minInterval);
        nextBlinkTime = currentTime + interval;
    }
    
    @Override
    protected void doApply(Skeleton skeleton, double time, double deltaTime) {
        Bone bone = targetBone != null ? skeleton.getBone(targetBone) : null;
        if (bone == null) return;
        
        // Check if it's time to blink
        if (!isBlinking && time >= nextBlinkTime) {
            isBlinking = true;
            blinkStartTime = time;
        }
        
        if (isBlinking) {
            double blinkProgress = (time - blinkStartTime) / blinkDuration;
            
            if (blinkProgress >= 1.0) {
                // Blink complete
                isBlinking = false;
                bone.setScaleY(1.0);
                scheduleNextBlink(time);
            } else {
                // Blink in progress - close then open
                double closeFactor;
                if (blinkProgress < 0.5) {
                    // Closing
                    closeFactor = blinkProgress * 2;
                } else {
                    // Opening
                    closeFactor = (1.0 - blinkProgress) * 2;
                }
                
                // Scale Y to simulate eyelid closing
                double scaleY = 1.0 - (closeFactor * 0.9 * intensity);
                bone.setScaleY(Math.max(0.1, scaleY));
            }
        }
    }
    
    @Override
    public void reset() {
        isBlinking = false;
        blinkStartTime = -1;
        nextBlinkTime = 0;
        scheduleNextBlink(0);
    }
}
