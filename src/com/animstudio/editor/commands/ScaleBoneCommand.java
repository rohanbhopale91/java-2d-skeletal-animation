package com.animstudio.editor.commands;

import com.animstudio.core.model.Bone;

/**
 * Command for scaling a bone's length.
 */
public class ScaleBoneCommand implements Command {
    
    private final Bone bone;
    private final double oldScaleX;
    private final double oldScaleY;
    private final double newScaleX;
    private final double newScaleY;
    
    public ScaleBoneCommand(Bone bone, double newScaleX, double newScaleY) {
        this.bone = bone;
        this.oldScaleX = bone.getScaleX();
        this.oldScaleY = bone.getScaleY();
        this.newScaleX = newScaleX;
        this.newScaleY = newScaleY;
    }
    
    @Override
    public void execute() {
        bone.setScaleX(newScaleX);
        bone.setScaleY(newScaleY);
    }
    
    @Override
    public void undo() {
        bone.setScaleX(oldScaleX);
        bone.setScaleY(oldScaleY);
    }
    
    @Override
    public String getDescription() {
        return String.format("Scale Bone: %s (%.2f, %.2f)", bone.getName(), newScaleX, newScaleY);
    }
    
    @Override
    public boolean canMergeWith(Command other) {
        if (other instanceof ScaleBoneCommand) {
            ScaleBoneCommand otherCmd = (ScaleBoneCommand) other;
            return otherCmd.bone == this.bone;
        }
        return false;
    }
    
    @Override
    public void mergeWith(Command other) {
        // Keep old scale from this command, take new scale from other
        // (effectively the merge just updates the target value)
    }
}
