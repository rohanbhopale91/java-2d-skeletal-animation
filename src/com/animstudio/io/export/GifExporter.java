package com.animstudio.io.export;

import com.animstudio.core.animation.AnimationClip;
import com.animstudio.core.model.Skeleton;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.embed.swing.SwingFXUtils;

import javax.imageio.*;
import javax.imageio.metadata.*;
import javax.imageio.stream.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.function.BiConsumer;

/**
 * Exports animation to animated GIF format.
 */
public class GifExporter {
    
    private final int width;
    private final int height;
    private final int frameRate;
    private Color backgroundColor = Color.WHITE;
    private boolean loop = true;
    private int colorDepth = 256; // GIF supports up to 256 colors
    
    public GifExporter(int width, int height, int frameRate) {
        this.width = width;
        this.height = height;
        this.frameRate = frameRate;
    }
    
    public void setBackgroundColor(Color color) {
        this.backgroundColor = color;
    }
    
    public void setLoop(boolean loop) {
        this.loop = loop;
    }
    
    public void setColorDepth(int colorDepth) {
        this.colorDepth = Math.min(256, Math.max(2, colorDepth));
    }
    
    /**
     * Export animation to animated GIF.
     */
    public void export(Skeleton skeleton, AnimationClip clip, File outputFile,
                       BiConsumer<Skeleton, GraphicsContext> renderer,
                       ImageExporter.ProgressCallback progressCallback) throws IOException {
        
        double duration = clip != null ? clip.getDuration() : 1.0;
        int totalFrames = (int) (duration * frameRate);
        int delayMs = 1000 / frameRate;
        
        // Get GIF writer
        ImageWriter gifWriter = ImageIO.getImageWritersByFormatName("gif").next();
        ImageWriteParam params = gifWriter.getDefaultWriteParam();
        
        try (ImageOutputStream outputStream = ImageIO.createImageOutputStream(outputFile)) {
            gifWriter.setOutput(outputStream);
            gifWriter.prepareWriteSequence(null);
            
            for (int frame = 0; frame < totalFrames; frame++) {
                double time = frame / (double) frameRate;
                
                // Apply animation pose
                if (clip != null) {
                    clip.apply(skeleton, time);
                }
                skeleton.updateWorldTransforms();
                
                // Render frame
                BufferedImage frameImage = renderFrame(skeleton, renderer);
                
                // Create metadata
                IIOMetadata metadata = createFrameMetadata(gifWriter, frameImage, delayMs, frame == 0);
                
                // Write frame
                gifWriter.writeToSequence(new IIOImage(frameImage, null, metadata), params);
                
                if (progressCallback != null) {
                    progressCallback.onProgress(frame + 1, totalFrames);
                }
            }
            
            gifWriter.endWriteSequence();
        } finally {
            gifWriter.dispose();
        }
    }
    
    /**
     * Render a single frame.
     */
    private BufferedImage renderFrame(Skeleton skeleton, BiConsumer<Skeleton, GraphicsContext> renderer) {
        Canvas canvas = new Canvas(width, height);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        
        // Fill background
        gc.setFill(backgroundColor);
        gc.fillRect(0, 0, width, height);
        
        // Render skeleton
        renderer.accept(skeleton, gc);
        
        // Create image
        WritableImage fxImage = new WritableImage(width, height);
        canvas.snapshot(null, fxImage);
        
        // Convert to BufferedImage (GIF needs indexed color)
        BufferedImage buffered = SwingFXUtils.fromFXImage(fxImage, null);
        BufferedImage indexed = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        indexed.getGraphics().drawImage(buffered, 0, 0, null);
        
        return indexed;
    }
    
    /**
     * Create GIF frame metadata with timing and loop info.
     */
    private IIOMetadata createFrameMetadata(ImageWriter writer, BufferedImage image, 
                                            int delayMs, boolean firstFrame) throws IOException {
        ImageTypeSpecifier imageType = ImageTypeSpecifier.createFromRenderedImage(image);
        IIOMetadata metadata = writer.getDefaultImageMetadata(imageType, null);
        
        String metaFormatName = metadata.getNativeMetadataFormatName();
        IIOMetadataNode root = (IIOMetadataNode) metadata.getAsTree(metaFormatName);
        
        // Graphics Control Extension (for timing)
        IIOMetadataNode gce = getOrCreateNode(root, "GraphicControlExtension");
        gce.setAttribute("disposalMethod", "restoreToBackgroundColor");
        gce.setAttribute("userInputFlag", "FALSE");
        gce.setAttribute("transparentColorFlag", "FALSE");
        gce.setAttribute("delayTime", String.valueOf(delayMs / 10)); // In 1/100 seconds
        gce.setAttribute("transparentColorIndex", "0");
        
        // Application Extension (for looping) - only on first frame
        if (firstFrame && loop) {
            IIOMetadataNode appExtensionsNode = getOrCreateNode(root, "ApplicationExtensions");
            IIOMetadataNode appExtension = new IIOMetadataNode("ApplicationExtension");
            appExtension.setAttribute("applicationID", "NETSCAPE");
            appExtension.setAttribute("authenticationCode", "2.0");
            
            // Loop forever (0 means infinite)
            byte[] loopData = new byte[]{1, 0, 0};
            appExtension.setUserObject(loopData);
            appExtensionsNode.appendChild(appExtension);
        }
        
        metadata.setFromTree(metaFormatName, root);
        return metadata;
    }
    
    /**
     * Get or create a child node with the given name.
     */
    private IIOMetadataNode getOrCreateNode(IIOMetadataNode root, String nodeName) {
        for (int i = 0; i < root.getLength(); i++) {
            if (root.item(i).getNodeName().equalsIgnoreCase(nodeName)) {
                return (IIOMetadataNode) root.item(i);
            }
        }
        IIOMetadataNode node = new IIOMetadataNode(nodeName);
        root.appendChild(node);
        return node;
    }
}
