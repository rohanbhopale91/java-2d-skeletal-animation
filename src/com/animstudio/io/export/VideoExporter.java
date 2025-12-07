package com.animstudio.io.export;

import com.animstudio.core.animation.AnimationClip;
import com.animstudio.core.model.Skeleton;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.embed.swing.SwingFXUtils;

import java.awt.image.BufferedImage;
import java.io.*;
import java.util.function.BiConsumer;

/**
 * Exports animation to video format using FFmpeg.
 * Requires FFmpeg to be installed and available in PATH.
 */
public class VideoExporter {
    
    private final int width;
    private final int height;
    private final int frameRate;
    private Color backgroundColor = Color.WHITE;
    private String codec = "libx264"; // Default H.264 codec
    private int quality = 23; // CRF value (lower = better quality, 0-51)
    private String pixelFormat = "yuv420p";
    private String ffmpegPath = "ffmpeg"; // Assume in PATH
    
    public VideoExporter(int width, int height, int frameRate) {
        this.width = width;
        this.height = height;
        this.frameRate = frameRate;
    }
    
    public void setBackgroundColor(Color color) {
        this.backgroundColor = color;
    }
    
    public void setCodec(String codec) {
        this.codec = codec;
    }
    
    public void setQuality(int quality) {
        this.quality = Math.max(0, Math.min(51, quality));
    }
    
    public void setPixelFormat(String pixelFormat) {
        this.pixelFormat = pixelFormat;
    }
    
    public void setFfmpegPath(String path) {
        this.ffmpegPath = path;
    }
    
    /**
     * Check if FFmpeg is available.
     */
    public boolean isFFmpegAvailable() {
        try {
            ProcessBuilder pb = new ProcessBuilder(ffmpegPath, "-version");
            Process process = pb.start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Export animation to MP4 video.
     */
    public void exportMP4(Skeleton skeleton, AnimationClip clip, File outputFile,
                          BiConsumer<Skeleton, GraphicsContext> renderer,
                          ImageExporter.ProgressCallback progressCallback) throws IOException, InterruptedException {
        export(skeleton, clip, outputFile, "libx264", "yuv420p", renderer, progressCallback);
    }
    
    /**
     * Export animation to WebM video.
     */
    public void exportWebM(Skeleton skeleton, AnimationClip clip, File outputFile,
                           BiConsumer<Skeleton, GraphicsContext> renderer,
                           ImageExporter.ProgressCallback progressCallback) throws IOException, InterruptedException {
        export(skeleton, clip, outputFile, "libvpx-vp9", "yuva420p", renderer, progressCallback);
    }
    
    /**
     * Export animation to video with specified codec.
     */
    public void export(Skeleton skeleton, AnimationClip clip, File outputFile,
                       String videoCodec, String pxFmt,
                       BiConsumer<Skeleton, GraphicsContext> renderer,
                       ImageExporter.ProgressCallback progressCallback) throws IOException, InterruptedException {
        
        if (!isFFmpegAvailable()) {
            throw new IOException("FFmpeg not found. Please install FFmpeg and ensure it's in your PATH.");
        }
        
        double duration = clip != null ? clip.getDuration() : 1.0;
        int totalFrames = (int) (duration * frameRate);
        
        // Create temporary directory for frames
        File tempDir = createTempDir();
        
        try {
            // Export frames as PNG sequence
            for (int frame = 0; frame < totalFrames; frame++) {
                double time = frame / (double) frameRate;
                
                // Apply animation pose
                if (clip != null) {
                    clip.apply(skeleton, time);
                }
                skeleton.updateWorldTransforms();
                
                // Render frame
                BufferedImage frameImage = renderFrame(skeleton, renderer);
                
                // Save frame
                File frameFile = new File(tempDir, String.format("frame_%06d.png", frame));
                javax.imageio.ImageIO.write(frameImage, "png", frameFile);
                
                if (progressCallback != null) {
                    progressCallback.onProgress(frame + 1, totalFrames);
                }
            }
            
            // Use FFmpeg to encode video
            encodeVideo(tempDir, outputFile, videoCodec, pxFmt);
            
        } finally {
            // Clean up temp files
            deleteDir(tempDir);
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
        
        return SwingFXUtils.fromFXImage(fxImage, null);
    }
    
    /**
     * Encode frames to video using FFmpeg.
     */
    private void encodeVideo(File frameDir, File outputFile, String videoCodec, String pxFmt) 
            throws IOException, InterruptedException {
        
        String inputPattern = new File(frameDir, "frame_%06d.png").getAbsolutePath();
        
        ProcessBuilder pb = new ProcessBuilder(
            ffmpegPath,
            "-y",                           // Overwrite output
            "-framerate", String.valueOf(frameRate),
            "-i", inputPattern,             // Input pattern
            "-c:v", videoCodec,             // Video codec
            "-crf", String.valueOf(quality), // Quality
            "-pix_fmt", pxFmt,              // Pixel format
            "-movflags", "+faststart",      // Web optimization for MP4
            outputFile.getAbsolutePath()
        );
        
        pb.redirectErrorStream(true);
        Process process = pb.start();
        
        // Read output
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // Can log FFmpeg output here if needed
            }
        }
        
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException("FFmpeg exited with code " + exitCode);
        }
    }
    
