package com.animstudio.core.ik;

import com.animstudio.core.math.MathUtil;
import com.animstudio.core.model.Bone;

import java.util.ArrayList;
import java.util.List;

/**
 * Inverse Kinematics constraint that controls a chain of bones to reach a target.
 */
public class IKConstraint {
    
    private String name;
    private final List<Bone> bones;
    private Bone target;
    private double mix = 1.0;
    private boolean bendPositive = true;
    private boolean compress = false;
    private boolean stretch = false;
    private double softness = 0;
    
    // Solver settings
    private int maxIterations = 10;
    private double tolerance = 0.01;
    
    public IKConstraint(String name) {
        this.name = name;
        this.bones = new ArrayList<>();
    }
    
    public IKConstraint(String name, List<Bone> bones, Bone target) {
        this.name = name;
        this.bones = new ArrayList<>(bones);
        this.target = target;
    }
    
    /**
     * Apply IK constraint to reach the target.
     */
    public void apply() {
        if (target == null || bones.isEmpty()) return;
        
        switch (bones.size()) {
            case 1:
                apply1(bones.get(0), target.getWorldTransform().getX(), 
                       target.getWorldTransform().getY(), compress, stretch, false, mix);
                break;
            case 2:
                apply2(bones.get(0), bones.get(1), 
                       target.getWorldTransform().getX(), target.getWorldTransform().getY(),
                       bendPositive ? 1 : -1, stretch, softness, mix);
                break;
            default:
                applyCCD();
                break;
        }
    }
    
    /**
     * Single bone IK - simply rotates bone to point at target.
     */
    private void apply1(Bone bone, double targetX, double targetY, 
                        boolean compress, boolean stretch, boolean uniform, double alpha) {
        
        double px = bone.getWorldTransform().getX();
        double py = bone.getWorldTransform().getY();
        
        double tx = targetX - px;
        double ty = targetY - py;
        
        // Calculate rotation needed
        double rotationIK = Math.toDegrees(Math.atan2(ty, tx)) - bone.getWorldTransform().getRotation();
        
        // Normalize angle
        rotationIK = MathUtil.wrapAngle(rotationIK);
        
        // Apply mix
        bone.setRotation(bone.getRotation() + rotationIK * alpha);
        
        // Handle stretch/compress
        if (stretch || compress) {
            double distance = Math.sqrt(tx * tx + ty * ty);
            double boneLength = bone.getLength();
            
            if ((stretch && distance > boneLength) || (compress && distance < boneLength)) {
                double scale = distance / boneLength;
                if (uniform) {
                    bone.setScaleX(bone.getScaleX() * scale);
                    bone.setScaleY(bone.getScaleY() * scale);
                } else {
                    bone.setScaleX(bone.getScaleX() * scale);
                }
            }
        }
    }
    
    /**
     * Two bone IK using analytical solution.
     */
    private void apply2(Bone parent, Bone child, double targetX, double targetY,
                        int bendDir, boolean stretch, double softness, double alpha) {
        
        double px = parent.getWorldTransform().getX();
        double py = parent.getWorldTransform().getY();
        
        double psx = parent.getScaleX();
        double csx = child.getScaleX();
        
        double l1 = parent.getLength() * psx;
        double l2 = child.getLength() * csx;
        
        double tx = targetX - px;
        double ty = targetY - py;
        
        double dd = tx * tx + ty * ty;
        double d = Math.sqrt(dd);
        
        // Apply softness
        if (softness > 0) {
            double l = l1 + l2;
            double soft = (l - softness);
            if (d > soft && soft > 0) {
                double softD = (d - soft) / softness;
                d = soft + softness * (1 - Math.exp(-softD));
            }
        }
        
        // Calculate angles using law of cosines
        double a1, a2;
        
        if (d < 0.0001) {
            a1 = 0;
            a2 = bendDir * 90;
        } else {
            double l1Sq = l1 * l1;
            double l2Sq = l2 * l2;
            
            double cos = (dd - l1Sq - l2Sq) / (2 * l1 * l2);
            
            if (cos < -1) {
                cos = -1;
                a2 = 180 * bendDir;
            } else if (cos > 1) {
                cos = 1;
                a2 = 0;
                if (stretch) {
                    double scale = d / (l1 + l2);
                    parent.setScaleX(psx * scale);
                }
            } else {
                a2 = Math.toDegrees(Math.acos(cos)) * bendDir;
            }
            
            double c = l1 + l2 * cos;
            double s = l2 * Math.sin(Math.toRadians(a2));
            a1 = Math.toDegrees(Math.atan2(ty * c - tx * s, tx * c + ty * s));
        }
        
        // Get parent's world rotation and calculate local rotations
        double parentRotation = parent.getWorldTransform().getRotation();
        double childRotation = child.getWorldTransform().getRotation();
        
        double r1 = a1 - parentRotation + parent.getRotation();
        double r2 = a2 - (childRotation - parentRotation) + child.getRotation();
        
        // Normalize angles
        r1 = MathUtil.wrapAngle(r1);
        r2 = MathUtil.wrapAngle(r2);
        
        // Apply with mix
        parent.setRotation(parent.getRotation() + (r1 - parent.getRotation()) * alpha);
        child.setRotation(child.getRotation() + (r2 - child.getRotation()) * alpha);
    }
    
