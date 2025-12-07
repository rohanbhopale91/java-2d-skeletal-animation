package com.animstudio.core.animation;

import com.animstudio.core.model.Skeleton;

import java.util.ArrayList;
import java.util.List;

/**
 * Runtime state machine for playing animations on a skeleton.
 * Supports layered playback and transitions.
 */
public class AnimationState {
    
    private final Skeleton skeleton;
    private final List<TrackEntry> tracks;
    private double timeScale;
    private final List<AnimationStateListener> listeners;
    
    public AnimationState(Skeleton skeleton) {
        this.skeleton = skeleton;
        this.tracks = new ArrayList<>();
        this.timeScale = 1.0;
        this.listeners = new ArrayList<>();
        
        // Create default track
        tracks.add(new TrackEntry(0));
    }
    
    /**
     * Set the animation for a track, replacing any existing animation.
     */
    public TrackEntry setAnimation(int trackIndex, AnimationClip clip, boolean loop) {
        ensureTrack(trackIndex);
        TrackEntry entry = tracks.get(trackIndex);
        entry.setAnimation(clip);
        entry.setLooping(loop);
        entry.setTime(0);
        
        for (AnimationStateListener listener : listeners) {
            listener.onAnimationStart(entry);
        }
        
        return entry;
    }
    
    /**
     * Add an animation to be played after the current one.
     */
    public TrackEntry addAnimation(int trackIndex, AnimationClip clip, boolean loop, double delay) {
        ensureTrack(trackIndex);
        TrackEntry current = tracks.get(trackIndex);
        
        TrackEntry next = new TrackEntry(trackIndex);
        next.setAnimation(clip);
        next.setLooping(loop);
        next.setDelay(delay);
        
        current.setNext(next);
        return next;
    }
    
    /**
     * Update animation state and apply to skeleton.
     */
    public void update(double deltaTime) {
        deltaTime *= timeScale;
        
        for (TrackEntry entry : tracks) {
            if (entry.getAnimation() == null) continue;
            
            entry.update(deltaTime);
            
            AnimationClip clip = entry.getAnimation();
            double time = entry.getTime();
            
            // Handle looping
            if (entry.isLooping() && time >= clip.getDuration()) {
                time = time % clip.getDuration();
                entry.setTime(time);
                
                for (AnimationStateListener listener : listeners) {
                    listener.onAnimationLoop(entry);
                }
            }
            
            // Handle completion
            if (!entry.isLooping() && time >= clip.getDuration()) {
                time = clip.getDuration();
                entry.setTime(time);
                
                if (!entry.isComplete()) {
                    entry.setComplete(true);
                    for (AnimationStateListener listener : listeners) {
                        listener.onAnimationComplete(entry);
                    }
                }
                
                // Check for queued animation
                if (entry.getNext() != null) {
                    TrackEntry next = entry.getNext();
                    entry.setAnimation(next.getAnimation());
                    entry.setLooping(next.isLooping());
                    entry.setTime(0);
                    entry.setComplete(false);
                    entry.setNext(next.getNext());
                    
                    for (AnimationStateListener listener : listeners) {
                        listener.onAnimationStart(entry);
                    }
                }
            }
            
            // Apply animation with mix/blend
            clip.apply(skeleton, time, entry.getAlpha());
            
            // Fire events
            List<AnimationEvent> events = clip.getEventsInRange(
                entry.getLastTime(), entry.getTime());
            for (AnimationEvent event : events) {
                for (AnimationStateListener listener : listeners) {
                    listener.onEvent(entry, event);
                }
            }
            
            entry.setLastTime(time);
        }
        
        skeleton.updateWorldTransforms();
    }
    
    /**
     * Apply current state without advancing time.
     */
    public void apply() {
        for (TrackEntry entry : tracks) {
            if (entry.getAnimation() == null) continue;
            entry.getAnimation().apply(skeleton, entry.getTime(), entry.getAlpha());
        }
        skeleton.updateWorldTransforms();
    }
    
    /**
     * Set the current time for track 0.
     */
    public void setTime(double time) {
        if (!tracks.isEmpty() && tracks.get(0).getAnimation() != null) {
            tracks.get(0).setTime(time);
        }
    }
    
    /**
     * Get the current time for track 0.
     */
    public double getTime() {
        if (!tracks.isEmpty()) {
            return tracks.get(0).getTime();
        }
        return 0;
    }
    
    /**
     * Clear all animations.
     */
    public void clearTracks() {
        for (TrackEntry entry : tracks) {
            entry.setAnimation(null);
            entry.setTime(0);
        }
    }
    
    /**
     * Clear a specific track.
     */
    public void clearTrack(int trackIndex) {
        if (trackIndex < tracks.size()) {
            TrackEntry entry = tracks.get(trackIndex);
            entry.setAnimation(null);
            entry.setTime(0);
        }
    }
    
    private void ensureTrack(int trackIndex) {
        while (tracks.size() <= trackIndex) {
            tracks.add(new TrackEntry(tracks.size()));
        }
    }
    
    public void addListener(AnimationStateListener listener) {
        listeners.add(listener);
    }
    
    public void removeListener(AnimationStateListener listener) {
        listeners.remove(listener);
    }
    
    public Skeleton getSkeleton() { return skeleton; }
    
    public double getTimeScale() { return timeScale; }
    public void setTimeScale(double timeScale) { this.timeScale = timeScale; }
    
    public TrackEntry getTrack(int index) {
        return index < tracks.size() ? tracks.get(index) : null;
    }
    
    /**
     * Entry for a single animation track.
     */
    public static class TrackEntry {
        private final int trackIndex;
        private AnimationClip animation;
        private double time;
        private double lastTime;
        private boolean looping;
        private boolean complete;
        private double alpha;
        private double delay;
        private TrackEntry next;
        
        public TrackEntry(int trackIndex) {
            this.trackIndex = trackIndex;
            this.alpha = 1.0;
        }
        
        public void update(double deltaTime) {
            if (delay > 0) {
                delay -= deltaTime;
                if (delay > 0) return;
                deltaTime = -delay;
            }
            time += deltaTime;
        }
        
        public int getTrackIndex() { return trackIndex; }
        
        public AnimationClip getAnimation() { return animation; }
        public void setAnimation(AnimationClip animation) { this.animation = animation; }
        
        public double getTime() { return time; }
        public void setTime(double time) { this.time = time; }
        
        public double getLastTime() { return lastTime; }
        public void setLastTime(double lastTime) { this.lastTime = lastTime; }
        
        public boolean isLooping() { return looping; }
        public void setLooping(boolean looping) { this.looping = looping; }
        
        public boolean isComplete() { return complete; }
        public void setComplete(boolean complete) { this.complete = complete; }
        
        public double getAlpha() { return alpha; }
        public void setAlpha(double alpha) { this.alpha = alpha; }
        
        public double getDelay() { return delay; }
        public void setDelay(double delay) { this.delay = delay; }
        
        public TrackEntry getNext() { return next; }
        public void setNext(TrackEntry next) { this.next = next; }
    }
    
    /**
     * Listener interface for animation events.
     */
    public interface AnimationStateListener {
        default void onAnimationStart(TrackEntry entry) {}
        default void onAnimationComplete(TrackEntry entry) {}
        default void onAnimationLoop(TrackEntry entry) {}
        default void onEvent(TrackEntry entry, AnimationEvent event) {}
    }
}
