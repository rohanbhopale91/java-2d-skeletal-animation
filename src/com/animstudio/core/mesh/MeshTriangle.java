package com.animstudio.core.mesh;

/**
 * A triangle in a mesh, defined by three vertex indices.
 */
public class MeshTriangle {
    
    public final int v1;
    public final int v2;
    public final int v3;
    
    public MeshTriangle(int v1, int v2, int v3) {
        this.v1 = v1;
        this.v2 = v2;
        this.v3 = v3;
    }
    
    /**
     * Check if a point is inside this triangle using barycentric coordinates.
     * 
     * @param px Point X
     * @param py Point Y
     * @param vertices Vertex array to get positions from
     * @return true if point is inside the triangle
     */
    public boolean containsPoint(float px, float py, MeshVertex[] vertices) {
        float x1 = vertices[v1].worldX;
        float y1 = vertices[v1].worldY;
        float x2 = vertices[v2].worldX;
        float y2 = vertices[v2].worldY;
        float x3 = vertices[v3].worldX;
        float y3 = vertices[v3].worldY;
        
        float d1 = sign(px, py, x1, y1, x2, y2);
        float d2 = sign(px, py, x2, y2, x3, y3);
        float d3 = sign(px, py, x3, y3, x1, y1);
        
        boolean hasNeg = (d1 < 0) || (d2 < 0) || (d3 < 0);
        boolean hasPos = (d1 > 0) || (d2 > 0) || (d3 > 0);
        
        return !(hasNeg && hasPos);
    }
    
    private float sign(float p1x, float p1y, float p2x, float p2y, float p3x, float p3y) {
        return (p1x - p3x) * (p2y - p3y) - (p2x - p3x) * (p1y - p3y);
    }
    
    /**
     * Calculate barycentric coordinates for a point in this triangle.
     * 
     * @return float[3] with barycentric weights for v1, v2, v3
     */
    public float[] getBarycentricCoords(float px, float py, MeshVertex[] vertices) {
        float x1 = vertices[v1].worldX;
        float y1 = vertices[v1].worldY;
        float x2 = vertices[v2].worldX;
        float y2 = vertices[v2].worldY;
        float x3 = vertices[v3].worldX;
        float y3 = vertices[v3].worldY;
        
        float det = (y2 - y3) * (x1 - x3) + (x3 - x2) * (y1 - y3);
        
        if (Math.abs(det) < 0.0001f) {
            return new float[]{0.33f, 0.33f, 0.34f};
        }
        
        float w1 = ((y2 - y3) * (px - x3) + (x3 - x2) * (py - y3)) / det;
        float w2 = ((y3 - y1) * (px - x3) + (x1 - x3) * (py - y3)) / det;
        float w3 = 1 - w1 - w2;
        
        return new float[]{w1, w2, w3};
    }
    
    /**
     * Calculate the area of this triangle.
     */
    public float getArea(MeshVertex[] vertices) {
        float x1 = vertices[v1].worldX;
        float y1 = vertices[v1].worldY;
        float x2 = vertices[v2].worldX;
        float y2 = vertices[v2].worldY;
        float x3 = vertices[v3].worldX;
        float y3 = vertices[v3].worldY;
        
        return Math.abs((x2 - x1) * (y3 - y1) - (x3 - x1) * (y2 - y1)) / 2;
    }
    
    @Override
    public String toString() {
        return String.format("Triangle(%d, %d, %d)", v1, v2, v3);
    }
}
