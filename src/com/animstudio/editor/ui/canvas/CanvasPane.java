package com.animstudio.editor.ui.canvas;

import com.animstudio.core.animation.AnimationClip;
import com.animstudio.core.math.Vector2;
import com.animstudio.core.model.Bone;
import com.animstudio.core.model.RegionAttachment;
import com.animstudio.core.model.Skeleton;
import com.animstudio.core.model.Slot;
import com.animstudio.core.util.TimeUtils;
import com.animstudio.editor.EditorContext;
import com.animstudio.editor.commands.MoveBoneCommand;
import com.animstudio.editor.commands.RotateBoneCommand;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.transform.Affine;

import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Canvas panel for displaying and editing the skeleton.
 */
public class CanvasPane extends Pane {
    
    private final Canvas canvas;
    private final GraphicsContext gc;
    
    // View transform
    private double viewX = 0;
    private double viewY = 0;
    private double zoom = 1.0;
    
    // Interaction state
    private enum DragMode { NONE, PAN, MOVE_BONE, ROTATE_BONE }
    private DragMode dragMode = DragMode.NONE;
    private double dragStartX, dragStartY;
    private double boneStartX, boneStartY, boneStartRotation;
    private Bone draggingBone;
    
    // Visual settings
    private boolean showBones = true;
    private boolean showGrid = true;
    private boolean showAttachments = true;
    private boolean onionSkinning = false;
    private boolean showIKConstraints = true;
    
    // Image cache for attachments
    private final Map<String, Image> imageCache = new HashMap<>();
    
    private final Color backgroundColor = Color.rgb(40, 44, 52);
    private final Color gridColor = Color.rgb(60, 64, 72);
    private final Color boneColor = Color.rgb(200, 200, 200);
    private final Color boneSelectedColor = Color.rgb(100, 180, 255);
    private final Color boneJointColor = Color.rgb(255, 180, 100);
    
    public CanvasPane() {
        canvas = new Canvas();
        getChildren().add(canvas);
        gc = canvas.getGraphicsContext2D();
        
        // Bind canvas size to pane
        canvas.widthProperty().bind(widthProperty());
        canvas.heightProperty().bind(heightProperty());
        
        // Redraw on resize
        widthProperty().addListener((obs, oldVal, newVal) -> repaint());
        heightProperty().addListener((obs, oldVal, newVal) -> repaint());
        
        // Mouse handlers
        canvas.setOnMousePressed(this::onMousePressed);
        canvas.setOnMouseDragged(this::onMouseDragged);
        canvas.setOnMouseReleased(this::onMouseReleased);
        canvas.setOnMouseMoved(this::onMouseMoved);
        canvas.setOnScroll(this::onScroll);
        
        // Initial view position (center of canvas)
        viewX = 400;
        viewY = 300;
    }
    
    public void repaint() {
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        
        if (w <= 0 || h <= 0) return;
        
        // Clear background
        gc.setFill(backgroundColor);
        gc.fillRect(0, 0, w, h);
        
        // Apply view transform
        gc.save();
        gc.translate(viewX, viewY);
        gc.scale(zoom, zoom);
        
        // Draw grid
        if (showGrid) {
            drawGrid();
        }
        
        // Draw skeleton
        Skeleton skeleton = EditorContext.getInstance().getCurrentSkeleton();
        if (skeleton != null && showBones) {
            // Draw onion skins first (behind current frame)
            if (onionSkinning) {
                drawOnionSkins(skeleton);
            }
            
            skeleton.updateWorldTransforms();
            drawSkeleton(skeleton);
            
            // Draw IK constraints
            if (showIKConstraints) {
                drawIKConstraints();
            }
        }
        
        gc.restore();
        
        // Draw UI overlay (not affected by view transform)
        drawOverlay();
    }
    
