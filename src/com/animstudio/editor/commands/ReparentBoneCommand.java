package com.animstudio.editor.commands;

import com.animstudio.core.model.Bone;

/**
 * Command for changing a bone's parent (reparenting).
 */
public class ReparentBoneCommand implements Command {
    
    private final Bone bone;
    private final Bone oldParent;
    private final Bone newParent;
    
    public ReparentBoneCommand(Bone bone, Bone newParent) {
        this.bone = bone;
        this.oldParent = bone.getParent();
        this.newParent = newParent;
    }
    
    @Override
    public void execute() {
        bone.setParent(newParent);
    }
    
    @Override
    public void undo() {
        bone.setParent(oldParent);
    }
    
    @Override
    public String getDescription() {
        String parentName = newParent != null ? newParent.getName() : "root";
        return "Reparent Bone: " + bone.getName() + " → " + parentName;
    }
}
