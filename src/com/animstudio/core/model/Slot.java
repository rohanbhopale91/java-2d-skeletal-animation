package com.animstudio.core.model;

import java.util.UUID;

/**
 * A slot is an attachment point on a bone where sprites/regions can be attached.
 * Slots define the draw order independent of bone hierarchy.
 */
public class Slot {
    
    private final String id;
    private String name;
    private Bone bone;
    private Attachment attachment;
    private int drawOrder;
    
    // Blend mode for rendering
    private BlendMode blendMode;
    
    // Color tint (RGBA)
    private float red;
    private float green;
    private float blue;
    private float alpha;
    
    public enum BlendMode {
        NORMAL,
        ADDITIVE,
        MULTIPLY,
        SCREEN
    }
    
    public Slot(String name, Bone bone) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.bone = bone;
        this.drawOrder = 0;
        this.blendMode = BlendMode.NORMAL;
        this.red = 1f;
        this.green = 1f;
        this.blue = 1f;
        this.alpha = 1f;
    }
    
    // === Getters and Setters ===
    
    public String getId() { return id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public Bone getBone() { return bone; }
    public void setBone(Bone bone) { this.bone = bone; }
    
    public Attachment getAttachment() { return attachment; }
    public void setAttachment(Attachment attachment) { this.attachment = attachment; }
    
    public int getDrawOrder() { return drawOrder; }
    public void setDrawOrder(int drawOrder) { this.drawOrder = drawOrder; }
    
    public BlendMode getBlendMode() { return blendMode; }
    public void setBlendMode(BlendMode blendMode) { this.blendMode = blendMode; }
    
    public float getRed() { return red; }
    public void setRed(float red) { this.red = red; }
    
    public float getGreen() { return green; }
    public void setGreen(float green) { this.green = green; }
    
    public float getBlue() { return blue; }
    public void setBlue(float blue) { this.blue = blue; }
    
    public float getAlpha() { return alpha; }
    public void setAlpha(float alpha) { this.alpha = alpha; }
    
    public void setColor(float r, float g, float b, float a) {
        this.red = r;
        this.green = g;
        this.blue = b;
        this.alpha = a;
    }
    
    @Override
    public String toString() {
        return String.format("Slot[%s, bone=%s, attachment=%s]", 
            name, bone != null ? bone.getName() : "null",
            attachment != null ? attachment.getName() : "null");
    }
}
