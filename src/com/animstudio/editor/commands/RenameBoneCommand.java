package com.animstudio.editor.commands;

import com.animstudio.core.model.Bone;

/**
 * Command for renaming a bone.
 */
public class RenameBoneCommand implements Command {
    
    private final Bone bone;
    private final String oldName;
    private final String newName;
    
    public RenameBoneCommand(Bone bone, String newName) {
        this.bone = bone;
        this.oldName = bone.getName();
        this.newName = newName;
    }
    
    @Override
    public void execute() {
        bone.setName(newName);
    }
    
    @Override
    public void undo() {
        bone.setName(oldName);
    }
    
    @Override
    public String getDescription() {
        return "Rename Bone: " + oldName + " → " + newName;
    }
}
