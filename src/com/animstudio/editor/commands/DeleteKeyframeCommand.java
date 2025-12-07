package com.animstudio.editor.commands;

import com.animstudio.core.animation.AnimationClip;
import com.animstudio.core.animation.Keyframe;
import com.animstudio.core.animation.KeyframeTrack;

/**
 * Command for deleting a keyframe from a track.
 */
public class DeleteKeyframeCommand implements Command {
    
    private final AnimationClip clip;
    private final String targetPath;
    private final KeyframeTrack.PropertyType propertyType;
    private final double time;
    private Keyframe<Double> removedKeyframe;
    
    public DeleteKeyframeCommand(AnimationClip clip, String targetPath, 
                                  KeyframeTrack.PropertyType propertyType, double time) {
        this.clip = clip;
        this.targetPath = targetPath;
        this.propertyType = propertyType;
        this.time = time;
    }
    
    @Override
    public void execute() {
        KeyframeTrack<Double> track = clip.getTrack(targetPath);
        if (track != null) {
            removedKeyframe = track.getKeyframeAt(time);
            track.removeKeyframe(time);
        }
    }
    
    @Override
    public void undo() {
        if (removedKeyframe != null) {
            KeyframeTrack<Double> track = clip.getOrCreateTrack(targetPath, propertyType);
            track.setKeyframe(removedKeyframe.getTime(), removedKeyframe.getValue(), 
                             removedKeyframe.getInterpolator());
        }
    }
    
    @Override
    public String getDescription() {
        return "Delete Keyframe at " + time;
    }
}
