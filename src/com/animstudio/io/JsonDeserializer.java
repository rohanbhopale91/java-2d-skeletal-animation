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
import com.animstudio.core.model.Bone;
import com.animstudio.core.model.RegionAttachment;
import com.animstudio.core.model.Skeleton;
import com.animstudio.core.model.Slot;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Deserializes animation data from JSON format.
 * Supports our native format plus import from Spine/DragonBones compatible formats.
 */
public class JsonDeserializer {
    
    public JsonDeserializer() {
    }
    
    /**
     * Deserialize a project file from JSON string.
     */
    public ProjectFile deserialize(String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        
        ProjectFile project = new ProjectFile();
        
        // Version check
        if (root.has("version")) {
            project.setFormatVersion(root.get("version").getAsInt());
        }
        
        // Basic metadata
        if (root.has("name")) {
            project.setName(root.get("name").getAsString());
        }
        if (root.has("author")) {
            project.setAuthor(root.get("author").getAsString());
        }
        if (root.has("description")) {
            project.setDescription(root.get("description").getAsString());
        }
        if (root.has("createdAt")) {
            project.setCreatedAt(LocalDateTime.parse(root.get("createdAt").getAsString()));
        }
        if (root.has("modifiedAt")) {
            project.setModifiedAt(LocalDateTime.parse(root.get("modifiedAt").getAsString()));
        }
        
        // Settings
        if (root.has("settings")) {
            deserializeSettings(root.getAsJsonObject("settings"), project);
        }
        
        // Skeleton
        if (root.has("skeleton")) {
            project.setSkeleton(deserializeSkeleton(root.getAsJsonObject("skeleton")));
        }
        
        // Animation clips
        if (root.has("animations")) {
            JsonArray clips = root.getAsJsonArray("animations");
            for (JsonElement elem : clips) {
                project.addAnimation(deserializeAnimationClip(elem.getAsJsonObject()));
            }
        }
        
        // Texture paths
        if (root.has("textures")) {
            JsonObject textures = root.getAsJsonObject("textures");
            for (Map.Entry<String, JsonElement> entry : textures.entrySet()) {
                project.addTexture(entry.getKey(), entry.getValue().getAsString());
            }
        }
        
        return project;
    }
    
