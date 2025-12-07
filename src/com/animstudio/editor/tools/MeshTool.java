package com.animstudio.editor.tools;

import com.animstudio.core.mesh.DeformableMesh;
import com.animstudio.core.mesh.MeshVertex;
import com.animstudio.core.model.Skeleton;
import com.animstudio.editor.EditorContext;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;

/**
 * Tool for creating and editing deformable meshes.
 */
public class MeshTool extends AbstractTool {
    
    private enum Mode {
        SELECT,         // Select vertices
        ADD_VERTEX,     // Add new vertices
        CREATE_TRIANGLE,// Create triangles from selected vertices
        WEIGHT_PAINT,   // Paint bone weights
        MOVE_VERTEX     // Move selected vertices
    }
    
    private Mode mode = Mode.SELECT;
    
    private DeformableMesh activeMesh;
    private final List<Integer> selectedVertices = new ArrayList<>();
    
    private double dragStartX, dragStartY;
    private boolean isDragging = false;
    
    // Weight painting settings
    private int paintBoneIndex = 0;
    private float paintWeight = 1.0f;
    private double brushRadius = 30;
    
    // Visual settings
    private static final double VERTEX_SIZE = 8;
    private static final Color VERTEX_COLOR = Color.LIGHTBLUE;
    private static final Color SELECTED_VERTEX_COLOR = Color.YELLOW;
    private static final Color MESH_COLOR = Color.rgb(100, 150, 200, 0.5);
    private static final Color EDGE_COLOR = Color.rgb(80, 120, 160);
    
    public MeshTool() {
        super("Mesh Tool", ToolCategory.MESH);
    }
    
    @Override
    public void activate(EditorContext context) {
        super.activate(context);
        mode = Mode.SELECT;
        selectedVertices.clear();
    }
    
    @Override
    public void deactivate() {
        selectedVertices.clear();
        super.deactivate();
    }
    
    @Override
    public void onMousePressed(MouseEvent event) {
        if (activeMesh == null || context == null) return;
        
        double x = event.getX();
        double y = event.getY();
        
        dragStartX = x;
        dragStartY = y;
        
        switch (mode) {
            case SELECT:
                handleSelect(x, y, event.isShiftDown());
                break;
                
            case ADD_VERTEX:
                addVertex(x, y);
                break;
                
            case CREATE_TRIANGLE:
                handleTriangleCreation(x, y);
                break;
                
            case WEIGHT_PAINT:
                paintWeights(x, y);
                break;
                
            case MOVE_VERTEX:
                isDragging = !selectedVertices.isEmpty();
                break;
        }
    }
    
    @Override
    public void onMouseDragged(MouseEvent event) {
        double x = event.getX();
        double y = event.getY();
        
        switch (mode) {
            case MOVE_VERTEX:
                if (isDragging) {
                    moveSelectedVertices(x - dragStartX, y - dragStartY);
                    dragStartX = x;
                    dragStartY = y;
                }
                break;
                
            case WEIGHT_PAINT:
                paintWeights(x, y);
                break;
        }
    }
    
    @Override
    public void onMouseReleased(MouseEvent event) {
        isDragging = false;
    }
    
    @Override
    public void onKeyPressed(KeyEvent event) {
        switch (event.getCode()) {
            case S:
                setMode(Mode.SELECT);
                break;
            case A:
                setMode(Mode.ADD_VERTEX);
                break;
            case T:
                setMode(Mode.CREATE_TRIANGLE);
                break;
            case W:
                setMode(Mode.WEIGHT_PAINT);
                break;
            case G:
                setMode(Mode.MOVE_VERTEX);
                break;
            case DELETE:
            case BACK_SPACE:
                deleteSelectedVertices();
                break;
            case ESCAPE:
                selectedVertices.clear();
                setMode(Mode.SELECT);
                break;
            case N:
                if (event.isControlDown()) {
                    createNewMesh();
                }
                break;
            case DIGIT1:
            case DIGIT2:
            case DIGIT3:
            case DIGIT4:
                // Quick bone selection for weight painting
                paintBoneIndex = event.getCode().ordinal() - event.getCode().DIGIT1.ordinal();
                break;
            case OPEN_BRACKET:
                brushRadius = Math.max(5, brushRadius - 5);
                break;
            case CLOSE_BRACKET:
                brushRadius = Math.min(100, brushRadius + 5);
                break;
        }
    }
    
