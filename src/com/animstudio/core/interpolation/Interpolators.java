package com.animstudio.core.interpolation;

/**
 * Factory and utilities for creating interpolators.
 */
public final class Interpolators {
    
    private Interpolators() {}
    
    /**
     * Create an interpolator from type name and optional parameters.
     */
    public static Interpolator create(String type, double... params) {
        switch (type.toLowerCase()) {
            case "linear":
                return LinearInterpolator.INSTANCE;
            case "stepped":
            case "constant":
                return SteppedInterpolator.INSTANCE;
            case "bezier":
                if (params.length >= 4) {
                    return new BezierInterpolator(params[0], params[1], params[2], params[3]);
                }
                return BezierInterpolator.EASE;
            case "ease":
                return BezierInterpolator.EASE;
            case "ease-in":
            case "easein":
                return BezierInterpolator.EASE_IN;
            case "ease-out":
            case "easeout":
                return BezierInterpolator.EASE_OUT;
            case "ease-in-out":
            case "easeinout":
                return BezierInterpolator.EASE_IN_OUT;
            default:
                return LinearInterpolator.INSTANCE;
        }
    }
    
    /**
     * Common easing functions as static methods.
     */
    public static double easeInQuad(double t) {
        return t * t;
    }
    
    public static double easeOutQuad(double t) {
        return t * (2 - t);
    }
    
    public static double easeInOutQuad(double t) {
        return t < 0.5 ? 2 * t * t : -1 + (4 - 2 * t) * t;
    }
    
    public static double easeInCubic(double t) {
        return t * t * t;
    }
    
    public static double easeOutCubic(double t) {
        double t1 = t - 1;
        return t1 * t1 * t1 + 1;
    }
    
    public static double easeInOutCubic(double t) {
        return t < 0.5 ? 4 * t * t * t : (t - 1) * (2 * t - 2) * (2 * t - 2) + 1;
    }
    
    public static double easeInElastic(double t) {
        if (t == 0 || t == 1) return t;
        double p = 0.3;
        return -Math.pow(2, 10 * (t - 1)) * Math.sin((t - 1 - p / 4) * (2 * Math.PI) / p);
    }
    
    public static double easeOutElastic(double t) {
        if (t == 0 || t == 1) return t;
        double p = 0.3;
        return Math.pow(2, -10 * t) * Math.sin((t - p / 4) * (2 * Math.PI) / p) + 1;
    }
    
    public static double easeOutBounce(double t) {
        if (t < 1 / 2.75) {
            return 7.5625 * t * t;
        } else if (t < 2 / 2.75) {
            t -= 1.5 / 2.75;
            return 7.5625 * t * t + 0.75;
        } else if (t < 2.5 / 2.75) {
            t -= 2.25 / 2.75;
            return 7.5625 * t * t + 0.9375;
        } else {
            t -= 2.625 / 2.75;
            return 7.5625 * t * t + 0.984375;
        }
    }
}
