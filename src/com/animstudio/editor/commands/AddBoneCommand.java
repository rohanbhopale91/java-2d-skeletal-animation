package com.animstudio.editor.commands;

import com.animstudio.core.model.Bone;
import com.animstudio.core.model.Skeleton;

/**
 * Command for adding a bone to a skeleton.
 */
public class AddBoneCommand implements Command {
    
    private final Skeleton skeleton;
    private final Bone bone;
    private boolean executed = false;
    
    public AddBoneCommand(Skeleton skeleton, Bone bone) {
        this.skeleton = skeleton;
        this.bone = bone;
    }
    
    @Override
    public void execute() {
        if (!executed) {
            skeleton.addBone(bone);
            executed = true;
        }
    }
    
    @Override
    public void undo() {
        if (executed) {
            skeleton.removeBone(bone);
            executed = false;
        }
    }
    
    @Override
    public String getDescription() {
        return "Add bone: " + bone.getName();
    }
}