    private void setMode(Mode newMode) {
        mode = newMode;
        if (context != null) {
            context.setStatusMessage("Mesh: " + mode.name().replace('_', ' '));
        }
    }
    
    private void handleSelect(double x, double y, boolean addToSelection) {
        int vertexIndex = findVertexAt(x, y);
        
        if (!addToSelection) {
            selectedVertices.clear();
        }
        
        if (vertexIndex >= 0) {
            if (selectedVertices.contains(vertexIndex)) {
                selectedVertices.remove(Integer.valueOf(vertexIndex));
            } else {
                selectedVertices.add(vertexIndex);
            }
        }
    }
    
    private void addVertex(double x, double y) {
        if (activeMesh == null) return;
        
        // Calculate UV based on position (simple normalization)
        float fx = (float) x;
        float fy = (float) y;
        float fu = fx / 100.0f;
        float fv = fy / 100.0f;
        
        int index = activeMesh.addVertex(fx, fy, fu, fv);
        
        // Auto-select new vertex
        selectedVertices.clear();
        selectedVertices.add(index);
    }
    
    private void handleTriangleCreation(double x, double y) {
        int vertexIndex = findVertexAt(x, y);
        
        if (vertexIndex >= 0 && !selectedVertices.contains(vertexIndex)) {
            selectedVertices.add(vertexIndex);
        }
        
        // Create triangle when we have 3 vertices selected
        if (selectedVertices.size() >= 3) {
            int v0 = selectedVertices.get(selectedVertices.size() - 3);
            int v1 = selectedVertices.get(selectedVertices.size() - 2);
            int v2 = selectedVertices.get(selectedVertices.size() - 1);
            
            activeMesh.addTriangle(v0, v1, v2);
            
            // Keep last two vertices selected for strip creation
            selectedVertices.clear();
            selectedVertices.add(v1);
            selectedVertices.add(v2);
            
            if (context != null) {
                context.setStatusMessage("Mesh: Triangle created");
            }
        }
    }
    
    private void paintWeights(double x, double y) {
        if (activeMesh == null) return;
        
        // Find all vertices within brush radius
        for (int i = 0; i < activeMesh.getVertexCount(); i++) {
            MeshVertex vertex = activeMesh.getVertex(i);
            double dx = vertex.x - x;
            double dy = vertex.y - y;
            double dist = Math.sqrt(dx * dx + dy * dy);
            
            if (dist < brushRadius) {
                // Calculate falloff
                double falloff = 1.0 - (dist / brushRadius);
                falloff = falloff * falloff; // Quadratic falloff
                
                float currentWeight = vertex.getBoneWeight(paintBoneIndex);
                float newWeight = (float) (currentWeight + (paintWeight - currentWeight) * falloff * 0.1);
                
                // Clear and re-add weight
                vertex.addBoneWeight(paintBoneIndex, newWeight);
                vertex.normalizeWeights();
            }
        }
    }
    
    private void moveSelectedVertices(double dx, double dy) {
        if (activeMesh == null) return;
        
        for (int index : selectedVertices) {
            MeshVertex vertex = activeMesh.getVertex(index);
            if (vertex != null) {
                vertex.x += (float) dx;
                vertex.y += (float) dy;
            }
        }
    }
    
    private void deleteSelectedVertices() {
        // Note: DeformableMesh doesn't have removeVertex, so we skip this for now
        // In a real implementation, you would need to rebuild the mesh without these vertices
        selectedVertices.clear();
        if (context != null) {
            context.setStatusMessage("Mesh: Delete not supported yet");
        }
    }
    
