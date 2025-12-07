package com.animstudio.editor.commands;

import com.animstudio.core.model.Bone;

/**
 * Command for moving a bone's position.
 */
public class MoveBoneCommand implements Command {
    
    private final Bone bone;
    private final double oldX, oldY;
    private double newX, newY;
    private long timestamp;
    
    public MoveBoneCommand(Bone bone, double newX, double newY) {
        this.bone = bone;
        this.oldX = bone.getX();
        this.oldY = bone.getY();
        this.newX = newX;
        this.newY = newY;
        this.timestamp = System.currentTimeMillis();
    }
    
    @Override
    public void execute() {
        bone.setX(newX);
        bone.setY(newY);
    }
    
    @Override
    public void undo() {
        bone.setX(oldX);
        bone.setY(oldY);
    }
    
    @Override
    public String getDescription() {
        return "Move " + bone.getName();
    }
    
    @Override
    public boolean canMergeWith(Command other) {
        if (!(other instanceof MoveBoneCommand)) return false;
        MoveBoneCommand otherMove = (MoveBoneCommand) other;
        // Merge if same bone and within 500ms
        return otherMove.bone == this.bone && 
               (otherMove.timestamp - this.timestamp) < 500;
    }
    
    @Override
    public void mergeWith(Command other) {
        MoveBoneCommand otherMove = (MoveBoneCommand) other;
        this.newX = otherMove.newX;
        this.newY = otherMove.newY;
        this.timestamp = otherMove.timestamp;
    }
}
