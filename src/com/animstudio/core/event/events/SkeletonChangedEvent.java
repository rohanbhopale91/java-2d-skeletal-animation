package com.animstudio.core.event.events;

import com.animstudio.core.event.EngineEvent;
import com.animstudio.core.model.Skeleton;

/**
 * Event fired when the skeleton structure changes.
 */
public class SkeletonChangedEvent extends EngineEvent {
    
    public enum ChangeType {
        LOADED,             // New skeleton loaded
        BONE_ADDED,         // Bone added to skeleton
        BONE_REMOVED,       // Bone removed from skeleton
        SLOT_ADDED,         // Slot added
        SLOT_REMOVED,       // Slot removed
        STRUCTURE_CHANGED   // General structural change
    }
    
    private final Skeleton skeleton;
    private final ChangeType changeType;
    
    public SkeletonChangedEvent(Skeleton skeleton, ChangeType changeType) {
        super(skeleton);
        this.skeleton = skeleton;
        this.changeType = changeType;
    }
    
    public Skeleton getSkeleton() { return skeleton; }
    public ChangeType getChangeType() { return changeType; }
}