    private void drawOnionSkins(Skeleton skeleton) {
        AnimationClip clip = EditorContext.getInstance().getCurrentAnimation();
        if (clip == null) return;
        
        double currentTime = EditorContext.getInstance().getCurrentTime();
        double frameTime = TimeUtils.framesToSeconds(1); // Duration of one frame in seconds
        
        // Store current bone transforms
        java.util.Map<String, double[]> savedTransforms = new java.util.HashMap<>();
        for (Bone bone : skeleton.getBones()) {
            savedTransforms.put(bone.getName(), new double[] {
                bone.getX(), bone.getY(), bone.getRotation(),
                bone.getScaleX(), bone.getScaleY()
            });
        }
        
        // Draw previous frames (blue tint)
        // Offsets are in frames, convert to seconds for time calculations
        int[] previousFrameOffsets = {-3, -2, -1};
        double[] previousAlphas = {0.1, 0.15, 0.25};
        Color previousColor = Color.rgb(100, 150, 255);
        
        for (int i = 0; i < previousFrameOffsets.length; i++) {
            double time = currentTime + (previousFrameOffsets[i] * frameTime);
            if (time >= 0 && time < clip.getDuration()) {
                skeleton.resetToSetupPose();
                clip.apply(skeleton, time);
                skeleton.updateWorldTransforms();
                drawOnionSkinFrame(skeleton, previousColor, previousAlphas[i]);
            }
        }
        
        // Draw next frames (red/orange tint)
        int[] nextFrameOffsets = {1, 2, 3};
        double[] nextAlphas = {0.25, 0.15, 0.1};
        Color nextColor = Color.rgb(255, 150, 100);
        
        for (int i = 0; i < nextFrameOffsets.length; i++) {
            double time = currentTime + (nextFrameOffsets[i] * frameTime);
            if (time >= 0 && time < clip.getDuration()) {
                skeleton.resetToSetupPose();
                clip.apply(skeleton, time);
                skeleton.updateWorldTransforms();
                drawOnionSkinFrame(skeleton, nextColor, nextAlphas[i]);
            }
        }
        
        // Restore original bone transforms
        for (Bone bone : skeleton.getBones()) {
            double[] saved = savedTransforms.get(bone.getName());
            if (saved != null) {
                bone.setX(saved[0]);
                bone.setY(saved[1]);
                bone.setRotation(saved[2]);
                bone.setScaleX(saved[3]);
                bone.setScaleY(saved[4]);
            }
        }
        skeleton.updateWorldTransforms();
    }
    
    private void drawOnionSkinFrame(Skeleton skeleton, Color tintColor, double alpha) {
        for (Bone bone : skeleton.getBonesInOrder()) {
            drawBoneOnionSkin(bone, tintColor, alpha);
        }
    }
    
    private void drawBoneOnionSkin(Bone bone, Color color, double alpha) {
        double x = bone.getWorldTransform().getX();
        double y = bone.getWorldTransform().getY();
        double rotation = Math.toRadians(bone.getWorldTransform().getRotation());
        double length = bone.getLength();
        
        gc.save();
        gc.translate(x, y);
        gc.rotate(Math.toDegrees(rotation));
        
        double boneWidth = 8.0 / zoom;
        
        // Bone body with tint color and alpha
        gc.setFill(color);
        gc.setGlobalAlpha(alpha);
        
        gc.beginPath();
        gc.moveTo(0, 0);
        gc.lineTo(length * 0.2, -boneWidth);
        gc.lineTo(length, 0);
        gc.lineTo(length * 0.2, boneWidth);
        gc.closePath();
        gc.fill();
        
        gc.setGlobalAlpha(1.0);
        gc.restore();
    }
    
    private void drawGrid() {
        gc.setStroke(gridColor);
        gc.setLineWidth(1.0 / zoom);
        
        double gridSize = 50;
        double extent = 500;
        
        for (double x = -extent; x <= extent; x += gridSize) {
            gc.strokeLine(x, -extent, x, extent);
        }
        for (double y = -extent; y <= extent; y += gridSize) {
            gc.strokeLine(-extent, y, extent, y);
        }
        
        // Origin axes
        gc.setStroke(Color.rgb(100, 60, 60));
        gc.setLineWidth(2.0 / zoom);
        gc.strokeLine(-extent, 0, extent, 0);
        
        gc.setStroke(Color.rgb(60, 100, 60));
        gc.strokeLine(0, -extent, 0, extent);
    }
    
    private void drawSkeleton(Skeleton skeleton) {
        Bone selectedBone = EditorContext.getInstance().getSelectedBone();
        
        // Draw attachments first (behind bones)
        if (showAttachments) {
            drawAttachments(skeleton);
        }
        
        // Draw bones
        for (Bone bone : skeleton.getBonesInOrder()) {
            boolean isSelected = bone == selectedBone;
            drawBone(bone, isSelected);
        }
    }
    
