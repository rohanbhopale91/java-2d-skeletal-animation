package com.animstudio.core.math;

/**
 * Mathematical utility functions for animation calculations.
 */
public final class MathUtil {
    
    public static final double PI = Math.PI;
    public static final double TWO_PI = Math.PI * 2;
    public static final double HALF_PI = Math.PI / 2;
    public static final double DEG_TO_RAD = Math.PI / 180.0;
    public static final double RAD_TO_DEG = 180.0 / Math.PI;
    
    private MathUtil() {} // Prevent instantiation
    
    /**
     * Linear interpolation between two values.
     */
    public static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }
    
    /**
     * Linear interpolation for floats.
     */
    public static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }
    
    /**
     * Clamp a value between min and max.
     */
    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
    
    /**
     * Clamp a value between 0 and 1.
     */
    public static double clamp01(double value) {
        return clamp(value, 0.0, 1.0);
    }
    
    /**
     * Normalize an angle to the range [-180, 180] degrees.
     */
    public static double normalizeAngleDeg(double degrees) {
        degrees = degrees % 360;
        if (degrees > 180) degrees -= 360;
        if (degrees < -180) degrees += 360;
        return degrees;
    }
    
    /**
     * Normalize an angle to the range [-PI, PI] radians.
     */
    public static double normalizeAngleRad(double radians) {
        radians = radians % TWO_PI;
        if (radians > PI) radians -= TWO_PI;
        if (radians < -PI) radians += TWO_PI;
        return radians;
    }
    
    /**
     * Interpolate between two angles (degrees), taking the shortest path.
     */
    public static double lerpAngleDeg(double a, double b, double t) {
        double diff = normalizeAngleDeg(b - a);
        return a + diff * t;
    }
    
    /**
     * Convert degrees to radians.
     */
    public static double toRadians(double degrees) {
        return degrees * DEG_TO_RAD;
    }
    
    /**
     * Convert radians to degrees.
     */
    public static double toDegrees(double radians) {
        return radians * RAD_TO_DEG;
    }
    
    /**
     * Check if two doubles are approximately equal.
     */
    public static boolean approximately(double a, double b, double epsilon) {
        return Math.abs(a - b) < epsilon;
    }
    
    /**
     * Check if two doubles are approximately equal (default epsilon 1e-6).
     */
    public static boolean approximately(double a, double b) {
        return approximately(a, b, 1e-6);
    }
    
    /**
     * Clamp a float value between min and max.
     */
    public static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
    
    /**
     * Wrap an angle to the range [-180, 180] degrees (float version).
     */
    public static float wrapAngle(float degrees) {
        degrees = degrees % 360;
        if (degrees > 180) degrees -= 360;
        if (degrees < -180) degrees += 360;
        return degrees;
    }
    
    /**
     * Wrap an angle to the range [-180, 180] degrees (double version).
     */
    public static double wrapAngle(double degrees) {
        degrees = degrees % 360;
        if (degrees > 180) degrees -= 360;
        if (degrees < -180) degrees += 360;
        return degrees;
    }
    
    /**
     * Smooth step interpolation (ease in/out).
     */
    public static double smoothStep(double t) {
        return t * t * (3 - 2 * t);
    }
    
    /**
     * Smoother step interpolation (Ken Perlin's version).
     */
    public static double smootherStep(double t) {
        return t * t * t * (t * (t * 6 - 15) + 10);
    }
    
    /**
     * Map a value from one range to another.
     */
    public static double map(double value, double inMin, double inMax, double outMin, double outMax) {
        return outMin + (outMax - outMin) * ((value - inMin) / (inMax - inMin));
    }
}
