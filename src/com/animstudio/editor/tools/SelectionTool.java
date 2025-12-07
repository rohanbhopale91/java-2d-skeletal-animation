package com.animstudio.editor.tools;

import com.animstudio.core.model.Bone;
import com.animstudio.editor.commands.TransformBoneCommand;
import com.animstudio.editor.EditorContext;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;

/**
 * Tool for selecting and transforming bones.
 */
public class SelectionTool extends AbstractTool {
    
    private enum Mode {
        SELECT,
        MOVE,
        ROTATE,
        SCALE
    }
    
    private Mode mode = Mode.SELECT;
    private Bone selectedBone;
    
    private double dragStartX, dragStartY;
    private double originalX, originalY, originalRotation, originalScaleX, originalScaleY;
    private boolean isDragging = false;
    
    // Visual settings
    private static final double HANDLE_SIZE = 10;
    private static final double ROTATION_RING_RADIUS = 50;
    private static final Color SELECTION_COLOR = Color.CYAN;
    private static final Color HANDLE_COLOR = Color.YELLOW;
    private static final Color ROTATION_COLOR = Color.ORANGE;
    
    public SelectionTool() {
        super("Selection Tool", ToolCategory.SELECTION);
    }
    
    @Override
    public void activate(EditorContext context) {
        super.activate(context);
        mode = Mode.SELECT;
    }
    
    @Override
    public void onMousePressed(MouseEvent event) {
        if (context == null) return;
        
        double x = event.getX();
        double y = event.getY();
        
        dragStartX = x;
        dragStartY = y;
        
        // Check if clicking on a handle for the selected bone
        if (selectedBone != null) {
            if (isOverRotationHandle(x, y)) {
                mode = Mode.ROTATE;
                originalRotation = selectedBone.getRotation();
                isDragging = true;
                return;
            } else if (isOverScaleHandle(x, y)) {
                mode = Mode.SCALE;
                originalScaleX = selectedBone.getScaleX();
                originalScaleY = selectedBone.getScaleY();
                isDragging = true;
                return;
            } else if (isOverMoveHandle(x, y)) {
                mode = Mode.MOVE;
                originalX = selectedBone.getX();
                originalY = selectedBone.getY();
                isDragging = true;
                return;
            }
        }
        
        // Otherwise, try to select a bone
        Bone bone = findBoneAt(x, y);
        if (bone != null) {
            selectedBone = bone;
            context.select(bone);
            mode = Mode.MOVE;
            originalX = bone.getX();
            originalY = bone.getY();
            isDragging = true;
        } else {
            selectedBone = null;
            context.clearSelection();
        }
    }
    
    @Override
    public void onMouseDragged(MouseEvent event) {
        if (!isDragging || selectedBone == null) return;
        
        double x = event.getX();
        double y = event.getY();
        
        switch (mode) {
            case MOVE:
                double dx = x - dragStartX;
                double dy = y - dragStartY;
                selectedBone.setX(originalX + dx);
                selectedBone.setY(originalY + dy);
                updateWorldTransforms();
                break;
                
            case ROTATE:
                double boneX = selectedBone.getWorldTransform().getX();
                double boneY = selectedBone.getWorldTransform().getY();
                
                double startAngle = Math.atan2(dragStartY - boneY, dragStartX - boneX);
                double currentAngle = Math.atan2(y - boneY, x - boneX);
                double deltaAngle = Math.toDegrees(currentAngle - startAngle);
                
                selectedBone.setRotation(originalRotation + deltaAngle);
                updateWorldTransforms();
                break;
                
            case SCALE:
                double scaleDx = (x - dragStartX) / 50.0;
                double newScaleX = originalScaleX + scaleDx;
                double newScaleY = event.isShiftDown() ? originalScaleY : originalScaleY + scaleDx;
                
                selectedBone.setScaleX(Math.max(0.1, newScaleX));
                selectedBone.setScaleY(Math.max(0.1, newScaleY));
                updateWorldTransforms();
                break;
        }
    }
    
    @Override
    public void onMouseReleased(MouseEvent event) {
        if (isDragging && selectedBone != null) {
            // Create command for undo
            TransformBoneCommand cmd = new TransformBoneCommand(
                selectedBone,
                originalX, originalY, originalRotation, originalScaleX, originalScaleY,
                selectedBone.getX(), selectedBone.getY(), 
                selectedBone.getRotation(), selectedBone.getScaleX(), selectedBone.getScaleY()
            );
            context.getCommandStack().execute(cmd);
        }
        
        isDragging = false;
        mode = Mode.SELECT;
    }
    
