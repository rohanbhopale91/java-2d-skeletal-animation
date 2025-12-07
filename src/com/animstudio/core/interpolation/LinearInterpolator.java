package com.animstudio.core.interpolation;

/**
 * Linear interpolation - no easing, constant velocity.
 */
public class LinearInterpolator implements Interpolator {
    
    public static final LinearInterpolator INSTANCE = new LinearInterpolator();
    
    @Override
    public double evaluate(double t) {
        return t;
    }
    
    @Override
    public String getType() {
        return "linear";
    }
}
