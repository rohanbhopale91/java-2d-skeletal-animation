package com.animstudio.core.event.events;

import com.animstudio.core.event.EngineEvent;
import com.animstudio.core.model.Bone;

/**
 * Event fired when a bone is modified (transform, properties, etc.).
 */
public class BoneModifiedEvent extends EngineEvent {
    
    public enum ModificationType {
        TRANSFORM,
        PROPERTY,
        PARENT,
        NAME,
        CREATED,
        DELETED
    }
    
    private final Bone bone;
    private final ModificationType modificationType;
    private final String propertyName;
    private final Object oldValue;
    private final Object newValue;
    
    public BoneModifiedEvent(Bone bone, ModificationType type) {
        this(bone, type, null, null, null);
    }
    
    public BoneModifiedEvent(Bone bone, ModificationType type, String propertyName, 
                             Object oldValue, Object newValue) {
        super(bone);
        this.bone = bone;
        this.modificationType = type;
        this.propertyName = propertyName;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }
    
    public Bone getBone() { return bone; }
    public ModificationType getModificationType() { return modificationType; }
    public String getPropertyName() { return propertyName; }
    public Object getOldValue() { return oldValue; }
    public Object getNewValue() { return newValue; }
}