    /**
     * Draws all slot attachments for the skeleton.
     */
    private void drawAttachments(Skeleton skeleton) {
        // Sort slots by draw order
        java.util.List<Slot> sortedSlots = new java.util.ArrayList<>(skeleton.getSlots());
        sortedSlots.sort((a, b) -> Integer.compare(a.getDrawOrder(), b.getDrawOrder()));
        
        for (Slot slot : sortedSlots) {
            if (slot.getAttachment() == null) continue;
            if (!(slot.getAttachment() instanceof RegionAttachment)) continue;
            
            RegionAttachment attachment = (RegionAttachment) slot.getAttachment();
            Bone bone = slot.getBone();
            if (bone == null) continue;
            
            drawRegionAttachment(bone, slot, attachment);
        }
    }
    
    /**
     * Draws a region attachment at the bone's position.
     */
    private void drawRegionAttachment(Bone bone, Slot slot, RegionAttachment attachment) {
        String imagePath = attachment.getImagePath();
        if (imagePath == null || imagePath.isEmpty()) return;
        
        // Get or load image
        Image image = imageCache.get(imagePath);
        if (image == null) {
            try {
                image = new Image(new FileInputStream(imagePath));
                imageCache.put(imagePath, image);
            } catch (Exception e) {
                // Could not load image, skip
                return;
            }
        }
        
        // Calculate world position
        double worldX = bone.getWorldTransform().getX();
        double worldY = bone.getWorldTransform().getY();
        double worldRotation = bone.getWorldTransform().getRotation();
        double worldScaleX = bone.getWorldTransform().getScaleX();
        double worldScaleY = bone.getWorldTransform().getScaleY();
        
        // Apply attachment offset
        worldX += attachment.getX() * worldScaleX;
        worldY += attachment.getY() * worldScaleY;
        worldRotation += attachment.getRotation();
        worldScaleX *= attachment.getScaleX();
        worldScaleY *= attachment.getScaleY();
        
        // Image dimensions
        double width = attachment.getWidth() > 0 ? attachment.getWidth() : image.getWidth();
        double height = attachment.getHeight() > 0 ? attachment.getHeight() : image.getHeight();
        
        // Pivot offset
        double pivotX = width * attachment.getPivotX();
        double pivotY = height * attachment.getPivotY();
        
        gc.save();
        
        // Apply slot alpha tint
        gc.setGlobalAlpha(slot.getAlpha());
        
        // Transform to attachment position
        gc.translate(worldX, worldY);
        gc.rotate(worldRotation);
        gc.scale(worldScaleX, worldScaleY);
        
        // Draw image centered on pivot
        gc.drawImage(image, -pivotX, -pivotY, width, height);
        
        gc.restore();
    }
    
    /**
     * Draws IK constraint visualizations (chains and targets).
     */
    private void drawIKConstraints() {
        com.animstudio.core.ik.IKManager ikManager = EditorContext.getInstance().getIKManager();
        if (ikManager == null) return;
        
        for (com.animstudio.core.ik.IKConstraint constraint : ikManager.getConstraints()) {
            if (constraint.getMix() <= 0) continue;
            
            java.util.List<com.animstudio.core.model.Bone> bones = constraint.getBones();
            if (bones.isEmpty()) continue;
            
            // Draw chain connections with cyan highlight
            gc.setStroke(Color.rgb(100, 220, 255, 0.7 * constraint.getMix()));
            gc.setLineWidth(4.0 / zoom);
            gc.setLineDashes(8 / zoom, 4 / zoom);
            
            for (int i = 0; i < bones.size() - 1; i++) {
                com.animstudio.core.model.Bone b1 = bones.get(i);
                com.animstudio.core.model.Bone b2 = bones.get(i + 1);
                
                double x1 = b1.getWorldTransform().getX();
                double y1 = b1.getWorldTransform().getY();
                double x2 = b2.getWorldTransform().getX();
                double y2 = b2.getWorldTransform().getY();
                
                gc.strokeLine(x1, y1, x2, y2);
            }
            
            gc.setLineDashes(null);
            
            // Draw target marker
            com.animstudio.core.model.Bone target = constraint.getTarget();
            if (target != null) {
                double tx = target.getWorldTransform().getX();
                double ty = target.getWorldTransform().getY();
                double size = 12.0 / zoom;
                
                // Orange target cross
                gc.setStroke(Color.rgb(255, 180, 80, 0.9));
                gc.setLineWidth(3.0 / zoom);
                
                gc.strokeLine(tx - size, ty, tx + size, ty);
                gc.strokeLine(tx, ty - size, tx, ty + size);
                
                // Target circle
                gc.setFill(Color.rgb(255, 180, 80, 0.3));
                gc.fillOval(tx - size, ty - size, size * 2, size * 2);
                
                gc.setStroke(Color.rgb(255, 180, 80));
                gc.setLineWidth(2.0 / zoom);
                gc.strokeOval(tx - size, ty - size, size * 2, size * 2);
                
                // Draw line from end effector to target
                if (!bones.isEmpty()) {
                    com.animstudio.core.model.Bone endBone = bones.get(bones.size() - 1);
                    double endX = endBone.getWorldTransform().getX();
                    double endY = endBone.getWorldTransform().getY();
                    
                    // Add bone length to get actual end point
                    double rot = Math.toRadians(endBone.getWorldTransform().getRotation());
                    endX += Math.cos(rot) * endBone.getLength();
                    endY += Math.sin(rot) * endBone.getLength();
                    
                    gc.setStroke(Color.rgb(255, 100, 100, 0.5));
                    gc.setLineWidth(1.5 / zoom);
                    gc.setLineDashes(4 / zoom, 4 / zoom);
                    gc.strokeLine(endX, endY, tx, ty);
                    gc.setLineDashes(null);
                }
            }
            
            // Draw constraint name label
            if (!bones.isEmpty()) {
                com.animstudio.core.model.Bone firstBone = bones.get(0);
                double labelX = firstBone.getWorldTransform().getX();
                double labelY = firstBone.getWorldTransform().getY() - 15 / zoom;
                
                gc.setFill(Color.rgb(100, 220, 255));
                gc.setFont(javafx.scene.text.Font.font(10 / zoom));
                gc.fillText("IK: " + constraint.getName(), labelX, labelY);
            }
        }
    }
    
