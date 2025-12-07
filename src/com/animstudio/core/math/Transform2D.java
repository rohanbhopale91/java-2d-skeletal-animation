package com.animstudio.core.math;

import java.awt.geom.AffineTransform;

/**
 * Mutable 2D transformation containing position, rotation, scale, and shear.
 * Used for bone local and world transforms.
 */
public class Transform2D {
    
    private double x;
    private double y;
    private double rotation;     // In degrees
    private double scaleX;
    private double scaleY;
    private double shearX;
    private double shearY;
    
    public Transform2D() {
        this.x = 0;
        this.y = 0;
        this.rotation = 0;
        this.scaleX = 1;
        this.scaleY = 1;
        this.shearX = 0;
        this.shearY = 0;
    }
    
    public Transform2D(double x, double y, double rotation, double scaleX, double scaleY) {
        this.x = x;
        this.y = y;
        this.rotation = rotation;
        this.scaleX = scaleX;
        this.scaleY = scaleY;
        this.shearX = 0;
        this.shearY = 0;
    }
    
    /**
     * Copy constructor.
     */
    public Transform2D(Transform2D other) {
        set(other);
    }
    
    /**
     * Set all values from another transform.
     */
    public void set(Transform2D other) {
        this.x = other.x;
        this.y = other.y;
        this.rotation = other.rotation;
        this.scaleX = other.scaleX;
        this.scaleY = other.scaleY;
        this.shearX = other.shearX;
        this.shearY = other.shearY;
    }
    
    /**
     * Reset to identity transform.
     */
    public void setIdentity() {
        x = 0;
        y = 0;
        rotation = 0;
        scaleX = 1;
        scaleY = 1;
        shearX = 0;
        shearY = 0;
    }
    
    /**
     * Multiply this transform by another (this = this * other).
     * Used to combine parent and child transforms: world = parent.world * child.local
     * 
     * Matrix representation (with shear):
     * | a  c  tx |   | scaleX * cos(r) - shearY * sin(r),  shearX * cos(r) - scaleY * sin(r),  x |
     * | b  d  ty | = | scaleX * sin(r) + shearY * cos(r),  shearX * sin(r) + scaleY * cos(r),  y |
     * | 0  0  1  |   | 0,                                  0,                                  1 |
     */
    public void multiply(Transform2D other) {
        // Get parent matrix components
        double parentRad = MathUtil.toRadians(rotation);
        double pCos = Math.cos(parentRad);
        double pSin = Math.sin(parentRad);
        
        // Parent matrix: [a, c, tx; b, d, ty; 0, 0, 1]
        double pa = scaleX * pCos - shearY * pSin;
        double pb = scaleX * pSin + shearY * pCos;
        double pc = shearX * pCos - scaleY * pSin;
        double pd = shearX * pSin + scaleY * pCos;
        
        // Get child matrix components
        double childRad = MathUtil.toRadians(other.rotation);
        double cCos = Math.cos(childRad);
        double cSin = Math.sin(childRad);
        
        double ca = other.scaleX * cCos - other.shearY * cSin;
        double cb = other.scaleX * cSin + other.shearY * cCos;
        double cc = other.shearX * cCos - other.scaleY * cSin;
        double cd = other.shearX * cSin + other.scaleY * cCos;
        
        // Result matrix = parent * child
        double ra = pa * ca + pc * cb;
        double rb = pb * ca + pd * cb;
        double rc = pa * cc + pc * cd;
        double rd = pb * cc + pd * cd;
        
        // Transform child's translation by parent matrix
        double newX = pa * other.x + pc * other.y + this.x;
        double newY = pb * other.x + pd * other.y + this.y;
        
        // Extract rotation, scale, shear from result matrix
        // Using polar decomposition for proper extraction
        double newRotation = MathUtil.toDegrees(Math.atan2(rb, ra));
        double cosR = Math.cos(MathUtil.toRadians(newRotation));
        double sinR = Math.sin(MathUtil.toRadians(newRotation));
        
        // Avoid division by zero
        double newScaleX, newScaleY, newShearX, newShearY;
        if (Math.abs(cosR) > 0.001) {
            newScaleX = ra / cosR;
            newShearY = (rb - newScaleX * sinR) / cosR;
            newShearX = (rc + scaleY * sinR) / cosR;
            newScaleY = (rd - newShearX * sinR) / cosR;
        } else {
            newScaleX = rb / sinR;
            newShearY = (ra + newScaleX * cosR) / sinR;
            newShearX = (rd - scaleY * cosR) / sinR;
            newScaleY = (-rc + newShearX * cosR) / sinR;
        }
        
        // Simplified approach: for typical skeleton animation, shear is minimal
        // Use a more robust extraction that preserves common transform chains
        this.x = newX;
        this.y = newY;
        this.rotation = MathUtil.normalizeAngleDeg(this.rotation + other.rotation);
        this.scaleX = this.scaleX * other.scaleX;
        this.scaleY = this.scaleY * other.scaleY;
        this.shearX = this.shearX + other.shearX;
        this.shearY = this.shearY + other.shearY;
    }
    
