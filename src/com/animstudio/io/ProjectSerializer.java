package com.animstudio.io;

import com.animstudio.core.animation.AnimationClip;
import com.animstudio.core.animation.BoneKeyframe;
import com.animstudio.core.animation.Keyframe;
import com.animstudio.core.animation.KeyframeTrack;
import com.animstudio.core.ik.IKConstraint;
import com.animstudio.core.ik.IKManager;
import com.animstudio.core.mesh.DeformableMesh;
import com.animstudio.core.mesh.MeshTriangle;
import com.animstudio.core.mesh.MeshVertex;
import com.animstudio.core.model.Bone;
import com.animstudio.core.model.Skeleton;
import com.animstudio.editor.project.AnimationProject;
import com.google.gson.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Serializes and deserializes AnimationProject to/from JSON files.
 */
public class ProjectSerializer {
    
    private final Gson gson;
    
    public ProjectSerializer() {
        this.gson = new GsonBuilder()
            .setPrettyPrinting()
            .serializeNulls()
            .create();
    }
    
    /**
     * Save a project to a file.
     */
    public void save(AnimationProject project, Path file) throws IOException {
        JsonObject root = new JsonObject();
        root.addProperty("name", project.getName());
        root.addProperty("version", project.getVersion());
        root.addProperty("canvasWidth", project.getCanvasWidth());
        root.addProperty("canvasHeight", project.getCanvasHeight());
        root.addProperty("frameRate", project.getFrameRate());
        
        // Serialize skeleton
        if (project.getSkeleton() != null) {
            root.add("skeleton", serializeSkeleton(project.getSkeleton()));
        }
        
        // Serialize animations
        JsonArray anims = new JsonArray();
        for (AnimationClip clip : project.getAnimations()) {
            anims.add(serializeAnimation(clip));
        }
        root.add("animations", anims);
        
        // Write to file
        try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            gson.toJson(root, writer);
        }
    }
    
    /**
     * Load a project from a file.
     */
    public AnimationProject load(Path file) throws IOException {
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            
            AnimationProject project = new AnimationProject();
            
            if (root.has("name")) {
                project.setName(root.get("name").getAsString());
            }
            if (root.has("version")) {
                project.setVersion(root.get("version").getAsString());
            }
            if (root.has("canvasWidth")) {
                project.setCanvasWidth(root.get("canvasWidth").getAsInt());
            }
            if (root.has("canvasHeight")) {
                project.setCanvasHeight(root.get("canvasHeight").getAsInt());
            }
            if (root.has("frameRate")) {
                project.setFrameRate(root.get("frameRate").getAsDouble());
            }
            
            // Load skeleton
            if (root.has("skeleton")) {
                project.setSkeleton(deserializeSkeleton(root.getAsJsonObject("skeleton")));
            }
            
            // Load animations
            if (root.has("animations")) {
                JsonArray anims = root.getAsJsonArray("animations");
                for (JsonElement elem : anims) {
                    project.addAnimation(deserializeAnimation(elem.getAsJsonObject()));
                }
            }
            
            return project;
        }
    }
    
    private JsonObject serializeSkeleton(Skeleton skeleton) {
        JsonObject obj = new JsonObject();
        obj.addProperty("name", skeleton.getName());
        
        JsonArray bones = new JsonArray();
        for (Bone bone : skeleton.getBones()) {
            bones.add(serializeBone(bone));
        }
        obj.add("bones", bones);
        
        // Serialize IK constraints
        IKManager ikManager = skeleton.getIKManager();
        if (ikManager != null && !ikManager.getConstraints().isEmpty()) {
            JsonArray ikConstraints = new JsonArray();
            for (IKConstraint constraint : ikManager.getConstraints()) {
                ikConstraints.add(serializeIKConstraint(constraint));
            }
            obj.add("ikConstraints", ikConstraints);
        }
        
        // Serialize meshes
        List<DeformableMesh> meshes = skeleton.getMeshes();
        if (meshes != null && !meshes.isEmpty()) {
            JsonArray meshArray = new JsonArray();
            for (DeformableMesh mesh : meshes) {
                meshArray.add(serializeMesh(mesh));
            }
            obj.add("meshes", meshArray);
        }
        
        return obj;
    }
    
    private JsonObject serializeIKConstraint(IKConstraint constraint) {
        JsonObject obj = new JsonObject();
        obj.addProperty("name", constraint.getName());
        obj.addProperty("mix", constraint.getMix());
        obj.addProperty("bendPositive", constraint.isBendPositive());
        obj.addProperty("stretch", constraint.isStretch());
        obj.addProperty("compress", constraint.isCompress());
        obj.addProperty("softness", constraint.getSoftness());
        obj.addProperty("maxIterations", constraint.getMaxIterations());
        
        // Bone chain
        JsonArray boneNames = new JsonArray();
        for (Bone bone : constraint.getBones()) {
            boneNames.add(bone.getName());
        }
        obj.add("bones", boneNames);
        
        // Target
        if (constraint.getTarget() != null) {
            obj.addProperty("target", constraint.getTarget().getName());
        }
        
        return obj;
    }
    
    private JsonObject serializeMesh(DeformableMesh mesh) {
        JsonObject obj = new JsonObject();
        obj.addProperty("name", mesh.getName());
        
        if (mesh.getTexturePath() != null) {
            obj.addProperty("texturePath", mesh.getTexturePath());
        }
        obj.addProperty("textureWidth", mesh.getTextureWidth());
        obj.addProperty("textureHeight", mesh.getTextureHeight());
        
        // Vertices
        JsonArray vertices = new JsonArray();
        for (MeshVertex v : mesh.getVertices()) {
            JsonObject vObj = new JsonObject();
            vObj.addProperty("x", v.x);
            vObj.addProperty("y", v.y);
            vObj.addProperty("u", v.u);
            vObj.addProperty("v", v.v);
            
            // Bone weights
            if (v.getWeightCount() > 0) {
                JsonArray weights = new JsonArray();
                int[] boneIndices = v.getBoneIndices();
                float[] boneWeights = v.getBoneWeights();
                for (int i = 0; i < v.getWeightCount(); i++) {
                    JsonObject wObj = new JsonObject();
                    wObj.addProperty("bone", boneIndices[i]);
                    wObj.addProperty("weight", boneWeights[i]);
                    weights.add(wObj);
                }
                vObj.add("weights", weights);
            }
            vertices.add(vObj);
        }
        obj.add("vertices", vertices);
        
        // Triangles
        JsonArray triangles = new JsonArray();
        for (MeshTriangle t : mesh.getTriangles()) {
            JsonArray tri = new JsonArray();
            tri.add(t.v1);
            tri.add(t.v2);
            tri.add(t.v3);
            triangles.add(tri);
        }
        obj.add("triangles", triangles);
        
        return obj;
    }
    
    private JsonObject serializeBone(Bone bone) {
        JsonObject obj = new JsonObject();
        obj.addProperty("name", bone.getName());
        obj.addProperty("x", bone.getX());
        obj.addProperty("y", bone.getY());
        obj.addProperty("rotation", bone.getRotation());
        obj.addProperty("scaleX", bone.getScaleX());
        obj.addProperty("scaleY", bone.getScaleY());
        obj.addProperty("length", bone.getLength());
        if (bone.getParent() != null) {
            obj.addProperty("parent", bone.getParent().getName());
        }
        return obj;
    }
    
    private Skeleton deserializeSkeleton(JsonObject obj) {
        String name = obj.has("name") ? obj.get("name").getAsString() : "Skeleton";
        Skeleton skeleton = new Skeleton(name);
        
        if (obj.has("bones")) {
            JsonArray bones = obj.getAsJsonArray("bones");
            List<JsonObject> boneObjects = new ArrayList<>();
            
            // First pass: create all bones
            for (JsonElement elem : bones) {
                JsonObject boneObj = elem.getAsJsonObject();
                boneObjects.add(boneObj);
                String boneName = boneObj.get("name").getAsString();
                Bone bone = new Bone(boneName);
                
                if (boneObj.has("x")) bone.setX(boneObj.get("x").getAsDouble());
                if (boneObj.has("y")) bone.setY(boneObj.get("y").getAsDouble());
                if (boneObj.has("rotation")) bone.setRotation(boneObj.get("rotation").getAsDouble());
                if (boneObj.has("scaleX")) bone.setScaleX(boneObj.get("scaleX").getAsDouble());
                if (boneObj.has("scaleY")) bone.setScaleY(boneObj.get("scaleY").getAsDouble());
                if (boneObj.has("length")) bone.setLength(boneObj.get("length").getAsDouble());
                
                bone.setToSetupPose();
                skeleton.addBone(bone);
            }
            
            // Second pass: setup parent relationships
            List<Bone> boneList = skeleton.getBonesInOrder();
            for (int i = 0; i < boneObjects.size(); i++) {
                JsonObject boneObj = boneObjects.get(i);
                if (boneObj.has("parent")) {
                    String parentName = boneObj.get("parent").getAsString();
                    Bone parent = skeleton.getBone(parentName);
                    Bone bone = boneList.get(i);
                    if (parent != null) {
                        bone.setParent(parent);
                    }
                }
            }
        }
        
        skeleton.updateWorldTransforms();
        
        // Load IK constraints (after bones are set up)
        if (obj.has("ikConstraints")) {
            JsonArray ikConstraints = obj.getAsJsonArray("ikConstraints");
            for (JsonElement elem : ikConstraints) {
                IKConstraint constraint = deserializeIKConstraint(elem.getAsJsonObject(), skeleton);
                if (constraint != null) {
                    skeleton.getIKManager().addConstraint(constraint);
                }
            }
        }
        
        // Load meshes
        if (obj.has("meshes")) {
            JsonArray meshes = obj.getAsJsonArray("meshes");
            for (JsonElement elem : meshes) {
                DeformableMesh mesh = deserializeMesh(elem.getAsJsonObject());
                if (mesh != null) {
                    mesh.setSkeleton(skeleton);
                    skeleton.addMesh(mesh);
                }
            }
        }
        
        return skeleton;
    }
    
    private IKConstraint deserializeIKConstraint(JsonObject obj, Skeleton skeleton) {
        String constraintName = obj.has("name") ? obj.get("name").getAsString() : "IK Constraint";
        IKConstraint constraint = new IKConstraint(constraintName);
        
        if (obj.has("mix")) constraint.setMix((float) obj.get("mix").getAsDouble());
        if (obj.has("bendPositive")) constraint.setBendPositive(obj.get("bendPositive").getAsBoolean());
        if (obj.has("stretch")) constraint.setStretch(obj.get("stretch").getAsBoolean());
        if (obj.has("compress")) constraint.setCompress(obj.get("compress").getAsBoolean());
        if (obj.has("softness")) constraint.setSoftness((float) obj.get("softness").getAsDouble());
        if (obj.has("maxIterations")) constraint.setMaxIterations(obj.get("maxIterations").getAsInt());
        
        // Load bone chain
        if (obj.has("bones")) {
            JsonArray boneNames = obj.getAsJsonArray("bones");
            for (JsonElement elem : boneNames) {
                Bone bone = skeleton.getBone(elem.getAsString());
                if (bone != null) {
                    constraint.addBone(bone);
                }
            }
        }
        
        // Load target
        if (obj.has("target")) {
            Bone target = skeleton.getBone(obj.get("target").getAsString());
            if (target != null) {
                constraint.setTarget(target);
            }
        }
        
        return constraint;
    }
    
    private DeformableMesh deserializeMesh(JsonObject obj) {
        String meshName = obj.has("name") ? obj.get("name").getAsString() : "Mesh";
        DeformableMesh mesh = new DeformableMesh(meshName);
        
        if (obj.has("texturePath")) {
            mesh.setTexturePath(obj.get("texturePath").getAsString());
        }
        if (obj.has("textureWidth")) {
            mesh.setTextureWidth(obj.get("textureWidth").getAsInt());
        }
        if (obj.has("textureHeight")) {
            mesh.setTextureHeight(obj.get("textureHeight").getAsInt());
        }
        
        // Load vertices
        if (obj.has("vertices")) {
            JsonArray vertices = obj.getAsJsonArray("vertices");
            for (JsonElement elem : vertices) {
                JsonObject vObj = elem.getAsJsonObject();
                float x = vObj.get("x").getAsFloat();
                float y = vObj.get("y").getAsFloat();
                float u = vObj.has("u") ? vObj.get("u").getAsFloat() : 0;
                float v = vObj.has("v") ? vObj.get("v").getAsFloat() : 0;
                
                if (vObj.has("weights")) {
                    JsonArray weights = vObj.getAsJsonArray("weights");
                    int[] boneIndices = new int[weights.size()];
                    float[] boneWeights = new float[weights.size()];
                    for (int i = 0; i < weights.size(); i++) {
                        JsonObject wObj = weights.get(i).getAsJsonObject();
                        boneIndices[i] = wObj.get("bone").getAsInt();
                        boneWeights[i] = wObj.get("weight").getAsFloat();
                    }
                    mesh.addVertex(x, y, u, v, boneIndices, boneWeights);
                } else {
                    mesh.addVertex(x, y, u, v);
                }
            }
        }
        
        // Load triangles
        if (obj.has("triangles")) {
            JsonArray triangles = obj.getAsJsonArray("triangles");
            for (JsonElement elem : triangles) {
                JsonArray tri = elem.getAsJsonArray();
                int v1 = tri.get(0).getAsInt();
                int v2 = tri.get(1).getAsInt();
                int v3 = tri.get(2).getAsInt();
                mesh.addTriangle(v1, v2, v3);
            }
        }
        
        return mesh;
    }
    
    private JsonObject serializeAnimation(AnimationClip clip) {
        JsonObject obj = new JsonObject();
        obj.addProperty("name", clip.getName());
        obj.addProperty("duration", clip.getDuration());
        obj.addProperty("looping", clip.isLooping());
        
        JsonArray timelines = new JsonArray();
        for (String boneName : clip.getBoneNames()) {
            AnimationClip.BoneKeyframes boneKeyframes = clip.getTimeline(boneName);
            if (boneKeyframes != null) {
                timelines.add(serializeTimeline(boneName, boneKeyframes));
            }
        }
        obj.add("timelines", timelines);
        
        return obj;
    }
    
    private JsonObject serializeTimeline(String boneName, AnimationClip.BoneKeyframes timeline) {
        JsonObject obj = new JsonObject();
        obj.addProperty("bone", boneName);
        
        JsonArray keyframes = new JsonArray();
        for (BoneKeyframe kf : timeline.getKeyframes()) {
            JsonObject kfObj = new JsonObject();
            kfObj.addProperty("frame", kf.getFrame());
            if (kf.getX() != null) kfObj.addProperty("x", kf.getX());
            if (kf.getY() != null) kfObj.addProperty("y", kf.getY());
            if (kf.getRotation() != null) kfObj.addProperty("rotation", kf.getRotation());
            if (kf.getScaleX() != null) kfObj.addProperty("scaleX", kf.getScaleX());
            if (kf.getScaleY() != null) kfObj.addProperty("scaleY", kf.getScaleY());
            keyframes.add(kfObj);
        }
        obj.add("keyframes", keyframes);
        
        return obj;
    }
    
    private AnimationClip deserializeAnimation(JsonObject obj) {
        String name = obj.has("name") ? obj.get("name").getAsString() : "animation";
        AnimationClip clip = new AnimationClip(name);
        
        if (obj.has("duration")) {
            clip.setDuration(obj.get("duration").getAsDouble());
        }
        if (obj.has("looping")) {
            clip.setLooping(obj.get("looping").getAsBoolean());
        }
        
        if (obj.has("timelines")) {
            JsonArray timelines = obj.getAsJsonArray("timelines");
            for (JsonElement elem : timelines) {
                JsonObject tlObj = elem.getAsJsonObject();
                String boneName = tlObj.get("bone").getAsString();
                
                if (tlObj.has("keyframes")) {
                    JsonArray keyframes = tlObj.getAsJsonArray("keyframes");
                    for (JsonElement kfElem : keyframes) {
                        JsonObject kfObj = kfElem.getAsJsonObject();
                        double frame = kfObj.get("frame").getAsDouble();
                        
                        BoneKeyframe kf = new BoneKeyframe(frame);
                        if (kfObj.has("x")) kf.setX(kfObj.get("x").getAsDouble());
                        if (kfObj.has("y")) kf.setY(kfObj.get("y").getAsDouble());
                        if (kfObj.has("rotation")) kf.setRotation(kfObj.get("rotation").getAsDouble());
                        if (kfObj.has("scaleX")) kf.setScaleX(kfObj.get("scaleX").getAsDouble());
                        if (kfObj.has("scaleY")) kf.setScaleY(kfObj.get("scaleY").getAsDouble());
                        
                        clip.addKeyframe(boneName, kf);
                    }
                }
            }
        }
        
        return clip;
    }
}
