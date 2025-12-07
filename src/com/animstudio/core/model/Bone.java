package com.animstudio.core.model;

import com.animstudio.core.math.Transform2D;
import com.animstudio.core.math.Vector2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Represents a single bone in a hierarchical skeleton.
 * Bones have parent-child relationships where child transforms are relative to parent.
 */
public class Bone {
    
    private final String id;
    private String name;
    private Bone parent;
    private final List<Bone> children;
    
    // Local transform (relative to parent)
    private final Transform2D localTransform;
    
    // World transform (computed from parent chain)
    private final Transform2D worldTransform;
    
    // Bone setup pose (the default/rest pose)
    private final Transform2D setupTransform;
    
    // Length of the bone (for visualization)
    private double length;
    
    // Whether this bone inherits rotation from parent
    private boolean inheritRotation;
    
    // Whether this bone inherits scale from parent
    private boolean inheritScale;
    
    // Draw order / z-index
    private int drawOrder;
    
    // Custom color for editor visualization
    private int color;
    
    public Bone(String name) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.children = new ArrayList<>();
        this.localTransform = new Transform2D();
        this.worldTransform = new Transform2D();
        this.setupTransform = new Transform2D();
        this.length = 50;
        this.inheritRotation = true;
        this.inheritScale = true;
        this.drawOrder = 0;
        this.color = 0xFFFFFF;
    }
    
    /**
     * Set the parent bone. Updates both this bone and the parent's children list.
     */
    public void setParent(Bone newParent) {
        if (this.parent != null) {
            this.parent.children.remove(this);
        }
        this.parent = newParent;
        if (newParent != null) {
            newParent.children.add(this);
        }
    }
    
    /**
     * Update this bone's world transform only (not children).
     * Useful for IK solvers that update bones one at a time.
     */
    public void updateWorldTransform() {
        if (parent == null) {
            worldTransform.set(localTransform);
        } else {
            worldTransform.set(parent.worldTransform);
            worldTransform.multiplyWithInheritance(localTransform, inheritRotation, inheritScale);
        }
    }
    
    /**
     * Compute world transform from parent chain.
     * Call this on root bone to update entire hierarchy.
     * 
     * World transform = parent.world * local (with inheritance options)
     */
    public void computeWorldTransform() {
        if (parent == null) {
            // Root bone: world = local
            worldTransform.set(localTransform);
        } else {
            // Child bone: world = parent.world * local
            worldTransform.set(parent.worldTransform);
            worldTransform.multiplyWithInheritance(localTransform, inheritRotation, inheritScale);
        }
        
        // Recursively update children
        for (Bone child : children) {
            child.computeWorldTransform();
        }
    }
    
    /**
     * Reset local transform to setup pose.
     */
    public void resetToSetupPose() {
        localTransform.set(setupTransform);
    }
    
    /**
     * Copy current local transform to setup pose.
     */
    public void setToSetupPose() {
        setupTransform.set(localTransform);
    }
    
    /**
     * Get the world position of the bone's origin.
     */
    public Vector2 getWorldPosition() {
        return worldTransform.getPosition();
    }
    
    /**
     * Get the world position of the bone's tip (end point).
     */
    public Vector2 getWorldTipPosition() {
        return worldTransform.transformPoint(new Vector2(length, 0));
    }
    
    /**
     * Get world rotation in degrees.
     */
    public double getWorldRotation() {
        return worldTransform.getRotation();
    }
    
    // === Getters and Setters ===
    
    public String getId() { return id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public Bone getParent() { return parent; }
    
    public List<Bone> getChildren() { return Collections.unmodifiableList(children); }
    
    public Transform2D getLocalTransform() { return localTransform; }
    public Transform2D getWorldTransform() { return worldTransform; }
    public Transform2D getSetupTransform() { return setupTransform; }
    
    public double getLength() { return length; }
    public void setLength(double length) { this.length = length; }
    
    public boolean isInheritRotation() { return inheritRotation; }
    public void setInheritRotation(boolean inherit) { this.inheritRotation = inherit; }
    
    public boolean isInheritScale() { return inheritScale; }
    public void setInheritScale(boolean inherit) { this.inheritScale = inherit; }
    
    public int getDrawOrder() { return drawOrder; }
    public void setDrawOrder(int order) { this.drawOrder = order; }
    
    public int getColor() { return color; }
    public void setColor(int color) { this.color = color; }
    
    // === Convenience methods for local transform ===
    
    public double getX() { return localTransform.getX(); }
    public void setX(double x) { localTransform.setX(x); }
    
    public double getY() { return localTransform.getY(); }
    public void setY(double y) { localTransform.setY(y); }
    
    public double getRotation() { return localTransform.getRotation(); }
    public void setRotation(double rotation) { localTransform.setRotation(rotation); }
    
    public double getScaleX() { return localTransform.getScaleX(); }
    public void setScaleX(double scaleX) { localTransform.setScaleX(scaleX); }
    
    public double getScaleY() { return localTransform.getScaleY(); }
    public void setScaleY(double scaleY) { localTransform.setScaleY(scaleY); }
    
    /**
     * Check if this bone is an ancestor of another bone.
     */
    public boolean isAncestorOf(Bone other) {
        Bone current = other.parent;
        while (current != null) {
            if (current == this) return true;
            current = current.parent;
        }
        return false;
    }
    
    /**
     * Get the depth of this bone in the hierarchy (root = 0).
     */
    public int getDepth() {
        int depth = 0;
        Bone current = parent;
        while (current != null) {
            depth++;
            current = current.parent;
        }
        return depth;
    }
    
    @Override
    public String toString() {
        return String.format("Bone[%s, parent=%s, children=%d]", 
            name, parent != null ? parent.name : "null", children.size());
    }
}