    /**
     * Multiply this transform by another with inheritance options.
     * Used for bones that may not inherit rotation or scale from parent.
     * 
     * @param other The child transform to apply
     * @param inheritRotation Whether to inherit parent rotation
     * @param inheritScale Whether to inherit parent scale
     */
    public void multiplyWithInheritance(Transform2D other, boolean inheritRotation, boolean inheritScale) {
        double parentRad = MathUtil.toRadians(rotation);
        double pCos = Math.cos(parentRad);
        double pSin = Math.sin(parentRad);
        
        // Calculate effective parent scale for position transformation
        double effScaleX = inheritScale ? scaleX : 1.0;
        double effScaleY = inheritScale ? scaleY : 1.0;
        
        // Transform child position by parent (always inherit translation)
        double newX, newY;
        if (inheritRotation) {
            newX = x + (other.x * pCos - other.y * pSin) * effScaleX;
            newY = y + (other.x * pSin + other.y * pCos) * effScaleY;
        } else {
            newX = x + other.x * effScaleX;
            newY = y + other.y * effScaleY;
        }
        
        // Apply inheritance for rotation and scale
        double newRotation = inheritRotation ? 
            MathUtil.normalizeAngleDeg(this.rotation + other.rotation) : other.rotation;
        double newScaleX = inheritScale ? this.scaleX * other.scaleX : other.scaleX;
        double newScaleY = inheritScale ? this.scaleY * other.scaleY : other.scaleY;
        
        this.x = newX;
        this.y = newY;
        this.rotation = newRotation;
        this.scaleX = newScaleX;
        this.scaleY = newScaleY;
        this.shearX = this.shearX + other.shearX;
        this.shearY = this.shearY + other.shearY;
    }
    
    /**
     * Transform a point by this transform.
     */
    public Vector2 transformPoint(Vector2 point) {
        double radians = MathUtil.toRadians(rotation);
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        
        // Apply scale
        double sx = point.x * scaleX;
        double sy = point.y * scaleY;
        
        // Apply shear
        double shx = sx + sy * shearX;
        double shy = sy + sx * shearY;
        
        // Apply rotation
        double rx = shx * cos - shy * sin;
        double ry = shx * sin + shy * cos;
        
        // Apply translation
        return new Vector2(rx + x, ry + y);
    }
    
    /**
     * Transform a point from world space to local space (inverse transform).
     */
    public Vector2 inverseTransformPoint(Vector2 point) {
        // Subtract translation
        double px = point.x - x;
        double py = point.y - y;
        
        // Inverse rotation
        double radians = MathUtil.toRadians(-rotation);
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        
        double rx = px * cos - py * sin;
        double ry = px * sin + py * cos;
        
        // Inverse shear (approximate for small shear values)
        double det = 1.0 - shearX * shearY;
        if (Math.abs(det) > 0.0001) {
            double shx = (rx - ry * shearX) / det;
            double shy = (ry - rx * shearY) / det;
            rx = shx;
            ry = shy;
        }
        
        // Inverse scale
        double sx = (Math.abs(scaleX) > 0.0001) ? rx / scaleX : rx;
        double sy = (Math.abs(scaleY) > 0.0001) ? ry / scaleY : ry;
        
        return new Vector2(sx, sy);
    }
    
    /**
     * Create a copy of this transform.
     */
    public Transform2D copy() {
        return new Transform2D(this);
    }
    
    /**
     * Convert to AWT AffineTransform for rendering.
     */
    public AffineTransform toAffineTransform() {
        AffineTransform at = new AffineTransform();
        at.translate(x, y);
        at.rotate(MathUtil.toRadians(rotation));
        at.scale(scaleX, scaleY);
        at.shear(shearX, shearY);
        return at;
    }
    
    /**
     * Interpolate between this transform and another.
     */
    public Transform2D lerp(Transform2D other, double t) {
        Transform2D result = new Transform2D();
        result.x = MathUtil.lerp(this.x, other.x, t);
        result.y = MathUtil.lerp(this.y, other.y, t);
        result.rotation = MathUtil.lerpAngleDeg(this.rotation, other.rotation, t);
        result.scaleX = MathUtil.lerp(this.scaleX, other.scaleX, t);
        result.scaleY = MathUtil.lerp(this.scaleY, other.scaleY, t);
        result.shearX = MathUtil.lerp(this.shearX, other.shearX, t);
        result.shearY = MathUtil.lerp(this.shearY, other.shearY, t);
        return result;
    }
    
    // Getters and Setters
    public double getX() { return x; }
    public void setX(double x) { this.x = x; }
    
    public double getY() { return y; }
    public void setY(double y) { this.y = y; }
    
    public void setTranslation(double x, double y) {
        this.x = x;
        this.y = y;
    }
    
    public Vector2 getPosition() { return new Vector2(x, y); }
    public void setPosition(Vector2 pos) { this.x = pos.x; this.y = pos.y; }
    
    public double getRotation() { return rotation; }
    public void setRotation(double rotation) { this.rotation = MathUtil.normalizeAngleDeg(rotation); }
    
    public double getScaleX() { return scaleX; }
    public void setScaleX(double scaleX) { this.scaleX = scaleX; }
    
    public double getScaleY() { return scaleY; }
    public void setScaleY(double scaleY) { this.scaleY = scaleY; }
    
    public void setScale(double scaleX, double scaleY) {
        this.scaleX = scaleX;
        this.scaleY = scaleY;
    }
    
    public double getShearX() { return shearX; }
    public void setShearX(double shearX) { this.shearX = shearX; }
    
    public double getShearY() { return shearY; }
    public void setShearY(double shearY) { this.shearY = shearY; }
    
    @Override
    public String toString() {
        return String.format("Transform2D(pos=(%.2f,%.2f), rot=%.2f°, scale=(%.2f,%.2f))",
            x, y, rotation, scaleX, scaleY);
    }
}
