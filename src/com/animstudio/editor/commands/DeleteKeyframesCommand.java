package com.animstudio.editor.commands;

import com.animstudio.core.animation.AnimationClip;
import com.animstudio.core.animation.Keyframe;
import com.animstudio.core.animation.KeyframeTrack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Command for deleting multiple keyframes at once.
 */
public class DeleteKeyframesCommand implements Command {
    
    private final AnimationClip clip;
    private final List<KeyframeSelection> selections;
    private final Map<String, List<Keyframe<?>>> removedKeyframes = new HashMap<>();
    
    /**
     * A single keyframe selection (track + time).
     */
    public static class KeyframeSelection {
        public final String trackPath;
        public final double time;
        
        public KeyframeSelection(String trackPath, double time) {
            this.trackPath = trackPath;
            this.time = time;
        }
    }
    
    public DeleteKeyframesCommand(AnimationClip clip, List<KeyframeSelection> selections) {
        this.clip = clip;
        this.selections = new ArrayList<>(selections);
    }
    
    @Override
    public void execute() {
        removedKeyframes.clear();
        
        for (KeyframeSelection sel : selections) {
            KeyframeTrack<?> track = clip.getTrack(sel.trackPath);
            if (track == null) continue;
            
            Keyframe<?> keyframe = track.getKeyframeAt(sel.time);
            if (keyframe != null) {
                // Store for undo
                removedKeyframes.computeIfAbsent(sel.trackPath, k -> new ArrayList<>()).add(keyframe);
                track.removeKeyframe(sel.time);
            }
        }
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public void undo() {
        for (Map.Entry<String, List<Keyframe<?>>> entry : removedKeyframes.entrySet()) {
            KeyframeTrack<?> track = clip.getTrack(entry.getKey());
            if (track == null) continue;
            
            for (Keyframe<?> kf : entry.getValue()) {
                // Re-add keyframe (cast as needed)
                if (track instanceof KeyframeTrack) {
                    KeyframeTrack<Object> typedTrack = (KeyframeTrack<Object>) track;
                    typedTrack.setKeyframe(kf.getTime(), kf.getValue(), kf.getInterpolator());
                }
            }
        }
    }
    
    @Override
    public String getDescription() {
        return "Delete " + selections.size() + " keyframe(s)";
    }
}