    private void drawBone(Bone bone, boolean selected) {
        double x = bone.getWorldTransform().getX();
        double y = bone.getWorldTransform().getY();
        double rotation = Math.toRadians(bone.getWorldTransform().getRotation());
        double length = bone.getLength();
        
        // Calculate bone end point
        double endX = x + Math.cos(rotation) * length;
        double endY = y + Math.sin(rotation) * length;
        
        // Draw bone shape (elongated diamond)
        gc.save();
        gc.translate(x, y);
        gc.rotate(Math.toDegrees(rotation));
        
        double boneWidth = 8.0 / zoom;
        double jointRadius = 6.0 / zoom;
        
        // Bone body
        gc.setFill(selected ? boneSelectedColor : boneColor);
        gc.setGlobalAlpha(0.8);
        
        gc.beginPath();
        gc.moveTo(0, 0);
        gc.lineTo(length * 0.2, -boneWidth);
        gc.lineTo(length, 0);
        gc.lineTo(length * 0.2, boneWidth);
        gc.closePath();
        gc.fill();
        
        gc.setGlobalAlpha(1.0);
        
        // Bone outline
        gc.setStroke(selected ? boneSelectedColor.brighter() : boneColor.darker());
        gc.setLineWidth(1.5 / zoom);
        gc.beginPath();
        gc.moveTo(0, 0);
        gc.lineTo(length * 0.2, -boneWidth);
        gc.lineTo(length, 0);
        gc.lineTo(length * 0.2, boneWidth);
        gc.closePath();
        gc.stroke();
        
        gc.restore();
        
        // Joint circle at origin
        gc.setFill(selected ? boneSelectedColor : boneJointColor);
        gc.fillOval(x - jointRadius, y - jointRadius, jointRadius * 2, jointRadius * 2);
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(1.0 / zoom);
        gc.strokeOval(x - jointRadius, y - jointRadius, jointRadius * 2, jointRadius * 2);
    }
    
    private void drawOverlay() {
        gc.setFill(Color.WHITE);
        gc.setFont(javafx.scene.text.Font.font(12));
        
        // Zoom level
        gc.fillText(String.format("Zoom: %.0f%%", zoom * 100), 10, 20);
        
        // Current time - show both frame and seconds
        double timeSeconds = EditorContext.getInstance().getCurrentTime();
        int frame = TimeUtils.secondsToNearestFrame(timeSeconds);
        gc.fillText(String.format("Frame: %d (%.2fs)", frame, timeSeconds), 10, 36);
        
        // Selected bone
        Bone selected = EditorContext.getInstance().getSelectedBone();
        if (selected != null) {
            gc.fillText("Selected: " + selected.getName(), 10, 52);
        }
    }
    
