package com.animstudio.core.event.events;

import com.animstudio.core.animation.AnimationClip;
import com.animstudio.core.event.EngineEvent;

/**
 * Event fired when the animation selection or playback state changes.
 */
public class AnimationChangedEvent extends EngineEvent {
    
    public enum ChangeType {
        SELECTED,           // New animation selected
        CREATED,            // New animation created
        DELETED,            // Animation deleted
        RENAMED,            // Animation renamed
        DURATION_CHANGED,   // Duration modified
        PLAYBACK_STARTED,   // Playback started
        PLAYBACK_STOPPED,   // Playback stopped
        TIME_CHANGED        // Playhead position changed
    }
    
    private final AnimationClip animation;
    private final ChangeType changeType;
    private final double time;
    
    public AnimationChangedEvent(AnimationClip animation, ChangeType changeType) {
        this(animation, changeType, 0);
    }
    
    public AnimationChangedEvent(AnimationClip animation, ChangeType changeType, double time) {
        super(animation);
        this.animation = animation;
        this.changeType = changeType;
        this.time = time;
    }
    
    public AnimationClip getAnimation() { return animation; }
    public ChangeType getChangeType() { return changeType; }
    public double getTime() { return time; }
}