    /**
     * Export using raw frame data without temp files (for faster encoding).
     * Pipes raw RGB data directly to FFmpeg.
     */
    public void exportDirect(Skeleton skeleton, AnimationClip clip, File outputFile,
                             BiConsumer<Skeleton, GraphicsContext> renderer,
                             ImageExporter.ProgressCallback progressCallback) throws IOException, InterruptedException {
        
        if (!isFFmpegAvailable()) {
            throw new IOException("FFmpeg not found. Please install FFmpeg and ensure it's in your PATH.");
        }
        
        double duration = clip != null ? clip.getDuration() : 1.0;
        int totalFrames = (int) (duration * frameRate);
        
        // Start FFmpeg process
        ProcessBuilder pb = new ProcessBuilder(
            ffmpegPath,
            "-y",
            "-f", "rawvideo",
            "-pixel_format", "rgb24",
            "-video_size", width + "x" + height,
            "-framerate", String.valueOf(frameRate),
            "-i", "-",                      // Read from stdin
            "-c:v", codec,
            "-crf", String.valueOf(quality),
            "-pix_fmt", pixelFormat,
            "-movflags", "+faststart",
            outputFile.getAbsolutePath()
        );
        
        Process process = pb.start();
        
        try (OutputStream ffmpegInput = process.getOutputStream()) {
            byte[] frameBuffer = new byte[width * height * 3];
            
            for (int frame = 0; frame < totalFrames; frame++) {
                double time = frame / (double) frameRate;
                
                // Apply animation pose
                if (clip != null) {
                    clip.apply(skeleton, time);
                }
                skeleton.updateWorldTransforms();
                
                // Render frame
                BufferedImage frameImage = renderFrame(skeleton, renderer);
                
                // Convert to raw RGB
                int bufferIndex = 0;
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        int rgb = frameImage.getRGB(x, y);
                        frameBuffer[bufferIndex++] = (byte) ((rgb >> 16) & 0xFF); // R
                        frameBuffer[bufferIndex++] = (byte) ((rgb >> 8) & 0xFF);  // G
                        frameBuffer[bufferIndex++] = (byte) (rgb & 0xFF);         // B
                    }
                }
                
                // Write to FFmpeg
                ffmpegInput.write(frameBuffer);
                
                if (progressCallback != null) {
                    progressCallback.onProgress(frame + 1, totalFrames);
                }
            }
        }
        
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException("FFmpeg exited with code " + exitCode);
        }
    }
    
    private File createTempDir() throws IOException {
        File tempDir = File.createTempFile("animstudio_export_", "");
        tempDir.delete();
        tempDir.mkdirs();
        return tempDir;
    }
    
    private void deleteDir(File dir) {
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    deleteDir(file);
                }
            }
        }
        dir.delete();
    }
    
    /**
     * Video export configuration.
     */
    public static class VideoConfig {
        public String codec = "libx264";
        public int quality = 23;
        public String pixelFormat = "yuv420p";
        public String preset = "medium"; // Encoding speed preset
        
        public static VideoConfig forMP4() {
            return new VideoConfig();
        }
        
        public static VideoConfig forWebM() {
            VideoConfig config = new VideoConfig();
            config.codec = "libvpx-vp9";
            config.pixelFormat = "yuva420p";
            return config;
        }
        
        public static VideoConfig forHighQuality() {
            VideoConfig config = new VideoConfig();
            config.quality = 18;
            config.preset = "slow";
            return config;
        }
    }
}