    // === Mouse Handling ===
    
    private void onMousePressed(MouseEvent e) {
        dragStartX = e.getX();
        dragStartY = e.getY();
        
        if (e.getButton() == MouseButton.MIDDLE || 
            (e.getButton() == MouseButton.PRIMARY && e.isAltDown())) {
            // Pan view
            dragMode = DragMode.PAN;
        } else if (e.getButton() == MouseButton.PRIMARY) {
            // Try to pick a bone
            Vector2 worldPos = screenToWorld(e.getX(), e.getY());
            Bone pickedBone = pickBone(worldPos.x, worldPos.y);
            
            if (pickedBone != null) {
                EditorContext.getInstance().select(pickedBone);
                draggingBone = pickedBone;
                boneStartX = pickedBone.getX();
                boneStartY = pickedBone.getY();
                boneStartRotation = pickedBone.getRotation();
                
                if (e.isShiftDown()) {
                    dragMode = DragMode.ROTATE_BONE;
                } else {
                    dragMode = DragMode.MOVE_BONE;
                }
            } else {
                EditorContext.getInstance().clearSelection();
                dragMode = DragMode.NONE;
            }
            repaint();
        }
    }
    
    private void onMouseDragged(MouseEvent e) {
        double dx = e.getX() - dragStartX;
        double dy = e.getY() - dragStartY;
        
        switch (dragMode) {
            case PAN:
                viewX += dx;
                viewY += dy;
                dragStartX = e.getX();
                dragStartY = e.getY();
                repaint();
                break;
                
            case MOVE_BONE:
                if (draggingBone != null) {
                    // Convert screen delta to world delta
                    double worldDx = dx / zoom;
                    double worldDy = dy / zoom;
                    
                    // If bone has a parent, need to account for parent's rotation
                    if (draggingBone.getParent() != null) {
                        double parentRot = Math.toRadians(draggingBone.getParent().getWorldTransform().getRotation());
                        double cos = Math.cos(-parentRot);
                        double sin = Math.sin(-parentRot);
                        double localDx = worldDx * cos - worldDy * sin;
                        double localDy = worldDx * sin + worldDy * cos;
                        worldDx = localDx;
                        worldDy = localDy;
                    }
                    
                    draggingBone.setX(boneStartX + worldDx);
                    draggingBone.setY(boneStartY + worldDy);
                    EditorContext.getInstance().getCurrentSkeleton().updateWorldTransforms();
                    repaint();
                }
                break;
                
            case ROTATE_BONE:
                if (draggingBone != null) {
                    // Calculate rotation based on mouse angle from bone origin
                    Vector2 boneWorld = new Vector2(
                        draggingBone.getWorldTransform().getX(),
                        draggingBone.getWorldTransform().getY()
                    );
                    Vector2 screenBone = worldToScreen(boneWorld.x, boneWorld.y);
                    
                    double startAngle = Math.atan2(dragStartY - screenBone.y, dragStartX - screenBone.x);
                    double currentAngle = Math.atan2(e.getY() - screenBone.y, e.getX() - screenBone.x);
                    double deltaAngle = Math.toDegrees(currentAngle - startAngle);
                    
                    draggingBone.setRotation(boneStartRotation + deltaAngle);
                    EditorContext.getInstance().getCurrentSkeleton().updateWorldTransforms();
                    repaint();
                }
                break;
        }
    }
    
    private void onMouseReleased(MouseEvent e) {
        // Create command for undo/redo
        if (draggingBone != null) {
            if (dragMode == DragMode.MOVE_BONE) {
                if (draggingBone.getX() != boneStartX || draggingBone.getY() != boneStartY) {
                    double newX = draggingBone.getX();
                    double newY = draggingBone.getY();
                    draggingBone.setX(boneStartX);
                    draggingBone.setY(boneStartY);
                    EditorContext.getInstance().getCommandStack().execute(
                        new MoveBoneCommand(draggingBone, newX, newY)
                    );
                }
            } else if (dragMode == DragMode.ROTATE_BONE) {
                if (draggingBone.getRotation() != boneStartRotation) {
                    double newRot = draggingBone.getRotation();
                    draggingBone.setRotation(boneStartRotation);
                    EditorContext.getInstance().getCommandStack().execute(
                        new RotateBoneCommand(draggingBone, newRot)
                    );
                }
            }
        }
        
        dragMode = DragMode.NONE;
        draggingBone = null;
    }
    