    /**
     * Deserialize from file.
     */
    public ProjectFile deserializeFromFile(File file) throws IOException {
        try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            StringBuilder sb = new StringBuilder();
            char[] buffer = new char[4096];
            int read;
            while ((read = reader.read(buffer)) != -1) {
                sb.append(buffer, 0, read);
            }
            return deserialize(sb.toString());
        }
    }
    
    /**
     * Deserialize project settings.
     */
    private void deserializeSettings(JsonObject settings, ProjectFile project) {
        if (settings.has("frameRate")) {
            project.setFrameRate(settings.get("frameRate").getAsInt());
        }
        if (settings.has("canvasWidth")) {
            project.setCanvasWidth(settings.get("canvasWidth").getAsInt());
        }
        if (settings.has("canvasHeight")) {
            project.setCanvasHeight(settings.get("canvasHeight").getAsInt());
        }
        if (settings.has("defaultDuration")) {
            project.setDefaultDuration(settings.get("defaultDuration").getAsDouble());
        }
    }
    
    /**
     * Deserialize a skeleton.
     */
    public Skeleton deserializeSkeleton(JsonObject skelObj) {
        String name = skelObj.has("name") ? skelObj.get("name").getAsString() : "Skeleton";
        Skeleton skeleton = new Skeleton(name);
        
        // First pass: create all bones
        Map<String, Bone> boneMap = new HashMap<>();
        if (skelObj.has("bones")) {
            JsonArray bones = skelObj.getAsJsonArray("bones");
            for (JsonElement elem : bones) {
                JsonObject boneObj = elem.getAsJsonObject();
                Bone bone = deserializeBone(boneObj);
                boneMap.put(bone.getName(), bone);
            }
        }
        
        // Second pass: set up hierarchy
        if (skelObj.has("bones")) {
            JsonArray bones = skelObj.getAsJsonArray("bones");
            for (JsonElement elem : bones) {
                JsonObject boneObj = elem.getAsJsonObject();
                String boneName = boneObj.get("name").getAsString();
                Bone bone = boneMap.get(boneName);
                
                if (boneObj.has("parent")) {
                    String parentName = boneObj.get("parent").getAsString();
                    Bone parent = boneMap.get(parentName);
                    if (parent != null) {
                        bone.setParent(parent);
                    }
                }
            }
        }
        
        // Add bones to skeleton (roots first)
        for (Bone bone : boneMap.values()) {
            if (bone.getParent() == null) {
                skeleton.addBone(bone);
            }
        }
        for (Bone bone : boneMap.values()) {
            if (bone.getParent() != null) {
                try {
                    skeleton.addBone(bone);
                } catch (IllegalArgumentException e) {
                    // Bone already added
                }
            }
        }
        
        // Deserialize slots
        if (skelObj.has("slots")) {
            JsonArray slots = skelObj.getAsJsonArray("slots");
            for (JsonElement elem : slots) {
                JsonObject slotObj = elem.getAsJsonObject();
                Slot slot = deserializeSlot(slotObj, skeleton);
                if (slot != null) {
                    skeleton.addSlot(slot);
                }
            }
        }
        
        return skeleton;
    }
    
    /**
     * Deserialize a bone.
     */
    private Bone deserializeBone(JsonObject boneObj) {
        String name = boneObj.get("name").getAsString();
        Bone bone = new Bone(name);
        
        // Local transform - use setX(), setY() etc.
        if (boneObj.has("x")) {
            bone.setX(boneObj.get("x").getAsDouble());
        }
        if (boneObj.has("y")) {
            bone.setY(boneObj.get("y").getAsDouble());
        }
        if (boneObj.has("rotation")) {
            bone.setRotation(boneObj.get("rotation").getAsDouble());
        }
        if (boneObj.has("scaleX")) {
            bone.setScaleX(boneObj.get("scaleX").getAsDouble());
        }
        if (boneObj.has("scaleY")) {
            bone.setScaleY(boneObj.get("scaleY").getAsDouble());
        }
        
        if (boneObj.has("length")) {
            bone.setLength(boneObj.get("length").getAsDouble());
        }
        if (boneObj.has("drawOrder")) {
            bone.setDrawOrder(boneObj.get("drawOrder").getAsInt());
        }
        if (boneObj.has("inheritRotation")) {
            bone.setInheritRotation(boneObj.get("inheritRotation").getAsBoolean());
        }
        if (boneObj.has("inheritScale")) {
            bone.setInheritScale(boneObj.get("inheritScale").getAsBoolean());
        }
        if (boneObj.has("color")) {
            bone.setColor(boneObj.get("color").getAsInt());
        }
        
        return bone;
    }
    
    /**
     * Deserialize a slot.
     */
    private Slot deserializeSlot(JsonObject slotObj, Skeleton skeleton) {
        String name = slotObj.get("name").getAsString();
        String boneName = slotObj.get("bone").getAsString();
        
        Bone bone = skeleton.getBone(boneName);
        if (bone == null) {
            return null;
        }
        
        Slot slot = new Slot(name, bone);
        
        if (slotObj.has("drawOrder")) {
            slot.setDrawOrder(slotObj.get("drawOrder").getAsInt());
        }
        if (slotObj.has("blendMode")) {
            slot.setBlendMode(Slot.BlendMode.valueOf(slotObj.get("blendMode").getAsString()));
        }
        
        // Color - use setRed(), setGreen(), etc.
        if (slotObj.has("red")) slot.setRed(slotObj.get("red").getAsFloat());
        if (slotObj.has("green")) slot.setGreen(slotObj.get("green").getAsFloat());
        if (slotObj.has("blue")) slot.setBlue(slotObj.get("blue").getAsFloat());
        if (slotObj.has("alpha")) slot.setAlpha(slotObj.get("alpha").getAsFloat());
        
        // Attachment
        if (slotObj.has("attachment")) {
            slot.setAttachment(deserializeAttachment(slotObj.getAsJsonObject("attachment")));
        }
        
        return slot;
    }
    
    /**
     * Deserialize an attachment.
     */
    private RegionAttachment deserializeAttachment(JsonObject attObj) {
        String name = attObj.get("name").getAsString();
        String type = attObj.has("type") ? attObj.get("type").getAsString() : "region";
        
        if ("region".equals(type)) {
            RegionAttachment region = new RegionAttachment(name);
            
            if (attObj.has("x")) region.setX(attObj.get("x").getAsDouble());
            if (attObj.has("y")) region.setY(attObj.get("y").getAsDouble());
            if (attObj.has("rotation")) region.setRotation(attObj.get("rotation").getAsDouble());
            if (attObj.has("scaleX")) region.setScaleX(attObj.get("scaleX").getAsDouble());
            if (attObj.has("scaleY")) region.setScaleY(attObj.get("scaleY").getAsDouble());
            if (attObj.has("width")) region.setWidth(attObj.get("width").getAsDouble());
            if (attObj.has("height")) region.setHeight(attObj.get("height").getAsDouble());
            if (attObj.has("regionName")) region.setRegionName(attObj.get("regionName").getAsString());
            if (attObj.has("regionX")) region.setRegionX(attObj.get("regionX").getAsInt());
            if (attObj.has("regionY")) region.setRegionY(attObj.get("regionY").getAsInt());
            if (attObj.has("regionWidth")) region.setRegionWidth(attObj.get("regionWidth").getAsInt());
            if (attObj.has("regionHeight")) region.setRegionHeight(attObj.get("regionHeight").getAsInt());
            if (attObj.has("pivotX")) region.setPivotX(attObj.get("pivotX").getAsDouble());
            if (attObj.has("pivotY")) region.setPivotY(attObj.get("pivotY").getAsDouble());
            
            return region;
        }
        
        return null;
    }
    
    /**
     * Deserialize an animation clip.
     */
    public AnimationClip deserializeAnimationClip(JsonObject clipObj) {
        String name = clipObj.has("name") ? clipObj.get("name").getAsString() : "Animation";
        
        AnimationClip clip = new AnimationClip(name);
        
        if (clipObj.has("duration")) {
            clip.setDuration(clipObj.get("duration").getAsDouble());
        }
        if (clipObj.has("looping")) {
            clip.setLooping(clipObj.get("looping").getAsBoolean());
        }
        
        // Deserialize tracks
        if (clipObj.has("tracks")) {
            JsonArray tracks = clipObj.getAsJsonArray("tracks");
            for (JsonElement elem : tracks) {
                deserializeTrack(elem.getAsJsonObject(), clip);
            }
        }
        
        // Deserialize events
        if (clipObj.has("events")) {
            JsonArray events = clipObj.getAsJsonArray("events");
            for (JsonElement elem : events) {
                clip.addEvent(deserializeEvent(elem.getAsJsonObject()));
            }
        }
        
        return clip;
    }
    
    /**
     * Deserialize a keyframe track.
     */
    private void deserializeTrack(JsonObject trackObj, AnimationClip clip) {
        String targetPath = trackObj.get("targetPath").getAsString();
        String propertyTypeName = trackObj.has("propertyType") ? 
            trackObj.get("propertyType").getAsString() : "TRANSLATION";
        
        KeyframeTrack.PropertyType propertyType;
        try {
            propertyType = KeyframeTrack.PropertyType.valueOf(propertyTypeName);
        } catch (IllegalArgumentException e) {
            propertyType = KeyframeTrack.PropertyType.TRANSLATION;
        }
        
        KeyframeTrack<Double> track = clip.getOrCreateTrack(targetPath, propertyType);
        
        // Deserialize keyframes - use setKeyframe instead of addKeyframe
        if (trackObj.has("keyframes")) {
            JsonArray keyframes = trackObj.getAsJsonArray("keyframes");
            for (JsonElement elem : keyframes) {
                JsonObject kfObj = elem.getAsJsonObject();
                double time = kfObj.get("time").getAsDouble();
                double value = kfObj.get("value").getAsDouble();
                
                Interpolator interp = LinearInterpolator.INSTANCE;
                if (kfObj.has("interpolator")) {
                    interp = deserializeInterpolator(kfObj.getAsJsonObject("interpolator"));
                }
                
                track.setKeyframe(time, value, interp);
            }
        }
    }
    
    /**
     * Deserialize an interpolator.
     */
    private Interpolator deserializeInterpolator(JsonObject interpObj) {
        String type = interpObj.has("type") ? interpObj.get("type").getAsString() : "linear";
        
        switch (type.toLowerCase()) {
            case "stepped":
            case "step":
                return SteppedInterpolator.INSTANCE;
            
            case "bezier":
                double x1 = interpObj.has("x1") ? interpObj.get("x1").getAsDouble() : 0.25;
                double y1 = interpObj.has("y1") ? interpObj.get("y1").getAsDouble() : 0.1;
                double x2 = interpObj.has("x2") ? interpObj.get("x2").getAsDouble() : 0.25;
                double y2 = interpObj.has("y2") ? interpObj.get("y2").getAsDouble() : 1.0;
                return new BezierInterpolator(x1, y1, x2, y2);
            
            case "ease-in":
            case "easein":
                return BezierInterpolator.EASE_IN;
            
            case "ease-out":
            case "easeout":
                return BezierInterpolator.EASE_OUT;
            
            case "ease-in-out":
            case "easeinout":
                return BezierInterpolator.EASE_IN_OUT;
            
            case "linear":
            default:
                return LinearInterpolator.INSTANCE;
        }
    }
    
    /**
     * Deserialize an animation event.
     */
    private AnimationEvent deserializeEvent(JsonObject eventObj) {
        double time = eventObj.get("time").getAsDouble();
        String name = eventObj.get("name").getAsString();
        
        AnimationEvent event = new AnimationEvent(name, time);
        
        if (eventObj.has("int")) {
            event.setIntValue(eventObj.get("int").getAsInt());
        }
        if (eventObj.has("float")) {
            event.setFloatValue(eventObj.get("float").getAsFloat());
        }
        if (eventObj.has("string")) {
            event.setStringValue(eventObj.get("string").getAsString());
        }
        
        return event;
    }
    
    /**
     * Deserialize a deformable mesh.
     */
    public DeformableMesh deserializeMesh(JsonObject meshObj, Skeleton skeleton) {
        String name = meshObj.has("name") ? meshObj.get("name").getAsString() : "Mesh";
        DeformableMesh mesh = new DeformableMesh(name);
        
        // Build bone name to index map
        Map<String, Integer> boneIndexMap = new HashMap<>();
        int index = 0;
        for (Bone bone : skeleton.getBonesInOrder()) {
            boneIndexMap.put(bone.getName(), index++);
        }
        
        // Deserialize vertices
        if (meshObj.has("vertices")) {
            JsonArray vertices = meshObj.getAsJsonArray("vertices");
            for (JsonElement elem : vertices) {
                JsonObject vObj = elem.getAsJsonObject();
                
                float x = vObj.get("x").getAsFloat();
                float y = vObj.get("y").getAsFloat();
                float u = vObj.has("u") ? vObj.get("u").getAsFloat() : 0;
                float v = vObj.has("v") ? vObj.get("v").getAsFloat() : 0;
                
                // Collect bone weights
                List<Integer> boneIndices = new ArrayList<>();
                List<Float> weights = new ArrayList<>();
                
                if (vObj.has("weights")) {
                    JsonArray weightsArray = vObj.getAsJsonArray("weights");
                    for (JsonElement wElem : weightsArray) {
                        JsonObject wObj = wElem.getAsJsonObject();
                        int boneIdx;
                        if (wObj.has("bone")) {
                            String boneName = wObj.get("bone").getAsString();
                            boneIdx = boneIndexMap.getOrDefault(boneName, -1);
                        } else {
                            boneIdx = wObj.get("boneIndex").getAsInt();
                        }
                        float weight = wObj.get("weight").getAsFloat();
                        
                        if (boneIdx >= 0) {
                            boneIndices.add(boneIdx);
                            weights.add(weight);
                        }
                    }
                }
                
                // Convert to arrays
                int[] boneIdxArray = boneIndices.stream().mapToInt(Integer::intValue).toArray();
                float[] weightArray = new float[weights.size()];
                for (int i = 0; i < weights.size(); i++) {
                    weightArray[i] = weights.get(i);
                }
                
                // Add vertex with weights
                if (boneIdxArray.length > 0) {
                    mesh.addVertex(x, y, u, v, boneIdxArray, weightArray);
                } else {
                    mesh.addVertex(x, y, u, v);
                }
            }
        }
        
        // Deserialize triangles
        if (meshObj.has("triangles")) {
            JsonArray triangles = meshObj.getAsJsonArray("triangles");
            for (JsonElement elem : triangles) {
                JsonArray triArray = elem.getAsJsonArray();
                int i0 = triArray.get(0).getAsInt();
                int i1 = triArray.get(1).getAsInt();
                int i2 = triArray.get(2).getAsInt();
                mesh.addTriangle(i0, i1, i2);
            }
        }
        
        return mesh;
    }
    
    /**
     * Import from Spine JSON format.
     */
    public ProjectFile importFromSpine(String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        ProjectFile project = new ProjectFile();
        
        // Skeleton info
        if (root.has("skeleton")) {
            JsonObject skelInfo = root.getAsJsonObject("skeleton");
            if (skelInfo.has("width")) {
                project.setCanvasWidth((int) skelInfo.get("width").getAsDouble());
            }
            if (skelInfo.has("height")) {
                project.setCanvasHeight((int) skelInfo.get("height").getAsDouble());
            }
        }
        
        // Create skeleton
        Skeleton skeleton = new Skeleton("Imported");
        Map<String, Bone> boneMap = new HashMap<>();
        
        // Import bones
        if (root.has("bones")) {
            JsonArray bones = root.getAsJsonArray("bones");
            
            // First pass: create bones
            for (JsonElement elem : bones) {
                JsonObject boneObj = elem.getAsJsonObject();
                String name = boneObj.get("name").getAsString();
                Bone bone = new Bone(name);
                
                if (boneObj.has("length")) bone.setLength(boneObj.get("length").getAsDouble());
                if (boneObj.has("x")) bone.setX(boneObj.get("x").getAsDouble());
                if (boneObj.has("y")) bone.setY(boneObj.get("y").getAsDouble());
                if (boneObj.has("rotation")) bone.setRotation(boneObj.get("rotation").getAsDouble());
                if (boneObj.has("scaleX")) bone.setScaleX(boneObj.get("scaleX").getAsDouble());
                if (boneObj.has("scaleY")) bone.setScaleY(boneObj.get("scaleY").getAsDouble());
                
                boneMap.put(name, bone);
            }
            
            // Second pass: setup hierarchy
            for (JsonElement elem : bones) {
                JsonObject boneObj = elem.getAsJsonObject();
                String name = boneObj.get("name").getAsString();
                
                if (boneObj.has("parent")) {
                    String parentName = boneObj.get("parent").getAsString();
                    Bone bone = boneMap.get(name);
                    Bone parent = boneMap.get(parentName);
                    if (bone != null && parent != null) {
                        bone.setParent(parent);
                    }
                }
            }
            
            // Add to skeleton
            for (Bone bone : boneMap.values()) {
                skeleton.addBone(bone);
            }
        }
        
        project.setSkeleton(skeleton);
        
        // Import animations
        if (root.has("animations")) {
            JsonObject animations = root.getAsJsonObject("animations");
            for (Map.Entry<String, JsonElement> entry : animations.entrySet()) {
                String animName = entry.getKey();
                AnimationClip clip = importSpineAnimation(animName, entry.getValue().getAsJsonObject());
                project.addAnimation(clip);
            }
        }
        
        return project;
    }
    
    private AnimationClip importSpineAnimation(String name, JsonObject animObj) {
        AnimationClip clip = new AnimationClip(name);
        double maxTime = 0;
        
        // Import bone animations
        if (animObj.has("bones")) {
            JsonObject bones = animObj.getAsJsonObject("bones");
            for (Map.Entry<String, JsonElement> boneEntry : bones.entrySet()) {
                String boneName = boneEntry.getKey();
                JsonObject boneAnim = boneEntry.getValue().getAsJsonObject();
                
                // Rotation
                if (boneAnim.has("rotate")) {
                    String targetPath = boneName + ".rotation";
                    KeyframeTrack<Double> track = clip.getOrCreateTrack(targetPath, 
                        KeyframeTrack.PropertyType.ROTATION);
                    
                    JsonArray rotations = boneAnim.getAsJsonArray("rotate");
                    for (JsonElement elem : rotations) {
                        JsonObject kf = elem.getAsJsonObject();
                        double time = kf.get("time").getAsDouble() * 30; // seconds to frames
                        double angle = kf.has("angle") ? kf.get("angle").getAsDouble() : 0;
                        track.setKeyframe(time, angle, LinearInterpolator.INSTANCE);
                        maxTime = Math.max(maxTime, time);
                    }
                }
                
                // Translation
                if (boneAnim.has("translate")) {
                    String targetPathX = boneName + ".x";
                    String targetPathY = boneName + ".y";
                    KeyframeTrack<Double> trackX = clip.getOrCreateTrack(targetPathX, 
                        KeyframeTrack.PropertyType.TRANSLATION);
                    KeyframeTrack<Double> trackY = clip.getOrCreateTrack(targetPathY, 
                        KeyframeTrack.PropertyType.TRANSLATION);
                    
                    JsonArray translations = boneAnim.getAsJsonArray("translate");
                    for (JsonElement elem : translations) {
                        JsonObject kf = elem.getAsJsonObject();
                        double time = kf.get("time").getAsDouble() * 30;
                        double x = kf.has("x") ? kf.get("x").getAsDouble() : 0;
                        double y = kf.has("y") ? kf.get("y").getAsDouble() : 0;
                        trackX.setKeyframe(time, x, LinearInterpolator.INSTANCE);
                        trackY.setKeyframe(time, y, LinearInterpolator.INSTANCE);
                        maxTime = Math.max(maxTime, time);
                    }
                }
                
                // Scale
                if (boneAnim.has("scale")) {
                    String targetPathX = boneName + ".scaleX";
                    String targetPathY = boneName + ".scaleY";
                    KeyframeTrack<Double> trackX = clip.getOrCreateTrack(targetPathX, 
                        KeyframeTrack.PropertyType.SCALE);
                    KeyframeTrack<Double> trackY = clip.getOrCreateTrack(targetPathY, 
                        KeyframeTrack.PropertyType.SCALE);
                    
                    JsonArray scales = boneAnim.getAsJsonArray("scale");
                    for (JsonElement elem : scales) {
                        JsonObject kf = elem.getAsJsonObject();
                        double time = kf.get("time").getAsDouble() * 30;
                        double sx = kf.has("x") ? kf.get("x").getAsDouble() : 1;
                        double sy = kf.has("y") ? kf.get("y").getAsDouble() : 1;
                        trackX.setKeyframe(time, sx, LinearInterpolator.INSTANCE);
                        trackY.setKeyframe(time, sy, LinearInterpolator.INSTANCE);
                        maxTime = Math.max(maxTime, time);
                    }
                }
            }
        }
        
        clip.setDuration(Math.max(60, maxTime + 1));
        return clip;
    }
}
