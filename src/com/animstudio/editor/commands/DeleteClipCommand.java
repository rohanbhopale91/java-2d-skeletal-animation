package com.animstudio.editor.commands;

import com.animstudio.core.animation.AnimationClip;
import com.animstudio.editor.EditorContext;
import com.animstudio.editor.project.Project;

/**
 * Command to delete an animation clip.
 */
public class DeleteClipCommand implements Command {
    
    private final Project project;
    private final AnimationClip clip;
    private final boolean wasSelected;
    
    public DeleteClipCommand(Project project, AnimationClip clip) {
        this.project = project;
        this.clip = clip;
        this.wasSelected = EditorContext.getInstance().getCurrentAnimation() == clip;
    }
    
    @Override
    public void execute() {
        project.removeAnimation(clip.getName());
        
        // If this was selected, select another
        if (wasSelected) {
            var clips = project.getAnimations();
            if (!clips.isEmpty()) {
                EditorContext.getInstance().setCurrentAnimation(clips.iterator().next());
            } else {
                EditorContext.getInstance().setCurrentAnimation(null);
            }
        }
    }
    
    @Override
    public void undo() {
        project.addAnimation(clip);
        
        if (wasSelected) {
            EditorContext.getInstance().setCurrentAnimation(clip);
        }
    }
    
    @Override
    public String getDescription() {
        return "Delete animation '" + clip.getName() + "'";
    }
}
