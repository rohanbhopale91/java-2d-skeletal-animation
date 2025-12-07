package com.animstudio.core.mesh;

import com.animstudio.core.math.Transform2D;
import com.animstudio.core.math.Vector2;
import com.animstudio.core.model.Bone;
import com.animstudio.core.model.Skeleton;

import java.util.ArrayList;
import java.util.List;

/**
 * A deformable mesh that can be bound to skeleton bones.
 * Used for mesh deformation / skinning.
 */
public class DeformableMesh {
    
    private String name;
    private final List<MeshVertex> vertices;
    private final List<MeshTriangle> triangles;
    private Skeleton skeleton;
    
    // Texture reference
    private String texturePath;
    private int textureWidth;
    private int textureHeight;
    
    // Mesh bounds (in local space)
    private float minX, minY, maxX, maxY;
    
    public DeformableMesh(String name) {
        this.name = name;
        this.vertices = new ArrayList<>();
        this.triangles = new ArrayList<>();
    }
    
    /**
     * Add a vertex to the mesh.
     */
    public int addVertex(float x, float y, float u, float v) {
        MeshVertex vertex = new MeshVertex(x, y, u, v);
        vertices.add(vertex);
        updateBounds(x, y);
        return vertices.size() - 1;
    }
    
    /**
     * Add a vertex with bone weights.
     */
    public int addVertex(float x, float y, float u, float v, int[] boneIndices, float[] weights) {
        MeshVertex vertex = new MeshVertex(x, y, u, v);
        for (int i = 0; i < boneIndices.length && i < weights.length; i++) {
            vertex.addBoneWeight(boneIndices[i], weights[i]);
        }
        vertex.normalizeWeights();
        vertices.add(vertex);
        updateBounds(x, y);
        return vertices.size() - 1;
    }
    
    /**
     * Add a triangle to the mesh.
     */
    public void addTriangle(int v1, int v2, int v3) {
        if (v1 >= 0 && v1 < vertices.size() &&
            v2 >= 0 && v2 < vertices.size() &&
            v3 >= 0 && v3 < vertices.size()) {
            triangles.add(new MeshTriangle(v1, v2, v3));
        }
    }
    
    /**
     * Bind all vertices to the nearest bone based on distance.
     */
    public void autoSkin(Skeleton skeleton, float maxDistance) {
        this.skeleton = skeleton;
        List<Bone> bones = new ArrayList<>(skeleton.getBones());
        
        for (MeshVertex vertex : vertices) {
            vertex.clearWeights();
            
            // Find bones within maxDistance
            List<Integer> nearBones = new ArrayList<>();
            List<Float> distances = new ArrayList<>();
            
            for (int i = 0; i < bones.size(); i++) {
                Bone bone = bones.get(i);
                float boneX = (float) bone.getWorldTransform().getX();
                float boneY = (float) bone.getWorldTransform().getY();
                
                float dx = vertex.x - boneX;
                float dy = vertex.y - boneY;
                float dist = (float) Math.sqrt(dx * dx + dy * dy);
                
                if (dist <= maxDistance) {
                    nearBones.add(i);
                    distances.add(dist);
                }
            }
            
            // Assign weights based on inverse distance
            if (!nearBones.isEmpty()) {
                float totalInvDist = 0;
                for (float dist : distances) {
                    totalInvDist += 1.0f / (dist + 0.001f);
                }
                
                for (int i = 0; i < nearBones.size(); i++) {
                    float invDist = 1.0f / (distances.get(i) + 0.001f);
                    float weight = invDist / totalInvDist;
                    vertex.addBoneWeight(nearBones.get(i), weight);
                }
                
                vertex.normalizeWeights();
            }
        }
    }
    
