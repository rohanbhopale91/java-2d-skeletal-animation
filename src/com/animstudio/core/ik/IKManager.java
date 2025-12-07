package com.animstudio.core.ik;

import com.animstudio.core.model.Skeleton;

import java.util.*;

/**
 * Manages IK constraints for a skeleton.
 */
public class IKManager {
    
    private final Skeleton skeleton;
    private final Map<String, IKConstraint> constraints;
    private final List<IKConstraint> orderedConstraints;
    private boolean enabled = true;
    
    public IKManager(Skeleton skeleton) {
        this.skeleton = skeleton;
        this.constraints = new LinkedHashMap<>();
        this.orderedConstraints = new ArrayList<>();
    }
    
    /**
     * Add an IK constraint.
     */
    public void addConstraint(IKConstraint constraint) {
        if (constraint == null || constraint.getName() == null) return;
        
        constraints.put(constraint.getName(), constraint);
        if (!orderedConstraints.contains(constraint)) {
            orderedConstraints.add(constraint);
        }
    }
    
    /**
     * Remove an IK constraint.
     */
    public void removeConstraint(String name) {
        IKConstraint constraint = constraints.remove(name);
        if (constraint != null) {
            orderedConstraints.remove(constraint);
        }
    }
    
    /**
     * Get a constraint by name.
     */
    public IKConstraint getConstraint(String name) {
        return constraints.get(name);
    }
    
    /**
     * Get all constraints.
     */
    public Collection<IKConstraint> getConstraints() {
        return Collections.unmodifiableCollection(orderedConstraints);
    }
    
    /**
     * Apply all enabled IK constraints.
     */
    public void applyConstraints() {
        if (!enabled) return;
        
        for (IKConstraint constraint : orderedConstraints) {
            if (constraint.getMix() > 0) {
                constraint.apply();
            }
        }
    }
    
    /**
     * Update constraint order (for priority).
     */
    public void setConstraintOrder(List<String> orderedNames) {
        orderedConstraints.clear();
        for (String name : orderedNames) {
            IKConstraint c = constraints.get(name);
            if (c != null) {
                orderedConstraints.add(c);
            }
        }
        
        // Add any remaining constraints not in the order list
        for (IKConstraint c : constraints.values()) {
            if (!orderedConstraints.contains(c)) {
                orderedConstraints.add(c);
            }
        }
    }
    
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    
    public Skeleton getSkeleton() { return skeleton; }
    
    public int getConstraintCount() { return constraints.size(); }
    
    public void clear() {
        constraints.clear();
        orderedConstraints.clear();
    }
}
