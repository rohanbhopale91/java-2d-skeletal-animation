package com.animstudio.core.util;

/**
 * Utility class for consistent time unit conversions throughout the application.
 * 
 * CONVENTION:
 * - Internal core (AnimationClip, Keyframe, EditorContext.currentTime) = SECONDS
 * - UI timeline visual display = FRAMES (converted from seconds for display)
 * - Default frame rate = 30 FPS
 */
public final class TimeUtils {
    
    /** Default frame rate for the application */
    public static final double DEFAULT_FRAME_RATE = 30.0;
    
    private TimeUtils() {} // Prevent instantiation
    
    /**
     * Convert frames to seconds.
     * @param frames Frame number (0-based)
     * @param frameRate Frames per second
     * @return Time in seconds
     */
    public static double framesToSeconds(double frames, double frameRate) {
        return frames / frameRate;
    }
    
    /**
     * Convert frames to seconds using default frame rate (30 FPS).
     * @param frames Frame number (0-based)
     * @return Time in seconds
     */
    public static double framesToSeconds(double frames) {
        return framesToSeconds(frames, DEFAULT_FRAME_RATE);
    }
    
    /**
     * Convert seconds to frames.
     * @param seconds Time in seconds
     * @param frameRate Frames per second
     * @return Frame number (can be fractional)
     */
    public static double secondsToFrames(double seconds, double frameRate) {
        return seconds * frameRate;
    }
    
    /**
     * Convert seconds to frames using default frame rate (30 FPS).
     * @param seconds Time in seconds
     * @return Frame number (can be fractional)
     */
    public static double secondsToFrames(double seconds) {
        return secondsToFrames(seconds, DEFAULT_FRAME_RATE);
    }
    
    /**
     * Convert seconds to the nearest integer frame.
     * @param seconds Time in seconds
     * @param frameRate Frames per second
     * @return Nearest integer frame number
     */
    public static int secondsToNearestFrame(double seconds, double frameRate) {
        return (int) Math.round(seconds * frameRate);
    }
    
    /**
     * Convert seconds to the nearest integer frame using default frame rate.
     * @param seconds Time in seconds
     * @return Nearest integer frame number
     */
    public static int secondsToNearestFrame(double seconds) {
        return secondsToNearestFrame(seconds, DEFAULT_FRAME_RATE);
    }
    
    /**
     * Snap a time value to the nearest frame boundary.
     * @param seconds Time in seconds
     * @param frameRate Frames per second
     * @return Time snapped to nearest frame (in seconds)
     */
    public static double snapToFrame(double seconds, double frameRate) {
        int frame = secondsToNearestFrame(seconds, frameRate);
        return framesToSeconds(frame, frameRate);
    }
    
    /**
     * Snap a time value to the nearest frame boundary using default frame rate.
     * @param seconds Time in seconds
     * @return Time snapped to nearest frame (in seconds)
     */
    public static double snapToFrame(double seconds) {
        return snapToFrame(seconds, DEFAULT_FRAME_RATE);
    }
    
    /**
     * Format time as a human-readable string (MM:SS:FF).
     * @param seconds Time in seconds
     * @param frameRate Frames per second
     * @return Formatted string like "00:05:15" (5 seconds, 15 frames)
     */
    public static String formatTime(double seconds, double frameRate) {
        if (seconds < 0) seconds = 0;
        
        int totalFrames = (int) (seconds * frameRate);
        int framesPerSecond = (int) frameRate;
        
        int displayFrames = totalFrames % framesPerSecond;
        int totalSeconds = totalFrames / framesPerSecond;
        int displaySeconds = totalSeconds % 60;
        int displayMinutes = totalSeconds / 60;
        
        return String.format("%02d:%02d:%02d", displayMinutes, displaySeconds, displayFrames);
    }
    
    /**
     * Format time using default frame rate.
     * @param seconds Time in seconds
     * @return Formatted string
     */
    public static String formatTime(double seconds) {
        return formatTime(seconds, DEFAULT_FRAME_RATE);
    }
    
    /**
     * Format time as just frame count.
     * @param seconds Time in seconds
     * @param frameRate Frames per second
     * @return Frame count as string
     */
    public static String formatAsFrames(double seconds, double frameRate) {
        return String.valueOf(secondsToNearestFrame(seconds, frameRate));
    }
    
    /**
     * Calculate duration in seconds from frame count.
     * @param frameCount Total number of frames
     * @param frameRate Frames per second
     * @return Duration in seconds
     */
    public static double durationFromFrames(int frameCount, double frameRate) {
        return frameCount / frameRate;
    }
    
    /**
     * Get frame count from duration in seconds.
     * @param durationSeconds Duration in seconds
     * @param frameRate Frames per second
     * @return Frame count
     */
    public static int framesToDuration(double durationSeconds, double frameRate) {
        return (int) Math.ceil(durationSeconds * frameRate);
    }
}