    private void createNewMesh() {
        if (context == null) return;
        
        activeMesh = new DeformableMesh("mesh_" + System.currentTimeMillis());
        
        // Register mesh with context
        context.addMesh(activeMesh);
        
        if (context != null) {
            context.setStatusMessage("Mesh: New mesh created");
        }
    }
    
    private int findVertexAt(double x, double y) {
        if (activeMesh == null) return -1;
        
        double threshold = VERTEX_SIZE;
        
        for (int i = 0; i < activeMesh.getVertexCount(); i++) {
            MeshVertex vertex = activeMesh.getVertex(i);
            double dx = vertex.x - x;
            double dy = vertex.y - y;
            
            if (Math.sqrt(dx * dx + dy * dy) < threshold) {
                return i;
            }
        }
        
        return -1;
    }
    
    @Override
    public void render(GraphicsContext gc) {
        if (activeMesh == null) return;
        
        // Draw mesh triangles
        gc.setFill(MESH_COLOR);
        gc.setStroke(EDGE_COLOR);
        gc.setLineWidth(1);
        
        for (int i = 0; i < activeMesh.getTriangleCount(); i++) {
            var triangle = activeMesh.getTriangles().get(i);
            
            MeshVertex v0 = activeMesh.getVertex(triangle.v1);
            MeshVertex v1 = activeMesh.getVertex(triangle.v2);
            MeshVertex v2 = activeMesh.getVertex(triangle.v3);
            
            double[] xPoints = {v0.x, v1.x, v2.x};
            double[] yPoints = {v0.y, v1.y, v2.y};
            
            gc.fillPolygon(xPoints, yPoints, 3);
            gc.strokePolygon(xPoints, yPoints, 3);
        }
        
        // Draw vertices
        for (int i = 0; i < activeMesh.getVertexCount(); i++) {
            MeshVertex vertex = activeMesh.getVertex(i);
            boolean isSelected = selectedVertices.contains(i);
            
            gc.setFill(isSelected ? SELECTED_VERTEX_COLOR : VERTEX_COLOR);
            gc.fillOval(vertex.x - VERTEX_SIZE / 2, 
                       vertex.y - VERTEX_SIZE / 2,
                       VERTEX_SIZE, VERTEX_SIZE);
            
            if (isSelected) {
                gc.setStroke(Color.WHITE);
                gc.setLineWidth(2);
                gc.strokeOval(vertex.x - VERTEX_SIZE / 2 - 2, 
                             vertex.y - VERTEX_SIZE / 2 - 2,
                             VERTEX_SIZE + 4, VERTEX_SIZE + 4);
            }
        }
        
        // Draw weight paint brush preview
        if (mode == Mode.WEIGHT_PAINT && context != null) {
            gc.setStroke(getWeightColor(paintWeight));
            gc.setLineWidth(2);
            gc.setLineDashes(5, 5);
            // Would need mouse position tracking to show brush
        }
    }
    
    private Color getWeightColor(float weight) {
        // Blue to Red gradient based on weight
        return Color.color(weight, 0, 1 - weight);
    }
    
    // === Property accessors ===
    
    public DeformableMesh getActiveMesh() { return activeMesh; }
    public void setActiveMesh(DeformableMesh mesh) { 
        this.activeMesh = mesh;
        selectedVertices.clear();
    }
    
    public int getPaintBoneIndex() { return paintBoneIndex; }
    public void setPaintBoneIndex(int index) { this.paintBoneIndex = index; }
    
    public float getPaintWeight() { return paintWeight; }
    public void setPaintWeight(float weight) { this.paintWeight = Math.max(0, Math.min(1, weight)); }
    
    public double getBrushRadius() { return brushRadius; }
    public void setBrushRadius(double radius) { this.brushRadius = Math.max(5, radius); }
    
    public Mode getMode() { return mode; }
    
    @Override
    public String getStatusMessage() {
        return "Mesh Tool: " + mode.name().replace('_', ' ') + 
               (mode == Mode.WEIGHT_PAINT ? " [Bone " + paintBoneIndex + ", Weight " + String.format("%.1f", paintWeight) + "]" : "");
    }
}
