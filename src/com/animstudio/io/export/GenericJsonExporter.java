package com.animstudio.io.export;

import com.animstudio.core.animation.AnimationClip;
import com.animstudio.core.animation.BoneKeyframe;
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
import java.util.List;

/**
 * Exports animation data to a simple, human-readable JSON format.
 * Designed for easy parsing in custom game engines.
 * 
 * Format structure:
 * {
 *   "format": "animstudio",
 *   "version": "1.0",
 *   "project": { ... },
 *   "skeleton": { bones: [...] },
 *   "animations": [ { name, duration, keyframes: {...} } ]
 * }
 */
public class GenericJsonExporter {
    
    private final Gson gson;
    private boolean prettyPrint = true;
    private boolean includeMetadata = true;
    private boolean flattenKeyframes = false;
    
    public GenericJsonExporter() {
        this.gson = new GsonBuilder()
            .setPrettyPrinting()
            .serializeNulls()
            .create();
    }
    
    public void setPrettyPrint(boolean prettyPrint) {
        this.prettyPrint = prettyPrint;
    }
    
    /**
     * Include project metadata (name, canvas size, etc.)
     */
    public void setIncludeMetadata(boolean include) {
        this.includeMetadata = include;
    }
    
    /**
     * Flatten keyframes to a single array per bone instead of per-property.
     */
    public void setFlattenKeyframes(boolean flatten) {
        this.flattenKeyframes = flatten;
    }
    
