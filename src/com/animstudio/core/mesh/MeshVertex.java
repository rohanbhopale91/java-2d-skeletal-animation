package com.animstudio.core.mesh;

/**
 * A vertex in a deformable mesh with bone weights.
 */
public class MeshVertex {
    
    // Position in mesh local space
    public float x;
    public float y;
    
    // UV coordinates for texture mapping
    public float u;
    public float v;
    
    // Bone weights (up to 4 bones per vertex)
    private final int[] boneIndices = new int[4];
    private final float[] boneWeights = new float[4];
    private int weightCount = 0;
    
    // Deformed position (calculated at runtime)
    public float worldX;
    public float worldY;
    
    public MeshVertex() {
        this(0, 0, 0, 0);
    }
    
    public MeshVertex(float x, float y) {
        this(x, y, 0, 0);
    }
    
    public MeshVertex(float x, float y, float u, float v) {
        this.x = x;
        this.y = y;
        this.u = u;
        this.v = v;
        this.worldX = x;
        this.worldY = y;
        
        // Initialize weights
        for (int i = 0; i < 4; i++) {
            boneIndices[i] = -1;
            boneWeights[i] = 0;
        }
    }
    
    /**
     * Add a bone weight influence to this vertex.
     * 
     * @param boneIndex Index of the bone in the skeleton
     * @param weight Weight of influence (0-1)
     */
    public void addBoneWeight(int boneIndex, float weight) {
        if (weightCount >= 4) {
            // Find smallest weight and replace if new weight is larger
            int minIndex = 0;
            float minWeight = boneWeights[0];
            for (int i = 1; i < 4; i++) {
                if (boneWeights[i] < minWeight) {
                    minWeight = boneWeights[i];
                    minIndex = i;
                }
            }
            if (weight > minWeight) {
                boneIndices[minIndex] = boneIndex;
                boneWeights[minIndex] = weight;
            }
        } else {
            boneIndices[weightCount] = boneIndex;
            boneWeights[weightCount] = weight;
            weightCount++;
        }
    }
    
    /**
     * Normalize all bone weights to sum to 1.0.
     */
    public void normalizeWeights() {
        float sum = 0;
        for (int i = 0; i < weightCount; i++) {
            sum += boneWeights[i];
        }
        
        if (sum > 0.0001f) {
            for (int i = 0; i < weightCount; i++) {
                boneWeights[i] /= sum;
            }
        }
    }
    
    /**
     * Clear all bone weights.
     */
    public void clearWeights() {
        for (int i = 0; i < 4; i++) {
            boneIndices[i] = -1;
            boneWeights[i] = 0;
        }
        weightCount = 0;
    }
    
    public int getWeightCount() { return weightCount; }
    
    public int getBoneIndex(int weightIndex) {
        return weightIndex < weightCount ? boneIndices[weightIndex] : -1;
    }
    
    public float getBoneWeight(int weightIndex) {
        return weightIndex < weightCount ? boneWeights[weightIndex] : 0;
    }
    
    public int[] getBoneIndices() { return boneIndices; }
    public float[] getBoneWeights() { return boneWeights; }
    
    /**
     * Copy this vertex.
     */
    public MeshVertex copy() {
        MeshVertex copy = new MeshVertex(x, y, u, v);
        for (int i = 0; i < weightCount; i++) {
            copy.addBoneWeight(boneIndices[i], boneWeights[i]);
        }
        return copy;
    }
    
    @Override
    public String toString() {
        return String.format("MeshVertex(%.2f, %.2f) uv(%.2f, %.2f) weights=%d", 
                            x, y, u, v, weightCount);
    }
}