    /**
     * CCD (Cyclic Coordinate Descent) IK for chains longer than 2 bones.
     */
    private void applyCCD() {
        if (bones.size() < 2 || target == null) return;
        
        double targetX = target.getWorldTransform().getX();
        double targetY = target.getWorldTransform().getY();
        
        Bone endEffector = bones.get(bones.size() - 1);
        
        for (int iteration = 0; iteration < maxIterations; iteration++) {
            // Check if we've reached the target
            double endX = endEffector.getWorldTransform().getX() + 
                         Math.cos(Math.toRadians(endEffector.getWorldTransform().getRotation())) * endEffector.getLength();
            double endY = endEffector.getWorldTransform().getY() + 
                         Math.sin(Math.toRadians(endEffector.getWorldTransform().getRotation())) * endEffector.getLength();
            
            double dx = targetX - endX;
            double dy = targetY - endY;
            double distSq = dx * dx + dy * dy;
            
            if (distSq < tolerance * tolerance) {
                break;
            }
            
            // Iterate from end effector to root
            for (int i = bones.size() - 1; i >= 0; i--) {
                Bone bone = bones.get(i);
                
                double boneX = bone.getWorldTransform().getX();
                double boneY = bone.getWorldTransform().getY();
                
                // Recalculate end effector position
                endX = endEffector.getWorldTransform().getX() + 
                      Math.cos(Math.toRadians(endEffector.getWorldTransform().getRotation())) * endEffector.getLength();
                endY = endEffector.getWorldTransform().getY() + 
                      Math.sin(Math.toRadians(endEffector.getWorldTransform().getRotation())) * endEffector.getLength();
                
                // Vector from bone to end effector
                double toEndX = endX - boneX;
                double toEndY = endY - boneY;
                double toEndAngle = Math.toDegrees(Math.atan2(toEndY, toEndX));
                
                // Vector from bone to target
                double toTargetX = targetX - boneX;
                double toTargetY = targetY - boneY;
                double toTargetAngle = Math.toDegrees(Math.atan2(toTargetY, toTargetX));
                
                // Rotation needed
                double deltaAngle = toTargetAngle - toEndAngle;
                deltaAngle = MathUtil.wrapAngle(deltaAngle);
                
                // Apply rotation with mix
                bone.setRotation(bone.getRotation() + deltaAngle * mix);
                
                // Update world transforms for the chain
                updateChainTransforms();
            }
        }
    }
    
    private void updateChainTransforms() {
        for (Bone bone : bones) {
            bone.updateWorldTransform();
        }
    }
    
    // === Getters and Setters ===
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public List<Bone> getBones() { return bones; }
    
    public void addBone(Bone bone) { bones.add(bone); }
    public void removeBone(Bone bone) { bones.remove(bone); }
    
    public Bone getTarget() { return target; }
    public void setTarget(Bone target) { this.target = target; }
    
    public double getMix() { return mix; }
    public void setMix(double mix) { this.mix = MathUtil.clamp(mix, 0, 1); }
    
    public boolean isBendPositive() { return bendPositive; }
    public void setBendPositive(boolean bendPositive) { this.bendPositive = bendPositive; }
    
    public boolean isCompress() { return compress; }
    public void setCompress(boolean compress) { this.compress = compress; }
    
    public boolean isStretch() { return stretch; }
    public void setStretch(boolean stretch) { this.stretch = stretch; }
    
    public double getSoftness() { return softness; }
    public void setSoftness(double softness) { this.softness = Math.max(0, softness); }
    
    public int getMaxIterations() { return maxIterations; }
    public void setMaxIterations(int maxIterations) { this.maxIterations = Math.max(1, maxIterations); }
    
    public double getTolerance() { return tolerance; }
    public void setTolerance(double tolerance) { this.tolerance = Math.max(0.001, tolerance); }
}
