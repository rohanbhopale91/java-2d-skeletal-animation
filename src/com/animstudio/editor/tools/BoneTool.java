package com.animstudio.editor.tools;

import com.animstudio.core.model.Bone;
import com.animstudio.core.model.Skeleton;
import com.animstudio.editor.commands.AddBoneCommand;
import com.animstudio.editor.EditorContext;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;

/**
 * Tool for creating new bones.
 */
public class BoneTool extends AbstractTool {
    
    private enum Mode {
        PLACE_ROOT,     // Placing a new root bone
        DRAW_BONE,      // Drawing bone from parent
        SET_LENGTH      // Setting bone length
    }
    
    private Mode mode = Mode.PLACE_ROOT;
    
    private Bone parentBone;
    private double startX, startY;
    private double endX, endY;
    private boolean isDrawing = false;
    
    // Preview settings
    private static final Color PREVIEW_COLOR = Color.LIMEGREEN;
    private static final Color PARENT_HIGHLIGHT = Color.YELLOW;
    
    public BoneTool() {
        super("Bone Tool", ToolCategory.RIGGING);
    }
    
    @Override
    public void activate(EditorContext context) {
        super.activate(context);
        mode = Mode.PLACE_ROOT;
        parentBone = null;
        isDrawing = false;
    }
    
    @Override
    public void deactivate() {
        isDrawing = false;
        super.deactivate();
    }
    
    @Override
    public void onMousePressed(MouseEvent event) {
        if (context == null) return;
        
        double x = event.getX();
        double y = event.getY();
        
        startX = x;
        startY = y;
        
        // Check if clicking on existing bone to set as parent
        Bone clickedBone = findBoneAt(x, y);
        
        if (event.isControlDown() || mode == Mode.PLACE_ROOT) {
            // Place bone at position without parent
            parentBone = null;
            mode = Mode.DRAW_BONE;
        } else if (clickedBone != null) {
            // Use clicked bone as parent
            parentBone = clickedBone;
            
            // Start from end of parent bone
            double angle = Math.toRadians(parentBone.getWorldTransform().getRotation());
            startX = parentBone.getWorldTransform().getX() + Math.cos(angle) * parentBone.getLength();
            startY = parentBone.getWorldTransform().getY() + Math.sin(angle) * parentBone.getLength();
            
            mode = Mode.DRAW_BONE;
        } else {
            // No bone clicked, place root
            parentBone = null;
            mode = Mode.DRAW_BONE;
        }
        
        endX = startX;
        endY = startY;
        isDrawing = true;
    }
    
    @Override
    public void onMouseDragged(MouseEvent event) {
        if (!isDrawing) return;
        
        endX = event.getX();
        endY = event.getY();
        
        // Snap angle if shift is held
        if (event.isShiftDown()) {
            double dx = endX - startX;
            double dy = endY - startY;
            double length = Math.sqrt(dx * dx + dy * dy);
            double angle = Math.atan2(dy, dx);
            
            // Snap to 45-degree increments
            angle = Math.round(angle / (Math.PI / 4)) * (Math.PI / 4);
            
            endX = startX + Math.cos(angle) * length;
            endY = startY + Math.sin(angle) * length;
        }
    }
    
    @Override
    public void onMouseReleased(MouseEvent event) {
        if (!isDrawing) return;
        
        double length = Math.sqrt((endX - startX) * (endX - startX) + (endY - startY) * (endY - startY));
        
        if (length > 5) { // Minimum bone length
            createBone(startX, startY, endX, endY);
        }
        
        isDrawing = false;
        mode = Mode.PLACE_ROOT;
    }
    
    @Override
    public void onKeyPressed(KeyEvent event) {
        switch (event.getCode()) {
            case ESCAPE:
                isDrawing = false;
                mode = Mode.PLACE_ROOT;
                parentBone = null;
                break;
        }
    }
    
