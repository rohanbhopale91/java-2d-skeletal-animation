package com.animstudio.core.interpolation;

/**
 * Stepped (constant) interpolation - holds value until next keyframe.
 */
public class SteppedInterpolator implements Interpolator {
    
    public static final SteppedInterpolator INSTANCE = new SteppedInterpolator();
    
    @Override
    public double evaluate(double t) {
        return 0; // Always return start value until t reaches 1
    }
    
    @Override
    public String getType() {
        return "stepped";
    }
}
