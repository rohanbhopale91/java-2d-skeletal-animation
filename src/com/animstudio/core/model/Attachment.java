package com.animstudio.core.model;

import java.util.UUID;

/**
 * Base class for attachments that can be bound to slots.
 * Attachments are the visual elements (sprites, meshes, etc.) displayed at bone positions.
 */
public abstract class Attachment {
    
    protected final String id;
    protected String name;
    
    // Offset from slot/bone position
    protected double x;
    protected double y;
    protected double rotation;
    protected double scaleX;
    protected double scaleY;
    
    public Attachment(String name) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.x = 0;
        this.y = 0;
        this.rotation = 0;
        this.scaleX = 1;
        this.scaleY = 1;
    }
    
    /**
     * Get the type name of this attachment for serialization.
     */
    public abstract String getType();
    
    // === Getters and Setters ===
    
    public String getId() { return id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public double getX() { return x; }
    public void setX(double x) { this.x = x; }
    
    public double getY() { return y; }
    public void setY(double y) { this.y = y; }
    
    public double getRotation() { return rotation; }
    public void setRotation(double rotation) { this.rotation = rotation; }
    
    public double getScaleX() { return scaleX; }
    public void setScaleX(double scaleX) { this.scaleX = scaleX; }
    
    public double getScaleY() { return scaleY; }
    public void setScaleY(double scaleY) { this.scaleY = scaleY; }
}
