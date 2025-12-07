package com.animstudio.editor.tools;

import com.animstudio.editor.EditorContext;
import com.animstudio.editor.ui.canvas.CanvasPane;

import java.util.*;

/**
 * Manages editor tools and tracks the currently active tool.
 */
public class ToolManager {
    
    private static ToolManager instance;
    
    private final Map<String, Tool> tools = new LinkedHashMap<>();
    private final Map<ToolCategory, List<Tool>> toolsByCategory = new EnumMap<>(ToolCategory.class);
    private Tool activeTool;
    private CanvasPane canvas;
    private EditorContext context;
    
    private ToolManager() {
        // Initialize category lists
        for (ToolCategory category : ToolCategory.values()) {
            toolsByCategory.put(category, new ArrayList<>());
        }
        
        // Register default tools
        registerTool(new SelectTool());
        registerTool(new TranslateTool());
        registerTool(new RotateTool());
        registerTool(new ScaleTool());
        registerTool(new CreateBoneTool());
        
        // New advanced tools
        registerTool(new SelectionTool());
        registerTool(new BoneTool());
        registerTool(new IKTool());
        registerTool(new MeshTool());
    }
    
    public static ToolManager getInstance() {
        if (instance == null) {
            instance = new ToolManager();
        }
        return instance;
    }
    
    public void setCanvas(CanvasPane canvas) {
        this.canvas = canvas;
    }
    
    public void setContext(EditorContext context) {
        this.context = context;
    }
    
    public void registerTool(Tool tool) {
        tools.put(tool.getId(), tool);
        
        // Also register by name for new tools
        if (!tool.getId().equals(tool.getName())) {
            tools.put(tool.getName(), tool);
        }
        
        // Add to category list
        ToolCategory category = tool.getCategory();
        if (category != null && !toolsByCategory.get(category).contains(tool)) {
            toolsByCategory.get(category).add(tool);
        }
    }
    
    public Tool getTool(String id) {
        return tools.get(id);
    }
    
    public void setActiveTool(String toolId) {
        Tool newTool = tools.get(toolId);
        if (newTool != null) {
            setActiveTool(newTool);
        }
    }
    
    public void setActiveTool(Tool tool) {
        if (activeTool != null && canvas != null) {
            activeTool.onDeactivate(canvas);
        }
        
        activeTool = tool;
        
        if (activeTool != null && canvas != null) {
            activeTool.onActivate(canvas);
        }
        
        // Also activate with EditorContext for new tools
        if (activeTool instanceof AbstractTool && context != null) {
            ((AbstractTool) activeTool).activate(context);
        }
    }
    
    public Tool getActiveTool() {
        return activeTool;
    }
    
    public Map<String, Tool> getTools() {
        return new LinkedHashMap<>(tools);
    }
    
    public List<Tool> getToolsInCategory(ToolCategory category) {
        return Collections.unmodifiableList(toolsByCategory.get(category));
    }
    
    public Collection<Tool> getAllTools() {
        return Collections.unmodifiableCollection(tools.values());
    }
}
