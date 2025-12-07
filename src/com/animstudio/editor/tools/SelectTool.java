package com.animstudio.editor.tools;

import com.animstudio.core.math.Vector2;
import com.animstudio.core.model.Bone;
import com.animstudio.core.model.Skeleton;
import com.animstudio.editor.EditorContext;
import com.animstudio.editor.ui.canvas.CanvasPane;

import javafx.scene.Cursor;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

/**
 * Selection tool for picking and manipulating bones.
 */
public class SelectTool implements Tool {
    
    private boolean dragging = false;
    private double startX, startY;
    
    @Override
    public String getId() { return "select"; }
    
    @Override
    public String getName() { return "Select"; }
    
    @Override
    public void onActivate(CanvasPane canvas) {
        canvas.setCursor(Cursor.DEFAULT);
    }
    
    @Override
    public void onDeactivate(CanvasPane canvas) {
        dragging = false;
    }
    
    @Override
    public void onMousePressed(MouseEvent event, CanvasPane canvas) {
        if (event.getButton() != MouseButton.PRIMARY) return;
        
        startX = event.getX();
        startY = event.getY();
        
        // Try to pick a bone
        Skeleton skeleton = EditorContext.getInstance().getCurrentSkeleton();
        if (skeleton == null) return;
        
        // Convert to world coordinates
        // Note: CanvasPane handles coordinate conversion internally
        // This is a simplified version
        Bone picked = pickBoneAt(skeleton, event.getX(), event.getY(), canvas);
        
        if (picked != null) {
            EditorContext.getInstance().select(picked);
            dragging = true;
        } else {
            EditorContext.getInstance().clearSelection();
        }
        
        canvas.repaint();
    }
    
    @Override
    public void onMouseDragged(MouseEvent event, CanvasPane canvas) {
        // Selection tool doesn't drag-move bones
        // That's handled by TranslateTool
    }
    
    @Override
    public void onMouseReleased(MouseEvent event, CanvasPane canvas) {
        dragging = false;
    }
    
    @Override
    public void onMouseMoved(MouseEvent event, CanvasPane canvas) {
        // Could show hover highlight
        Skeleton skeleton = EditorContext.getInstance().getCurrentSkeleton();
        if (skeleton != null) {
            Bone hovered = pickBoneAt(skeleton, event.getX(), event.getY(), canvas);
            canvas.setCursor(hovered != null ? Cursor.HAND : Cursor.DEFAULT);
        }
    }
    
    @Override
    public void onKeyPressed(KeyEvent event, CanvasPane canvas) {
        switch (event.getCode()) {
            case ESCAPE:
                EditorContext.getInstance().clearSelection();
                canvas.repaint();
                break;
            case DELETE:
            case BACK_SPACE:
                deleteSelectedBone();
                canvas.repaint();
                break;
            default:
                break;
        }
    }
    
    private Bone pickBoneAt(Skeleton skeleton, double screenX, double screenY, CanvasPane canvas) {
        // Use skeleton's picking method
        // The actual coordinate conversion is handled by CanvasPane
        double threshold = 20.0 / canvas.getZoom();
        
        // This is simplified - actual implementation would need proper screen-to-world conversion
        // from CanvasPane, which we expose through a method
        return skeleton.findBoneAtPosition(screenX, screenY, threshold);
    }
    
    private void deleteSelectedBone() {
        Bone selected = EditorContext.getInstance().getSelectedBone();
        if (selected == null || selected.getParent() == null) return;
        
        Skeleton skeleton = EditorContext.getInstance().getCurrentSkeleton();
        skeleton.removeBone(selected);
        skeleton.updateWorldTransforms();
        EditorContext.getInstance().clearSelection();
    }
}
