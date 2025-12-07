package com.animstudio.io.imports;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.paint.Color;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Imports images for use as textures or sprite references.
 * Supports PNG, JPG, GIF, BMP formats.
 */
public class ImageImporter {
    
    /**
     * Import an image file.
     */
    public Image importImage(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            return new Image(fis);
        }
    }
    
    /**
     * Import an image with specific dimensions (scaled).
     */
    public Image importImage(File file, int width, int height) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            return new Image(fis, width, height, true, true);
        }
    }
    
    /**
     * Import multiple images from a directory.
     */
    public List<Image> importDirectory(File directory, String... extensions) throws IOException {
        List<Image> images = new ArrayList<>();
        
        File[] files = directory.listFiles((dir, name) -> {
            String lower = name.toLowerCase();
            for (String ext : extensions) {
                if (lower.endsWith(ext.toLowerCase())) {
                    return true;
                }
            }
            return false;
        });
        
        if (files != null) {
            for (File file : files) {
                images.add(importImage(file));
            }
        }
        
        return images;
    }
    
    /**
     * Import a sprite sheet and split it into individual frames.
     */
    public List<Image> importSpriteSheet(File file, int frameWidth, int frameHeight) throws IOException {
        List<Image> frames = new ArrayList<>();
        Image spriteSheet = importImage(file);
        
        int cols = (int) (spriteSheet.getWidth() / frameWidth);
        int rows = (int) (spriteSheet.getHeight() / frameHeight);
        
        PixelReader reader = spriteSheet.getPixelReader();
        
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                javafx.scene.image.WritableImage frame = 
                    new javafx.scene.image.WritableImage(reader, 
                        col * frameWidth, row * frameHeight, 
                        frameWidth, frameHeight);
                frames.add(frame);
            }
        }
        
        return frames;
    }
    
    /**
     * Import result with metadata.
     */
    public ImageImportResult importWithMetadata(File file) throws IOException {
        Image image = importImage(file);
        return new ImageImportResult(
            file.getName(),
            file.getAbsolutePath(),
            image,
            (int) image.getWidth(),
            (int) image.getHeight()
        );
    }
    
    /**
     * Result of image import operation.
     */
    public static class ImageImportResult {
        public final String name;
        public final String path;
        public final Image image;
        public final int width;
        public final int height;
        
        public ImageImportResult(String name, String path, Image image, int width, int height) {
            this.name = name;
            this.path = path;
            this.image = image;
            this.width = width;
            this.height = height;
        }
    }
    
    /**
     * Detect if an image has transparency.
     */
    public boolean hasTransparency(Image image) {
        PixelReader reader = image.getPixelReader();
        int width = (int) image.getWidth();
        int height = (int) image.getHeight();
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color color = reader.getColor(x, y);
                if (color.getOpacity() < 1.0) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Get supported file extensions.
     */
    public static String[] getSupportedExtensions() {
        return new String[]{".png", ".jpg", ".jpeg", ".gif", ".bmp"};
    }
    
    /**
     * Check if a file is a supported image format.
     */
    public static boolean isSupported(File file) {
        String name = file.getName().toLowerCase();
        for (String ext : getSupportedExtensions()) {
            if (name.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }
}
