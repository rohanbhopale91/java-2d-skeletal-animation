package com.animstudio.io;

import com.animstudio.core.animation.AnimationClip;
import com.animstudio.core.animation.AnimationEvent;
import com.animstudio.core.animation.Keyframe;
import com.animstudio.core.animation.KeyframeTrack;
import com.animstudio.core.interpolation.BezierInterpolator;
import com.animstudio.core.interpolation.Interpolator;
import com.animstudio.core.interpolation.LinearInterpolator;
import com.animstudio.core.interpolation.SteppedInterpolator;
import com.animstudio.core.mesh.DeformableMesh;
import com.animstudio.core.mesh.MeshVertex;
import com.animstudio.core.model.Attachment;
import com.animstudio.core.model.Bone;
import com.animstudio.core.model.RegionAttachment;
import com.animstudio.core.model.Skeleton;
import com.animstudio.core.model.Slot;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Serializes animation data to JSON format.
 * Supports our native format plus export to Spine/DragonBones compatible formats.
 */
public class JsonSerializer {
    
    private final Gson gson;
    private final boolean prettyPrint;
    
    public JsonSerializer() {
        this(true);
    }
    
    public JsonSerializer(boolean prettyPrint) {
        this.prettyPrint = prettyPrint;
        GsonBuilder builder = new GsonBuilder();
        if (prettyPrint) {
            builder.setPrettyPrinting();
        }
        this.gson = builder.create();
    }
    
    /**
     * Serialize a project file to JSON string.
     */
    public String serialize(ProjectFile project) {
        JsonObject root = new JsonObject();
        
        // Version and metadata
        root.addProperty("version", ProjectFile.FORMAT_VERSION);
        root.addProperty("name", project.getName());
        root.addProperty("author", project.getAuthor());
        root.addProperty("description", project.getDescription());
        if (project.getCreatedAt() != null) {
            root.addProperty("createdAt", project.getCreatedAt().toString());
        }
        if (project.getModifiedAt() != null) {
            root.addProperty("modifiedAt", project.getModifiedAt().toString());
        }
        
        // Settings
        root.add("settings", serializeSettings(project));
        
        // Skeleton
        if (project.getSkeleton() != null) {
            root.add("skeleton", serializeSkeleton(project.getSkeleton()));
        }
        
        // Animation clips
        JsonArray clipsArray = new JsonArray();
        for (AnimationClip clip : project.getAnimations()) {
            clipsArray.add(serializeAnimationClip(clip));
        }
        root.add("animations", clipsArray);
        
        // Texture paths
        JsonObject texturesObj = new JsonObject();
        for (Map.Entry<String, String> entry : project.getTexturePaths().entrySet()) {
            texturesObj.addProperty(entry.getKey(), entry.getValue());
        }
        root.add("textures", texturesObj);
        
        return gson.toJson(root);
    }
    
