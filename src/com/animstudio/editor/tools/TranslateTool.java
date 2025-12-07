package com.animstudio.editor.tools;

import com.animstudio.core.model.Bone;
import com.animstudio.core.model.Skeleton;
import com.animstudio.editor.EditorContext;
import com.animstudio.editor.commands.MoveBoneCommand;
import com.animstudio.editor.ui.canvas.CanvasPane;

import javafx.scene.Cursor;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

/**
 * Tool for translating (moving) bones.
 */
public class TranslateTool implements Tool {
    
    private boolean dragging = false;
    private Bone targetBone;
    private double startX, startY;
    private double boneStartX, boneStartY;
    
    @Override
    public String getId() { return "translate"; }
    
    @Override
    public String getName() { return "Move"; }
    
    @Override
    public void onActivate(CanvasPane canvas) {
        canvas.setCursor(Cursor.MOVE);
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
        startX = event.getX();
        startY = event.getY();
        boneStartX = targetBone.getX();
        boneStartY = targetBone.getY();
    }
    
    @Override
    public void onMouseDragged(MouseEvent event, CanvasPane canvas) {
        if (!dragging || targetBone == null) return;
        
        double dx = event.getX() - startX;
        double dy = event.getY() - startY;
        
        // Convert screen delta to local bone coordinates
        double zoom = canvas.getZoom();
        double worldDx = dx / zoom;
        double worldDy = dy / zoom;
        
        // Account for parent rotation
        if (targetBone.getParent() != null) {
            double parentRot = Math.toRadians(targetBone.getParent().getWorldTransform().getRotation());
            double cos = Math.cos(-parentRot);
            double sin = Math.sin(-parentRot);
            double localDx = worldDx * cos - worldDy * sin;
            double localDy = worldDx * sin + worldDy * cos;
            worldDx = localDx;
            worldDy = localDy;
        }
        
        targetBone.setX(boneStartX + worldDx);
        targetBone.setY(boneStartY + worldDy);
        
        EditorContext.getInstance().getCurrentSkeleton().updateWorldTransforms();
        canvas.repaint();
    }
    
    @Override
    public void onMouseReleased(MouseEvent event, CanvasPane canvas) {
        if (!dragging || targetBone == null) return;
        
        // Create undo command if position changed
        double newX = targetBone.getX();
        double newY = targetBone.getY();
        
        if (newX != boneStartX || newY != boneStartY) {
            // Restore original position and execute command
            targetBone.setX(boneStartX);
            targetBone.setY(boneStartY);
            
            EditorContext.getInstance().getCommandStack().execute(
                new MoveBoneCommand(targetBone, newX, newY)
            );
        }
        
        dragging = false;
        targetBone = null;
    }
    
    @Override
    public void onMouseMoved(MouseEvent event, CanvasPane canvas) {
        // Show appropriate cursor based on selection
        Bone selected = EditorContext.getInstance().getSelectedBone();
        canvas.setCursor(selected != null ? Cursor.MOVE : Cursor.DEFAULT);
    }
}