    /**
     * Update vertex world positions based on bone transforms.
     */
    public void updateDeformation() {
        if (skeleton == null) return;
        
        List<Bone> bones = new ArrayList<>(skeleton.getBones());
        
        for (MeshVertex vertex : vertices) {
            if (vertex.getWeightCount() == 0) {
                // No weights - keep original position
                vertex.worldX = vertex.x;
                vertex.worldY = vertex.y;
                continue;
            }
            
            // Blend bone transforms
            float worldX = 0;
            float worldY = 0;
            
            for (int i = 0; i < vertex.getWeightCount(); i++) {
                int boneIndex = vertex.getBoneIndex(i);
                float weight = vertex.getBoneWeight(i);
                
                if (boneIndex >= 0 && boneIndex < bones.size()) {
                    Bone bone = bones.get(boneIndex);
                    Transform2D transform = bone.getWorldTransform();
                    
                    // Transform vertex by bone
                    Vector2 point = new Vector2(vertex.x, vertex.y);
                    Vector2 transformed = transform.transformPoint(point);
                    
                    worldX += (float) transformed.x * weight;
                    worldY += (float) transformed.y * weight;
                }
            }
            
            vertex.worldX = worldX;
            vertex.worldY = worldY;
        }
    }
    
    /**
     * Check if a point is inside the mesh.
     */
    public boolean containsPoint(float x, float y) {
        MeshVertex[] verts = vertices.toArray(new MeshVertex[0]);
        for (MeshTriangle tri : triangles) {
            if (tri.containsPoint(x, y, verts)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Find the vertex nearest to a point.
     */
    public int findNearestVertex(float x, float y) {
        int nearest = -1;
        float nearestDist = Float.MAX_VALUE;
        
        for (int i = 0; i < vertices.size(); i++) {
            MeshVertex v = vertices.get(i);
            float dx = v.worldX - x;
            float dy = v.worldY - y;
            float dist = dx * dx + dy * dy;
            
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = i;
            }
        }
        
        return nearest;
    }
    
    /**
     * Create a simple quad mesh.
     */
    public static DeformableMesh createQuad(String name, float width, float height) {
        DeformableMesh mesh = new DeformableMesh(name);
        
        float hw = width / 2;
        float hh = height / 2;
        
        mesh.addVertex(-hw, -hh, 0, 0);  // Bottom-left
        mesh.addVertex(hw, -hh, 1, 0);   // Bottom-right
        mesh.addVertex(hw, hh, 1, 1);    // Top-right
        mesh.addVertex(-hw, hh, 0, 1);   // Top-left
        
        mesh.addTriangle(0, 1, 2);
        mesh.addTriangle(0, 2, 3);
        
        return mesh;
    }
    
    /**
     * Create a mesh grid for more detailed deformation.
     */
    public static DeformableMesh createGrid(String name, float width, float height, int cols, int rows) {
        DeformableMesh mesh = new DeformableMesh(name);
        
        float cellW = width / cols;
        float cellH = height / rows;
        float startX = -width / 2;
        float startY = -height / 2;
        
        // Create vertices
        for (int y = 0; y <= rows; y++) {
            for (int x = 0; x <= cols; x++) {
                float vx = startX + x * cellW;
                float vy = startY + y * cellH;
                float u = (float) x / cols;
                float v = (float) y / rows;
                mesh.addVertex(vx, vy, u, v);
            }
        }
        
        // Create triangles
        int vertsPerRow = cols + 1;
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {
                int i = y * vertsPerRow + x;
                mesh.addTriangle(i, i + 1, i + vertsPerRow);
                mesh.addTriangle(i + 1, i + vertsPerRow + 1, i + vertsPerRow);
            }
        }
        
        return mesh;
    }
    
    private void updateBounds(float x, float y) {
        if (vertices.size() == 1) {
            minX = maxX = x;
            minY = maxY = y;
        } else {
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
        }
    }
    
    // === Getters and Setters ===
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public List<MeshVertex> getVertices() { return vertices; }
    public List<MeshTriangle> getTriangles() { return triangles; }
    
    public MeshVertex getVertex(int index) {
        return index >= 0 && index < vertices.size() ? vertices.get(index) : null;
    }
    
    public int getVertexCount() { return vertices.size(); }
    public int getTriangleCount() { return triangles.size(); }
    
    public Skeleton getSkeleton() { return skeleton; }
    public void setSkeleton(Skeleton skeleton) { this.skeleton = skeleton; }
    
    public String getTexturePath() { return texturePath; }
    public void setTexturePath(String texturePath) { this.texturePath = texturePath; }
    
    public int getTextureWidth() { return textureWidth; }
    public void setTextureWidth(int textureWidth) { this.textureWidth = textureWidth; }
    
    public int getTextureHeight() { return textureHeight; }
    public void setTextureHeight(int textureHeight) { this.textureHeight = textureHeight; }
    
    public float getMinX() { return minX; }
    public float getMinY() { return minY; }
    public float getMaxX() { return maxX; }
    public float getMaxY() { return maxY; }
    
    public float getWidth() { return maxX - minX; }
    public float getHeight() { return maxY - minY; }
    
    // === Additional methods for editor support ===
    
    /**
     * Create a deep copy of this mesh.
     */
    public DeformableMesh duplicate() {
        DeformableMesh copy = new DeformableMesh(this.name + "_copy");
        copy.skeleton = this.skeleton;
        copy.texturePath = this.texturePath;
        copy.textureWidth = this.textureWidth;
        copy.textureHeight = this.textureHeight;
        copy.minX = this.minX;
        copy.minY = this.minY;
        copy.maxX = this.maxX;
        copy.maxY = this.maxY;
        
        // Copy vertices
        for (MeshVertex v : this.vertices) {
            copy.vertices.add(v.copy());
        }
        
        // Copy triangles
        for (MeshTriangle t : this.triangles) {
            copy.triangles.add(new MeshTriangle(t.v1, t.v2, t.v3));
        }
        
        return copy;
    }
    
    /**
     * Normalize all vertex weights so they sum to 1.0.
     */
    public void normalizeWeights() {
        for (MeshVertex vertex : vertices) {
            vertex.normalizeWeights();
        }
    }
    
    /**
     * Auto-generate weights for all vertices based on bone proximity.
     * This is a convenience method that calls autoSkin with a reasonable default distance.
     */
    public void autoGenerateWeights(Skeleton skeleton) {
        // Calculate a reasonable max distance based on mesh size
        float meshSize = Math.max(getWidth(), getHeight());
        float maxDistance = meshSize > 0 ? meshSize : 200.0f;
        autoSkin(skeleton, maxDistance);
    }
    
    /**
     * Remove a vertex by index.
     */
    public void removeVertex(int index) {
        if (index < 0 || index >= vertices.size()) return;
        
        vertices.remove(index);
        
        // Rebuild triangles, excluding those that referenced the removed vertex
        // and adjusting indices for vertices after the removed one
        List<MeshTriangle> newTriangles = new ArrayList<>();
        for (MeshTriangle tri : triangles) {
            if (tri.v1 == index || tri.v2 == index || tri.v3 == index) {
                // Skip triangles that reference the removed vertex
                continue;
            }
            // Adjust indices for vertices after the removed one
            int newV1 = tri.v1 > index ? tri.v1 - 1 : tri.v1;
            int newV2 = tri.v2 > index ? tri.v2 - 1 : tri.v2;
            int newV3 = tri.v3 > index ? tri.v3 - 1 : tri.v3;
            newTriangles.add(new MeshTriangle(newV1, newV2, newV3));
        }
        triangles.clear();
        triangles.addAll(newTriangles);
        
        recalculateBounds();
    }
    
    /**
     * Remove a triangle by index.
     */
    public void removeTriangle(int index) {
        if (index >= 0 && index < triangles.size()) {
            triangles.remove(index);
        }
    }
    
    /**
     * Recalculate the bounding box from all vertices.
     */
    public void recalculateBounds() {
        if (vertices.isEmpty()) {
            minX = minY = maxX = maxY = 0;
            return;
        }
        
        minX = Float.MAX_VALUE;
        minY = Float.MAX_VALUE;
        maxX = Float.MIN_VALUE;
        maxY = Float.MIN_VALUE;
        
        for (MeshVertex v : vertices) {
            minX = Math.min(minX, v.x);
            minY = Math.min(minY, v.y);
            maxX = Math.max(maxX, v.x);
            maxY = Math.max(maxY, v.y);
        }
    }
}
