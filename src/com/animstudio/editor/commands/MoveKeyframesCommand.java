package com.animstudio.editor.commands;

import com.animstudio.core.animation.AnimationClip;
import com.animstudio.core.animation.Keyframe;
import com.animstudio.core.animation.KeyframeTrack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Command for moving multiple keyframes in time.
 */
public class MoveKeyframesCommand implements Command {
    
    private final AnimationClip clip;
    private final List<KeyframeMove> moves;
    private final Map<String, List<KeyframeBackup>> backups = new HashMap<>();
    
    /**
     * A single keyframe move operation.
     */
    public static class KeyframeMove {
        public final String trackPath;
        public final double originalTime;
        public final double newTime;
        
        public KeyframeMove(String trackPath, double originalTime, double newTime) {
            this.trackPath = trackPath;
            this.originalTime = originalTime;
            this.newTime = newTime;
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
    
    public MoveKeyframesCommand(AnimationClip clip, List<KeyframeMove> moves) {
        this.clip = clip;
        this.moves = new ArrayList<>(moves);
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public void execute() {
        backups.clear();
        
        // First, backup all keyframes that will be affected
        for (KeyframeMove move : moves) {
            KeyframeTrack<?> track = clip.getTrack(move.trackPath);
            if (track == null) continue;
            
            Keyframe<?> originalKf = track.getKeyframeAt(move.originalTime);
            if (originalKf != null) {
                backups.computeIfAbsent(move.trackPath, k -> new ArrayList<>())
                       .add(new KeyframeBackup(originalKf));
            }
            
            // Also check if there's a keyframe at the destination that will be overwritten
            Keyframe<?> destKf = track.getKeyframeAt(move.newTime);
            if (destKf != null && Math.abs(destKf.getTime() - move.originalTime) > 0.001) {
                backups.computeIfAbsent(move.trackPath + "_dest", k -> new ArrayList<>())
                       .add(new KeyframeBackup(destKf));
            }
        }
        
        // Now move keyframes
        for (KeyframeMove move : moves) {
            KeyframeTrack<?> track = clip.getTrack(move.trackPath);
            if (track == null) continue;
            
            Keyframe<?> kf = track.getKeyframeAt(move.originalTime);
            if (kf == null) continue;
            
            // Remove from original position
            track.removeKeyframe(move.originalTime);
            
            // Add at new position
            KeyframeTrack<Object> typedTrack = (KeyframeTrack<Object>) track;
            typedTrack.setKeyframe(move.newTime, kf.getValue(), kf.getInterpolator());
        }
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public void undo() {
        // Remove all keyframes at their new positions
        for (KeyframeMove move : moves) {
            KeyframeTrack<?> track = clip.getTrack(move.trackPath);
            if (track != null) {
                track.removeKeyframe(move.newTime);
            }
        }
        
        // Restore all backed up keyframes
        for (Map.Entry<String, List<KeyframeBackup>> entry : backups.entrySet()) {
            String trackPath = entry.getKey().replace("_dest", "");
            KeyframeTrack<?> track = clip.getTrack(trackPath);
            if (track == null) continue;
            
            KeyframeTrack<Object> typedTrack = (KeyframeTrack<Object>) track;
            for (KeyframeBackup backup : entry.getValue()) {
                typedTrack.setKeyframe(backup.time, backup.value, backup.interpolator);
            }
        }
    }
    
    @Override
    public String getDescription() {
        return "Move " + moves.size() + " keyframe(s)";
    }
}