    /**
     * Serialize and write to file.
     */
    public void serializeToFile(ProjectFile project, File file) throws IOException {
        String json = serialize(project);
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            writer.write(json);
        }
    }
    
    /**
     * Serialize project settings.
     */
    private JsonObject serializeSettings(ProjectFile project) {
        JsonObject settings = new JsonObject();
        settings.addProperty("frameRate", project.getFrameRate());
        settings.addProperty("canvasWidth", project.getCanvasWidth());
        settings.addProperty("canvasHeight", project.getCanvasHeight());
        settings.addProperty("defaultDuration", project.getDefaultDuration());
        return settings;
    }
    
    /**
     * Serialize a skeleton.
     */
    public JsonObject serializeSkeleton(Skeleton skeleton) {
        JsonObject skelObj = new JsonObject();
        skelObj.addProperty("name", skeleton.getName());
        
        // Serialize bones - use getBonesInOrder() for proper hierarchy
        JsonArray bonesArray = new JsonArray();
        for (Bone bone : skeleton.getBonesInOrder()) {
            bonesArray.add(serializeBone(bone));
        }
        skelObj.add("bones", bonesArray);
        
        // Serialize slots
        JsonArray slotsArray = new JsonArray();
        for (Slot slot : skeleton.getSlots()) {
            slotsArray.add(serializeSlot(slot));
        }
        skelObj.add("slots", slotsArray);
        
        return skelObj;
    }
    
    /**
     * Serialize a bone.
     */
    private JsonObject serializeBone(Bone bone) {
        JsonObject boneObj = new JsonObject();
        boneObj.addProperty("name", bone.getName());
        
        if (bone.getParent() != null) {
            boneObj.addProperty("parent", bone.getParent().getName());
        }
        
        // Local transform - use getX(), getY() etc.
        boneObj.addProperty("x", bone.getX());
        boneObj.addProperty("y", bone.getY());
        boneObj.addProperty("rotation", bone.getRotation());
        boneObj.addProperty("scaleX", bone.getScaleX());
        boneObj.addProperty("scaleY", bone.getScaleY());
        
        boneObj.addProperty("length", bone.getLength());
        boneObj.addProperty("drawOrder", bone.getDrawOrder());
        boneObj.addProperty("inheritRotation", bone.isInheritRotation());
        boneObj.addProperty("inheritScale", bone.isInheritScale());
        boneObj.addProperty("color", bone.getColor());
        
        return boneObj;
    }
    
    /**
     * Serialize a slot.
     */
    private JsonObject serializeSlot(Slot slot) {
        JsonObject slotObj = new JsonObject();
        slotObj.addProperty("name", slot.getName());
        slotObj.addProperty("bone", slot.getBone().getName());
        slotObj.addProperty("drawOrder", slot.getDrawOrder());
        slotObj.addProperty("blendMode", slot.getBlendMode().name());
        
        // Color - use getRed(), getGreen(), etc.
        slotObj.addProperty("red", slot.getRed());
        slotObj.addProperty("green", slot.getGreen());
        slotObj.addProperty("blue", slot.getBlue());
        slotObj.addProperty("alpha", slot.getAlpha());
        
        // Attachment
        if (slot.getAttachment() != null) {
            slotObj.add("attachment", serializeAttachment(slot.getAttachment()));
        }
        
        return slotObj;
    }
    
    /**
     * Serialize an attachment.
     */
    private JsonObject serializeAttachment(Attachment attachment) {
        JsonObject attObj = new JsonObject();
        attObj.addProperty("name", attachment.getName());
        attObj.addProperty("type", attachment.getType());
        attObj.addProperty("x", attachment.getX());
        attObj.addProperty("y", attachment.getY());
        attObj.addProperty("rotation", attachment.getRotation());
        attObj.addProperty("scaleX", attachment.getScaleX());
        attObj.addProperty("scaleY", attachment.getScaleY());
        
        if (attachment instanceof RegionAttachment) {
            RegionAttachment region = (RegionAttachment) attachment;
            attObj.addProperty("regionName", region.getRegionName());
            attObj.addProperty("regionX", region.getRegionX());
            attObj.addProperty("regionY", region.getRegionY());
            attObj.addProperty("regionWidth", region.getRegionWidth());
            attObj.addProperty("regionHeight", region.getRegionHeight());
            attObj.addProperty("width", region.getWidth());
            attObj.addProperty("height", region.getHeight());
            attObj.addProperty("pivotX", region.getPivotX());
            attObj.addProperty("pivotY", region.getPivotY());
        }
        
        return attObj;
    }
    
    /**
     * Serialize an animation clip.
     */
    public JsonObject serializeAnimationClip(AnimationClip clip) {
        JsonObject clipObj = new JsonObject();
        clipObj.addProperty("name", clip.getName());
        clipObj.addProperty("duration", clip.getDuration());
        clipObj.addProperty("looping", clip.isLooping());
        
        // Serialize tracks
        JsonArray tracksArray = new JsonArray();
        for (KeyframeTrack<Double> track : clip.getTracks()) {
            tracksArray.add(serializeTrack(track));
        }
        clipObj.add("tracks", tracksArray);
        
        // Serialize events
        JsonArray eventsArray = new JsonArray();
        for (AnimationEvent event : clip.getEvents()) {
            eventsArray.add(serializeEvent(event));
        }
        clipObj.add("events", eventsArray);
        
        return clipObj;
    }
    
    /**
     * Serialize a keyframe track.
     */
    private JsonObject serializeTrack(KeyframeTrack<Double> track) {
        JsonObject trackObj = new JsonObject();
        trackObj.addProperty("targetPath", track.getTargetPath());
        trackObj.addProperty("boneName", track.getBoneName());
        trackObj.addProperty("propertyName", track.getPropertyName());
        trackObj.addProperty("propertyType", track.getPropertyType().name());
        
        // Serialize keyframes
        JsonArray keyframesArray = new JsonArray();
        for (Keyframe<Double> keyframe : track.getKeyframes()) {
            keyframesArray.add(serializeKeyframe(keyframe));
        }
        trackObj.add("keyframes", keyframesArray);
        
        return trackObj;
    }
    
    /**
     * Serialize a keyframe.
     */
    private JsonObject serializeKeyframe(Keyframe<Double> keyframe) {
        JsonObject kfObj = new JsonObject();
        kfObj.addProperty("time", keyframe.getTime());
        kfObj.addProperty("value", keyframe.getValue());
        
        // Serialize interpolator
        Interpolator interp = keyframe.getInterpolator();
        if (interp != null) {
            JsonObject interpObj = new JsonObject();
            interpObj.addProperty("type", interp.getType());
            
            if (interp instanceof BezierInterpolator) {
                BezierInterpolator bezier = (BezierInterpolator) interp;
                interpObj.addProperty("x1", bezier.getX1());
                interpObj.addProperty("y1", bezier.getY1());
                interpObj.addProperty("x2", bezier.getX2());
                interpObj.addProperty("y2", bezier.getY2());
            }
            
            kfObj.add("interpolator", interpObj);
        }
        
        return kfObj;
    }
    
    /**
     * Serialize an animation event.
     */
    private JsonObject serializeEvent(AnimationEvent event) {
        JsonObject eventObj = new JsonObject();
        eventObj.addProperty("time", event.getTime());
        eventObj.addProperty("name", event.getName());
        
        if (event.getIntValue() != 0) {
            eventObj.addProperty("int", event.getIntValue());
        }
        if (event.getFloatValue() != 0) {
            eventObj.addProperty("float", event.getFloatValue());
        }
        if (event.getStringValue() != null && !event.getStringValue().isEmpty()) {
            eventObj.addProperty("string", event.getStringValue());
        }
        
        return eventObj;
    }
    
    /**
     * Serialize a deformable mesh.
     */
    public JsonObject serializeMesh(DeformableMesh mesh, Skeleton skeleton) {
        JsonObject meshObj = new JsonObject();
        meshObj.addProperty("name", mesh.getName());
        
        // Serialize vertices with bone weights
        JsonArray verticesArray = new JsonArray();
        List<Bone> bones = skeleton.getBonesInOrder();
        
        for (MeshVertex vertex : mesh.getVertices()) {
            JsonObject vObj = new JsonObject();
            vObj.addProperty("x", vertex.x);
            vObj.addProperty("y", vertex.y);
            vObj.addProperty("u", vertex.u);
            vObj.addProperty("v", vertex.v);
            
            // Bone weights
            int[] boneIndices = vertex.getBoneIndices();
            float[] boneWeights = vertex.getBoneWeights();
            int weightCount = vertex.getWeightCount();
            
            JsonArray weightsArray = new JsonArray();
            for (int i = 0; i < weightCount; i++) {
                JsonObject weightObj = new JsonObject();
                int boneIndex = boneIndices[i];
                if (boneIndex >= 0 && boneIndex < bones.size()) {
                    weightObj.addProperty("bone", bones.get(boneIndex).getName());
                } else {
                    weightObj.addProperty("boneIndex", boneIndex);
                }
                weightObj.addProperty("weight", boneWeights[i]);
                weightsArray.add(weightObj);
            }
            vObj.add("weights", weightsArray);
            
            verticesArray.add(vObj);
        }
        meshObj.add("vertices", verticesArray);
        
        // Serialize triangles
        JsonArray trianglesArray = new JsonArray();
        for (var triangle : mesh.getTriangles()) {
            JsonArray triArray = new JsonArray();
            triArray.add(triangle.v1);
            triArray.add(triangle.v2);
            triArray.add(triangle.v3);
            trianglesArray.add(triArray);
        }
        meshObj.add("triangles", trianglesArray);
        
        return meshObj;
    }
    
    /**
     * Export to Spine JSON format (simplified).
     */
    public String exportToSpineFormat(ProjectFile project) {
        JsonObject root = new JsonObject();
        
        // Skeleton info
        JsonObject skelInfo = new JsonObject();
        Skeleton skeleton = project.getSkeleton();
        if (skeleton != null) {
            skelInfo.addProperty("hash", skeleton.getName().hashCode());
            skelInfo.addProperty("spine", "3.8");
            skelInfo.addProperty("width", (double) project.getCanvasWidth());
            skelInfo.addProperty("height", (double) project.getCanvasHeight());
        }
        root.add("skeleton", skelInfo);
        
        // Bones
        if (skeleton != null) {
            JsonArray bonesArray = new JsonArray();
            for (Bone bone : skeleton.getBonesInOrder()) {
                JsonObject boneObj = new JsonObject();
                boneObj.addProperty("name", bone.getName());
                if (bone.getParent() != null) {
                    boneObj.addProperty("parent", bone.getParent().getName());
                }
                boneObj.addProperty("length", bone.getLength());
                boneObj.addProperty("x", bone.getX());
                boneObj.addProperty("y", bone.getY());
                boneObj.addProperty("rotation", bone.getRotation());
                bonesArray.add(boneObj);
            }
            root.add("bones", bonesArray);
        }
        
        // Animations
        JsonObject animations = new JsonObject();
        for (AnimationClip clip : project.getAnimations()) {
            animations.add(clip.getName(), exportClipToSpineFormat(clip));
        }
        root.add("animations", animations);
        
        return gson.toJson(root);
    }
    
    private JsonObject exportClipToSpineFormat(AnimationClip clip) {
        JsonObject animObj = new JsonObject();
        JsonObject bonesObj = new JsonObject();
        
        for (KeyframeTrack<Double> track : clip.getTracks()) {
            String boneName = track.getBoneName();
            String property = track.getPropertyName();
            
            JsonObject boneAnim = (JsonObject) bonesObj.get(boneName);
            if (boneAnim == null) {
                boneAnim = new JsonObject();
                bonesObj.add(boneName, boneAnim);
            }
            
            // Convert property name to Spine format
            String spineProp = convertToSpineProperty(property);
            
            JsonArray keyframes = new JsonArray();
            for (Keyframe<Double> kf : track.getKeyframes()) {
                JsonObject kfObj = new JsonObject();
                kfObj.addProperty("time", kf.getTime() / 30.0); // Convert frames to seconds
                
                if ("rotate".equals(spineProp)) {
                    kfObj.addProperty("angle", kf.getValue());
                } else {
                    kfObj.addProperty(property.equals("x") ? "x" : "y", kf.getValue());
                }
                
                keyframes.add(kfObj);
            }
            
            boneAnim.add(spineProp, keyframes);
        }
        
        animObj.add("bones", bonesObj);
        return animObj;
    }
    
    private String convertToSpineProperty(String property) {
        switch (property) {
            case "rotation": return "rotate";
            case "x":
            case "y": return "translate";
            case "scaleX":
            case "scaleY": return "scale";
            default: return property;
        }
    }
    
    /**
     * Get the interpolator type string.
     */
    public static String getInterpolatorType(Interpolator interpolator) {
        if (interpolator == null) {
            return "linear";
        }
        return interpolator.getType();
    }
}
