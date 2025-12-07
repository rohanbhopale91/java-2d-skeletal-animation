package com.animstudio.editor.tools;

import com.animstudio.core.model.Bone;
import com.animstudio.core.model.Skeleton;
import com.animstudio.editor.EditorContext;
import com.animstudio.editor.ui.canvas.CanvasPane;

import javafx.scene.Cursor;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

/**
 * Tool for creating new bones in the skeleton.
 */
public class CreateBoneTool implements Tool {
    
    private boolean creating = false;
    private Bone newBone;
    private double startX, startY;
    
    @Override
    public String getId() { return "create_bone"; }
    
    @Override
    public String getName() { return "Create Bone"; }
    
    @Override
    public void onActivate(CanvasPane canvas) {
        canvas.setCursor(Cursor.CROSSHAIR);
    }
    
    @Override
    public void onDeactivate(CanvasPane canvas) {
        creating = false;
        newBone = null;
    }
    
    @Override
    public void onMousePressed(MouseEvent event, CanvasPane canvas) {
        if (event.getButton() != MouseButton.PRIMARY) return;
        
        Skeleton skeleton = EditorContext.getInstance().getCurrentSkeleton();
        if (skeleton == null) return;
        
        startX = event.getX();
        startY = event.getY();
        
        // Determine parent bone
        Bone parent = EditorContext.getInstance().getSelectedBone();
        if (parent == null) {
            parent = skeleton.getRootBone();
        }
        
        // Create new bone
        String name = "bone_" + (skeleton.getBoneCount() + 1);
        newBone = new Bone(name);
        newBone.setParent(parent);
        
        // Calculate local position relative to parent
        // This is simplified - should use proper coordinate conversion
        double zoom = canvas.getZoom();
        double parentWorldX = parent.getWorldTransform().getX();
        double parentWorldY = parent.getWorldTransform().getY();
        
        // Convert click to world, then to parent-local
        double worldX = (startX - canvas.getWidth() / 2) / zoom;
        double worldY = (startY - canvas.getHeight() / 2) / zoom;
        
        double localX = worldX - parentWorldX;
        double localY = worldY - parentWorldY;
        
        // Account for parent rotation
        double parentRot = Math.toRadians(parent.getWorldTransform().getRotation());
        double cos = Math.cos(-parentRot);
        double sin = Math.sin(-parentRot);
        double rotatedX = localX * cos - localY * sin;
        double rotatedY = localX * sin + localY * cos;
        
        newBone.setX(rotatedX);
        newBone.setY(rotatedY);
        newBone.setLength(0);
        
        skeleton.addBone(newBone);
        skeleton.updateWorldTransforms();
        
        creating = true;
        canvas.repaint();
    }
    
    @Override
    public void onMouseDragged(MouseEvent event, CanvasPane canvas) {
        if (!creating || newBone == null) return;
        
        Skeleton skeleton = EditorContext.getInstance().getCurrentSkeleton();
        
        double zoom = canvas.getZoom();
        double dx = (event.getX() - startX) / zoom;
        double dy = (event.getY() - startY) / zoom;
        
        // Calculate length from drag distance
        double length = Math.sqrt(dx * dx + dy * dy);
        newBone.setLength(length);
        
        // Calculate rotation from drag angle
        double angle = Math.toDegrees(Math.atan2(dy, dx));
        
        // Account for parent rotation
        if (newBone.getParent() != null) {
            angle -= newBone.getParent().getWorldTransform().getRotation();
        }
        
        newBone.setRotation(angle);
        
        skeleton.updateWorldTransforms();
        canvas.repaint();
    }
    
    @Override
    public void onMouseReleased(MouseEvent event, CanvasPane canvas) {
        if (!creating || newBone == null) return;
        
        // Finalize the bone
        newBone.setToSetupPose();
        
        // Select the new bone
        EditorContext.getInstance().select(newBone);
        
        creating = false;
        newBone = null;
        
        canvas.repaint();
    }
    
    @Override
    public void onMouseMoved(MouseEvent event, CanvasPane canvas) {
        // Keep crosshair cursor
        canvas.setCursor(Cursor.CROSSHAIR);
    }
}