    private void createBone(double x1, double y1, double x2, double y2) {
        Skeleton skeleton = context.getActiveSkeleton();
        if (skeleton == null) return;
        
        double dx = x2 - x1;
        double dy = y2 - y1;
        double length = Math.sqrt(dx * dx + dy * dy);
        double rotation = Math.toDegrees(Math.atan2(dy, dx));
        
        String name = "bone_" + (skeleton.getBoneCount() + 1);
        Bone newBone = new Bone(name);
        
        if (parentBone != null) {
            newBone.setParent(parentBone);
            
            // Calculate local position relative to parent
            double parentAngle = Math.toRadians(parentBone.getWorldTransform().getRotation());
            double parentEndX = parentBone.getWorldTransform().getX() + Math.cos(parentAngle) * parentBone.getLength();
            double parentEndY = parentBone.getWorldTransform().getY() + Math.sin(parentAngle) * parentBone.getLength();
            
            // Set position relative to parent's end
            newBone.setX(x1 - parentEndX);
            newBone.setY(y1 - parentEndY);
            
            // Calculate local rotation
            double localRotation = rotation - parentBone.getWorldTransform().getRotation();
            newBone.setRotation(localRotation);
        } else {
            // Root bone - use world coordinates
            newBone.setX(x1);
            newBone.setY(y1);
            newBone.setRotation(rotation);
        }
        
        newBone.setLength(length);
        newBone.setToSetupPose();
        
        // Execute command
        AddBoneCommand cmd = new AddBoneCommand(skeleton, newBone);
        context.getCommandStack().execute(cmd);
        
        skeleton.updateWorldTransforms();
        
        // Select new bone
        context.select(newBone);
        
        if (context != null) {
            context.setStatusMessage("Created bone: " + name);
        }
    }
    
    private Bone findBoneAt(double x, double y) {
        if (context == null || context.getActiveSkeleton() == null) return null;
        
        double threshold = 20;
        
        for (Bone bone : context.getActiveSkeleton().getBones()) {
            double bx = bone.getWorldTransform().getX();
            double by = bone.getWorldTransform().getY();
            
            if (Math.sqrt((x - bx) * (x - bx) + (y - by) * (y - by)) < threshold) {
                return bone;
            }
            
            // Also check bone end
            double angle = Math.toRadians(bone.getWorldTransform().getRotation());
            double ex = bx + Math.cos(angle) * bone.getLength();
            double ey = by + Math.sin(angle) * bone.getLength();
            
            if (Math.sqrt((x - ex) * (x - ex) + (y - ey) * (y - ey)) < threshold) {
                return bone;
            }
        }
        
        return null;
    }
    
    @Override
    public void render(GraphicsContext gc) {
        // Highlight parent bone
        if (parentBone != null) {
            double bx = parentBone.getWorldTransform().getX();
            double by = parentBone.getWorldTransform().getY();
            double angle = Math.toRadians(parentBone.getWorldTransform().getRotation());
            double ex = bx + Math.cos(angle) * parentBone.getLength();
            double ey = by + Math.sin(angle) * parentBone.getLength();
            
            gc.setStroke(PARENT_HIGHLIGHT);
            gc.setLineWidth(4);
            gc.strokeLine(bx, by, ex, ey);
        }
        
        // Draw bone preview
        if (isDrawing) {
            gc.setStroke(PREVIEW_COLOR);
            gc.setLineWidth(3);
            gc.setLineDashes(5, 5);
            gc.strokeLine(startX, startY, endX, endY);
            gc.setLineDashes(null);
            
            // Draw endpoints
            gc.setFill(PREVIEW_COLOR);
            gc.fillOval(startX - 5, startY - 5, 10, 10);
            gc.fillRect(endX - 5, endY - 5, 10, 10);
            
            // Show length
            double length = Math.sqrt((endX - startX) * (endX - startX) + (endY - startY) * (endY - startY));
            gc.setFill(Color.WHITE);
            gc.fillText(String.format("%.1f", length), (startX + endX) / 2 + 10, (startY + endY) / 2 - 10);
        }
    }
    
    @Override
    public String getStatusMessage() {
        if (isDrawing) {
            double length = Math.sqrt((endX - startX) * (endX - startX) + (endY - startY) * (endY - startY));
            return String.format("Bone Tool: Drawing bone (%.1f px)", length);
        }
        return "Bone Tool: Click and drag to create bone. Click existing bone first for parent.";
    }
}