    /**
     * Export a project to generic JSON format.
     */
    public void export(AnimationProject project, File outputFile) throws IOException {
        JsonObject root = new JsonObject();
        
        // Format identifier
        root.addProperty("format", "animstudio");
        root.addProperty("version", "1.0");
        
        // Project metadata
        if (includeMetadata) {
            JsonObject projectMeta = new JsonObject();
            projectMeta.addProperty("name", project.getName());
            projectMeta.addProperty("canvasWidth", project.getCanvasWidth());
            projectMeta.addProperty("canvasHeight", project.getCanvasHeight());
            projectMeta.addProperty("frameRate", project.getFrameRate());
            root.add("project", projectMeta);
        }
        
        // Skeleton
        if (project.getSkeleton() != null) {
            root.add("skeleton", exportSkeleton(project.getSkeleton()));
        }
        
        // Animations
        root.add("animations", exportAnimations(project));
        
        // Write to file
        Gson outputGson = prettyPrint ? gson : new Gson();
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(outputFile), StandardCharsets.UTF_8)) {
            outputGson.toJson(root, writer);
        }
    }
    
    private JsonObject exportSkeleton(Skeleton skeleton) {
        JsonObject skelObj = new JsonObject();
        skelObj.addProperty("name", skeleton.getName());
        skelObj.addProperty("boneCount", skeleton.getBoneCount());
        
        JsonArray bones = new JsonArray();
        for (Bone bone : skeleton.getBonesInOrder()) {
            JsonObject boneObj = new JsonObject();
            boneObj.addProperty("name", bone.getName());
            boneObj.addProperty("parent", bone.getParent() != null ? bone.getParent().getName() : null);
            boneObj.addProperty("length", bone.getLength());
            boneObj.addProperty("inheritRotation", bone.isInheritRotation());
            boneObj.addProperty("inheritScale", bone.isInheritScale());
            boneObj.addProperty("drawOrder", bone.getDrawOrder());
            
            // Setup pose
            JsonObject setupPose = new JsonObject();
            setupPose.addProperty("x", bone.getSetupTransform().getX());
            setupPose.addProperty("y", bone.getSetupTransform().getY());
            setupPose.addProperty("rotation", bone.getSetupTransform().getRotation());
            setupPose.addProperty("scaleX", bone.getSetupTransform().getScaleX());
            setupPose.addProperty("scaleY", bone.getSetupTransform().getScaleY());
            boneObj.add("setupPose", setupPose);
            
            bones.add(boneObj);
        }
        skelObj.add("bones", bones);
        
        // Export IK constraints
        IKManager ikManager = skeleton.getIKManager();
        if (ikManager != null && !ikManager.getConstraints().isEmpty()) {
            JsonArray ikArray = new JsonArray();
            for (IKConstraint c : ikManager.getConstraints()) {
                JsonObject ikObj = new JsonObject();
                ikObj.addProperty("name", c.getName());
                ikObj.addProperty("mix", c.getMix());
                ikObj.addProperty("bendPositive", c.isBendPositive());
                
                JsonArray chainBones = new JsonArray();
                for (Bone b : c.getBones()) {
                    chainBones.add(b.getName());
                }
                ikObj.add("bones", chainBones);
                
                if (c.getTarget() != null) {
                    ikObj.addProperty("target", c.getTarget().getName());
                }
                ikArray.add(ikObj);
            }
            skelObj.add("ikConstraints", ikArray);
        }
        
        // Export meshes
        List<DeformableMesh> meshes = skeleton.getMeshes();
        if (meshes != null && !meshes.isEmpty()) {
            JsonArray meshArray = new JsonArray();
            for (DeformableMesh mesh : meshes) {
                JsonObject meshObj = new JsonObject();
                meshObj.addProperty("name", mesh.getName());
                meshObj.addProperty("vertexCount", mesh.getVertexCount());
                meshObj.addProperty("triangleCount", mesh.getTriangleCount());
                
                // Vertices
                JsonArray verts = new JsonArray();
                for (MeshVertex v : mesh.getVertices()) {
                    JsonObject vObj = new JsonObject();
                    vObj.addProperty("x", v.x);
                    vObj.addProperty("y", v.y);
                    vObj.addProperty("u", v.u);
                    vObj.addProperty("v", v.v);
                    
                    if (v.getWeightCount() > 0) {
                        JsonArray weights = new JsonArray();
                        int[] bi = v.getBoneIndices();
                        float[] bw = v.getBoneWeights();
                        for (int i = 0; i < v.getWeightCount(); i++) {
                            JsonObject wObj = new JsonObject();
                            wObj.addProperty("bone", bi[i]);
                            wObj.addProperty("weight", bw[i]);
                            weights.add(wObj);
                        }
                        vObj.add("weights", weights);
                    }
                    verts.add(vObj);
                }
                meshObj.add("vertices", verts);
                
                // Triangles
                JsonArray tris = new JsonArray();
                for (MeshTriangle t : mesh.getTriangles()) {
                    JsonArray tri = new JsonArray();
                    tri.add(t.v1);
                    tri.add(t.v2);
                    tri.add(t.v3);
                    tris.add(tri);
                }
                meshObj.add("triangles", tris);
                
                meshArray.add(meshObj);
            }
            skelObj.add("meshes", meshArray);
        }
        
        return skelObj;
    }
    
    private JsonArray exportAnimations(AnimationProject project) {
        JsonArray animations = new JsonArray();
        
        for (AnimationClip clip : project.getAnimations()) {
            JsonObject animObj = new JsonObject();
            animObj.addProperty("name", clip.getName());
            animObj.addProperty("duration", clip.getDuration());
            animObj.addProperty("looping", clip.isLooping());
            
            // Keyframes per bone
            JsonObject keyframes = new JsonObject();
            for (String boneName : clip.getBoneNames()) {
                AnimationClip.BoneKeyframes timeline = clip.getTimeline(boneName);
                if (timeline != null && !timeline.getKeyframes().isEmpty()) {
                    if (flattenKeyframes) {
                        keyframes.add(boneName, exportFlattenedKeyframes(timeline));
                    } else {
                        keyframes.add(boneName, exportSeparatedKeyframes(timeline));
                    }
                }
            }
            animObj.add("keyframes", keyframes);
            
            animations.add(animObj);
        }
        
        return animations;
    }
    
    /**
     * Export keyframes with all properties in one object per keyframe.
     */
    private JsonArray exportFlattenedKeyframes(AnimationClip.BoneKeyframes timeline) {
        JsonArray frames = new JsonArray();
        
        for (BoneKeyframe kf : timeline.getKeyframes()) {
            JsonObject frameObj = new JsonObject();
            frameObj.addProperty("time", kf.getTime());
            
            if (kf.getX() != null) frameObj.addProperty("x", kf.getX());
            if (kf.getY() != null) frameObj.addProperty("y", kf.getY());
            if (kf.getRotation() != null) frameObj.addProperty("rotation", kf.getRotation());
            if (kf.getScaleX() != null) frameObj.addProperty("scaleX", kf.getScaleX());
            if (kf.getScaleY() != null) frameObj.addProperty("scaleY", kf.getScaleY());
            
            frames.add(frameObj);
        }
        
        return frames;
    }
    
    /**
     * Export keyframes separated by property type.
     */
    private JsonObject exportSeparatedKeyframes(AnimationClip.BoneKeyframes timeline) {
        JsonObject result = new JsonObject();
        
        JsonArray translate = new JsonArray();
        JsonArray rotate = new JsonArray();
        JsonArray scale = new JsonArray();
        
        for (BoneKeyframe kf : timeline.getKeyframes()) {
            double time = kf.getTime();
            
            // Translation
            if (kf.getX() != null || kf.getY() != null) {
                JsonObject t = new JsonObject();
                t.addProperty("time", time);
                if (kf.getX() != null) t.addProperty("x", kf.getX());
                if (kf.getY() != null) t.addProperty("y", kf.getY());
                translate.add(t);
            }
            
            // Rotation
            if (kf.getRotation() != null) {
                JsonObject r = new JsonObject();
                r.addProperty("time", time);
                r.addProperty("value", kf.getRotation());
                rotate.add(r);
            }
            
            // Scale
            if (kf.getScaleX() != null || kf.getScaleY() != null) {
                JsonObject s = new JsonObject();
                s.addProperty("time", time);
                if (kf.getScaleX() != null) s.addProperty("x", kf.getScaleX());
                if (kf.getScaleY() != null) s.addProperty("y", kf.getScaleY());
                scale.add(s);
            }
        }
        
        if (translate.size() > 0) result.add("translate", translate);
        if (rotate.size() > 0) result.add("rotate", rotate);
        if (scale.size() > 0) result.add("scale", scale);
        
        return result;
    }
}
