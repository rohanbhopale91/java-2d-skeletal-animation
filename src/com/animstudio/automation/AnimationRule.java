package com.animstudio.automation;

import com.animstudio.core.model.Skeleton;

/**
 * Base interface for procedural animation rules.
 * Rules can be applied to skeletons to create automatic animations.
 */
public interface AnimationRule {
    
    /**
     * Get the unique name of this rule.
     */
    String getName();
    
    /**
     * Get a description of what this rule does.
     */
    String getDescription();
    
    /**
     * Check if the rule is currently enabled.
     */
    boolean isEnabled();
    
    /**
     * Enable or disable the rule.
     */
    void setEnabled(boolean enabled);
    
    /**
     * Apply the rule to a skeleton at the given time.
     * @param skeleton The skeleton to modify
     * @param time Current animation time in seconds
     * @param deltaTime Time since last update in seconds
     */
    void apply(Skeleton skeleton, double time, double deltaTime);
    
    /**
     * Reset the rule to its initial state.
     */
    void reset();
    
    /**
     * Get the target bone name this rule affects (null for multiple/all bones).
     */
    String getTargetBone();
    
    /**
     * Set the target bone name.
     */
    void setTargetBone(String boneName);
}
