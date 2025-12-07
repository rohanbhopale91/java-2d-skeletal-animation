package com.animstudio.editor.commands;

import com.animstudio.core.animation.AnimationClip;
import com.animstudio.core.animation.Keyframe;
import com.animstudio.core.animation.KeyframeTrack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Command for pasting copied keyframes at a target time.
 */
public class PasteKeyframesCommand implements Command {
    
    private final AnimationClip clip;
    private final List<CopiedKeyframe> copiedKeyframes;
    private final double targetTime;
    private final Map<String, List<KeyframeBackup>> overwrittenKeyframes = new HashMap<>();
    
    /**
     * Represents a copied keyframe ready for pasting.
     */
    public static class CopiedKeyframe {
        public final String trackPath;
        public final double relativeTime;  // Time relative to copy reference point
        public final Object value;
        public final com.animstudio.core.interpolation.Interpolator interpolator;
        public final KeyframeTrack.PropertyType propertyType;
        
        public CopiedKeyframe(String trackPath, double relativeTime, Object value,
                              com.animstudio.core.interpolation.Interpolator interpolator,
                              KeyframeTrack.PropertyType propertyType) {
            this.trackPath = trackPath;
            this.relativeTime = relativeTime;
            this.value = value;
            this.interpolator = interpolator;
            this.propertyType = propertyType;
        }
    }
    
    private static class KeyframeBackup {
        final double time;
        final Object value;
        final com.animstudio.core.interpolation.Interpolator interpolator;
        
        KeyframeBackup(Keyframe<?> kf) {
            this.time = kf.getTime();
            this.value = kf.getValue();
            this.interpolator = kf.getInterpolator();
        }
    }
    
    public PasteKeyframesCommand(AnimationClip clip, List<CopiedKeyframe> copiedKeyframes, double targetTime) {
        this.clip = clip;
        this.copiedKeyframes = new ArrayList<>(copiedKeyframes);
        this.targetTime = targetTime;
    }
    
    @Override
    public void execute() {
        overwrittenKeyframes.clear();
        
        for (CopiedKeyframe copied : copiedKeyframes) {
            double pasteTime = targetTime + copied.relativeTime;
            if (pasteTime < 0) pasteTime = 0;
            
            // Get or create track
            KeyframeTrack<Double> track = clip.getOrCreateTrack(
                copied.trackPath, copied.propertyType);
            
            // Backup any existing keyframe at paste location
            Keyframe<?> existing = track.getKeyframeAt(pasteTime);
            if (existing != null) {
                overwrittenKeyframes.computeIfAbsent(copied.trackPath, k -> new ArrayList<>())
                                   .add(new KeyframeBackup(existing));
            }
            
            // Paste the keyframe - convert value to Double
            Double valueToSet;
            if (copied.value instanceof Double) {
                valueToSet = (Double) copied.value;
            } else if (copied.value instanceof Number) {
                valueToSet = ((Number) copied.value).doubleValue();
            } else {
                valueToSet = 0.0;
            }
            track.setKeyframe(pasteTime, valueToSet, copied.interpolator);
        }
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public void undo() {
        // Remove pasted keyframes
        for (CopiedKeyframe copied : copiedKeyframes) {
            double pasteTime = targetTime + copied.relativeTime;
            if (pasteTime < 0) pasteTime = 0;
            
            KeyframeTrack<?> track = clip.getTrack(copied.trackPath);
            if (track != null) {
                track.removeKeyframe(pasteTime);
            }
        }
        
        // Restore overwritten keyframes
        for (Map.Entry<String, List<KeyframeBackup>> entry : overwrittenKeyframes.entrySet()) {
            KeyframeTrack<?> track = clip.getTrack(entry.getKey());
            if (track == null) continue;
            
            KeyframeTrack<Double> typedTrack = (KeyframeTrack<Double>) track;
            for (KeyframeBackup backup : entry.getValue()) {
                Double value;
                if (backup.value instanceof Double) {
                    value = (Double) backup.value;
                } else if (backup.value instanceof Number) {
                    value = ((Number) backup.value).doubleValue();
                } else {
                    value = 0.0;
                }
                typedTrack.setKeyframe(backup.time, value, backup.interpolator);
            }
        }
    }
    
    @Override
    public String getDescription() {
        return "Paste " + copiedKeyframes.size() + " keyframe(s)";
    }
}
