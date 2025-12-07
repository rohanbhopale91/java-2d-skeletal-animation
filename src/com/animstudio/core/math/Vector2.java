package com.animstudio.core.math;

import java.util.Objects;

/**
 * Immutable 2D vector class for positions, directions, and scales.
 */
public final class Vector2 {
    
    public static final Vector2 ZERO = new Vector2(0, 0);
    public static final Vector2 ONE = new Vector2(1, 1);
    public static final Vector2 UP = new Vector2(0, -1);
    public static final Vector2 DOWN = new Vector2(0, 1);
    public static final Vector2 LEFT = new Vector2(-1, 0);
    public static final Vector2 RIGHT = new Vector2(1, 0);
    
    public final double x;
    public final double y;
    
    public Vector2(double x, double y) {
        this.x = x;
        this.y = y;
    }
    
    public Vector2 add(Vector2 other) {
        return new Vector2(x + other.x, y + other.y);
    }
    
    public Vector2 add(double dx, double dy) {
        return new Vector2(x + dx, y + dy);
    }
    
    public Vector2 subtract(Vector2 other) {
        return new Vector2(x - other.x, y - other.y);
    }
    
    public Vector2 multiply(double scalar) {
        return new Vector2(x * scalar, y * scalar);
    }
    
    public Vector2 multiply(Vector2 other) {
        return new Vector2(x * other.x, y * other.y);
    }
    
    public Vector2 divide(double scalar) {
        return new Vector2(x / scalar, y / scalar);
    }
    
    public double length() {
        return Math.sqrt(x * x + y * y);
    }
    
    public double lengthSquared() {
        return x * x + y * y;
    }
    
    public Vector2 normalize() {
        double len = length();
        if (len < 1e-10) return ZERO;
        return new Vector2(x / len, y / len);
    }
    
    public double dot(Vector2 other) {
        return x * other.x + y * other.y;
    }
    
    /**
     * 2D cross product (returns scalar z-component).
     */
    public double cross(Vector2 other) {
        return x * other.y - y * other.x;
    }
    
    /**
     * Rotate this vector by the given angle (in radians).
     */
    public Vector2 rotate(double radians) {
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        return new Vector2(x * cos - y * sin, x * sin + y * cos);
    }
    
    /**
     * Linear interpolation between this vector and another.
     */
    public Vector2 lerp(Vector2 other, double t) {
        return new Vector2(
            MathUtil.lerp(x, other.x, t),
            MathUtil.lerp(y, other.y, t)
        );
    }
    
    /**
     * Distance to another vector.
     */
    public double distanceTo(Vector2 other) {
        return subtract(other).length();
    }
    
    /**
     * Distance to another vector (alias for distanceTo).
     */
    public double distance(Vector2 other) {
        return distanceTo(other);
    }
    
    /**
     * Squared distance to another vector (faster, no sqrt).
     */
    public double distanceSquared(Vector2 other) {
        double dx = x - other.x;
        double dy = y - other.y;
        return dx * dx + dy * dy;
    }
    
    /**
     * Angle of this vector in radians (atan2).
     */
    public double angle() {
        return Math.atan2(y, x);
    }
    
    /**
     * Create a vector from an angle (radians) and magnitude.
     */
    public static Vector2 fromAngle(double radians, double magnitude) {
        return new Vector2(
            Math.cos(radians) * magnitude,
            Math.sin(radians) * magnitude
        );
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Vector2)) return false;
        Vector2 other = (Vector2) obj;
        return MathUtil.approximately(x, other.x) && MathUtil.approximately(y, other.y);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }
    
    @Override
    public String toString() {
        return String.format("Vector2(%.3f, %.3f)", x, y);
    }
}
