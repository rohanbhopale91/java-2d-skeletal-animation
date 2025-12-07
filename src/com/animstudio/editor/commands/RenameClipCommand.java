package com.animstudio.editor.commands;

import com.animstudio.core.animation.AnimationClip;
import com.animstudio.editor.project.Project;

/**
 * Command to rename an animation clip.
 */
public class RenameClipCommand implements Command {
    
    private final Project project;
    private final AnimationClip clip;
    private final String oldName;
    private final String newName;
    
    public RenameClipCommand(Project project, AnimationClip clip, String newName) {
        this.project = project;
        this.clip = clip;
        this.oldName = clip.getName();
        this.newName = newName;
    }
    
    @Override
    public void execute() {
        // Remove with old name, update, add with new name
        project.removeAnimation(oldName);
        clip.setName(newName);
        project.addAnimation(clip);
    }
    
    @Override
    public void undo() {
        project.removeAnimation(newName);
        clip.setName(oldName);
        project.addAnimation(clip);
    }
    
    @Override
    public String getDescription() {
        return "Rename animation '" + oldName + "' to '" + newName + "'";
    }
}
