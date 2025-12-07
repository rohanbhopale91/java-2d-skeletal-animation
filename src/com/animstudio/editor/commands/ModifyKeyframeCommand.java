package com.animstudio.editor.commands;

import com.animstudio.core.animation.AnimationClip;
import com.animstudio.core.animation.Keyframe;
import com.animstudio.core.animation.KeyframeTrack;
import com.animstudio.core.interpolation.Interpolator;

/**
 * Command for modifying a keyframe's value.
 */
public class ModifyKeyframeCommand implements Command {
    
    private final KeyframeTrack<Double> track;
    private final double time;
    private final Double oldValue;
    private final Double newValue;
    private final Interpolator interpolator;
    
    public ModifyKeyframeCommand(KeyframeTrack<Double> track, double time, 
                                  Double oldValue, Double newValue, Interpolator interpolator) {
        this.track = track;
        this.time = time;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.interpolator = interpolator;
    }
    
    @Override
    public void execute() {
        track.setKeyframe(time, newValue, interpolator);
    }
    
    @Override
    public void undo() {
        if (oldValue != null) {
            track.setKeyframe(time, oldValue, interpolator);
        } else {
            track.removeKeyframe(time);
        }
    }
    
    @Override
    public String getDescription() {
        return String.format("Modify Keyframe at %.1f", time);
    }
    
    @Override
    public boolean canMergeWith(Command other) {
        if (other instanceof ModifyKeyframeCommand) {
            ModifyKeyframeCommand otherCmd = (ModifyKeyframeCommand) other;
            return otherCmd.track == this.track && otherCmd.time == this.time;
        }
        return false;
    }
}
