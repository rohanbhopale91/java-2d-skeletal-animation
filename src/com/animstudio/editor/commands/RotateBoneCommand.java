package com.animstudio.editor.commands;

import com.animstudio.core.model.Bone;

/**
 * Command for rotating a bone.
 */
public class RotateBoneCommand implements Command {
    
    private final Bone bone;
    private final double oldRotation;
    private double newRotation;
    private long timestamp;
    
    public RotateBoneCommand(Bone bone, double newRotation) {
        this.bone = bone;
        this.oldRotation = bone.getRotation();
        this.newRotation = newRotation;
        this.timestamp = System.currentTimeMillis();
    }
    
    @Override
    public void execute() {
        bone.setRotation(newRotation);
    }
    
    @Override
    public void undo() {
        bone.setRotation(oldRotation);
    }
    
    @Override
    public String getDescription() {
        return "Rotate " + bone.getName();
    }
    
    @Override
    public boolean canMergeWith(Command other) {
        if (!(other instanceof RotateBoneCommand)) return false;
        RotateBoneCommand otherRotate = (RotateBoneCommand) other;
        return otherRotate.bone == this.bone && 
               (otherRotate.timestamp - this.timestamp) < 500;
    }
    
    @Override
    public void mergeWith(Command other) {
        RotateBoneCommand otherRotate = (RotateBoneCommand) other;
        this.newRotation = otherRotate.newRotation;
        this.timestamp = otherRotate.timestamp;
    }
}
