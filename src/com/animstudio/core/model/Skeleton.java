package com.animstudio.core.model;

import com.animstudio.core.ik.IKManager;
import com.animstudio.core.mesh.DeformableMesh;

import java.util.*;

/**
 * Represents a complete skeleton consisting of hierarchical bones and attachment slots.
 */
public class Skeleton {
    
    private final String id;
    private String name;
    private Bone rootBone;
    private final Map<String, Bone> bonesByName;
    private final Map<String, Bone> bonesById;
    private final List<Slot> slots;
    private final Map<String, Slot> slotsByName;
    
    // IK and mesh support
    private IKManager ikManager;
    private final List<DeformableMesh> meshes;
    
    // Skeleton dimensions for editor
    private double width;
    private double height;
    
    public Skeleton(String name) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.bonesByName = new LinkedHashMap<>();
        this.bonesById = new HashMap<>();
        this.slots = new ArrayList<>();
        this.slotsByName = new HashMap<>();
        this.meshes = new ArrayList<>();
        this.ikManager = new IKManager(this);
        this.width = 200;
        this.height = 200;
    }
    
    /**
     * Add a bone to the skeleton.
     */
    public void addBone(Bone bone) {
        if (bonesByName.containsKey(bone.getName())) {
            throw new IllegalArgumentException("Bone with name '" + bone.getName() + "' already exists");
        }
        bonesByName.put(bone.getName(), bone);
        bonesById.put(bone.getId(), bone);
        
        if (bone.getParent() == null && rootBone == null) {
            rootBone = bone;
        }
    }
    
    /**
     * Remove a bone and all its children from the skeleton.
     * This method:
     * 1. Recursively removes all child bones
     * 2. Removes slots attached to this bone
     * 3. Returns the list of removed bone names (for animation track cleanup)
     * 
     * @param bone The bone to remove
     * @return List of removed bone names (including children)
     */
    public List<String> removeBone(Bone bone) {
        List<String> removedBoneNames = new ArrayList<>();
        removeBoneRecursive(bone, removedBoneNames);
        
        // Update world transforms if we still have a root
        if (rootBone != null) {
            updateWorldTransforms();
        }
        
        return removedBoneNames;
    }
    
    private void removeBoneRecursive(Bone bone, List<String> removedNames) {
        // Remove all children first (recursively)
        for (Bone child : new ArrayList<>(bone.getChildren())) {
            removeBoneRecursive(child, removedNames);
        }
        
        // Track removed bone name
        removedNames.add(bone.getName());
        
        // Remove from parent
        if (bone.getParent() != null) {
            bone.setParent(null);
        }
        
        // Remove any slots attached to this bone
        slots.removeIf(slot -> slot.getBone() == bone);
        slotsByName.entrySet().removeIf(entry -> entry.getValue().getBone() == bone);
        
        // Remove from maps
        bonesByName.remove(bone.getName());
        bonesById.remove(bone.getId());
        
        if (bone == rootBone) {
            rootBone = null;
        }
    }
    
    /**
     * Get a bone by name.
     */
    public Bone getBone(String name) {
        return bonesByName.get(name);
    }
    
    /**
     * Get a bone by ID.
     */
    public Bone getBoneById(String id) {
        return bonesById.get(id);
    }
    
    /**
     * Get all bones in the skeleton.
     */
    public Collection<Bone> getBones() {
        return Collections.unmodifiableCollection(bonesByName.values());
    }
    
    /**
     * Get all bones as a flat list in hierarchy order (parents before children).
     */
    public List<Bone> getBonesInOrder() {
        List<Bone> result = new ArrayList<>();
        if (rootBone != null) {
            collectBonesInOrder(rootBone, result);
        }
        return result;
    }
    
    private void collectBonesInOrder(Bone bone, List<Bone> result) {
        result.add(bone);
        for (Bone child : bone.getChildren()) {
            collectBonesInOrder(child, result);
        }
    }
    
    /**
     * Add a slot to the skeleton.
     */
    public void addSlot(Slot slot) {
        if (slotsByName.containsKey(slot.getName())) {
            throw new IllegalArgumentException("Slot with name '" + slot.getName() + "' already exists");
        }
        slots.add(slot);
        slotsByName.put(slot.getName(), slot);
    }
    
    /**
     * Remove a slot from the skeleton.
     */
    public void removeSlot(Slot slot) {
        slots.remove(slot);
        slotsByName.remove(slot.getName());
    }
    
    /**
     * Get a slot by name.
     */
    public Slot getSlot(String name) {
        return slotsByName.get(name);
    }
    
    /**
     * Find a slot attached to the given bone.
     * @param bone The bone to search for
     * @return The first slot attached to the bone, or null if none found
     */
    public Slot findSlotForBone(Bone bone) {
        if (bone == null) return null;
        for (Slot slot : slots) {
            if (slot.getBone() == bone) {
                return slot;
            }
        }
        return null;
    }
    
    /**
     * Get all slots.
     */
    public List<Slot> getSlots() {
        return Collections.unmodifiableList(slots);
    }
    
    /**
     * Get slots sorted by draw order.
     */
    public List<Slot> getSlotsByDrawOrder() {
        List<Slot> sorted = new ArrayList<>(slots);
        sorted.sort(Comparator.comparingInt(Slot::getDrawOrder));
        return sorted;
    }
    
    /**
     * Update world transforms for all bones.
     */
    public void updateWorldTransforms() {
        if (rootBone != null) {
            rootBone.computeWorldTransform();
        }
    }
    
    /**
     * Get the world-space bounding box of all bones.
     * Useful for zoom-to-fit functionality in the editor.
     * 
     * @return double[4] containing [minX, minY, maxX, maxY], or null if no bones
     */
    public double[] getBoneWorldBounds() {
        if (bonesByName.isEmpty()) {
            return null;
        }
        
        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        double maxY = -Double.MAX_VALUE;
        
        for (Bone bone : bonesByName.values()) {
            // Include bone origin
            double bx = bone.getWorldTransform().getX();
            double by = bone.getWorldTransform().getY();
            minX = Math.min(minX, bx);
            minY = Math.min(minY, by);
            maxX = Math.max(maxX, bx);
            maxY = Math.max(maxY, by);
            
            // Include bone tip
            var tipPos = bone.getWorldTipPosition();
            minX = Math.min(minX, tipPos.x);
            minY = Math.min(minY, tipPos.y);
            maxX = Math.max(maxX, tipPos.x);
            maxY = Math.max(maxY, tipPos.y);
        }
        
        // Add some padding
        double padding = 20;
        return new double[] { minX - padding, minY - padding, maxX + padding, maxY + padding };
    }
    
    /**
     * Get the center point of all bones in world space.
     * @return Center point as [x, y], or [0, 0] if no bones
     */
    public double[] getBoneWorldCenter() {
        double[] bounds = getBoneWorldBounds();
        if (bounds == null) {
            return new double[] { 0, 0 };
        }
        return new double[] { 
            (bounds[0] + bounds[2]) / 2, 
            (bounds[1] + bounds[3]) / 2 
        };
    }
    
    /**
     * Reset all bones to their setup pose.
     */
    public void resetToSetupPose() {
        for (Bone bone : bonesByName.values()) {
            bone.resetToSetupPose();
        }
        updateWorldTransforms();
    }
    
    /**
     * Find a bone at the given world position (for editor picking).
     */
    public Bone findBoneAtPosition(double worldX, double worldY, double threshold) {
        Bone closest = null;
        double closestDist = threshold;
        
        for (Bone bone : bonesByName.values()) {
            // Check distance to bone origin
            double dx = bone.getWorldTransform().getX() - worldX;
            double dy = bone.getWorldTransform().getY() - worldY;
            double dist = Math.sqrt(dx * dx + dy * dy);
            
            if (dist < closestDist) {
                closestDist = dist;
                closest = bone;
            }
        }
        
        return closest;
    }
    
    // === Getters and Setters ===
    
    public String getId() { return id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public Bone getRootBone() { return rootBone; }
    
    public int getBoneCount() { return bonesByName.size(); }
    public int getSlotCount() { return slots.size(); }
    
    public double getWidth() { return width; }
    public void setWidth(double width) { this.width = width; }
    
    public double getHeight() { return height; }
    public void setHeight(double height) { this.height = height; }
    
    // === IK Manager ===
    
    /**
     * Get the IK manager for this skeleton.
     */
    public IKManager getIKManager() { return ikManager; }
    
    /**
     * Set the IK manager for this skeleton.
     */
    public void setIKManager(IKManager manager) { this.ikManager = manager; }
    
    // === Deformable Meshes ===
    
    /**
     * Get all deformable meshes attached to this skeleton.
     */
    public List<DeformableMesh> getMeshes() { 
        return Collections.unmodifiableList(meshes); 
    }
    
    /**
     * Add a deformable mesh to this skeleton.
     */
    public void addMesh(DeformableMesh mesh) {
        if (mesh != null && !meshes.contains(mesh)) {
            meshes.add(mesh);
        }
    }
    
    /**
     * Remove a deformable mesh from this skeleton.
     */
    public void removeMesh(DeformableMesh mesh) {
        meshes.remove(mesh);
    }
    
    /**
     * Get a mesh by name.
     */
    public DeformableMesh getMesh(String name) {
        for (DeformableMesh mesh : meshes) {
            if (mesh.getName().equals(name)) {
                return mesh;
            }
        }
        return null;
    }
    
    /**
     * Find a bone by name (alias for getBone for compatibility).
     */
    public Bone findBone(String name) {
        return bonesByName.get(name);
    }
    
    @Override
    public String toString() {
        return String.format("Skeleton[%s, bones=%d, slots=%d]", name, getBoneCount(), getSlotCount());
    }
}
