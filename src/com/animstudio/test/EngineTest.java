package com.animstudio.test;

import com.animstudio.core.animation.*;
import com.animstudio.core.event.EngineEventBus;
import com.animstudio.core.event.events.BoneModifiedEvent;
import com.animstudio.core.interpolation.BezierInterpolator;
import com.animstudio.core.interpolation.LinearInterpolator;
import com.animstudio.core.model.*;

/**
 * Test application to verify Phase 1 engine components.
 */
public class EngineTest {
    
    public static void main(String[] args) {
        System.out.println("=== AnimStudio Engine Test ===\n");
        
        // Test 1: Create a skeleton with hierarchical bones
        System.out.println("--- Test 1: Skeleton Creation ---");
        Skeleton skeleton = createTestSkeleton();
        System.out.println("Created: " + skeleton);
        for (Bone bone : skeleton.getBonesInOrder()) {
            System.out.println("  " + "  ".repeat(bone.getDepth()) + bone.getName() + 
                " (depth=" + bone.getDepth() + ")");
        }
        
        // Test 2: Bone transforms and hierarchy
        System.out.println("\n--- Test 2: Bone Transforms ---");
        Bone spine = skeleton.getBone("spine");
        Bone head = skeleton.getBone("head");
        
        spine.setRotation(15);  // Rotate spine
        skeleton.updateWorldTransforms();
        
        System.out.println("Spine local rotation: " + spine.getRotation() + "°");
        System.out.println("Spine world rotation: " + spine.getWorldRotation() + "°");
        System.out.println("Head local rotation: " + head.getRotation() + "°");
        System.out.println("Head world rotation: " + head.getWorldRotation() + "° (inherited from spine)");
        
        // Test 3: Animation creation
        System.out.println("\n--- Test 3: Animation Creation ---");
        AnimationClip idleAnim = createIdleAnimation();
        System.out.println("Created: " + idleAnim);
        for (KeyframeTrack<Double> track : idleAnim.getTracks()) {
            System.out.println("  Track: " + track.getTargetPath() + 
                " (" + track.getKeyframeCount() + " keyframes)");
        }
        
        // Test 4: Animation playback
        System.out.println("\n--- Test 4: Animation Playback ---");
        skeleton.resetToSetupPose();
        
        System.out.println("Evaluating animation at different times:");
        for (double t = 0; t <= 60; t += 15) {
            idleAnim.apply(skeleton, t);
            skeleton.updateWorldTransforms();
            spine = skeleton.getBone("spine");
            System.out.printf("  t=%.0f: spine.rotation = %.2f°%n", t, spine.getRotation());
        }
        
        // Test 5: AnimationState playback
        System.out.println("\n--- Test 5: AnimationState ---");
        skeleton.resetToSetupPose();
        AnimationState state = new AnimationState(skeleton);
        state.setAnimation(0, idleAnim, true);
        
        state.addListener(new AnimationState.AnimationStateListener() {
            @Override
            public void onAnimationLoop(AnimationState.TrackEntry entry) {
                System.out.println("  [Event] Animation looped!");
            }
        });
        
        System.out.println("Simulating 2 seconds of playback (at 30fps):");
        for (int frame = 0; frame < 65; frame++) {
            state.update(1.0);  // 1 frame per update
            if (frame % 15 == 0) {
                System.out.printf("  Frame %d: spine.rotation = %.2f°%n", 
                    frame, skeleton.getBone("spine").getRotation());
            }
        }
        
        // Test 6: Event system
        System.out.println("\n--- Test 6: Event System ---");
        EngineEventBus eventBus = EngineEventBus.getInstance();
        eventBus.setLogging(true);
        
        eventBus.subscribe(BoneModifiedEvent.class, event -> {
            System.out.println("  Received: " + event.getBone().getName() + 
                " modified (" + event.getModificationType() + ")");
        });
        
        // Simulate firing an event
        Bone testBone = skeleton.getBone("arm_left");
        testBone.setRotation(45);
        eventBus.publish(new BoneModifiedEvent(testBone, BoneModifiedEvent.ModificationType.TRANSFORM));
        
        // Test 7: Interpolation
        System.out.println("\n--- Test 7: Interpolation ---");
        System.out.println("Linear interpolation:");
        for (double t = 0; t <= 1; t += 0.25) {
            System.out.printf("  t=%.2f: %.3f%n", t, LinearInterpolator.INSTANCE.evaluate(t));
        }
        
        System.out.println("Bezier ease-in-out interpolation:");
        for (double t = 0; t <= 1; t += 0.25) {
            System.out.printf("  t=%.2f: %.3f%n", t, BezierInterpolator.EASE_IN_OUT.evaluate(t));
        }
        
        System.out.println("\n=== All Tests Passed! ===");
    }
    
    private static Skeleton createTestSkeleton() {
        Skeleton skeleton = new Skeleton("TestCharacter");
        
        // Create bones
        Bone root = new Bone("root");
        Bone spine = new Bone("spine");
        Bone head = new Bone("head");
        Bone armLeft = new Bone("arm_left");
        Bone armRight = new Bone("arm_right");
        Bone legLeft = new Bone("leg_left");
        Bone legRight = new Bone("leg_right");
        
        // Setup hierarchy
        spine.setParent(root);
        head.setParent(spine);
        armLeft.setParent(spine);
        armRight.setParent(spine);
        legLeft.setParent(root);
        legRight.setParent(root);
        
        // Setup transforms
        root.setY(100);
        spine.setY(-30);
        head.setY(-40);
        armLeft.setX(-20);
        armRight.setX(20);
        legLeft.setX(-10);
        legLeft.setY(30);
        legRight.setX(10);
        legRight.setY(30);
        
        // Set as setup pose
        for (Bone bone : new Bone[]{root, spine, head, armLeft, armRight, legLeft, legRight}) {
            bone.setToSetupPose();
            skeleton.addBone(bone);
        }
        
        skeleton.updateWorldTransforms();
        return skeleton;
    }
    
    private static AnimationClip createIdleAnimation() {
        AnimationClip clip = new AnimationClip("idle");
        clip.setDuration(60);
        clip.setLooping(true);
        
        // Spine breathing motion
        KeyframeTrack<Double> spineRotTrack = clip.getOrCreateTrack("spine.rotation", 
            KeyframeTrack.PropertyType.ROTATION);
        spineRotTrack.setKeyframe(0, 0.0, BezierInterpolator.EASE_IN_OUT);
        spineRotTrack.setKeyframe(30, 5.0, BezierInterpolator.EASE_IN_OUT);
        spineRotTrack.setKeyframe(60, 0.0, BezierInterpolator.EASE_IN_OUT);
        
        // Spine scale (breathing)
        KeyframeTrack<Double> spineScaleTrack = clip.getOrCreateTrack("spine.scaleY", 
            KeyframeTrack.PropertyType.SCALE);
        spineScaleTrack.setKeyframe(0, 1.0);
        spineScaleTrack.setKeyframe(30, 1.02);
        spineScaleTrack.setKeyframe(60, 1.0);
        
        // Head slight bob
        KeyframeTrack<Double> headRotTrack = clip.getOrCreateTrack("head.rotation", 
            KeyframeTrack.PropertyType.ROTATION);
        headRotTrack.setKeyframe(0, 0.0);
        headRotTrack.setKeyframe(15, -2.0);
        headRotTrack.setKeyframe(45, 2.0);
        headRotTrack.setKeyframe(60, 0.0);
        
        return clip;
    }
}
