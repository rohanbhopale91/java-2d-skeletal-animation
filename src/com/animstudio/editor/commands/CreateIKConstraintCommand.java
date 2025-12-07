package com.animstudio.editor.commands;

import com.animstudio.core.ik.IKConstraint;
import com.animstudio.core.ik.IKManager;
import com.animstudio.core.model.Bone;
import com.animstudio.editor.EditorContext;

import java.util.List;

/**
 * Command to create a new IK constraint.
 */
public class CreateIKConstraintCommand implements Command {
    
    private final IKManager ikManager;
    private final String constraintName;
    private final List<Bone> chainBones;
    private final Bone targetBone;
    private IKConstraint createdConstraint;
    
    public CreateIKConstraintCommand(IKManager ikManager, String name, 
                                     List<Bone> chainBones, Bone targetBone) {
        this.ikManager = ikManager;
        this.constraintName = name;
        this.chainBones = chainBones;
        this.targetBone = targetBone;
    }
    
    @Override
    public void execute() {
        createdConstraint = new IKConstraint(constraintName, chainBones, targetBone);
        ikManager.addConstraint(createdConstraint);
    }
    
    @Override
    public void undo() {
        if (createdConstraint != null) {
            ikManager.removeConstraint(constraintName);
        }
    }
    
    @Override
    public String getDescription() {
        return "Create IK Constraint '" + constraintName + "'";
    }
    
    public IKConstraint getCreatedConstraint() {
        return createdConstraint;
    }
}
