package com.animstudio.core.animation;

import com.animstudio.core.model.Bone;
import com.animstudio.core.model.Skeleton;
import com.animstudio.core.model.Slot;
import com.animstudio.core.util.TimeUtils;

import java.util.*;

/**
 * An animation clip containing multiple tracks.
 * Represents a single animation like "idle", "walk", "attack".
 * 
 * TIME CONVENTION: All time values (duration, keyframe times) are in SECONDS.
 * The UI may display frames but converts to seconds when calling AnimationClip methods.
 */
public class AnimationClip {
    
    private final String id;
    private String name;
    private double duration;            // Duration in SECONDS
    private boolean looping;
    
    // Tracks organized by target path (e.g., "spine.rotation", "arm_L.x")
    private final Map<String, KeyframeTrack<Double>> doubleTracks;
    
    // Events at specific times (in seconds)
    private final List<AnimationEvent> events;
    
    /**
     * Create a new animation clip with default 2-second duration.
     */
    public AnimationClip(String name) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.duration = 2.0;  // Default 2 seconds (60 frames at 30fps)
        this.looping = true;
        this.doubleTracks = new LinkedHashMap<>();
        this.events = new ArrayList<>();
    }
    
    /**
     * Get or create a double track for the given target path.
     */
    public KeyframeTrack<Double> getOrCreateTrack(String targetPath, KeyframeTrack.PropertyType type) {
        return doubleTracks.computeIfAbsent(targetPath, 
            path -> KeyframeTrack.createDoubleTrack(path, type));
    }
    
    /**
     * Get track by target path.
     */
    public KeyframeTrack<Double> getTrack(String targetPath) {
        return doubleTracks.get(targetPath);
    }
    
    /**
     * Get all tracks for a specific bone.
     */
    public List<KeyframeTrack<Double>> getTracksForBone(String boneName) {
        List<KeyframeTrack<Double>> result = new ArrayList<>();
        for (KeyframeTrack<Double> track : doubleTracks.values()) {
            if (track.getBoneName().equals(boneName)) {
                result.add(track);
            }
        }
        return result;
    }
    
    /**
     * Get all tracks.
     */
    public Collection<KeyframeTrack<Double>> getTracks() {
        return Collections.unmodifiableCollection(doubleTracks.values());
    }
    
    /**
     * Remove a track.
     */
    public void removeTrack(String targetPath) {
        doubleTracks.remove(targetPath);
    }
    
    /**
     * Apply this animation to a skeleton at the given time.
     */
    public void apply(Skeleton skeleton, double time, double alpha) {
        for (KeyframeTrack<Double> track : doubleTracks.values()) {
            Double value = track.evaluate(time);
            if (value == null) continue;
            
            String boneName = track.getBoneName();
            String property = track.getPropertyName();
            
            Bone bone = skeleton.getBone(boneName);
            if (bone != null) {
                applyBoneProperty(bone, property, value, alpha);
            } else {
                // Check if it's a slot property
                Slot slot = skeleton.getSlot(boneName);
                if (slot != null) {
                    applySlotProperty(slot, property, value, alpha);
                }
            }
        }
    }
    
    /**
     * Apply this animation fully (alpha = 1).
     */
    public void apply(Skeleton skeleton, double time) {
        apply(skeleton, time, 1.0);
    }
    
    private void applyBoneProperty(Bone bone, String property, double value, double alpha) {
        switch (property) {
            case "x":
                if (alpha >= 1) bone.setX(value);
                else bone.setX(lerp(bone.getX(), value, alpha));
                break;
            case "y":
                if (alpha >= 1) bone.setY(value);
                else bone.setY(lerp(bone.getY(), value, alpha));
                break;
            case "rotation":
                if (alpha >= 1) bone.setRotation(value);
                else bone.setRotation(lerp(bone.getRotation(), value, alpha));
                break;
            case "scaleX":
                if (alpha >= 1) bone.setScaleX(value);
                else bone.setScaleX(lerp(bone.getScaleX(), value, alpha));
                break;
            case "scaleY":
                if (alpha >= 1) bone.setScaleY(value);
                else bone.setScaleY(lerp(bone.getScaleY(), value, alpha));
                break;
            case "shearX":
                if (alpha >= 1) bone.getLocalTransform().setShearX(value);
                else bone.getLocalTransform().setShearX(lerp(bone.getLocalTransform().getShearX(), value, alpha));
                break;
            case "shearY":
                if (alpha >= 1) bone.getLocalTransform().setShearY(value);
                else bone.getLocalTransform().setShearY(lerp(bone.getLocalTransform().getShearY(), value, alpha));
                break;
        }
    }
    
    private void applySlotProperty(Slot slot, String property, double value, double alpha) {
        switch (property) {
            case "alpha":
                if (alpha >= 1) slot.setAlpha((float) value);
                else slot.setAlpha((float) lerp(slot.getAlpha(), value, alpha));
                break;
            case "red":
                if (alpha >= 1) slot.setRed((float) value);
                else slot.setRed((float) lerp(slot.getRed(), value, alpha));
                break;
            case "green":
                if (alpha >= 1) slot.setGreen((float) value);
                else slot.setGreen((float) lerp(slot.getGreen(), value, alpha));
                break;
            case "blue":
                if (alpha >= 1) slot.setBlue((float) value);
                else slot.setBlue((float) lerp(slot.getBlue(), value, alpha));
                break;
        }
    }
    
    private double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }
    
    /**
     * Add an animation event.
     */
    public void addEvent(AnimationEvent event) {
        events.add(event);
        events.sort(Comparator.comparingDouble(AnimationEvent::getTime));
    }
    
    /**
     * Get events in a time range.
     */
    public List<AnimationEvent> getEventsInRange(double startTime, double endTime) {
        List<AnimationEvent> result = new ArrayList<>();
        for (AnimationEvent event : events) {
            if (event.getTime() >= startTime && event.getTime() < endTime) {
                result.add(event);
            }
        }
        return result;
    }
    
    /**
     * Calculate actual duration from track content.
     */
    public double calculateDuration() {
        double maxTime = 0;
        for (KeyframeTrack<Double> track : doubleTracks.values()) {
            maxTime = Math.max(maxTime, track.getEndTime());
        }
        return maxTime;
    }
    
    /**
     * Auto-set duration based on last keyframe.
     */
    public void fitDurationToKeyframes() {
        this.duration = calculateDuration();
    }
    
    // === Getters and Setters ===
    
    public String getId() { return id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public double getDuration() { return duration; }
    public void setDuration(double duration) { this.duration = duration; }
    
    public boolean isLooping() { return looping; }
    public void setLooping(boolean looping) { this.looping = looping; }
    
    public List<AnimationEvent> getEvents() {
        return Collections.unmodifiableList(events);
    }
    
    /**
     * Get all unique bone names that have keyframes in this clip.
     */
    public Set<String> getBoneNames() {
        Set<String> names = new LinkedHashSet<>();
        for (KeyframeTrack<Double> track : doubleTracks.values()) {
            names.add(track.getBoneName());
        }
        return names;
    }
    
    /**
     * Get all timeline/track names (for compatibility).
     */
    public Set<String> getTimelineNames() {
        return getBoneNames();
    }
    
    /**
     * Get a consolidated view of keyframes for a bone (compatibility method).
     * Returns null if no tracks exist for this bone.
     */
    public BoneKeyframes getTimeline(String boneName) {
        List<KeyframeTrack<Double>> tracks = getTracksForBone(boneName);
        if (tracks.isEmpty()) return null;
        return new BoneKeyframes(boneName, tracks);
    }
    
    /**
     * Add a keyframe for a bone (convenience method).
     * Creates tracks as needed.
     * @param boneName Name of the bone
     * @param kf BoneKeyframe with time in seconds
     */
    public void addKeyframe(String boneName, BoneKeyframe kf) {
        double time = kf.getTime();  // Time is now in seconds
        if (kf.getX() != null) {
            KeyframeTrack<Double> track = getOrCreateTrack(boneName + ".x", KeyframeTrack.PropertyType.TRANSLATION);
            track.setKeyframe(time, kf.getX());
        }
        if (kf.getY() != null) {
            KeyframeTrack<Double> track = getOrCreateTrack(boneName + ".y", KeyframeTrack.PropertyType.TRANSLATION);
            track.setKeyframe(time, kf.getY());
        }
        if (kf.getRotation() != null) {
            KeyframeTrack<Double> track = getOrCreateTrack(boneName + ".rotation", KeyframeTrack.PropertyType.ROTATION);
            track.setKeyframe(time, kf.getRotation());
        }
        if (kf.getScaleX() != null) {
            KeyframeTrack<Double> track = getOrCreateTrack(boneName + ".scaleX", KeyframeTrack.PropertyType.SCALE);
            track.setKeyframe(time, kf.getScaleX());
        }
        if (kf.getScaleY() != null) {
            KeyframeTrack<Double> track = getOrCreateTrack(boneName + ".scaleY", KeyframeTrack.PropertyType.SCALE);
            track.setKeyframe(time, kf.getScaleY());
        }
    }
    
    /**
     * Remove a keyframe at a specific time (in seconds) for a bone.
     * @param boneName Name of the bone
     * @param timeSeconds Time in seconds
     */
    public void removeKeyframe(String boneName, double timeSeconds) {
        for (KeyframeTrack<Double> track : getTracksForBone(boneName)) {
            track.removeKeyframe(timeSeconds);
        }
    }
    
    /**
     * Copy this animation clip.
     */
    public AnimationClip copy() {
        AnimationClip copy = new AnimationClip(this.name);
        copy.setDuration(this.duration);
        copy.setLooping(this.looping);
        // Deep copy tracks  
        for (Map.Entry<String, KeyframeTrack<Double>> entry : this.doubleTracks.entrySet()) {
            KeyframeTrack<Double> trackCopy = KeyframeTrack.createDoubleTrack(
                entry.getValue().getTargetPath(), entry.getValue().getPropertyType());
            for (Keyframe<Double> kf : entry.getValue().getKeyframes()) {
                trackCopy.setKeyframe(kf.getTime(), kf.getValue());
            }
            copy.doubleTracks.put(entry.getKey(), trackCopy);
        }
        // Copy events
        for (AnimationEvent event : this.events) {
            copy.events.add(event);
        }
        return copy;
    }
    
    /**
     * Helper class for bone keyframe access.
     */
    public static class BoneKeyframes {
        private final String boneName;
        private final List<KeyframeTrack<Double>> tracks;
        
        BoneKeyframes(String boneName, List<KeyframeTrack<Double>> tracks) {
            this.boneName = boneName;
            this.tracks = tracks;
        }
        
        public String getBoneName() { return boneName; }
        
        public List<BoneKeyframe> getKeyframes() {
            // Collect all unique frame times
            Set<Double> times = new TreeSet<>();
            for (KeyframeTrack<Double> track : tracks) {
                for (Keyframe<Double> kf : track.getKeyframes()) {
                    times.add(kf.getTime());
                }
            }
            
            // Build keyframes
            List<BoneKeyframe> result = new ArrayList<>();
            for (double time : times) {
                BoneKeyframe kf = new BoneKeyframe(time);
                for (KeyframeTrack<Double> track : tracks) {
                    Keyframe<Double> tkf = track.getKeyframeAt(time);
                    if (tkf != null) {
                        String prop = track.getPropertyName();
                        switch (prop) {
                            case "x": kf.setX(tkf.getValue()); break;
                            case "y": kf.setY(tkf.getValue()); break;
                            case "rotation": kf.setRotation(tkf.getValue()); break;
                            case "scaleX": kf.setScaleX(tkf.getValue()); break;
                            case "scaleY": kf.setScaleY(tkf.getValue()); break;
                        }
                    }
                }
                result.add(kf);
            }
            return result;
        }
        
        /**
         * Get keyframe at the specified time (in seconds).
         * @param timeSeconds Time in seconds
         * @return BoneKeyframe at that time, or null if none exists
         */
        public BoneKeyframe getKeyframeAt(double timeSeconds) {
            for (BoneKeyframe kf : getKeyframes()) {
                if (Math.abs(kf.getTime() - timeSeconds) < 0.001) {
                    return kf;
                }
            }
            return null;
        }
    }
    
    @Override
    public String toString() {
        return String.format("AnimationClip[%s, duration=%.2fs, tracks=%d, looping=%b]",
            name, duration, doubleTracks.size(), looping);
    }
}
