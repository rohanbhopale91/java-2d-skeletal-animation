package com.animstudio.core.interpolation;

/**
 * Cubic Bezier interpolation for smooth easing curves.
 * Control points define the curve shape (like CSS cubic-bezier).
 */
public class BezierInterpolator implements Interpolator {
    
    // Standard easing presets
    public static final BezierInterpolator EASE = new BezierInterpolator(0.25, 0.1, 0.25, 1.0);
    public static final BezierInterpolator EASE_IN = new BezierInterpolator(0.42, 0.0, 1.0, 1.0);
    public static final BezierInterpolator EASE_OUT = new BezierInterpolator(0.0, 0.0, 0.58, 1.0);
    public static final BezierInterpolator EASE_IN_OUT = new BezierInterpolator(0.42, 0.0, 0.58, 1.0);
    
    // Control points (x1, y1, x2, y2)
    // Start point is (0, 0), end point is (1, 1)
    private final double x1;
    private final double y1;
    private final double x2;
    private final double y2;
    
    public BezierInterpolator(double x1, double y1, double x2, double y2) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
    }
    
    @Override
    public double evaluate(double t) {
        if (t <= 0) return 0;
        if (t >= 1) return 1;
        
        // Find the t parameter for the x coordinate using Newton's method
        double tBezier = t;
        for (int i = 0; i < 8; i++) {
            double x = bezierX(tBezier) - t;
            if (Math.abs(x) < 1e-6) break;
            double dx = bezierXDerivative(tBezier);
            if (Math.abs(dx) < 1e-6) break;
            tBezier -= x / dx;
        }
        
        // Clamp to [0, 1]
        tBezier = Math.max(0, Math.min(1, tBezier));
        
        return bezierY(tBezier);
    }
    
    private double bezierX(double t) {
        double t2 = t * t;
        double t3 = t2 * t;
        double mt = 1 - t;
        double mt2 = mt * mt;
        
        // B(t) = 3(1-t)²t·P1 + 3(1-t)t²·P2 + t³
        return 3 * mt2 * t * x1 + 3 * mt * t2 * x2 + t3;
    }
    
    private double bezierY(double t) {
        double t2 = t * t;
        double t3 = t2 * t;
        double mt = 1 - t;
        double mt2 = mt * mt;
        
        return 3 * mt2 * t * y1 + 3 * mt * t2 * y2 + t3;
    }
    
    private double bezierXDerivative(double t) {
        double t2 = t * t;
        double mt = 1 - t;
        
        // Derivative: 3(1-t)²·P1 + 6(1-t)t·(P2-P1) + 3t²·(1-P2)
        return 3 * mt * mt * x1 + 6 * mt * t * (x2 - x1) + 3 * t2 * (1 - x2);
    }
    
    @Override
    public String getType() {
        return "bezier";
    }
    
    public double getX1() { return x1; }
    public double getY1() { return y1; }
    public double getX2() { return x2; }
    public double getY2() { return y2; }
    
    /**
     * Get control points as array [x1, y1, x2, y2].
     */
    public double[] getControlPoints() {
        return new double[] { x1, y1, x2, y2 };
    }
    
    @Override
    public String toString() {
        return String.format("BezierInterpolator(%.3f, %.3f, %.3f, %.3f)", x1, y1, x2, y2);
    }
}
