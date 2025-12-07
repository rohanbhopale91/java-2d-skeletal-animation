package com.animstudio.editor.commands;

import com.animstudio.core.ik.IKConstraint;

/**
 * Command to modify IK constraint properties.
 */
public class ModifyIKConstraintCommand implements Command {
    
    private final IKConstraint constraint;
    private final String property;
    private final Object oldValue;
    private final Object newValue;
    
    public ModifyIKConstraintCommand(IKConstraint constraint, String property, 
                                      Object oldValue, Object newValue) {
        this.constraint = constraint;
        this.property = property;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }
    
    @Override
    public void execute() {
        applyValue(newValue);
    }
    
    @Override
    public void undo() {
        applyValue(oldValue);
    }
    
    private void applyValue(Object value) {
        switch (property) {
            case "mix":
                constraint.setMix((Double) value);
                break;
            case "bendPositive":
                constraint.setBendPositive((Boolean) value);
                break;
            case "compress":
                constraint.setCompress((Boolean) value);
                break;
            case "stretch":
                constraint.setStretch((Boolean) value);
                break;
            case "softness":
                constraint.setSoftness((Double) value);
                break;
            case "maxIterations":
                constraint.setMaxIterations((Integer) value);
                break;
            case "tolerance":
                constraint.setTolerance((Double) value);
                break;
            case "name":
                constraint.setName((String) value);
                break;
        }
    }
    
    @Override
    public String getDescription() {
        return "Modify IK Constraint '" + constraint.getName() + "' " + property;
    }
    
    @Override
    public boolean canMergeWith(Command other) {
        if (other instanceof ModifyIKConstraintCommand) {
            ModifyIKConstraintCommand otherCmd = (ModifyIKConstraintCommand) other;
            return otherCmd.constraint == this.constraint 
                && otherCmd.property.equals(this.property);
        }
        return false;
    }
    
    @Override
    public void mergeWith(Command other) {
        // The CommandStack handles merging by keeping track of the latest value
        // This implementation doesn't need to do anything since we use immutable commands
    }
}
