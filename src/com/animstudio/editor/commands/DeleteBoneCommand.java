package com.animstudio.editor.commands;

import com.animstudio.core.model.Bone;
import com.animstudio.core.model.Skeleton;

/**
 * Command for deleting a bone from the skeleton.
 */
public class DeleteBoneCommand implements Command {
    
    private final Skeleton skeleton;
    private final Bone bone;
    private final Bone parent;
    private final int drawOrder;
    private final double x, y, rotation, scaleX, scaleY, length;
    
    public DeleteBoneCommand(Skeleton skeleton, Bone bone) {
        this.skeleton = skeleton;
        this.bone = bone;
        this.parent = bone.getParent();
        this.drawOrder = bone.getDrawOrder();
        this.x = bone.getX();
        this.y = bone.getY();
        this.rotation = bone.getRotation();
        this.scaleX = bone.getScaleX();
        this.scaleY = bone.getScaleY();
        this.length = bone.getLength();
    }
    
    @Override
    public void execute() {
        skeleton.removeBone(bone);
    }
    
    @Override
    public void undo() {
        // Restore bone with its properties
        bone.setX(x);
        bone.setY(y);
        bone.setRotation(rotation);
        bone.setScaleX(scaleX);
        bone.setScaleY(scaleY);
        bone.setLength(length);
        bone.setDrawOrder(drawOrder);
        
        if (parent != null) {
            bone.setParent(parent);
        }
        skeleton.addBone(bone);
    }
    
    @Override
    public String getDescription() {
        return "Delete Bone: " + bone.getName();
    }
}
