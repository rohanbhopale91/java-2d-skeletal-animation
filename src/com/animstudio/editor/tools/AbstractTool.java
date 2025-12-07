package com.animstudio.editor.tools;

import com.animstudio.editor.EditorContext;
import com.animstudio.editor.ui.canvas.CanvasPane;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;

/**
 * Abstract base class for editor tools that bridges old and new tool systems.
 */
public abstract class AbstractTool implements Tool {
    
    protected final String name;
    protected final ToolCategory category;
    protected EditorContext context;
    protected CanvasPane canvas;
    protected boolean active;
    
    public AbstractTool(String name, ToolCategory category) {
        this.name = name;
        this.category = category;
    }
    
    @Override
    public String getId() {
        return name.toLowerCase().replace(" ", "_");
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public ToolCategory getCategory() {
        return category;
    }
    
    /**
     * Activate the tool with EditorContext (new system).
     */
    public void activate(EditorContext context) {
        this.context = context;
        this.active = true;
    }
    
    /**
     * Deactivate the tool.
     */
    public void deactivate() {
        this.active = false;
    }
    
    // === Old Tool interface implementation ===
    
    @Override
    public void onActivate(CanvasPane canvas) {
        this.canvas = canvas;
        // Also get EditorContext from canvas if available
        if (context == null) {
            context = com.animstudio.editor.EditorContext.getInstance();
        }
        this.active = true;
    }
    
    @Override
    public void onDeactivate(CanvasPane canvas) {
        deactivate();
    }
    
    @Override
    public void onMousePressed(MouseEvent event, CanvasPane canvas) {
        onMousePressed(event);
    }
    
    @Override
    public void onMouseDragged(MouseEvent event, CanvasPane canvas) {
        onMouseDragged(event);
    }
    
    @Override
    public void onMouseReleased(MouseEvent event, CanvasPane canvas) {
        onMouseReleased(event);
    }
    
    @Override
    public void onMouseMoved(MouseEvent event, CanvasPane canvas) {
        onMouseMoved(event);
    }
    
    @Override
    public void onKeyPressed(KeyEvent event, CanvasPane canvas) {
        onKeyPressed(event);
    }
    
    @Override
    public void onKeyReleased(KeyEvent event, CanvasPane canvas) {
        onKeyReleased(event);
    }
    
    @Override
    public void onScroll(ScrollEvent event, CanvasPane canvas) {
        onScroll(event);
    }
    
    // === New simplified event methods for subclasses ===
    
    public void onMousePressed(MouseEvent event) {}
    public void onMouseDragged(MouseEvent event) {}
    public void onMouseReleased(MouseEvent event) {}
    public void onMouseMoved(MouseEvent event) {}
    public void onKeyPressed(KeyEvent event) {}
    public void onKeyReleased(KeyEvent event) {}
    public void onScroll(ScrollEvent event) {}
    
    /**
     * Render tool overlay graphics.
     */
    public void render(GraphicsContext gc) {}
    
    /**
     * Get status message for this tool.
     */
    public String getStatusMessage() {
        return name;
    }
    
    public boolean isActive() { return active; }
}
