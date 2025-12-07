package com.animstudio.editor.project;

import com.animstudio.core.animation.AnimationClip;
import com.animstudio.core.model.Skeleton;

import java.util.ArrayList;
import java.util.List;

/**
 * A serializable project structure for saving/loading.
 * This is a simpler data class used for file I/O.
 */
public class AnimationProject {
    
    private String name = "Untitled";
    private String version = "1.0";
    private Skeleton skeleton;
    private final List<AnimationClip> animations = new ArrayList<>();
    
    // Canvas settings
    private int canvasWidth = 800;
    private int canvasHeight = 600;
    private double frameRate = 30.0;
    
    public AnimationProject() {}
    
    public AnimationProject(String name) {
        this.name = name;
    }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    
    public Skeleton getSkeleton() { return skeleton; }
    public void setSkeleton(Skeleton skeleton) { this.skeleton = skeleton; }
    
    public List<AnimationClip> getAnimations() { return animations; }
    
    public void addAnimation(AnimationClip clip) {
        if (clip != null && !animations.contains(clip)) {
            animations.add(clip);
        }
    }
    
    public void removeAnimation(AnimationClip clip) {
        animations.remove(clip);
    }
    
    public int getCanvasWidth() { return canvasWidth; }
    public void setCanvasWidth(int width) { this.canvasWidth = width; }
    
    public int getCanvasHeight() { return canvasHeight; }
    public void setCanvasHeight(int height) { this.canvasHeight = height; }
    
    public double getFrameRate() { return frameRate; }
    public void setFrameRate(double rate) { this.frameRate = rate; }
}
