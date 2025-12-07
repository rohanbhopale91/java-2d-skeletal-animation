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
 * Exports animation data to Spine JSON format.
 * This enables integration with game engines that support Spine runtime.
 * 
 * @see <a href="http://esotericsoftware.com/spine-json-format">Spine JSON Format</a>
 */
public class SpineExporter {
    
    private final Gson gson;
    private boolean prettyPrint = true;
    private String spineVersion = "4.1";
    
    public SpineExporter() {
        this.gson = new GsonBuilder()
            .setPrettyPrinting()
            .serializeNulls()
            .create();
    }
    
    public void setPrettyPrint(boolean prettyPrint) {
        this.prettyPrint = prettyPrint;
    }
    
    public void setSpineVersion(String version) {
        this.spineVersion = version;
    }
    
    /**
     * Export a project to Spine JSON format.
     */
    public void export(AnimationProject project, File outputFile) throws IOException {
        JsonObject root = new JsonObject();
        
        // Skeleton metadata
        JsonObject skeletonMeta = new JsonObject();
        skeletonMeta.addProperty("spine", spineVersion);
        skeletonMeta.addProperty("width", project.getCanvasWidth());
        skeletonMeta.addProperty("height", project.getCanvasHeight());
        skeletonMeta.addProperty("fps", project.getFrameRate());
        skeletonMeta.addProperty("hash", generateHash(project));
        root.add("skeleton", skeletonMeta);
        
        // Bones
        Skeleton skeleton = project.getSkeleton();
        if (skeleton != null) {
            root.add("bones", exportBones(skeleton));
        }
        
        // Slots (we'll create simple slots for each bone)
        if (skeleton != null) {
            root.add("slots", exportSlots(skeleton));
        }
        
        // Skins
        root.add("skins", exportSkins());
        
        // Animations
        root.add("animations", exportAnimations(project));
        
        // Write to file
        Gson outputGson = prettyPrint ? gson : new Gson();
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(outputFile), StandardCharsets.UTF_8)) {
            outputGson.toJson(root, writer);
        }
    }
    
    private JsonArray exportBones(Skeleton skeleton) {
        JsonArray bones = new JsonArray();
        
        for (Bone bone : skeleton.getBonesInOrder()) {
            JsonObject boneObj = new JsonObject();
            boneObj.addProperty("name", bone.getName());
            
            if (bone.getParent() != null) {
                boneObj.addProperty("parent", bone.getParent().getName());
            }
            
            // Bone setup pose (from setupTransform)
            boneObj.addProperty("x", bone.getSetupTransform().getX());
            boneObj.addProperty("y", bone.getSetupTransform().getY());
            boneObj.addProperty("rotation", bone.getSetupTransform().getRotation());
            boneObj.addProperty("scaleX", bone.getSetupTransform().getScaleX());
            boneObj.addProperty("scaleY", bone.getSetupTransform().getScaleY());
            boneObj.addProperty("length", bone.getLength());
            
            bones.add(boneObj);
        }
        
        return bones;
    }
    
    private JsonArray exportSlots(Skeleton skeleton) {
        JsonArray slots = new JsonArray();
        
        for (Bone bone : skeleton.getBonesInOrder()) {
            JsonObject slot = new JsonObject();
            slot.addProperty("name", bone.getName());
            slot.addProperty("bone", bone.getName());
            slots.add(slot);
        }
        
        return slots;
    }
    
    private JsonArray exportSkins() {
        JsonArray skins = new JsonArray();
        
        // Default skin
        JsonObject defaultSkin = new JsonObject();
        defaultSkin.addProperty("name", "default");
        defaultSkin.add("attachments", new JsonObject());
        skins.add(defaultSkin);
        
        return skins;
    }
    
    private JsonObject exportAnimations(AnimationProject project) {
        JsonObject animations = new JsonObject();
        
        for (AnimationClip clip : project.getAnimations()) {
            JsonObject animObj = new JsonObject();
            
            // Bone timelines
            JsonObject bones = new JsonObject();
            for (String boneName : clip.getBoneNames()) {
                AnimationClip.BoneKeyframes timeline = clip.getTimeline(boneName);
                if (timeline != null && !timeline.getKeyframes().isEmpty()) {
                    bones.add(boneName, exportBoneTimeline(timeline));
                }
            }
            
            if (bones.size() > 0) {
                animObj.add("bones", bones);
            }
            
            animations.add(clip.getName(), animObj);
        }
        
        return animations;
    }
    
    private JsonObject exportBoneTimeline(AnimationClip.BoneKeyframes timeline) {
        JsonObject boneTimeline = new JsonObject();
        
        // Separate channels
        JsonArray rotate = new JsonArray();
        JsonArray translate = new JsonArray();
        JsonArray scale = new JsonArray();
        
        for (BoneKeyframe kf : timeline.getKeyframes()) {
            double time = kf.getTime(); // Time in seconds
            
            // Rotation
            if (kf.getRotation() != null) {
                JsonObject rotKf = new JsonObject();
                rotKf.addProperty("time", time);
                rotKf.addProperty("value", kf.getRotation());
                rotate.add(rotKf);
            }
            
            // Translation
            if (kf.getX() != null || kf.getY() != null) {
                JsonObject transKf = new JsonObject();
                transKf.addProperty("time", time);
                if (kf.getX() != null) transKf.addProperty("x", kf.getX());
                if (kf.getY() != null) transKf.addProperty("y", kf.getY());
                translate.add(transKf);
            }
            
            // Scale
            if (kf.getScaleX() != null || kf.getScaleY() != null) {
                JsonObject scaleKf = new JsonObject();
                scaleKf.addProperty("time", time);
                if (kf.getScaleX() != null) scaleKf.addProperty("x", kf.getScaleX());
                if (kf.getScaleY() != null) scaleKf.addProperty("y", kf.getScaleY());
                scale.add(scaleKf);
            }
        }
        
        if (rotate.size() > 0) boneTimeline.add("rotate", rotate);
        if (translate.size() > 0) boneTimeline.add("translate", translate);
        if (scale.size() > 0) boneTimeline.add("scale", scale);
        
        return boneTimeline;
    }
    
    private String generateHash(AnimationProject project) {
        // Simple hash for versioning
        int hash = project.getName().hashCode();
        hash = 31 * hash + project.getAnimations().size();
        if (project.getSkeleton() != null) {
            hash = 31 * hash + project.getSkeleton().getBoneCount();
        }
        return Integer.toHexString(hash);
    }
}
