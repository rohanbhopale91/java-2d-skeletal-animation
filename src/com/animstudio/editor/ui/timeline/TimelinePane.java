package com.animstudio.editor.ui.timeline;

import com.animstudio.core.animation.AnimationClip;
import com.animstudio.core.animation.Keyframe;
import com.animstudio.core.animation.KeyframeTrack;
import com.animstudio.core.model.Bone;
import com.animstudio.core.util.TimeUtils;
import com.animstudio.editor.EditorContext;
import com.animstudio.editor.commands.AddKeyframeCommand;

import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.util.HashMap;
import java.util.Map;

/**
 * Timeline panel for animation editing.
 * 
 * <p>Timing convention:
 * <ul>
 *   <li>Internal timing uses SECONDS (matches EditorContext and animation system)</li>
 *   <li>Display shows FRAMES for user convenience (converted via TimeUtils)</li>
 *   <li>Frame rate is 30 FPS by default</li>
 * </ul>
 * 
 * <p>Key features:
 * <ul>
 *   <li>Frame number display with current frame indicator</li>
 *   <li>Playhead scrubbing with optional snap-to-frame</li>
 *   <li>Zoom in/out with mouse wheel and buttons</li>
 *   <li>Duration display and editing</li>
 *   <li>Keyframe selection and editing</li>
 * </ul>
 */
public class TimelinePane extends BorderPane {
    
    private static final int HEADER_HEIGHT = 30;
    private static final int TRACK_HEIGHT = 24;
    private static final double MIN_PIXELS_PER_FRAME = 3;
    private static final double MAX_PIXELS_PER_FRAME = 60;
    private static final double DEFAULT_PIXELS_PER_FRAME = 10;
    
    /** Frame rate for display conversion. Use TimeUtils.DEFAULT_FRAME_RATE for consistency. */
    private static final double FRAME_RATE = TimeUtils.DEFAULT_FRAME_RATE;
    
    // UI Components
    private final ToolBar toolbar;
    private final ScrollPane trackScrollPane;
    private final VBox trackContainer;
    private final Canvas rulerCanvas;
    private final Canvas keyframeCanvas;
    private Slider zoomSlider;
    private Label frameLabel;
    private Label timeLabel;
    private Label durationLabel;
    private Spinner<Double> durationSpinner;
    private CheckBox snapToFrameCheckBox;
    
    // State
    private AnimationClip currentClip;
    private double viewOffsetX = 0;
    private double pixelsPerFrame = DEFAULT_PIXELS_PER_FRAME;
    private int selectedKeyframeFrame = -1;
    private KeyframeTrack<?> selectedTrack = null;
    private boolean snapToFrame = true;
    private boolean isDraggingPlayhead = false;
    
    // Track to row mapping
    private final Map<KeyframeTrack<?>, Integer> trackRows = new HashMap<>();
    
