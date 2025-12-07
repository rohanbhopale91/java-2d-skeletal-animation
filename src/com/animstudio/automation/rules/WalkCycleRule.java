package com.animstudio.automation.rules;

import com.animstudio.automation.AbstractRule;
import com.animstudio.core.model.Bone;
import com.animstudio.core.model.Skeleton;

/**
 * Procedural walk cycle rule.
 * Animates leg bones in a walking pattern.
 */
public class WalkCycleRule extends AbstractRule {
    
    private String leftThighBone = "leftThigh";
    private String leftShinBone = "leftShin";
    private String leftFootBone = "leftFoot";
    private String rightThighBone = "rightThigh";
    private String rightShinBone = "rightShin";
    private String rightFootBone = "rightFoot";
    
    private double cycleSpeed = 1.0; // cycles per second
    private double thighSwing = 30.0; // degrees of thigh rotation
    private double shinBend = 45.0; // degrees of shin bend
    private double footRoll = 20.0; // degrees of foot roll
    
    private double phase = 0;
    
    public WalkCycleRule() {
        super("walkCycle", "Procedural walking animation for leg bones");
    }
    
    public void setLeftLegBones(String thigh, String shin, String foot) {
        this.leftThighBone = thigh;
        this.leftShinBone = shin;
        this.leftFootBone = foot;
    }
    
    public void setRightLegBones(String thigh, String shin, String foot) {
        this.rightThighBone = thigh;
        this.rightShinBone = shin;
        this.rightFootBone = foot;
    }
    
    public void setCycleSpeed(double speed) {
        this.cycleSpeed = Math.max(0.1, Math.min(5.0, speed));
    }
    
    public double getCycleSpeed() {
        return cycleSpeed;
    }
    
    public void setThighSwing(double degrees) {
        this.thighSwing = Math.max(0, Math.min(60, degrees));
    }
    
    public void setShinBend(double degrees) {
        this.shinBend = Math.max(0, Math.min(90, degrees));
    }
    
    public void setFootRoll(double degrees) {
        this.footRoll = Math.max(0, Math.min(45, degrees));
    }
    
    @Override
    protected void doApply(Skeleton skeleton, double time, double deltaTime) {
        // Advance phase
        phase += deltaTime * cycleSpeed * 2 * Math.PI;
        if (phase > 2 * Math.PI) {
            phase -= 2 * Math.PI;
        }
        
        // Left leg (phase 0)
        applyLegPose(skeleton, leftThighBone, leftShinBone, leftFootBone, phase);
        
        // Right leg (phase offset by PI for alternating motion)
        applyLegPose(skeleton, rightThighBone, rightShinBone, rightFootBone, phase + Math.PI);
    }
    
    private void applyLegPose(Skeleton skeleton, String thighName, String shinName, String footName, double p) {
        Bone thigh = skeleton.getBone(thighName);
        Bone shin = skeleton.getBone(shinName);
        Bone foot = skeleton.getBone(footName);
        
        // Thigh: swings forward and backward
        if (thigh != null) {
            double thighAngle = Math.sin(p) * thighSwing * intensity;
            thigh.setRotation(thighAngle);
        }
        
        // Shin: bends during swing phase
        if (shin != null) {
            // Shin bends more when leg is swinging forward
            double bendFactor = Math.max(0, Math.sin(p + Math.PI / 4));
            double shinAngle = bendFactor * shinBend * intensity;
            shin.setRotation(shinAngle);
        }
        
        // Foot: rolls to stay flat during contact
        if (foot != null) {
            // Counteract some of the leg rotation
            double footAngle = -Math.sin(p) * footRoll * intensity;
            foot.setRotation(footAngle);
        }
    }
    
    @Override
    public void reset() {
        phase = 0;
    }
    
    public void setPhase(double phase) {
        this.phase = phase % (2 * Math.PI);
    }
    
    public double getPhase() {
        return phase;
    }
}
