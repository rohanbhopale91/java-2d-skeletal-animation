package com.animstudio.editor.ui.timeline;

import com.animstudio.core.animation.AnimationClip;
import com.animstudio.core.animation.Keyframe;
import com.animstudio.core.animation.KeyframeTrack;
import com.animstudio.core.model.Bone;
import com.animstudio.core.util.TimeUtils;
import com.animstudio.editor.EditorContext;
import com.animstudio.editor.commands.AddKeyframeCommand;
import com.animstudio.editor.commands.DeleteKeyframesCommand;
import com.animstudio.editor.commands.MoveKeyframesCommand;
import com.animstudio.editor.commands.PasteKeyframesCommand;

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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    
    // Multi-selection support
    private final Set<SelectedKeyframe> selectedKeyframes = new HashSet<>();
    private boolean isDraggingKeyframes = false;
    private double dragStartFrame = 0;
    
    // Box selection support
    private boolean isBoxSelecting = false;
    private double boxStartX, boxStartY;
    private double boxEndX, boxEndY;
    
    // Clipboard for copy/paste
    private final List<PasteKeyframesCommand.CopiedKeyframe> clipboard = new ArrayList<>();
    private double clipboardReferenceTime = 0;
    
    // Onion skinning
    private boolean onionSkinningEnabled = false;
    private int onionFramesBefore = 3;
    private int onionFramesAfter = 2;
    
    // Drag start point for keyframe dragging
    private double dragStartX = 0;
    private double dragStartY = 0;
    
    /**
     * Represents a selected keyframe (track + time).
     */
    public static class SelectedKeyframe {
        public final KeyframeTrack<?> track;
        public final double time;
        
        public SelectedKeyframe(KeyframeTrack<?> track, double time) {
            this.track = track;
            this.time = time;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof SelectedKeyframe)) return false;
            SelectedKeyframe other = (SelectedKeyframe) obj;
            return track == other.track && Math.abs(time - other.time) < 0.001;
        }
        
        @Override
        public int hashCode() {
            return track.hashCode() * 31 + (int)(time * 1000);
        }
    }
    
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
        keyframeCanvas.setOnMousePressed(this::onKeyframeCanvasPressed);
        keyframeCanvas.setOnMouseDragged(this::onKeyframeCanvasDrag);
        keyframeCanvas.setOnMouseReleased(this::onKeyframeCanvasReleased);
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
            } else if (e.getCode() == KeyCode.DELETE || e.getCode() == KeyCode.BACK_SPACE) {
                deleteSelectedKeyframes();
                e.consume();
            } else if (e.getCode() == KeyCode.C && e.isControlDown()) {
                copySelectedKeyframes();
                e.consume();
            } else if (e.getCode() == KeyCode.V && e.isControlDown()) {
                pasteKeyframes();
                e.consume();
            } else if (e.getCode() == KeyCode.A && e.isControlDown()) {
                selectAllKeyframes();
                e.consume();
            } else if (e.getCode() == KeyCode.ESCAPE) {
                clearKeyframeSelection();
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
        
        // Onion skinning controls
        CheckBox onionCheckBox = new CheckBox("Onion");
        onionCheckBox.setSelected(false);
        onionCheckBox.setTooltip(new Tooltip("Show onion skinning (ghost frames)"));
        onionCheckBox.selectedProperty().addListener((obs, old, val) -> {
            onionSkinningEnabled = val;
            EditorContext.getInstance().getMainController().getCanvasPane().repaint();
        });
        
        Spinner<Integer> onionBeforeSpinner = new Spinner<>(0, 10, onionFramesBefore);
        onionBeforeSpinner.setPrefWidth(55);
        onionBeforeSpinner.setTooltip(new Tooltip("Frames shown before current"));
        onionBeforeSpinner.valueProperty().addListener((obs, old, val) -> {
            onionFramesBefore = val;
            if (onionSkinningEnabled) {
                EditorContext.getInstance().getMainController().getCanvasPane().repaint();
            }
        });
        
        Spinner<Integer> onionAfterSpinner = new Spinner<>(0, 10, onionFramesAfter);
        onionAfterSpinner.setPrefWidth(55);
        onionAfterSpinner.setTooltip(new Tooltip("Frames shown after current"));
        onionAfterSpinner.valueProperty().addListener((obs, old, val) -> {
            onionFramesAfter = val;
            if (onionSkinningEnabled) {
                EditorContext.getInstance().getMainController().getCanvasPane().repaint();
            }
        });
        
        Label onionLabel = new Label("←");
        Label onionLabel2 = new Label("→");
        
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
            onionCheckBox, onionLabel, onionBeforeSpinner, onionLabel2, onionAfterSpinner,
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
                    
                    // Use multi-selection aware check
                    boolean isSelected = isKeyframeSelected(track, keyframe.getTime());
                    
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
        
        // Draw box selection if active
        if (isBoxSelecting) {
            gc.setStroke(Color.rgb(100, 150, 255, 0.8));
            gc.setFill(Color.rgb(100, 150, 255, 0.2));
            double bx = Math.min(boxStartX, boxEndX);
            double by = Math.min(boxStartY, boxEndY);
            double bw = Math.abs(boxEndX - boxStartX);
            double bh = Math.abs(boxEndY - boxStartY);
            gc.fillRect(bx, by, bw, bh);
            gc.strokeRect(bx, by, bw, bh);
        }
    }
    
    private void onKeyframeCanvasPressed(MouseEvent e) {
        if (e.getButton() == MouseButton.PRIMARY) {
            requestFocus(); // For keyboard shortcuts
            
            // Store drag start point
            dragStartX = e.getX();
            dragStartY = e.getY();
            
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
                                SelectedKeyframe clicked = new SelectedKeyframe(track, kf.getTime());
                                
                                if (e.isControlDown()) {
                                    // Toggle selection
                                    if (selectedKeyframes.contains(clicked)) {
                                        selectedKeyframes.remove(clicked);
                                    } else {
                                        selectedKeyframes.add(clicked);
                                    }
                                } else if (e.isShiftDown()) {
                                    // Add to selection
                                    selectedKeyframes.add(clicked);
                                } else {
                                    // Check if clicking on already selected keyframe
                                    if (!isKeyframeSelected(track, kf.getTime())) {
                                        // Replace selection
                                        selectedKeyframes.clear();
                                        selectedTrack = track;
                                        selectedKeyframeFrame = kfFrame;
                                    }
                                    // Start dragging
                                    isDraggingKeyframes = true;
                                    dragStartFrame = frame;
                                }
                                repaint();
                                return;
                            }
                        }
                    }
                }
            }
            
            // Clicked empty area - start box selection or clear selection
            if (!e.isControlDown() && !e.isShiftDown()) {
                selectedKeyframes.clear();
                selectedTrack = null;
                selectedKeyframeFrame = -1;
            }
            
            // Start box selection
            isBoxSelecting = true;
            boxStartX = e.getX();
            boxStartY = e.getY();
            boxEndX = e.getX();
            boxEndY = e.getY();
            
            repaint();
        }
    }
    
    private void onKeyframeCanvasDrag(MouseEvent e) {
        if (isBoxSelecting) {
            // Update box selection
            boxEndX = e.getX();
            boxEndY = e.getY();
            repaint();
        } else if (isDraggingKeyframes) {
            // Preview drag (just update playhead for now)
            double frame = (e.getX() + viewOffsetX) / pixelsPerFrame;
            double time = frame / FRAME_RATE;
            EditorContext.getInstance().setCurrentTime(Math.max(0, time));
        } else {
            // Scrub timeline
            double frame = (e.getX() + viewOffsetX) / pixelsPerFrame;
            double time = frame / FRAME_RATE;
            EditorContext.getInstance().setCurrentTime(Math.max(0, time));
        }
    }
    
    private void onKeyframeCanvasReleased(MouseEvent e) {
        if (isBoxSelecting) {
            // Complete box selection
            isBoxSelecting = false;
            selectKeyframesInBox();
            repaint();
        } else if (isDraggingKeyframes) {
            // Complete keyframe drag
            isDraggingKeyframes = false;
            double endFrame = (e.getX() + viewOffsetX) / pixelsPerFrame;
            double frameDelta = endFrame - dragStartFrame;
            
            if (Math.abs(frameDelta) >= 0.5) {
                // Move keyframes by frame delta (convert to seconds)
                double timeDelta = frameDelta / FRAME_RATE;
                moveSelectedKeyframes(timeDelta);
            }
        }
        isDraggingPlayhead = false;
    }
    
    /**
     * Select all keyframes within the box selection area.
     */
    private void selectKeyframesInBox() {
        if (currentClip == null) return;
        
        double minX = Math.min(boxStartX, boxEndX);
        double maxX = Math.max(boxStartX, boxEndX);
        double minY = Math.min(boxStartY, boxEndY);
        double maxY = Math.max(boxStartY, boxEndY);
        
        for (Map.Entry<KeyframeTrack<?>, Integer> entry : trackRows.entrySet()) {
            KeyframeTrack<?> track = entry.getKey();
            int row = entry.getValue();
            double trackY = row * TRACK_HEIGHT + TRACK_HEIGHT / 2.0;
            
            if (trackY >= minY && trackY <= maxY) {
                for (Keyframe<?> kf : track.getKeyframes()) {
                    double kfX = kf.getTime() * FRAME_RATE * pixelsPerFrame - viewOffsetX;
                    if (kfX >= minX && kfX <= maxX) {
                        selectedKeyframes.add(new SelectedKeyframe(track, kf.getTime()));
                    }
                }
            }
        }
    }
    
    /**
     * Move selected keyframes by given time delta.
     */
    private void moveSelectedKeyframes(double timeDelta) {
        if (currentClip == null) return;
        
        List<MoveKeyframesCommand.KeyframeMove> moves = new ArrayList<>();
        
        // Collect from multi-selection
        for (SelectedKeyframe sk : selectedKeyframes) {
            double newTime = Math.max(0, sk.time + timeDelta);
            moves.add(new MoveKeyframesCommand.KeyframeMove(
                sk.track.getTargetPath(), sk.time, newTime));
        }
        
        // Also check single selection
        if (moves.isEmpty() && selectedTrack != null && selectedKeyframeFrame >= 0) {
            double time = selectedKeyframeFrame / (double) FRAME_RATE;
            double newTime = Math.max(0, time + timeDelta);
            moves.add(new MoveKeyframesCommand.KeyframeMove(
                selectedTrack.getTargetPath(), time, newTime));
        }
        
        if (!moves.isEmpty()) {
            EditorContext.getInstance().getCommandStack().execute(
                new MoveKeyframesCommand(currentClip, moves)
            );
            
            // Update selection to new positions
            Set<SelectedKeyframe> newSelection = new HashSet<>();
            for (SelectedKeyframe sk : selectedKeyframes) {
                newSelection.add(new SelectedKeyframe(sk.track, Math.max(0, sk.time + timeDelta)));
            }
            selectedKeyframes.clear();
            selectedKeyframes.addAll(newSelection);
            
            if (selectedKeyframeFrame >= 0) {
                selectedKeyframeFrame = (int) Math.round((selectedKeyframeFrame / (double) FRAME_RATE + timeDelta) * FRAME_RATE);
                selectedKeyframeFrame = Math.max(0, selectedKeyframeFrame);
            }
            
            buildTrackList();
            repaint();
        }
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
    
    // =====================================================================
    // Multi-Selection Methods
    // =====================================================================
    
    /**
     * Delete all selected keyframes (multi-selection aware).
     */
    private void deleteSelectedKeyframes() {
        if (currentClip == null) return;
        
        // If multi-selection is active
        if (!selectedKeyframes.isEmpty()) {
            List<DeleteKeyframesCommand.KeyframeSelection> toDelete = new ArrayList<>();
            for (SelectedKeyframe sk : selectedKeyframes) {
                toDelete.add(new DeleteKeyframesCommand.KeyframeSelection(sk.track.getTargetPath(), sk.time));
            }
            EditorContext.getInstance().getCommandStack().execute(
                new DeleteKeyframesCommand(currentClip, toDelete)
            );
            selectedKeyframes.clear();
            selectedTrack = null;
            selectedKeyframeFrame = -1;
            buildTrackList();
            repaint();
            return;
        }
        
        // Fall back to single selection
        if (selectedTrack != null && selectedKeyframeFrame >= 0) {
            List<DeleteKeyframesCommand.KeyframeSelection> single = new ArrayList<>();
            single.add(new DeleteKeyframesCommand.KeyframeSelection(
                selectedTrack.getTargetPath(), selectedKeyframeFrame / (double) FRAME_RATE));
            EditorContext.getInstance().getCommandStack().execute(
                new DeleteKeyframesCommand(currentClip, single)
            );
            selectedTrack = null;
            selectedKeyframeFrame = -1;
            buildTrackList();
            repaint();
        }
    }
    
    /**
     * Copy selected keyframes to clipboard.
     */
    private void copySelectedKeyframes() {
        clipboard.clear();
        
        // Find the earliest time for relative positioning
        double earliestTime = Double.MAX_VALUE;
        
        // Collect keyframes to copy
        List<SelectedKeyframe> toCopy = new ArrayList<>();
        if (!selectedKeyframes.isEmpty()) {
            toCopy.addAll(selectedKeyframes);
        } else if (selectedTrack != null && selectedKeyframeFrame >= 0) {
            toCopy.add(new SelectedKeyframe(selectedTrack, selectedKeyframeFrame / (double) FRAME_RATE));
        }
        
        if (toCopy.isEmpty()) return;
        
        // Find earliest time
        for (SelectedKeyframe sk : toCopy) {
            earliestTime = Math.min(earliestTime, sk.time);
        }
        clipboardReferenceTime = earliestTime;
        
        // Copy keyframes with relative times
        for (SelectedKeyframe sk : toCopy) {
            KeyframeTrack<?> track = sk.track;
            Keyframe<?> kf = track.getKeyframeAt(sk.time);
            if (kf == null) {
                // Try to find by approximate time
                for (Keyframe<?> k : track.getKeyframes()) {
                    if (Math.abs(k.getTime() - sk.time) < 0.001) {
                        kf = k;
                        break;
                    }
                }
            }
            if (kf != null) {
                double relativeTime = kf.getTime() - earliestTime;
                clipboard.add(new PasteKeyframesCommand.CopiedKeyframe(
                    track.getTargetPath(),
                    relativeTime,
                    kf.getValue(),
                    kf.getInterpolator(),
                    track.getPropertyType()
                ));
            }
        }
    }
    
    /**
     * Paste keyframes from clipboard at current time.
     */
    private void pasteKeyframes() {
        if (clipboard.isEmpty() || currentClip == null) return;
        
        double pasteTime = EditorContext.getInstance().getCurrentTime();
        
        EditorContext.getInstance().getCommandStack().execute(
            new PasteKeyframesCommand(currentClip, new ArrayList<>(clipboard), pasteTime)
        );
        
        buildTrackList();
        repaint();
    }
    
    /**
     * Select all keyframes in the current clip.
     */
    private void selectAllKeyframes() {
        if (currentClip == null) return;
        
        selectedKeyframes.clear();
        for (KeyframeTrack<?> track : currentClip.getTracks()) {
            for (Keyframe<?> kf : track.getKeyframes()) {
                selectedKeyframes.add(new SelectedKeyframe(track, kf.getTime()));
            }
        }
        repaint();
    }
    
    /**
     * Clear all keyframe selection.
     */
    private void clearKeyframeSelection() {
        selectedKeyframes.clear();
        selectedTrack = null;
        selectedKeyframeFrame = -1;
        isBoxSelecting = false;
        repaint();
    }
    
    /**
     * Check if a keyframe is selected (multi-selection aware).
     */
    private boolean isKeyframeSelected(KeyframeTrack<?> track, double time) {
        // Check multi-selection
        for (SelectedKeyframe sk : selectedKeyframes) {
            if (sk.track == track && Math.abs(sk.time - time) < 0.001) {
                return true;
            }
        }
        // Check single selection
        if (track == selectedTrack) {
            double singleTime = selectedKeyframeFrame / (double) FRAME_RATE;
            return Math.abs(singleTime - time) < 0.001;
        }
        return false;
    }
    
    /**
     * Get count of selected keyframes.
     */
    public int getSelectedKeyframeCount() {
        if (!selectedKeyframes.isEmpty()) {
            return selectedKeyframes.size();
        }
        return (selectedTrack != null && selectedKeyframeFrame >= 0) ? 1 : 0;
    }
    
    // =====================================================================
    // Onion Skinning Accessors
    // =====================================================================
    
    /**
     * Check if onion skinning is enabled.
     */
    public boolean isOnionSkinningEnabled() {
        return onionSkinningEnabled;
    }
    
    /**
     * Get number of frames to show before current frame in onion skinning.
     */
    public int getOnionFramesBefore() {
        return onionFramesBefore;
    }
    
    /**
     * Get number of frames to show after current frame in onion skinning.
     */
    public int getOnionFramesAfter() {
        return onionFramesAfter;
    }
    
    /**
     * Get the frame rate for time conversion.
     */
    public double getFrameRate() {
        return FRAME_RATE;
    }
}
