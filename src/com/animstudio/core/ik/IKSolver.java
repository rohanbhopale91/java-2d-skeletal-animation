package com.animstudio.core.ik;

import com.animstudio.core.math.MathUtil;
import com.animstudio.core.math.Vector2;
import com.animstudio.core.model.Bone;

import java.util.List;

/**
 * Static IK solver utilities for various IK algorithms.
 */
public final class IKSolver {
    
    private IKSolver() {}
    
    /**
     * FABRIK (Forward And Backward Reaching Inverse Kinematics) solver.
     * More stable than CCD for long chains.
     * 
     * @param chain List of bones in the chain (root to tip)
     * @param targetX Target X position
     * @param targetY Target Y position
     * @param maxIterations Maximum solver iterations
     * @param tolerance Distance tolerance for convergence
     * @return true if target was reached within tolerance
     */
    public static boolean solveFABRIK(List<Bone> chain, double targetX, double targetY,
                                       int maxIterations, double tolerance) {
        if (chain == null || chain.isEmpty()) return false;
        
        int n = chain.size();
        
        // Store joint positions
        double[] posX = new double[n + 1];
        double[] posY = new double[n + 1];
        double[] lengths = new double[n];
        
        // Initialize positions from current bone configuration
        for (int i = 0; i < n; i++) {
            Bone bone = chain.get(i);
            posX[i] = bone.getWorldTransform().getX();
            posY[i] = bone.getWorldTransform().getY();
            lengths[i] = bone.getLength();
        }
        
        // End effector position
        Bone lastBone = chain.get(n - 1);
        double angle = Math.toRadians(lastBone.getWorldTransform().getRotation());
        posX[n] = posX[n - 1] + Math.cos(angle) * lengths[n - 1];
        posY[n] = posY[n - 1] + Math.sin(angle) * lengths[n - 1];
        
        // Check if target is reachable
        double totalLength = 0;
        for (double len : lengths) totalLength += len;
        
        double rootToTargetX = targetX - posX[0];
        double rootToTargetY = targetY - posY[0];
        double rootToTargetDist = Math.sqrt(rootToTargetX * rootToTargetX + rootToTargetY * rootToTargetY);
        
        // If target is unreachable, stretch toward it
        if (rootToTargetDist > totalLength) {
            double dirX = rootToTargetX / rootToTargetDist;
            double dirY = rootToTargetY / rootToTargetDist;
            
            for (int i = 0; i < n; i++) {
                posX[i + 1] = posX[i] + dirX * lengths[i];
                posY[i + 1] = posY[i] + dirY * lengths[i];
            }
            
            applyPositionsToBones(chain, posX, posY);
            return false;
        }
        
        // Store root position (it's fixed)
        double rootX = posX[0];
        double rootY = posY[0];
        
        // FABRIK iterations
        for (int iter = 0; iter < maxIterations; iter++) {
            // Check convergence
            double dx = posX[n] - targetX;
            double dy = posY[n] - targetY;
            if (dx * dx + dy * dy < tolerance * tolerance) {
                applyPositionsToBones(chain, posX, posY);
                return true;
            }
            
            // Forward reaching (from end effector to root)
            posX[n] = targetX;
            posY[n] = targetY;
            
            for (int i = n - 1; i >= 0; i--) {
                double toNextX = posX[i + 1] - posX[i];
                double toNextY = posY[i + 1] - posY[i];
                double dist = Math.sqrt(toNextX * toNextX + toNextY * toNextY);
                
                if (dist > 0.0001) {
                    double ratio = lengths[i] / dist;
                    posX[i] = posX[i + 1] - toNextX * ratio;
                    posY[i] = posY[i + 1] - toNextY * ratio;
                }
            }
            
            // Backward reaching (from root to end effector)
            posX[0] = rootX;
            posY[0] = rootY;
            
            for (int i = 0; i < n; i++) {
                double toNextX = posX[i + 1] - posX[i];
                double toNextY = posY[i + 1] - posY[i];
                double dist = Math.sqrt(toNextX * toNextX + toNextY * toNextY);
                
                if (dist > 0.0001) {
                    double ratio = lengths[i] / dist;
                    posX[i + 1] = posX[i] + toNextX * ratio;
                    posY[i + 1] = posY[i] + toNextY * ratio;
                }
            }
        }
        
        applyPositionsToBones(chain, posX, posY);
        return false;
    }
    
    /**
     * Apply solved positions back to bones as rotations.
     */
    private static void applyPositionsToBones(List<Bone> chain, double[] posX, double[] posY) {
        for (int i = 0; i < chain.size(); i++) {
            Bone bone = chain.get(i);
            
            // Calculate world rotation from position to next position
            double dx = posX[i + 1] - posX[i];
            double dy = posY[i + 1] - posY[i];
            double worldRotation = Math.toDegrees(Math.atan2(dy, dx));
            
            // Convert to local rotation
            double parentWorldRotation = 0;
            if (bone.getParent() != null) {
                parentWorldRotation = bone.getParent().getWorldTransform().getRotation();
            }
            
            double localRotation = worldRotation - parentWorldRotation;
            bone.setRotation(MathUtil.wrapAngle(localRotation));
            bone.updateWorldTransform();
        }
    }
    
    /**
     * Solve simple look-at constraint - make a bone point toward a target.
     */
    public static void solveLookAt(Bone bone, double targetX, double targetY, double mix) {
        if (bone == null) return;
        
        double boneX = bone.getWorldTransform().getX();
        double boneY = bone.getWorldTransform().getY();
        
        double dx = targetX - boneX;
        double dy = targetY - boneY;
        
        double targetAngle = Math.toDegrees(Math.atan2(dy, dx));
        double currentWorldAngle = bone.getWorldTransform().getRotation();
        
        double deltaAngle = MathUtil.wrapAngle(targetAngle - currentWorldAngle);
        
        bone.setRotation(bone.getRotation() + deltaAngle * mix);
        bone.updateWorldTransform();
    }
    
    /**
     * Solve a path constraint - distribute bones along a path.
     * 
     * @param bones Bones to distribute
     * @param pathPoints Points defining the path
     * @param spacing Spacing mode: 0 = fixed length, 1 = proportional
     */
    public static void solvePath(List<Bone> bones, List<Vector2> pathPoints, int spacing) {
        if (bones == null || bones.isEmpty() || pathPoints == null || pathPoints.size() < 2) {
            return;
        }
        
        // Distribute bones along path
        double currentDist = 0;
        
        for (Bone bone : bones) {
            double boneLength = bone.getLength();
            double targetDist = currentDist;
            
            // Walk along path to find position
            double walked = 0;
            Vector2 dir = new Vector2(1, 0);
            
            for (int i = 1; i < pathPoints.size(); i++) {
                Vector2 p0 = pathPoints.get(i - 1);
                Vector2 p1 = pathPoints.get(i);
                double segLen = p0.distance(p1);
                
                if (walked + segLen >= targetDist) {
                    dir = new Vector2(p1.x - p0.x, p1.y - p0.y).normalize();
                    break;
                }
                
                walked += segLen;
                dir = new Vector2(p1.x - p0.x, p1.y - p0.y).normalize();
            }
            
            // Apply rotation to bone
            double angle = Math.toDegrees(Math.atan2(dir.y, dir.x));
            double parentAngle = bone.getParent() != null ? bone.getParent().getWorldTransform().getRotation() : 0;
            bone.setRotation(MathUtil.wrapAngle(angle - parentAngle));
            
            currentDist += boneLength;
        }
    }
}