    @Override
    public void onKeyPressed(KeyEvent event) {
        switch (event.getCode()) {
            case G:
                if (selectedBone != null) {
                    mode = Mode.MOVE;
                    originalX = selectedBone.getX();
                    originalY = selectedBone.getY();
                }
                break;
            case R:
                if (selectedBone != null) {
                    mode = Mode.ROTATE;
                    originalRotation = selectedBone.getRotation();
                }
                break;
            case S:
                if (selectedBone != null) {
                    mode = Mode.SCALE;
                    originalScaleX = selectedBone.getScaleX();
                    originalScaleY = selectedBone.getScaleY();
                }
                break;
            case ESCAPE:
                if (isDragging && selectedBone != null) {
                    // Cancel current operation
                    selectedBone.setX(originalX);
                    selectedBone.setY(originalY);
                    selectedBone.setRotation(originalRotation);
                    selectedBone.setScaleX(originalScaleX);
                    selectedBone.setScaleY(originalScaleY);
                    updateWorldTransforms();
                }
                isDragging = false;
                mode = Mode.SELECT;
                break;
        }
    }
    
    private void updateWorldTransforms() {
        if (context != null && context.getActiveSkeleton() != null) {
            context.getActiveSkeleton().updateWorldTransforms();
        }
    }
    
    private Bone findBoneAt(double x, double y) {
        if (context == null || context.getActiveSkeleton() == null) return null;
        
        double threshold = 15;
        
        for (Bone bone : context.getActiveSkeleton().getBones()) {
            double bx = bone.getWorldTransform().getX();
            double by = bone.getWorldTransform().getY();
            
            if (Math.sqrt((x - bx) * (x - bx) + (y - by) * (y - by)) < threshold) {
                return bone;
            }
        }
        
        return null;
    }
    
    private boolean isOverMoveHandle(double x, double y) {
        if (selectedBone == null) return false;
        double bx = selectedBone.getWorldTransform().getX();
        double by = selectedBone.getWorldTransform().getY();
        return Math.sqrt((x - bx) * (x - bx) + (y - by) * (y - by)) < HANDLE_SIZE;
    }
    
    private boolean isOverRotationHandle(double x, double y) {
        if (selectedBone == null) return false;
        double bx = selectedBone.getWorldTransform().getX();
        double by = selectedBone.getWorldTransform().getY();
        double dist = Math.sqrt((x - bx) * (x - bx) + (y - by) * (y - by));
        return Math.abs(dist - ROTATION_RING_RADIUS) < HANDLE_SIZE;
    }
    
    private boolean isOverScaleHandle(double x, double y) {
        if (selectedBone == null) return false;
        double angle = Math.toRadians(selectedBone.getWorldTransform().getRotation());
        double length = selectedBone.getLength();
        double hx = selectedBone.getWorldTransform().getX() + Math.cos(angle) * length;
        double hy = selectedBone.getWorldTransform().getY() + Math.sin(angle) * length;
        return Math.sqrt((x - hx) * (x - hx) + (y - hy) * (y - hy)) < HANDLE_SIZE;
    }
    
    @Override
    public void render(GraphicsContext gc) {
        if (selectedBone == null) return;
        
        double bx = selectedBone.getWorldTransform().getX();
        double by = selectedBone.getWorldTransform().getY();
        
        // Draw rotation ring
        gc.setStroke(ROTATION_COLOR);
        gc.setLineWidth(2);
        gc.setLineDashes(5, 5);
        gc.strokeOval(bx - ROTATION_RING_RADIUS, by - ROTATION_RING_RADIUS, 
                     ROTATION_RING_RADIUS * 2, ROTATION_RING_RADIUS * 2);
        gc.setLineDashes(null);
        
        // Draw move handle (center)
        gc.setFill(HANDLE_COLOR);
        gc.fillOval(bx - HANDLE_SIZE / 2, by - HANDLE_SIZE / 2, HANDLE_SIZE, HANDLE_SIZE);
        
        // Draw scale handle (end of bone)
        double angle = Math.toRadians(selectedBone.getWorldTransform().getRotation());
        double length = selectedBone.getLength();
        double hx = bx + Math.cos(angle) * length;
        double hy = by + Math.sin(angle) * length;
        
        gc.setFill(SELECTION_COLOR);
        gc.fillRect(hx - HANDLE_SIZE / 2, hy - HANDLE_SIZE / 2, HANDLE_SIZE, HANDLE_SIZE);
        
        // Draw selection highlight on bone
        gc.setStroke(SELECTION_COLOR);
        gc.setLineWidth(3);
        gc.strokeLine(bx, by, hx, hy);
    }
    
    @Override
    public String getStatusMessage() {
        return mode == Mode.SELECT ? "Selection Tool: Click to select bone" : 
               "Selection Tool: " + mode.name();
    }
    
    public Bone getSelectedBone() { return selectedBone; }
    public void setSelectedBone(Bone bone) { this.selectedBone = bone; }
}
