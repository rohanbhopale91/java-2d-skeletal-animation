package com.animstudio.core.model;

/**
 * A region attachment displays a rectangular region from a texture atlas.
 */
public class RegionAttachment extends Attachment {
    
    // Reference to the atlas region
    private String regionName;
    
    // Path to the image file (for standalone images)
    private String imagePath;
    
    // Source rectangle in the atlas
    private int regionX;
    private int regionY;
    private int regionWidth;
    private int regionHeight;
    
    // Pivot point (origin) for rotation
    private double pivotX;
    private double pivotY;
    
    // Display dimensions (can differ from region dimensions)
    private double width;
    private double height;
    
    public RegionAttachment(String name) {
        super(name);
        this.regionName = name;
        this.pivotX = 0.5;
        this.pivotY = 0.5;
    }
    
    @Override
    public String getType() {
        return "region";
    }
    
    // === Getters and Setters ===
    
    public String getRegionName() { return regionName; }
    public void setRegionName(String regionName) { this.regionName = regionName; }
    
    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }
    
    public int getRegionX() { return regionX; }
    public void setRegionX(int regionX) { this.regionX = regionX; }
    
    public int getRegionY() { return regionY; }
    public void setRegionY(int regionY) { this.regionY = regionY; }
    
    public int getRegionWidth() { return regionWidth; }
    public void setRegionWidth(int regionWidth) { this.regionWidth = regionWidth; }
    
    public int getRegionHeight() { return regionHeight; }
    public void setRegionHeight(int regionHeight) { this.regionHeight = regionHeight; }
    
    public double getPivotX() { return pivotX; }
    public void setPivotX(double pivotX) { this.pivotX = pivotX; }
    
    public double getPivotY() { return pivotY; }
    public void setPivotY(double pivotY) { this.pivotY = pivotY; }
    
    public double getWidth() { return width; }
    public void setWidth(double width) { this.width = width; }
    
    public double getHeight() { return height; }
    public void setHeight(double height) { this.height = height; }
    
    /**
     * Set the region bounds from atlas.
     */
    public void setRegion(int x, int y, int width, int height) {
        this.regionX = x;
        this.regionY = y;
        this.regionWidth = width;
        this.regionHeight = height;
        this.width = width;
        this.height = height;
    }
    
    @Override
    public String toString() {
        return String.format("RegionAttachment[%s, region=%s, size=%dx%d]", 
            name, regionName, regionWidth, regionHeight);
    }
}
