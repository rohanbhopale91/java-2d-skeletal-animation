package com.animstudio.core.event.events;

import com.animstudio.core.animation.Keyframe;
import com.animstudio.core.animation.KeyframeTrack;
import com.animstudio.core.event.EngineEvent;

/**
 * Event fired when a keyframe is added, removed, or modified.
 */
public class KeyframeModifiedEvent extends EngineEvent {
    
    public enum ModificationType {
        ADDED,
        REMOVED,
        VALUE_CHANGED,
        TIME_CHANGED,
        INTERPOLATION_CHANGED
    }
    
    private final KeyframeTrack<?> track;
    private final Keyframe<?> keyframe;
    private final ModificationType modificationType;
    
    public KeyframeModifiedEvent(KeyframeTrack<?> track, Keyframe<?> keyframe, 
                                  ModificationType type) {
        super(track);
        this.track = track;
        this.keyframe = keyframe;
        this.modificationType = type;
    }
    
    public KeyframeTrack<?> getTrack() { return track; }
    public Keyframe<?> getKeyframe() { return keyframe; }
    public ModificationType getModificationType() { return modificationType; }
}
