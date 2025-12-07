package com.animstudio.editor.commands;

import com.animstudio.core.animation.AnimationClip;
import com.animstudio.core.animation.Keyframe;
import com.animstudio.core.animation.KeyframeTrack;
import com.animstudio.core.interpolation.Interpolator;
import com.animstudio.core.interpolation.LinearInterpolator;

/**
 * Command for adding a keyframe.
 */
public class AddKeyframeCommand implements Command {
    
    private final AnimationClip clip;
    private final String targetPath;
    private final KeyframeTrack.PropertyType propertyType;
    private final double time;
    private final double value;
    private final Interpolator interpolator;
    private Keyframe<Double> previousKeyframe;
    
    public AddKeyframeCommand(AnimationClip clip, String targetPath, 
                              KeyframeTrack.PropertyType propertyType,
                              double time, double value) {
        this(clip, targetPath, propertyType, time, value, LinearInterpolator.INSTANCE);
    }
    
    public AddKeyframeCommand(AnimationClip clip, String targetPath,
                              KeyframeTrack.PropertyType propertyType,
                              double time, double value, Interpolator interpolator) {
        this.clip = clip;
        this.targetPath = targetPath;
        this.propertyType = propertyType;
        this.time = time;
        this.value = value;
        this.interpolator = interpolator;
    }
    
    @Override
    public void execute() {
        KeyframeTrack<Double> track = clip.getOrCreateTrack(targetPath, propertyType);
        // Save existing keyframe at this time for undo
        previousKeyframe = track.getKeyframeAt(time);
        track.setKeyframe(time, value, interpolator);
    }
    
    @Override
    public void undo() {
        KeyframeTrack<Double> track = clip.getTrack(targetPath);
        if (track != null) {
            if (previousKeyframe != null) {
                track.setKeyframe(previousKeyframe.getTime(), 
                    previousKeyframe.getValue(), 
                    previousKeyframe.getInterpolator());
            } else {
                track.removeKeyframe(time);
            }
        }
    }
    
    @Override
    public String getDescription() {
        return "Add Keyframe at " + time;
    }
}