    public TimelinePane() {
        // Toolbar
        toolbar = createToolbar();
        setTop(toolbar);
        
        // Track names on left
        trackContainer = new VBox();
        trackContainer.setPrefWidth(150);
        trackContainer.setStyle("-fx-background-color: #2d2d30;");
        
        trackScrollPane = new ScrollPane(trackContainer);
        trackScrollPane.setFitToWidth(true);
        trackScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        
        // Timeline area
        VBox timelineArea = new VBox();
        
        // Ruler
        rulerCanvas = new Canvas();
        rulerCanvas.setHeight(HEADER_HEIGHT);
        rulerCanvas.setOnMousePressed(this::onRulerClick);
        rulerCanvas.setOnMouseDragged(this::onRulerDrag);
        rulerCanvas.setOnMouseReleased(e -> isDraggingPlayhead = false);
        
        // Keyframe canvas
        keyframeCanvas = new Canvas();
        keyframeCanvas.setOnMousePressed(this::onKeyframeCanvasClick);
        keyframeCanvas.setOnMouseDragged(this::onKeyframeCanvasDrag);
        keyframeCanvas.setOnMouseReleased(e -> isDraggingPlayhead = false);
        keyframeCanvas.setOnScroll(this::onMouseScroll);
        
        ScrollPane keyframeScrollPane = new ScrollPane(keyframeCanvas);
        keyframeScrollPane.setFitToHeight(true);
        keyframeScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        
        // Sync scrolling
        keyframeScrollPane.vvalueProperty().bindBidirectional(trackScrollPane.vvalueProperty());
        
        timelineArea.getChildren().addAll(rulerCanvas, keyframeScrollPane);
        VBox.setVgrow(keyframeScrollPane, Priority.ALWAYS);
        
        // Split between track names and timeline
        SplitPane splitPane = new SplitPane(trackScrollPane, timelineArea);
        splitPane.setDividerPositions(0.15);
        splitPane.setOrientation(Orientation.HORIZONTAL);
        
        setCenter(splitPane);
        
        // Bind canvas sizes
        rulerCanvas.widthProperty().bind(widthProperty().subtract(150));
        keyframeCanvas.widthProperty().bind(widthProperty().subtract(150));
        
        // Size listeners
        widthProperty().addListener((obs, old, val) -> repaint());
        heightProperty().addListener((obs, old, val) -> {
            keyframeCanvas.setHeight(Math.max(200, val.doubleValue() - HEADER_HEIGHT - 50));
            repaint();
        });
        
        // Keyboard shortcuts
        setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.LEFT) {
                stepFrame(-1);
                e.consume();
            } else if (e.getCode() == KeyCode.RIGHT) {
                stepFrame(1);
                e.consume();
            } else if (e.getCode() == KeyCode.HOME) {
                EditorContext.getInstance().setCurrentTime(0);
                e.consume();
            } else if (e.getCode() == KeyCode.END && currentClip != null) {
                EditorContext.getInstance().setCurrentTime(currentClip.getDuration());
                e.consume();
            } else if (e.getCode() == KeyCode.SPACE) {
                EditorContext.getInstance().togglePlayback();
                e.consume();
            } else if (e.getCode() == KeyCode.PLUS || e.getCode() == KeyCode.EQUALS) {
                zoomIn();
                e.consume();
            } else if (e.getCode() == KeyCode.MINUS) {
                zoomOut();
                e.consume();
            }
        });
        
        setFocusTraversable(true);
    }
    
    private ToolBar createToolbar() {
        ToolBar tb = new ToolBar();
        
        // Playback controls
        Button playButton = new Button("▶");
        playButton.setTooltip(new Tooltip("Play/Pause (Space)"));
        playButton.setOnAction(e -> EditorContext.getInstance().togglePlayback());
        
        Button stopButton = new Button("■");
        stopButton.setTooltip(new Tooltip("Stop and Reset"));
        stopButton.setOnAction(e -> {
            EditorContext.getInstance().playingProperty().set(false);
            EditorContext.getInstance().setCurrentTime(0);
        });
        
        Button prevFrame = new Button("◄");
        prevFrame.setTooltip(new Tooltip("Previous Frame (Left Arrow)"));
        prevFrame.setOnAction(e -> stepFrame(-1));
        
        Button nextFrame = new Button("►");
        nextFrame.setTooltip(new Tooltip("Next Frame (Right Arrow)"));
        nextFrame.setOnAction(e -> stepFrame(1));
        
        Button firstFrame = new Button("⏮");
        firstFrame.setTooltip(new Tooltip("First Frame (Home)"));
        firstFrame.setOnAction(e -> EditorContext.getInstance().setCurrentTime(0));
        
        Button lastFrame = new Button("⏭");
        lastFrame.setTooltip(new Tooltip("Last Frame (End)"));
        lastFrame.setOnAction(e -> {
            if (currentClip != null) {
                EditorContext.getInstance().setCurrentTime(currentClip.getDuration());
            }
        });
        
        // Keyframe controls
        Button addKeyButton = new Button("+ Key");
        addKeyButton.setTooltip(new Tooltip("Add Keyframe at Current Time (K)"));
        addKeyButton.setOnAction(e -> addKeyframeAtCurrentTime());
        
        Button removeKeyButton = new Button("- Key");
        removeKeyButton.setTooltip(new Tooltip("Remove Selected Keyframe (Delete)"));
        removeKeyButton.setOnAction(e -> removeSelectedKeyframe());
        
        // Snap to frame checkbox
        snapToFrameCheckBox = new CheckBox("Snap");
        snapToFrameCheckBox.setSelected(true);
        snapToFrameCheckBox.setTooltip(new Tooltip("Snap playhead to nearest frame"));
        snapToFrameCheckBox.selectedProperty().addListener((obs, old, val) -> snapToFrame = val);
        
        // Zoom controls
        Button zoomOutButton = new Button("−");
        zoomOutButton.setTooltip(new Tooltip("Zoom Out (-)"));
        zoomOutButton.setOnAction(e -> zoomOut());
        
        zoomSlider = new Slider(MIN_PIXELS_PER_FRAME, MAX_PIXELS_PER_FRAME, DEFAULT_PIXELS_PER_FRAME);
        zoomSlider.setPrefWidth(100);
        zoomSlider.valueProperty().addListener((obs, old, val) -> {
            pixelsPerFrame = val.doubleValue();
            repaint();
        });
        
        Button zoomInButton = new Button("+");
        zoomInButton.setTooltip(new Tooltip("Zoom In (+)"));
        zoomInButton.setOnAction(e -> zoomIn());
        
        Button zoomFitButton = new Button("Fit");
        zoomFitButton.setTooltip(new Tooltip("Fit Timeline to View"));
        zoomFitButton.setOnAction(e -> zoomToFit());
        
        // Frame and time display
        frameLabel = new Label("Frame: 0");
        frameLabel.setMinWidth(80);
        frameLabel.setStyle("-fx-font-family: monospace; -fx-font-weight: bold;");
        
        timeLabel = new Label("0:00.000");
        timeLabel.setMinWidth(70);
        timeLabel.setStyle("-fx-font-family: monospace;");
        
        // Duration controls
        Label durLabel = new Label("Duration:");
        durationSpinner = new Spinner<>(0.1, 300.0, 2.0, 0.1);
        durationSpinner.setEditable(true);
        durationSpinner.setPrefWidth(80);
        durationSpinner.setTooltip(new Tooltip("Animation duration in seconds"));
        durationSpinner.valueProperty().addListener((obs, old, val) -> {
            if (currentClip != null && val != null) {
                currentClip.setDuration(val);
                repaint();
            }
        });
        
        durationLabel = new Label("(60 frames)");
        durationLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #888;");
        
        // Update displays when current time changes
        EditorContext.getInstance().currentTimeProperty().addListener((obs, old, val) -> {
            double time = val.doubleValue();
            int frame = (int) Math.round(time * FRAME_RATE);
            frameLabel.setText("Frame: " + frame);
            
            int minutes = (int) (time / 60);
            double seconds = time % 60;
            timeLabel.setText(String.format("%d:%06.3f", minutes, seconds));
            repaint();
        });
        
        tb.getItems().addAll(
            firstFrame, prevFrame, playButton, stopButton, nextFrame, lastFrame,
            new Separator(),
            addKeyButton, removeKeyButton,
            new Separator(),
            snapToFrameCheckBox,
            new Separator(),
            zoomOutButton, zoomSlider, zoomInButton, zoomFitButton,
            new Separator(),
            frameLabel, timeLabel,
            new Separator(),
            durLabel, durationSpinner, durationLabel
        );
        
        return tb;
    }
    
    public void setClip(AnimationClip clip) {
        this.currentClip = clip;
        buildTrackList();
        updateDurationDisplay();
        repaint();
    }
    
    private void buildTrackList() {
        trackContainer.getChildren().clear();
        trackRows.clear();
        
        if (currentClip == null) return;
        
        int row = 0;
        for (KeyframeTrack<?> track : currentClip.getTracks()) {
            String trackName = track.getTargetPath();
            
            Label label = new Label(trackName);
            label.setPadding(new Insets(4, 8, 4, 8));
            label.setPrefHeight(TRACK_HEIGHT);
            label.setMaxWidth(Double.MAX_VALUE);
            label.setStyle("-fx-background-color: " + (row % 2 == 0 ? "#3c3c3c" : "#333333") + ";");
            
            trackContainer.getChildren().add(label);
            trackRows.put(track, row);
            row++;
        }
    }
    
    public void repaint() {
        paintRuler();
        paintKeyframes();
    }
    
    /**
     * Alias for repaint() for compatibility.
     */
    public void refresh() {
        repaint();
    }
    
    private void paintRuler() {
        double w = rulerCanvas.getWidth();
        double h = rulerCanvas.getHeight();
        
        GraphicsContext gc = rulerCanvas.getGraphicsContext2D();
        
        // Background
        gc.setFill(Color.rgb(45, 45, 48));
        gc.fillRect(0, 0, w, h);
        
        // Time markers
        gc.setStroke(Color.rgb(100, 100, 100));
        gc.setFill(Color.rgb(180, 180, 180));
        gc.setFont(javafx.scene.text.Font.font(10));
        
        int startFrame = (int) (viewOffsetX / pixelsPerFrame);
        int endFrame = startFrame + (int) (w / pixelsPerFrame) + 1;
        
        // Determine tick interval based on zoom level
        int tickInterval = 1;
        int labelInterval = (int) FRAME_RATE; // Default: label every second
        
        if (pixelsPerFrame < 5) {
            tickInterval = 10;
            labelInterval = 30;
        } else if (pixelsPerFrame < 10) {
            tickInterval = 5;
            labelInterval = 15;
        } else if (pixelsPerFrame < 20) {
            tickInterval = 2;
            labelInterval = 10;
        }
        
        for (int frame = startFrame; frame <= endFrame; frame++) {
            if (frame < 0) continue;
            double x = frame * pixelsPerFrame - viewOffsetX;
            
            if (frame % labelInterval == 0) {
                // Major tick with label
                gc.setStroke(Color.rgb(150, 150, 150));
                gc.strokeLine(x, h - 18, x, h);
                
                // Show frame number and seconds
                String label;
                if (frame % (int) FRAME_RATE == 0) {
                    label = String.format("%ds (f%d)", (int)(frame / FRAME_RATE), frame);
                } else {
                    label = String.format("f%d", frame);
                }
                gc.fillText(label, x + 2, 12);
            } else if (frame % ((int) FRAME_RATE / 2) == 0) {
                // Half-second tick
                gc.setStroke(Color.rgb(120, 120, 120));
                gc.strokeLine(x, h - 12, x, h);
            } else if (frame % tickInterval == 0) {
                // Minor tick
                gc.setStroke(Color.rgb(70, 70, 70));
                gc.strokeLine(x, h - 6, x, h);
            }
        }
        
        // End marker (duration)
        if (currentClip != null) {
            double endX = currentClip.getDuration() * FRAME_RATE * pixelsPerFrame - viewOffsetX;
            gc.setStroke(Color.rgb(100, 100, 200));
            gc.setLineWidth(2);
            gc.strokeLine(endX, 0, endX, h);
            gc.setLineWidth(1);
            
            // End marker label
            gc.setFill(Color.rgb(100, 100, 200));
            gc.fillText("END", endX - 20, h - 4);
        }
        
        // Current time indicator (playhead)
        double currentTime = EditorContext.getInstance().getCurrentTime();
        double currentX = currentTime * FRAME_RATE * pixelsPerFrame - viewOffsetX;
        
        // Playhead line
        gc.setFill(Color.rgb(255, 100, 100));
        gc.fillRect(currentX - 1, 0, 2, h);
        
        // Playhead triangle (larger and more visible)
        gc.beginPath();
        gc.moveTo(currentX - 8, 0);
        gc.lineTo(currentX + 8, 0);
        gc.lineTo(currentX, 12);
        gc.closePath();
        gc.fill();
        
        // Current frame number on playhead
        int currentFrame = (int) Math.round(currentTime * FRAME_RATE);
        gc.setFill(Color.WHITE);
        gc.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 10));
        String frameText = String.valueOf(currentFrame);
        gc.fillText(frameText, currentX + 10, 20);
        gc.setFont(javafx.scene.text.Font.font(10));
    }
    
    private void paintKeyframes() {
        double w = keyframeCanvas.getWidth();
        double h = keyframeCanvas.getHeight();
        
        GraphicsContext gc = keyframeCanvas.getGraphicsContext2D();
        
        // Background
        gc.setFill(Color.rgb(37, 37, 38));
        gc.fillRect(0, 0, w, h);
        
        // Grid lines
        gc.setStroke(Color.rgb(50, 50, 52));
        
        int startFrame = (int) (viewOffsetX / pixelsPerFrame);
        int endFrame = startFrame + (int) (w / pixelsPerFrame) + 1;
        
        for (int frame = startFrame; frame <= endFrame; frame++) {
            if (frame < 0) continue;
            double x = frame * pixelsPerFrame - viewOffsetX;
            gc.setStroke(frame % (int) FRAME_RATE == 0 ? Color.rgb(70, 70, 75) : Color.rgb(45, 45, 47));
            gc.strokeLine(x, 0, x, h);
        }
        
        // Track row backgrounds
        int row = 0;
        for (int y = 0; y < h; y += TRACK_HEIGHT) {
            gc.setFill(row % 2 == 0 ? Color.rgb(42, 42, 45) : Color.rgb(37, 37, 38));
            gc.fillRect(0, y, w, TRACK_HEIGHT);
            row++;
        }
        
        // End marker line in keyframe area
        if (currentClip != null) {
            double endX = currentClip.getDuration() * FRAME_RATE * pixelsPerFrame - viewOffsetX;
            gc.setStroke(Color.rgb(100, 100, 200, 0.5));
            gc.setLineWidth(2);
            gc.strokeLine(endX, 0, endX, h);
            gc.setLineWidth(1);
        }
        
        // Draw keyframes with interpolation curves
        if (currentClip != null) {
            for (KeyframeTrack<?> track : currentClip.getTracks()) {
                Integer trackRow = trackRows.get(track);
                if (trackRow == null) continue;
                
                double trackY = trackRow * TRACK_HEIGHT + TRACK_HEIGHT / 2.0;
                java.util.List<? extends Keyframe<?>> keyframes = track.getKeyframes();
                
                // Draw interpolation lines between keyframes
                if (keyframes.size() > 1) {
                    gc.setStroke(Color.rgb(120, 120, 120, 0.5));
                    gc.setLineWidth(1);
                    for (int i = 0; i < keyframes.size() - 1; i++) {
                        Keyframe<?> kf1 = keyframes.get(i);
                        Keyframe<?> kf2 = keyframes.get(i + 1);
                        double x1 = kf1.getTime() * FRAME_RATE * pixelsPerFrame - viewOffsetX;
                        double x2 = kf2.getTime() * FRAME_RATE * pixelsPerFrame - viewOffsetX;
                        
                        // Simple line for linear, dashed for stepped
                        if (kf1.getInterpolator() instanceof com.animstudio.core.interpolation.SteppedInterpolator) {
                            gc.setLineDashes(4, 4);
                        } else {
                            gc.setLineDashes(null);
                        }
                        gc.strokeLine(x1, trackY, x2, trackY);
                    }
                    gc.setLineDashes(null);
                }
                
                // Draw keyframe diamonds
                for (Keyframe<?> keyframe : keyframes) {
                    double kfX = keyframe.getTime() * FRAME_RATE * pixelsPerFrame - viewOffsetX;
                    int kfFrame = (int) Math.round(keyframe.getTime() * FRAME_RATE);
                    
                    boolean isSelected = track == selectedTrack && kfFrame == selectedKeyframeFrame;
                    
                    // Color based on interpolation type
                    Color fillColor;
                    if (isSelected) {
                        fillColor = Color.rgb(100, 200, 255);
                    } else if (keyframe.getInterpolator() instanceof com.animstudio.core.interpolation.SteppedInterpolator) {
                        fillColor = Color.rgb(255, 150, 150); // Red-ish for stepped
                    } else if (keyframe.getInterpolator() instanceof com.animstudio.core.interpolation.BezierInterpolator) {
                        fillColor = Color.rgb(150, 255, 150); // Green for bezier
                    } else {
                        fillColor = Color.rgb(255, 200, 100); // Orange for linear
                    }
                    
                    // Draw diamond with outline
                    double size = isSelected ? 8 : 6;
                    gc.setFill(fillColor);
                    gc.beginPath();
                    gc.moveTo(kfX, trackY - size);
                    gc.lineTo(kfX + size, trackY);
                    gc.lineTo(kfX, trackY + size);
                    gc.lineTo(kfX - size, trackY);
                    gc.closePath();
                    gc.fill();
                    
                    // Outline for selected keyframe
                    if (isSelected) {
                        gc.setStroke(Color.WHITE);
                        gc.setLineWidth(2);
                        gc.beginPath();
                        gc.moveTo(kfX, trackY - size);
                        gc.lineTo(kfX + size, trackY);
                        gc.lineTo(kfX, trackY + size);
                        gc.lineTo(kfX - size, trackY);
                        gc.closePath();
                        gc.stroke();
                        gc.setLineWidth(1);
                    }
                }
            }
        }
        
        // Current time line (playhead)
        double currentTime = EditorContext.getInstance().getCurrentTime();
        double currentX = currentTime * FRAME_RATE * pixelsPerFrame - viewOffsetX;
        
        gc.setStroke(Color.rgb(255, 100, 100));
        gc.setLineWidth(2);
        gc.strokeLine(currentX, 0, currentX, h);
        gc.setLineWidth(1);
    }
    
    private void onKeyframeCanvasClick(MouseEvent e) {
        if (e.getButton() == MouseButton.PRIMARY) {
            // Set current time
            double frame = (e.getX() + viewOffsetX) / pixelsPerFrame;
            double time = frame / FRAME_RATE;
            EditorContext.getInstance().setCurrentTime(Math.max(0, time));
            
            // Check if clicking on a keyframe
            if (currentClip != null) {
                int clickedFrame = (int) Math.round(frame);
                int clickedRow = (int) (e.getY() / TRACK_HEIGHT);
                
                for (Map.Entry<KeyframeTrack<?>, Integer> entry : trackRows.entrySet()) {
                    if (entry.getValue() == clickedRow) {
                        KeyframeTrack<?> track = entry.getKey();
                        for (Keyframe<?> kf : track.getKeyframes()) {
                            int kfFrame = (int) Math.round(kf.getTime() * FRAME_RATE);
                            if (Math.abs(kfFrame - clickedFrame) <= 1) {
                                selectedTrack = track;
                                selectedKeyframeFrame = kfFrame;
                                repaint();
                                return;
                            }
                        }
                    }
                }
            }
            
            selectedTrack = null;
            selectedKeyframeFrame = -1;
            repaint();
        }
    }
    
    private void onKeyframeCanvasDrag(MouseEvent e) {
        // Scrub timeline
        double frame = (e.getX() + viewOffsetX) / pixelsPerFrame;
        double time = frame / FRAME_RATE;
        EditorContext.getInstance().setCurrentTime(Math.max(0, time));
    }
    
    private void stepFrame(int delta) {
        double currentTime = EditorContext.getInstance().getCurrentTime();
        double frameTime = 1.0 / FRAME_RATE;
        EditorContext.getInstance().setCurrentTime(Math.max(0, currentTime + delta * frameTime));
    }
    
    private void addKeyframeAtCurrentTime() {
        Bone selectedBone = EditorContext.getInstance().getSelectedBone();
        if (selectedBone == null || currentClip == null) return;
        
        double time = EditorContext.getInstance().getCurrentTime();
        
        // Find or create tracks for this bone
        String rotationTrackName = selectedBone.getName() + ".rotation";
        
        // Add keyframe via command
        EditorContext.getInstance().getCommandStack().execute(
            new AddKeyframeCommand(currentClip, rotationTrackName, 
                KeyframeTrack.PropertyType.ROTATION, time, selectedBone.getRotation())
        );
        
        buildTrackList();
        repaint();
    }
    
    private void removeSelectedKeyframe() {
        if (selectedTrack == null || selectedKeyframeFrame < 0) return;
        
        double time = selectedKeyframeFrame / (double) FRAME_RATE;
        ((KeyframeTrack<?>) selectedTrack).removeKeyframe(time);
        
        selectedTrack = null;
        selectedKeyframeFrame = -1;
        
        repaint();
    }
    
    // === Ruler and Playhead Methods ===
    
    private void onRulerClick(MouseEvent e) {
        if (e.getButton() == MouseButton.PRIMARY) {
            isDraggingPlayhead = true;
            setTimeFromMouseX(e.getX());
        }
    }
    
    private void onRulerDrag(MouseEvent e) {
        if (isDraggingPlayhead) {
            setTimeFromMouseX(e.getX());
        }
    }
    
    private void setTimeFromMouseX(double mouseX) {
        double frame = (mouseX + viewOffsetX) / pixelsPerFrame;
        double time = frame / FRAME_RATE;
        
        // Snap to frame if enabled
        if (snapToFrame) {
            int nearestFrame = (int) Math.round(frame);
            time = nearestFrame / FRAME_RATE;
        }
        
        // Clamp to valid range
        time = Math.max(0, time);
        if (currentClip != null) {
            time = Math.min(time, currentClip.getDuration());
        }
        
        EditorContext.getInstance().setCurrentTime(time);
    }
    
    // === Zoom Methods ===
    
    private void onMouseScroll(ScrollEvent e) {
        if (e.isControlDown()) {
            // Zoom with Ctrl+Scroll
            double zoomFactor = e.getDeltaY() > 0 ? 1.1 : 0.9;
            double newPixelsPerFrame = pixelsPerFrame * zoomFactor;
            newPixelsPerFrame = Math.max(MIN_PIXELS_PER_FRAME, Math.min(MAX_PIXELS_PER_FRAME, newPixelsPerFrame));
            
            // Zoom towards mouse position
            double mouseFrame = (e.getX() + viewOffsetX) / pixelsPerFrame;
            pixelsPerFrame = newPixelsPerFrame;
            viewOffsetX = mouseFrame * pixelsPerFrame - e.getX();
            viewOffsetX = Math.max(0, viewOffsetX);
            
            zoomSlider.setValue(pixelsPerFrame);
            repaint();
            e.consume();
        } else if (e.isShiftDown()) {
            // Horizontal scroll with Shift+Scroll
            viewOffsetX -= e.getDeltaY() * 2;
            viewOffsetX = Math.max(0, viewOffsetX);
            repaint();
            e.consume();
        }
    }
    
    private void zoomIn() {
        double newPixelsPerFrame = Math.min(MAX_PIXELS_PER_FRAME, pixelsPerFrame * 1.2);
        pixelsPerFrame = newPixelsPerFrame;
        zoomSlider.setValue(pixelsPerFrame);
        repaint();
    }
    
    private void zoomOut() {
        double newPixelsPerFrame = Math.max(MIN_PIXELS_PER_FRAME, pixelsPerFrame / 1.2);
        pixelsPerFrame = newPixelsPerFrame;
        zoomSlider.setValue(pixelsPerFrame);
        repaint();
    }
    
    private void zoomToFit() {
        if (currentClip == null || rulerCanvas.getWidth() <= 0) return;
        
        double totalFrames = currentClip.getDuration() * FRAME_RATE;
        double availableWidth = rulerCanvas.getWidth() - 40; // Leave some margin
        
        if (totalFrames > 0 && availableWidth > 0) {
            pixelsPerFrame = availableWidth / totalFrames;
            pixelsPerFrame = Math.max(MIN_PIXELS_PER_FRAME, Math.min(MAX_PIXELS_PER_FRAME, pixelsPerFrame));
            viewOffsetX = 0;
            zoomSlider.setValue(pixelsPerFrame);
            repaint();
        }
    }
    
    /**
     * Update the duration spinner when clip changes.
     */
    private void updateDurationDisplay() {
        if (currentClip != null) {
            durationSpinner.getValueFactory().setValue(currentClip.getDuration());
            int totalFrames = (int) Math.round(currentClip.getDuration() * FRAME_RATE);
            durationLabel.setText("(" + totalFrames + " frames)");
        }
    }
    
    public AnimationClip getCurrentClip() {
        return currentClip;
    }
    
    /**
     * Get current zoom level (pixels per frame).
     */
    public double getPixelsPerFrame() {
        return pixelsPerFrame;
    }
    
    /**
     * Set zoom level.
     */
    public void setPixelsPerFrame(double pxPerFrame) {
        this.pixelsPerFrame = Math.max(MIN_PIXELS_PER_FRAME, Math.min(MAX_PIXELS_PER_FRAME, pxPerFrame));
        zoomSlider.setValue(pixelsPerFrame);
        repaint();
    }
    
    /**
     * Scroll to make current time visible.
     */
    public void scrollToCurrentTime() {
        double currentTime = EditorContext.getInstance().getCurrentTime();
        double currentX = currentTime * FRAME_RATE * pixelsPerFrame;
        double viewWidth = rulerCanvas.getWidth();
        
        if (currentX < viewOffsetX || currentX > viewOffsetX + viewWidth) {
            viewOffsetX = Math.max(0, currentX - viewWidth / 2);
            repaint();
        }
    }
}
