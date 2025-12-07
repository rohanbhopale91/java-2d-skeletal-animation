package com.animstudio.io.export;

import com.animstudio.core.animation.AnimationClip;
import com.animstudio.core.model.Skeleton;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.embed.swing.SwingFXUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Exports animation frames to PNG images.
 * Supports single frame, frame sequence, and sprite sheet exports.
 */
public class ImageExporter {
    
    private final int width;
    private final int height;
    private final int frameRate;
    private Color backgroundColor = Color.TRANSPARENT;
    private boolean antialiasing = true;
    
    public ImageExporter(int width, int height, int frameRate) {
        this.width = width;
        this.height = height;
        this.frameRate = frameRate;
    }
    
    public void setBackgroundColor(Color color) {
        this.backgroundColor = color;
    }
    
    public void setAntialiasing(boolean antialiasing) {
        this.antialiasing = antialiasing;
    }
    
    /**
     * Export a single frame at the specified time.
     */
    public void exportFrame(Skeleton skeleton, AnimationClip clip, double time, 
                            File outputFile, BiConsumer<Skeleton, GraphicsContext> renderer) throws IOException {
        // Apply animation pose
        if (clip != null) {
            clip.apply(skeleton, time);
        }
        skeleton.updateWorldTransforms();
        
        // Render frame
        WritableImage image = renderFrame(skeleton, renderer);
        
        // Write to file
        BufferedImage bufferedImage = SwingFXUtils.fromFXImage(image, null);
        String format = getFormat(outputFile);
        ImageIO.write(bufferedImage, format, outputFile);
    }
    
    /**
     * Export a sequence of frames.
     */
    public void exportSequence(Skeleton skeleton, AnimationClip clip, 
                               File outputDir, String baseName,
                               BiConsumer<Skeleton, GraphicsContext> renderer,
                               ProgressCallback progressCallback) throws IOException {
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }
        
        double duration = clip != null ? clip.getDuration() : 1.0;
        int totalFrames = (int) (duration * frameRate);
        
