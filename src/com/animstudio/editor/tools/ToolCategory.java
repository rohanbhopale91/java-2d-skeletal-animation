package com.animstudio.editor.tools;

/**
 * Categories for organizing editor tools.
 */
public enum ToolCategory {
    SELECTION("Selection", "Select and transform bones"),
    RIGGING("Rigging", "Create and edit skeleton"),
    MESH("Mesh", "Create and edit meshes"),
    ANIMATION("Animation", "Animation editing tools"),
    IK("IK", "Inverse kinematics tools"),
    VIEW("View", "Navigation and view tools");
    
    private final String displayName;
    private final String description;
    
    ToolCategory(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
}
