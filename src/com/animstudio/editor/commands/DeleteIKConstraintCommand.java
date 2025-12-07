package com.animstudio.editor.commands;

import com.animstudio.core.ik.IKConstraint;
import com.animstudio.core.ik.IKManager;

/**
 * Command to delete an IK constraint.
 */
public class DeleteIKConstraintCommand implements Command {
    
    private final IKManager ikManager;
    private final IKConstraint constraint;
    private final String constraintName;
    
    public DeleteIKConstraintCommand(IKManager ikManager, IKConstraint constraint) {
        this.ikManager = ikManager;
        this.constraint = constraint;
        this.constraintName = constraint.getName();
    }
    
    @Override
    public void execute() {
        ikManager.removeConstraint(constraintName);
    }
    
    @Override
    public void undo() {
        ikManager.addConstraint(constraint);
    }
    
    @Override
    public String getDescription() {
        return "Delete IK Constraint '" + constraintName + "'";
    }
}