        for (int frame = 0; frame < totalFrames; frame++) {
            double time = frame / (double) frameRate;
            
            // Apply animation pose
            if (clip != null) {
                clip.apply(skeleton, time);
            }
            skeleton.updateWorldTransforms();
            
            // Render frame
            WritableImage image = renderFrame(skeleton, renderer);
            
            // Write to file
            String filename = String.format("%s_%04d.png", baseName, frame);
            File outputFile = new File(outputDir, filename);
            BufferedImage bufferedImage = SwingFXUtils.fromFXImage(image, null);
            ImageIO.write(bufferedImage, "png", outputFile);
            
            if (progressCallback != null) {
                progressCallback.onProgress(frame + 1, totalFrames);
            }
        }
    }
    
    /**
     * Export frames as a sprite sheet.
     */
    public SpriteSheetResult exportSpriteSheet(Skeleton skeleton, AnimationClip clip,
                                               File outputFile, int columns,
                                               BiConsumer<Skeleton, GraphicsContext> renderer,
                                               ProgressCallback progressCallback) throws IOException {
        double duration = clip != null ? clip.getDuration() : 1.0;
        int totalFrames = (int) (duration * frameRate);
        
        if (columns <= 0) {
            columns = (int) Math.ceil(Math.sqrt(totalFrames));
        }
        int rows = (int) Math.ceil(totalFrames / (double) columns);
        
        int sheetWidth = width * columns;
        int sheetHeight = height * rows;
        
        // Create sprite sheet image
        BufferedImage spriteSheet = new BufferedImage(sheetWidth, sheetHeight, 
            backgroundColor.equals(Color.TRANSPARENT) ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
        
        java.awt.Graphics2D g2d = spriteSheet.createGraphics();
        
        // Fill background
        if (!backgroundColor.equals(Color.TRANSPARENT)) {
            g2d.setColor(new java.awt.Color(
                (float) backgroundColor.getRed(),
                (float) backgroundColor.getGreen(),
                (float) backgroundColor.getBlue(),
                (float) backgroundColor.getOpacity()
            ));
            g2d.fillRect(0, 0, sheetWidth, sheetHeight);
        }
        
        // Render each frame
        List<SpriteFrame> frames = new ArrayList<>();
        
        for (int frame = 0; frame < totalFrames; frame++) {
            double time = frame / (double) frameRate;
            
            // Apply animation pose
            if (clip != null) {
                clip.apply(skeleton, time);
            }
            skeleton.updateWorldTransforms();
            
            // Render frame
            WritableImage fxImage = renderFrame(skeleton, renderer);
            BufferedImage frameImage = SwingFXUtils.fromFXImage(fxImage, null);
            
            // Calculate position in sprite sheet
            int col = frame % columns;
            int row = frame / columns;
            int x = col * width;
            int y = row * height;
            
            // Draw frame to sprite sheet
            g2d.drawImage(frameImage, x, y, null);
            
            // Record frame info
            frames.add(new SpriteFrame(frame, x, y, width, height, time));
            
            if (progressCallback != null) {
                progressCallback.onProgress(frame + 1, totalFrames);
            }
        }
        
        g2d.dispose();
        
        // Write sprite sheet
        String format = getFormat(outputFile);
        ImageIO.write(spriteSheet, format, outputFile);
        
        return new SpriteSheetResult(outputFile, sheetWidth, sheetHeight, columns, rows, frames);
    }
    
    /**
     * Render a single frame to a WritableImage.
     */
    private WritableImage renderFrame(Skeleton skeleton, BiConsumer<Skeleton, GraphicsContext> renderer) {
        Canvas canvas = new Canvas(width, height);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        
        // Clear background
        if (backgroundColor.equals(Color.TRANSPARENT)) {
            gc.clearRect(0, 0, width, height);
        } else {
            gc.setFill(backgroundColor);
            gc.fillRect(0, 0, width, height);
        }
        
        // Render skeleton
        renderer.accept(skeleton, gc);
        
        // Create image
        WritableImage image = new WritableImage(width, height);
        canvas.snapshot(null, image);
        
        return image;
    }
    
    private String getFormat(File file) {
        String name = file.getName().toLowerCase();
        if (name.endsWith(".png")) return "png";
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) return "jpg";
        if (name.endsWith(".gif")) return "gif";
        if (name.endsWith(".bmp")) return "bmp";
        return "png";
    }
    
    /**
     * Progress callback interface.
     */
    public interface ProgressCallback {
        void onProgress(int current, int total);
    }
    
    /**
     * Result of sprite sheet export.
     */
    public static class SpriteSheetResult {
        public final File file;
        public final int width;
        public final int height;
        public final int columns;
        public final int rows;
        public final List<SpriteFrame> frames;
        
        public SpriteSheetResult(File file, int width, int height, int columns, int rows, 
                                 List<SpriteFrame> frames) {
            this.file = file;
            this.width = width;
            this.height = height;
            this.columns = columns;
            this.rows = rows;
            this.frames = frames;
        }
        
        /**
         * Generate JSON metadata for the sprite sheet.
         */
        public String toJson() {
            StringBuilder sb = new StringBuilder();
            sb.append("{\n");
            sb.append("  \"image\": \"").append(file.getName()).append("\",\n");
            sb.append("  \"width\": ").append(width).append(",\n");
            sb.append("  \"height\": ").append(height).append(",\n");
            sb.append("  \"columns\": ").append(columns).append(",\n");
            sb.append("  \"rows\": ").append(rows).append(",\n");
            sb.append("  \"frames\": [\n");
            for (int i = 0; i < frames.size(); i++) {
                SpriteFrame f = frames.get(i);
                sb.append("    {\"index\": ").append(f.index)
                  .append(", \"x\": ").append(f.x)
                  .append(", \"y\": ").append(f.y)
                  .append(", \"width\": ").append(f.width)
                  .append(", \"height\": ").append(f.height)
                  .append(", \"time\": ").append(f.time).append("}");
                if (i < frames.size() - 1) sb.append(",");
                sb.append("\n");
            }
            sb.append("  ]\n");
            sb.append("}\n");
            return sb.toString();
        }
    }
    
    /**
     * Individual frame in a sprite sheet.
     */
    public static class SpriteFrame {
        public final int index;
        public final int x;
        public final int y;
        public final int width;
        public final int height;
        public final double time;
        
        public SpriteFrame(int index, int x, int y, int width, int height, double time) {
            this.index = index;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.time = time;
        }
    }
}