    private void onMouseMoved(MouseEvent e) {
        // Could show hover state
    }
    
    private void onScroll(ScrollEvent e) {
        double factor = e.getDeltaY() > 0 ? 1.1 : 0.9;
        
        // Zoom toward mouse position
        double mouseX = e.getX();
        double mouseY = e.getY();
        
        viewX = mouseX - (mouseX - viewX) * factor;
        viewY = mouseY - (mouseY - viewY) * factor;
        zoom *= factor;
        
        // Clamp zoom
        zoom = Math.max(0.1, Math.min(5.0, zoom));
        
        repaint();
    }
    
    // === Coordinate Conversion ===
    
    private Vector2 screenToWorld(double screenX, double screenY) {
        double worldX = (screenX - viewX) / zoom;
        double worldY = (screenY - viewY) / zoom;
        return new Vector2(worldX, worldY);
    }
    
    private Vector2 worldToScreen(double worldX, double worldY) {
        double screenX = worldX * zoom + viewX;
        double screenY = worldY * zoom + viewY;
        return new Vector2(screenX, screenY);
    }
    
    private Bone pickBone(double worldX, double worldY) {
        Skeleton skeleton = EditorContext.getInstance().getCurrentSkeleton();
        if (skeleton == null) return null;
        
        double threshold = 20.0 / zoom;
        return skeleton.findBoneAtPosition(worldX, worldY, threshold);
    }
    
    // === View Controls ===
    
    public void resetView() {
        viewX = getWidth() / 2;
        viewY = getHeight() / 2;
        zoom = 1.0;
        repaint();
    }
    
    public void setShowBones(boolean show) { this.showBones = show; repaint(); }
    public void setShowGrid(boolean show) { this.showGrid = show; repaint(); }
    public void setOnionSkinning(boolean enabled) { this.onionSkinning = enabled; repaint(); }
    public void setShowAttachments(boolean show) { this.showAttachments = show; repaint(); }
    public void setShowIKConstraints(boolean show) { this.showIKConstraints = show; repaint(); }
    
    /**
     * Clears the image cache. Call when assets are reloaded.
     */
    public void clearImageCache() { imageCache.clear(); }

    public double getZoom() { return zoom; }
    public void setZoom(double zoom) { this.zoom = zoom; repaint(); }
    
    /**
     * Zoom to fit the skeleton in the visible canvas area.
     */
    public void zoomToFit() {
        Skeleton skeleton = EditorContext.getInstance().getCurrentSkeleton();
        if (skeleton == null) {
            resetView();
            return;
        }
        
        // Calculate bounding box of all bones
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE, maxY = Double.MIN_VALUE;
        
        for (Bone bone : skeleton.getBones()) {
            double wx = bone.getWorldPosition().x;
            double wy = bone.getWorldPosition().y;
            minX = Math.min(minX, wx);
            minY = Math.min(minY, wy);
            maxX = Math.max(maxX, wx);
            maxY = Math.max(maxY, wy);
            
            // Include bone end point
            double endX = wx + Math.cos(Math.toRadians(bone.getWorldRotation())) * bone.getLength();
            double endY = wy + Math.sin(Math.toRadians(bone.getWorldRotation())) * bone.getLength();
            minX = Math.min(minX, endX);
            minY = Math.min(minY, endY);
            maxX = Math.max(maxX, endX);
            maxY = Math.max(maxY, endY);
        }
        
        if (minX == Double.MAX_VALUE) {
            resetView();
            return;
        }
        
        // Add padding
        double padding = 50;
        double boneWidth = (maxX - minX) + padding * 2;
        double boneHeight = (maxY - minY) + padding * 2;
        
        double canvasWidth = getWidth();
        double canvasHeight = getHeight();
        
        // Calculate zoom to fit
        double zoomX = canvasWidth / boneWidth;
        double zoomY = canvasHeight / boneHeight;
        this.zoom = Math.min(zoomX, zoomY);
        this.zoom = Math.max(0.1, Math.min(5.0, this.zoom)); // Clamp zoom
        
        // Center view on skeleton
        double centerX = (minX + maxX) / 2;
        double centerY = (minY + maxY) / 2;
        this.viewX = canvasWidth / 2 - centerX * zoom;
        this.viewY = canvasHeight / 2 - centerY * zoom;
        
        repaint();
    }
}