package com.animstudio.editor.tools;

import com.animstudio.editor.ui.canvas.CanvasPane;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;

/**
 * Interface for editor tools that handle canvas interaction.
 */
public interface Tool {
    
    /**
     * @return Unique identifier for this tool
     */
    String getId();
    
    /**
     * @return Display name for this tool
     */
    String getName();
    
    /**
     * @return Category this tool belongs to (for grouping in UI)
     */
    default ToolCategory getCategory() { return ToolCategory.SELECTION; }
    
    /**
     * @return Path to tool icon (optional)
     */
    default String getIconPath() { return null; }
    
    /**
     * Called when this tool becomes active.
     */
    void onActivate(CanvasPane canvas);
    
    /**
     * Called when this tool is deactivated.
     */
    void onDeactivate(CanvasPane canvas);
    
    /**
     * Handle mouse press event.
     */
    void onMousePressed(MouseEvent event, CanvasPane canvas);
    
    /**
     * Handle mouse drag event.
     */
    void onMouseDragged(MouseEvent event, CanvasPane canvas);
    
    /**
     * Handle mouse release event.
     */
    void onMouseReleased(MouseEvent event, CanvasPane canvas);
    
    /**
     * Handle mouse move event (no button pressed).
     */
    void onMouseMoved(MouseEvent event, CanvasPane canvas);
    
    /**
     * Handle scroll event.
     */
    default void onScroll(ScrollEvent event, CanvasPane canvas) {}
    
    /**
     * Handle key press event.
     */
    default void onKeyPressed(KeyEvent event, CanvasPane canvas) {}
    
    /**
     * Handle key release event.
     */
    default void onKeyReleased(KeyEvent event, CanvasPane canvas) {}
}
