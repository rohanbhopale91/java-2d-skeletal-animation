package com.animstudio.core.interpolation;

/**
 * Interface for easing/interpolation functions.
 * Takes a normalized time t (0-1) and returns the eased value.
 */
public interface Interpolator {
    
    /**
     * Evaluate the interpolation at time t.
     * @param t Normalized time from 0 to 1
     * @return Eased value, typically 0-1 but can overshoot
     */
    double evaluate(double t);
    
    /**
     * Get the type name for serialization.
     */
    String getType();
}
