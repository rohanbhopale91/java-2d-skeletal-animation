package com.animstudio.editor.commands;

import com.animstudio.core.animation.AnimationClip;
import com.animstudio.core.animation.Keyframe;
import com.animstudio.core.animation.KeyframeTrack;
import com.animstudio.editor.EditorContext;
import com.animstudio.editor.project.Project;

/**
 * Command to duplicate an animation clip.
 */
public class DuplicateClipCommand implements Command {
    
    private final Project project;
    private final AnimationClip sourceClip;
    private AnimationClip duplicatedClip;
    
    public DuplicateClipCommand(Project project, AnimationClip sourceClip) {
        this.project = project;
        this.sourceClip = sourceClip;
    }
    
    @Override
    public void execute() {
        // Generate unique name
        String baseName = sourceClip.getName() + "_copy";
        String newName = baseName;
        int counter = 1;
        while (project.getAnimation(newName) != null) {
            newName = baseName + counter++;
        }
        
        // Create duplicate
        duplicatedClip = new AnimationClip(newName);
        duplicatedClip.setDuration(sourceClip.getDuration());
        duplicatedClip.setLooping(sourceClip.isLooping());
        
        // Copy all tracks and keyframes
        // AnimationClip uses Map<String, KeyframeTrack<Double>>
        for (KeyframeTrack<Double> sourceTrack : sourceClip.getTracks()) {
            // Get or create the track in the new clip
            KeyframeTrack<Double> newTrack = duplicatedClip.getOrCreateTrack(
                sourceTrack.getTargetPath(),
                sourceTrack.getPropertyType()
            );
            
            // Copy all keyframes using setKeyframe
            for (Keyframe<Double> kf : sourceTrack.getKeyframes()) {
                newTrack.setKeyframe(kf.getTime(), kf.getValue(), kf.getInterpolator());
            }
        }
        
        project.addAnimation(duplicatedClip);
        EditorContext.getInstance().setCurrentAnimation(duplicatedClip);
    }
    
    @Override
    public void undo() {
        if (duplicatedClip != null) {
            project.removeAnimation(duplicatedClip.getName());
            EditorContext.getInstance().setCurrentAnimation(sourceClip);
        }
    }
    
    @Override
    public String getDescription() {
        return "Duplicate animation '" + sourceClip.getName() + "'";
    }
    
    public AnimationClip getDuplicatedClip() {
        return duplicatedClip;
    }
}
