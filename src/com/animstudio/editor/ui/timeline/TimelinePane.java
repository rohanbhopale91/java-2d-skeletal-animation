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
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
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
 */
public class TimelinePane extends BorderPane {
    
    private static final int HEADER_HEIGHT = 30;
    private static final int TRACK_HEIGHT = 24;
    private static final int PIXELS_PER_FRAME = 10;
    
    /** Frame rate for display conversion. Use TimeUtils.DEFAULT_FRAME_RATE for consistency. */
    private static final double FRAME_RATE = TimeUtils.DEFAULT_FRAME_RATE;
    
    // UI Components
    private final ToolBar toolbar;
    private final ScrollPane trackScrollPane;
    private final VBox trackContainer;
    private final Canvas rulerCanvas;
    private final Canvas keyframeCanvas;
    private Slider zoomSlider;
    
    // State
    private AnimationClip currentClip;
    private double viewOffsetX = 0;
    private double pixelsPerFrame = PIXELS_PER_FRAME;
    private int selectedKeyframeFrame = -1;
    private KeyframeTrack<?> selectedTrack = null;
    
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
        
        // Keyframe canvas
        keyframeCanvas = new Canvas();
        keyframeCanvas.setOnMousePressed(this::onKeyframeCanvasClick);
        keyframeCanvas.setOnMouseDragged(this::onKeyframeCanvasDrag);
        
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
    }
    
    private ToolBar createToolbar() {
        ToolBar tb = new ToolBar();
        
        // Playback controls
        Button playButton = new Button("▶");
        playButton.setOnAction(e -> EditorContext.getInstance().togglePlayback());
        
        Button stopButton = new Button("■");
        stopButton.setOnAction(e -> {
            EditorContext.getInstance().playingProperty().set(false);
            EditorContext.getInstance().setCurrentTime(0);
        });
        
        Button prevFrame = new Button("◄");
        prevFrame.setOnAction(e -> stepFrame(-1));
        
        Button nextFrame = new Button("►");
        nextFrame.setOnAction(e -> stepFrame(1));
        
        // Keyframe controls
        Button addKeyButton = new Button("+ Key");
        addKeyButton.setOnAction(e -> addKeyframeAtCurrentTime());
        
        Button removeKeyButton = new Button("- Key");
        removeKeyButton.setOnAction(e -> removeSelectedKeyframe());
        
        // Zoom
        Label zoomLabel = new Label("Zoom:");
        zoomSlider = new Slider(5, 50, PIXELS_PER_FRAME);
        zoomSlider.setPrefWidth(100);
        zoomSlider.valueProperty().addListener((obs, old, val) -> {
            pixelsPerFrame = val.doubleValue();
            repaint();
        });
        
        // Time display
        Label timeLabel = new Label("0:00.000");
        EditorContext.getInstance().currentTimeProperty().addListener((obs, old, val) -> {
            double time = val.doubleValue();
            int minutes = (int) (time / 60);
            double seconds = time % 60;
            timeLabel.setText(String.format("%d:%06.3f", minutes, seconds));
            repaint();
        });
        
        tb.getItems().addAll(
            playButton, stopButton,
            new Separator(),
            prevFrame, nextFrame,
            new Separator(),
            addKeyButton, removeKeyButton,
            new Separator(),
            zoomLabel, zoomSlider,
            new Separator(),
            timeLabel
        );
        
        return tb;
    }
    
    public void setClip(AnimationClip clip) {
        this.currentClip = clip;
        buildTrackList();
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
        
        for (int frame = startFrame; frame <= endFrame; frame++) {
            double x = frame * pixelsPerFrame - viewOffsetX;
            
            if (frame % FRAME_RATE == 0) {
                // Major tick (seconds)
                gc.setStroke(Color.rgb(150, 150, 150));
                gc.strokeLine(x, h - 15, x, h);
                gc.fillText(String.valueOf(frame / FRAME_RATE) + "s", x + 2, 12);
            } else if (frame % 5 == 0) {
                // Medium tick
                gc.setStroke(Color.rgb(100, 100, 100));
                gc.strokeLine(x, h - 10, x, h);
            } else {
                // Minor tick
                gc.setStroke(Color.rgb(70, 70, 70));
                gc.strokeLine(x, h - 5, x, h);
            }
        }
        
        // Current time indicator
        double currentTime = EditorContext.getInstance().getCurrentTime();
        double currentX = currentTime * FRAME_RATE * pixelsPerFrame - viewOffsetX;
        
        gc.setFill(Color.rgb(255, 100, 100));
        gc.fillRect(currentX - 1, 0, 2, h);
        
        // Playhead triangle
        gc.beginPath();
        gc.moveTo(currentX - 6, 0);
        gc.lineTo(currentX + 6, 0);
        gc.lineTo(currentX, 10);
        gc.closePath();
        gc.fill();
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
            double x = frame * pixelsPerFrame - viewOffsetX;
            gc.setStroke(frame % FRAME_RATE == 0 ? Color.rgb(60, 60, 62) : Color.rgb(45, 45, 47));
            gc.strokeLine(x, 0, x, h);
        }
        
        // Track row backgrounds
        int row = 0;
        for (int y = 0; y < h; y += TRACK_HEIGHT) {
            gc.setFill(row % 2 == 0 ? Color.rgb(42, 42, 45) : Color.rgb(37, 37, 38));
            gc.fillRect(0, y, w, TRACK_HEIGHT);
            row++;
        }
        
        // Draw keyframes
        if (currentClip != null) {
            for (KeyframeTrack<?> track : currentClip.getTracks()) {
                Integer trackRow = trackRows.get(track);
                if (trackRow == null) continue;
                
                double trackY = trackRow * TRACK_HEIGHT + TRACK_HEIGHT / 2.0;
                
                for (Keyframe<?> keyframe : track.getKeyframes()) {
                    double kfX = keyframe.getTime() * FRAME_RATE * pixelsPerFrame - viewOffsetX;
                    
                    // Keyframe diamond
                    gc.setFill(track == selectedTrack && (int)(keyframe.getTime() * FRAME_RATE) == selectedKeyframeFrame
                        ? Color.rgb(100, 180, 255)
                        : Color.rgb(255, 180, 100));
                    
                    double size = 6;
                    gc.beginPath();
                    gc.moveTo(kfX, trackY - size);
                    gc.lineTo(kfX + size, trackY);
                    gc.lineTo(kfX, trackY + size);
                    gc.lineTo(kfX - size, trackY);
                    gc.closePath();
                    gc.fill();
                }
            }
        }
        
        // Current time line
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
    
    public AnimationClip getCurrentClip() {
        return currentClip;
    }
}
