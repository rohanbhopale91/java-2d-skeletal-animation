package com.animstudio.io.export;

import com.animstudio.core.animation.AnimationClip;
import com.animstudio.core.animation.BoneKeyframe;
import com.animstudio.core.model.Bone;
import com.animstudio.core.model.Skeleton;
import com.animstudio.editor.project.AnimationProject;
import com.google.gson.*;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * Exports animation data to DragonBones JSON format.
 * This enables integration with game engines that support DragonBones runtime.
 * 
 * @see <a href="https://docs.egret.com/dragonbones/docs/en/">DragonBones Documentation</a>
 */
public class DragonBonesExporter {
    
    private final Gson gson;
    private boolean prettyPrint = true;
    private String formatVersion = "5.5";
    
    public DragonBonesExporter() {
        this.gson = new GsonBuilder()
            .setPrettyPrinting()
            .serializeNulls()
            .create();
    }
    
    public void setPrettyPrint(boolean prettyPrint) {
        this.prettyPrint = prettyPrint;
    }
    
    public void setFormatVersion(String version) {
        this.formatVersion = version;
    }
    
    /**
     * Export a project to DragonBones JSON format.
     */
    public void export(AnimationProject project, File outputFile) throws IOException {
        JsonObject root = new JsonObject();
        
        // DragonBones header
        root.addProperty("version", formatVersion);
        root.addProperty("compatibleVersion", "5.5");
        root.addProperty("name", project.getName());
        root.addProperty("frameRate", (int) project.getFrameRate());
        
        // Armature (skeleton)
        JsonArray armatures = new JsonArray();
        if (project.getSkeleton() != null) {
            armatures.add(exportArmature(project));
        }
        root.add("armature", armatures);
        
        // Write to file
        Gson outputGson = prettyPrint ? gson : new Gson();
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(outputFile), StandardCharsets.UTF_8)) {
            outputGson.toJson(root, writer);
        }
    }
    
    private JsonObject exportArmature(AnimationProject project) {
        JsonObject armature = new JsonObject();
        Skeleton skeleton = project.getSkeleton();
        
        armature.addProperty("type", "Armature");
        armature.addProperty("name", skeleton.getName());
        armature.addProperty("frameRate", (int) project.getFrameRate());
        
        // AABB (bounding box)
        JsonObject aabb = new JsonObject();
        aabb.addProperty("x", -project.getCanvasWidth() / 2.0);
        aabb.addProperty("y", -project.getCanvasHeight() / 2.0);
        aabb.addProperty("width", project.getCanvasWidth());
        aabb.addProperty("height", project.getCanvasHeight());
        armature.add("aabb", aabb);
        
        // Bones
        armature.add("bone", exportBones(skeleton));
        
        // Slots
        armature.add("slot", exportSlots(skeleton));
        
        // Skins
        armature.add("skin", exportSkins(skeleton));
        
        // Animations
        armature.add("animation", exportAnimations(project));
        
        // Default actions
        JsonArray defaultActions = new JsonArray();
        JsonObject playAction = new JsonObject();
        playAction.addProperty("type", "play");
        if (!project.getAnimations().isEmpty()) {
            playAction.addProperty("name", project.getAnimations().get(0).getName());
        }
        defaultActions.add(playAction);
        armature.add("defaultActions", defaultActions);
        
        return armature;
    }
    
    private JsonArray exportBones(Skeleton skeleton) {
        JsonArray bones = new JsonArray();
        
        for (Bone bone : skeleton.getBonesInOrder()) {
            JsonObject boneObj = new JsonObject();
            boneObj.addProperty("name", bone.getName());
            
            if (bone.getParent() != null) {
                boneObj.addProperty("parent", bone.getParent().getName());
            }
            
            boneObj.addProperty("length", bone.getLength());
            
            // Transform from setup pose
            JsonObject transform = new JsonObject();
            transform.addProperty("x", bone.getSetupTransform().getX());
            transform.addProperty("y", bone.getSetupTransform().getY());
            transform.addProperty("skX", bone.getSetupTransform().getRotation());
            transform.addProperty("skY", bone.getSetupTransform().getRotation());
            transform.addProperty("scX", bone.getSetupTransform().getScaleX());
            transform.addProperty("scY", bone.getSetupTransform().getScaleY());
            boneObj.add("transform", transform);
            
            bones.add(boneObj);
        }
        
        return bones;
    }
    
    private JsonArray exportSlots(Skeleton skeleton) {
        JsonArray slots = new JsonArray();
        
        int displayIndex = 0;
        for (Bone bone : skeleton.getBonesInOrder()) {
            JsonObject slot = new JsonObject();
            slot.addProperty("name", bone.getName());
            slot.addProperty("parent", bone.getName());
            slot.addProperty("displayIndex", displayIndex++);
            slots.add(slot);
        }
        
        return slots;
    }
    
    private JsonArray exportSkins(Skeleton skeleton) {
        JsonArray skins = new JsonArray();
        
        // Default skin
        JsonObject defaultSkin = new JsonObject();
        defaultSkin.addProperty("name", "default");
        
        JsonArray skinSlots = new JsonArray();
        for (Bone bone : skeleton.getBonesInOrder()) {
            JsonObject skinSlot = new JsonObject();
            skinSlot.addProperty("name", bone.getName());
            skinSlot.add("display", new JsonArray()); // Empty displays
            skinSlots.add(skinSlot);
        }
        defaultSkin.add("slot", skinSlots);
        
        skins.add(defaultSkin);
        return skins;
    }
    
    private JsonArray exportAnimations(AnimationProject project) {
        JsonArray animations = new JsonArray();
        int frameRate = (int) project.getFrameRate();
        
        for (AnimationClip clip : project.getAnimations()) {
            JsonObject animObj = new JsonObject();
            animObj.addProperty("name", clip.getName());
            animObj.addProperty("duration", (int)(clip.getDuration() * frameRate));
            animObj.addProperty("playTimes", clip.isLooping() ? 0 : 1);
            
            // Bone timelines
            JsonArray boneTimelines = new JsonArray();
            for (String boneName : clip.getBoneNames()) {
                AnimationClip.BoneKeyframes timeline = clip.getTimeline(boneName);
                if (timeline != null && !timeline.getKeyframes().isEmpty()) {
                    boneTimelines.add(exportBoneTimeline(boneName, timeline, frameRate));
                }
            }
            animObj.add("bone", boneTimelines);
            
            animations.add(animObj);
        }
        
        return animations;
    }
    
    private JsonObject exportBoneTimeline(String boneName, AnimationClip.BoneKeyframes timeline, int frameRate) {
        JsonObject boneTimeline = new JsonObject();
        boneTimeline.addProperty("name", boneName);
        
        // DragonBones uses separate frame arrays for translate, rotate, scale
        JsonArray rotateFrames = new JsonArray();
        JsonArray translateFrames = new JsonArray();
        JsonArray scaleFrames = new JsonArray();
        
        for (BoneKeyframe kf : timeline.getKeyframes()) {
            int frameNumber = (int)(kf.getTime() * frameRate);
            
            // Rotation frame
            if (kf.getRotation() != null) {
                JsonObject rotFrame = new JsonObject();
                rotFrame.addProperty("duration", 1); // DragonBones uses duration, not time
                rotFrame.addProperty("rotate", kf.getRotation());
                rotateFrames.add(rotFrame);
            }
            
            // Translation frame
            if (kf.getX() != null || kf.getY() != null) {
                JsonObject transFrame = new JsonObject();
                transFrame.addProperty("duration", 1);
                if (kf.getX() != null) transFrame.addProperty("x", kf.getX());
                if (kf.getY() != null) transFrame.addProperty("y", kf.getY());
                translateFrames.add(transFrame);
            }
            
            // Scale frame
            if (kf.getScaleX() != null || kf.getScaleY() != null) {
                JsonObject scaleFrame = new JsonObject();
                scaleFrame.addProperty("duration", 1);
                if (kf.getScaleX() != null) scaleFrame.addProperty("x", kf.getScaleX());
                if (kf.getScaleY() != null) scaleFrame.addProperty("y", kf.getScaleY());
                scaleFrames.add(scaleFrame);
            }
        }
        
        if (rotateFrames.size() > 0) boneTimeline.add("rotateFrame", rotateFrames);
        if (translateFrames.size() > 0) boneTimeline.add("translateFrame", translateFrames);
        if (scaleFrames.size() > 0) boneTimeline.add("scaleFrame", scaleFrames);
        
        return boneTimeline;
    }
}
