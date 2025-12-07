package com.animstudio.editor.tools;

import com.animstudio.core.model.Bone;
import com.animstudio.editor.EditorContext;
import com.animstudio.editor.ui.canvas.CanvasPane;

import javafx.scene.Cursor;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

/**
 * Tool for scaling bones.
 */
public class ScaleTool implements Tool {
    
    private boolean dragging = false;
    private Bone targetBone;
    private double startX, startY;
    private double boneStartScaleX, boneStartScaleY;
    private double boneStartLength;
    
    @Override
    public String getId() { return "scale"; }
    
    @Override
    public String getName() { return "Scale"; }
    
    @Override
    public void onActivate(CanvasPane canvas) {
        canvas.setCursor(Cursor.SE_RESIZE);
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
        boneStartScaleX = targetBone.getScaleX();
        boneStartScaleY = targetBone.getScaleY();
        boneStartLength = targetBone.getLength();
    }
    
    @Override
    public void onMouseDragged(MouseEvent event, CanvasPane canvas) {
        if (!dragging || targetBone == null) return;
        
        double dx = event.getX() - startX;
        double dy = event.getY() - startY;
        
        // Horizontal drag changes X scale/length
        // Vertical drag changes Y scale
        double scaleFactorX = 1 + dx / 100.0;
        double scaleFactorY = 1 - dy / 100.0;
        
        // Clamp scale factors
        scaleFactorX = Math.max(0.1, Math.min(10.0, scaleFactorX));
        scaleFactorY = Math.max(0.1, Math.min(10.0, scaleFactorY));
        
        if (event.isShiftDown()) {
            // Uniform scale
            double avgFactor = (scaleFactorX + scaleFactorY) / 2;
            targetBone.setScaleX(boneStartScaleX * avgFactor);
            targetBone.setScaleY(boneStartScaleY * avgFactor);
        } else if (event.isControlDown()) {
            // Scale length only
            targetBone.setLength(boneStartLength * scaleFactorX);
        } else {
            // Non-uniform scale
            targetBone.setScaleX(boneStartScaleX * scaleFactorX);
            targetBone.setScaleY(boneStartScaleY * scaleFactorY);
        }
        
        EditorContext.getInstance().getCurrentSkeleton().updateWorldTransforms();
        canvas.repaint();
    }
    
    @Override
    public void onMouseReleased(MouseEvent event, CanvasPane canvas) {
        if (!dragging || targetBone == null) return;
        
        // Could create undo command here for scale changes
        // For now, changes are committed directly
        
        dragging = false;
        targetBone = null;
    }
    
    @Override
    public void onMouseMoved(MouseEvent event, CanvasPane canvas) {
        Bone selected = EditorContext.getInstance().getSelectedBone();
        canvas.setCursor(selected != null ? Cursor.SE_RESIZE : Cursor.DEFAULT);
    }
}
