package com.animstudio.editor.commands;

import com.animstudio.core.model.Bone;

/**
 * Command for transforming a bone (position, rotation, scale).
 */
public class TransformBoneCommand implements Command {
    
    private final Bone bone;
    
    private final double oldX, oldY, oldRotation, oldScaleX, oldScaleY;
    private final double newX, newY, newRotation, newScaleX, newScaleY;
    
    public TransformBoneCommand(Bone bone,
                                 double oldX, double oldY, double oldRotation, double oldScaleX, double oldScaleY,
                                 double newX, double newY, double newRotation, double newScaleX, double newScaleY) {
        this.bone = bone;
        this.oldX = oldX;
        this.oldY = oldY;
        this.oldRotation = oldRotation;
        this.oldScaleX = oldScaleX;
        this.oldScaleY = oldScaleY;
        this.newX = newX;
        this.newY = newY;
        this.newRotation = newRotation;
        this.newScaleX = newScaleX;
        this.newScaleY = newScaleY;
    }
    
    @Override
    public void execute() {
        bone.setX(newX);
        bone.setY(newY);
        bone.setRotation(newRotation);
        bone.setScaleX(newScaleX);
        bone.setScaleY(newScaleY);
    }
    
    @Override
    public void undo() {
        bone.setX(oldX);
        bone.setY(oldY);
        bone.setRotation(oldRotation);
        bone.setScaleX(oldScaleX);
        bone.setScaleY(oldScaleY);
    }
    
    @Override
    public String getDescription() {
        return "Transform " + bone.getName();
    }
    
    @Override
    public boolean canMergeWith(Command other) {
        if (other instanceof TransformBoneCommand) {
            TransformBoneCommand otherCmd = (TransformBoneCommand) other;
            return otherCmd.bone == this.bone;
        }
        return false;
    }
    
    @Override
    public void mergeWith(Command other) {
        if (other instanceof TransformBoneCommand) {
            // Note: merging modifies the "new" values to the latest
            // Since this is called after canMergeWith, we know it's the same bone
        }
    }
}
