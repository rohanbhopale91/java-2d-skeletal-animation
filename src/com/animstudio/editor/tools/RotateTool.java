package com.animstudio.editor.tools;

import com.animstudio.core.model.Bone;
import com.animstudio.editor.EditorContext;
import com.animstudio.editor.commands.RotateBoneCommand;
import com.animstudio.editor.ui.canvas.CanvasPane;

import javafx.scene.Cursor;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

/**
 * Tool for rotating bones.
 */
public class RotateTool implements Tool {
    
    private boolean dragging = false;
    private Bone targetBone;
    private double startAngle;
    private double boneStartRotation;
    private double boneWorldX, boneWorldY;
    
    @Override
    public String getId() { return "rotate"; }
    
    @Override
    public String getName() { return "Rotate"; }
    
    @Override
    public void onActivate(CanvasPane canvas) {
        canvas.setCursor(Cursor.CROSSHAIR);
    }
    
    @Override
    public void onDeactivate(CanvasPane canvas) {
        dragging = false;
        targetBone = null;
    }
    
    @Override
    public void onMousePressed(MouseEvent event, CanvasPane canvas) {
        if (event.getButton() != MouseButton.PRIMARY) return;
        
        targetBone = EditorContext.getInstance().getSelectedBone();
        if (targetBone == null) return;
        
        dragging = true;
        boneStartRotation = targetBone.getRotation();
        
        // Get bone world position for angle calculation
        boneWorldX = targetBone.getWorldTransform().getX();
        boneWorldY = targetBone.getWorldTransform().getY();
        
        // Calculate initial angle from bone to mouse
        startAngle = calculateAngle(event.getX(), event.getY(), canvas);
    }
    
    @Override
    public void onMouseDragged(MouseEvent event, CanvasPane canvas) {
        if (!dragging || targetBone == null) return;
        
        double currentAngle = calculateAngle(event.getX(), event.getY(), canvas);
        double deltaAngle = currentAngle - startAngle;
        
        // Apply rotation
        targetBone.setRotation(boneStartRotation + deltaAngle);
        
        EditorContext.getInstance().getCurrentSkeleton().updateWorldTransforms();
        canvas.repaint();
    }
    
    @Override
    public void onMouseReleased(MouseEvent event, CanvasPane canvas) {
        if (!dragging || targetBone == null) return;
        
        double newRotation = targetBone.getRotation();
        
        if (newRotation != boneStartRotation) {
            // Restore original and execute command
            targetBone.setRotation(boneStartRotation);
            
            EditorContext.getInstance().getCommandStack().execute(
                new RotateBoneCommand(targetBone, newRotation)
            );
        }
        
        dragging = false;
        targetBone = null;
    }
    
    @Override
    public void onMouseMoved(MouseEvent event, CanvasPane canvas) {
        Bone selected = EditorContext.getInstance().getSelectedBone();
        canvas.setCursor(selected != null ? Cursor.CROSSHAIR : Cursor.DEFAULT);
    }
    
    private double calculateAngle(double mouseX, double mouseY, CanvasPane canvas) {
        // Convert bone world position to screen coordinates
        // This is simplified - should use CanvasPane's worldToScreen
        double zoom = canvas.getZoom();
        double screenBoneX = boneWorldX * zoom + canvas.getWidth() / 2;
        double screenBoneY = boneWorldY * zoom + canvas.getHeight() / 2;
        
        double dx = mouseX - screenBoneX;
        double dy = mouseY - screenBoneY;
        
        return Math.toDegrees(Math.atan2(dy, dx));
    }
}
