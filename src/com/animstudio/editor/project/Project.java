package com.animstudio.editor.project;

import com.animstudio.core.animation.AnimationClip;
import com.animstudio.core.model.Skeleton;

import java.util.*;

/**
 * Represents a complete animation project.
 */
public class Project {
    
    private String name;
    private Skeleton skeleton;
    private final Map<String, AnimationClip> animations;
    private final Map<String, String> metadata;
    
    // Project settings
    private double frameRate = 30.0;
    private int canvasWidth = 800;
    private int canvasHeight = 600;
    
    public Project(String name) {
        this.name = name;
        this.animations = new LinkedHashMap<>();
        this.metadata = new HashMap<>();
        this.metadata.put("created", new Date().toString());
    }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public Skeleton getSkeleton() { return skeleton; }
    public void setSkeleton(Skeleton skeleton) { this.skeleton = skeleton; }
    
    public void addAnimation(AnimationClip clip) {
        animations.put(clip.getName(), clip);
    }
    
    public void removeAnimation(String name) {
        animations.remove(name);
    }
    
    public AnimationClip getAnimation(String name) {
        return animations.get(name);
    }
    
    public Collection<AnimationClip> getAnimations() {
        return Collections.unmodifiableCollection(animations.values());
    }
    
    public List<String> getAnimationNames() {
        return new ArrayList<>(animations.keySet());
    }
    
    public double getFrameRate() { return frameRate; }
    public void setFrameRate(double frameRate) { this.frameRate = frameRate; }
    
    public int getCanvasWidth() { return canvasWidth; }
    public void setCanvasWidth(int width) { this.canvasWidth = width; }
    
    public int getCanvasHeight() { return canvasHeight; }
    public void setCanvasHeight(int height) { this.canvasHeight = height; }
    
    public void setMetadata(String key, String value) {
        metadata.put(key, value);
    }
    
    public String getMetadata(String key) {
        return metadata.get(key);
    }
    
    public Map<String, String> getAllMetadata() {
        return new HashMap<>(metadata);
    }
}
