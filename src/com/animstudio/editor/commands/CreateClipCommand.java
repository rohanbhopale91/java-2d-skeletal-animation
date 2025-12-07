package com.animstudio.editor.commands;

import com.animstudio.core.animation.AnimationClip;
import com.animstudio.editor.EditorContext;
import com.animstudio.editor.project.Project;

/**
 * Command to create a new animation clip.
 */
public class CreateClipCommand implements Command {
    
    private final Project project;
    private final String clipName;
    private final double duration;
    private AnimationClip createdClip;
    
    public CreateClipCommand(Project project, String clipName, double duration) {
        this.project = project;
        this.clipName = clipName;
        this.duration = duration;
    }
    
    public CreateClipCommand(Project project, String clipName) {
        this(project, clipName, 2.0); // Default 2 seconds
    }
    
    @Override
    public void execute() {
        createdClip = new AnimationClip(clipName);
        createdClip.setDuration(duration);
        project.addAnimation(createdClip);
        
        // Select the new clip
        EditorContext.getInstance().setCurrentAnimation(createdClip);
    }
    
    @Override
    public void undo() {
        if (createdClip != null) {
            project.removeAnimation(createdClip.getName());
            
            // Select another clip if available
            var clips = project.getAnimations();
            if (!clips.isEmpty()) {
                EditorContext.getInstance().setCurrentAnimation(clips.iterator().next());
            } else {
                EditorContext.getInstance().setCurrentAnimation(null);
            }
        }
    }
    
    @Override
    public String getDescription() {
        return "Create animation '" + clipName + "'";
    }
    
    public AnimationClip getCreatedClip() {
        return createdClip;
    }
}
