package com.animstudio.editor.ui.timeline;

import com.animstudio.core.animation.Keyframe;
import com.animstudio.core.animation.KeyframeTrack;
import com.animstudio.core.interpolation.BezierInterpolator;
import com.animstudio.core.interpolation.Interpolator;
import com.animstudio.core.interpolation.LinearInterpolator;
import com.animstudio.core.interpolation.SteppedInterpolator;

import javafx.geometry.Insets;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

/**
 * A curve editor for editing keyframe interpolation.
 * Displays a normalized curve (0-1 on both axes) representing
 * the easing function between two keyframes.
 */
public class CurveEditorPane extends VBox {
    
    private static final int CANVAS_SIZE = 200;
    private static final int PADDING = 20;
    private static final double HANDLE_RADIUS = 6;
    
    private final Canvas canvas;
    private final ComboBox<String> interpolationTypeCombo;
    private final Spinner<Double> cp1xSpinner;
    private final Spinner<Double> cp1ySpinner;
    private final Spinner<Double> cp2xSpinner;
    private final Spinner<Double> cp2ySpinner;
    
    private Keyframe<?> targetKeyframe;
    private KeyframeTrack<?> targetTrack;
    
    // Bezier control points (normalized 0-1)
    private double cp1x = 0.25, cp1y = 0.1;
    private double cp2x = 0.75, cp2y = 0.9;
    
    // Dragging state
    private enum DragHandle { NONE, CP1, CP2 }
    private DragHandle dragging = DragHandle.NONE;
    
    private Runnable onChangeCallback;
    
    public CurveEditorPane() {
        setSpacing(10);
        setPadding(new Insets(10));
        
        // Title
        Label title = new Label("Curve Editor");
        title.setStyle("-fx-font-weight: bold;");
        
        // Interpolation type selector
        HBox typeBox = new HBox(10);
        typeBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        
        Label typeLabel = new Label("Type:");
        interpolationTypeCombo = new ComboBox<>();
        interpolationTypeCombo.getItems().addAll("Linear", "Bezier", "Stepped");
        interpolationTypeCombo.setValue("Linear");
        interpolationTypeCombo.setOnAction(e -> onInterpolationTypeChanged());
        
        typeBox.getChildren().addAll(typeLabel, interpolationTypeCombo);
        
        // Canvas for curve display
        canvas = new Canvas(CANVAS_SIZE + PADDING * 2, CANVAS_SIZE + PADDING * 2);
        canvas.setOnMousePressed(this::onMousePressed);
        canvas.setOnMouseDragged(this::onMouseDragged);
        canvas.setOnMouseReleased(e -> dragging = DragHandle.NONE);
        
        // Control point spinners (for precise editing)
        GridPane controlGrid = new GridPane();
        controlGrid.setHgap(10);
        controlGrid.setVgap(5);
        
        cp1xSpinner = createSpinner(0, 1, 0.25);
        cp1ySpinner = createSpinner(-0.5, 1.5, 0.1);
        cp2xSpinner = createSpinner(0, 1, 0.75);
        cp2ySpinner = createSpinner(-0.5, 1.5, 0.9);
        
        controlGrid.add(new Label("CP1 X:"), 0, 0);
        controlGrid.add(cp1xSpinner, 1, 0);
        controlGrid.add(new Label("Y:"), 2, 0);
        controlGrid.add(cp1ySpinner, 3, 0);
        
        controlGrid.add(new Label("CP2 X:"), 0, 1);
        controlGrid.add(cp2xSpinner, 1, 1);
        controlGrid.add(new Label("Y:"), 2, 1);
        controlGrid.add(cp2ySpinner, 3, 1);
        
        // Preset buttons
        HBox presetBox = new HBox(5);
        presetBox.getChildren().addAll(
            createPresetButton("Ease In", 0.42, 0, 1, 1),
            createPresetButton("Ease Out", 0, 0, 0.58, 1),
            createPresetButton("Ease In-Out", 0.42, 0, 0.58, 1),
            createPresetButton("Back In", 0.6, -0.28, 0.735, 0.045)
        );
        
        // Apply button
        Button applyButton = new Button("Apply to Keyframe");
        applyButton.setMaxWidth(Double.MAX_VALUE);
        applyButton.setOnAction(e -> applyToKeyframe());
        
        getChildren().addAll(
            title,
            typeBox,
            canvas,
            controlGrid,
            new Label("Presets:"),
            presetBox,
            new Separator(),
            applyButton
        );
        
        // Spinner listeners
        cp1xSpinner.valueProperty().addListener((obs, old, val) -> { cp1x = val; repaint(); });
        cp1ySpinner.valueProperty().addListener((obs, old, val) -> { cp1y = val; repaint(); });
        cp2xSpinner.valueProperty().addListener((obs, old, val) -> { cp2x = val; repaint(); });
        cp2ySpinner.valueProperty().addListener((obs, old, val) -> { cp2y = val; repaint(); });
        
        // Initial paint
        repaint();
        updateControlsEnabled();
    }
    
    private Spinner<Double> createSpinner(double min, double max, double initial) {
        Spinner<Double> spinner = new Spinner<>(min, max, initial, 0.05);
        spinner.setEditable(true);
        spinner.setPrefWidth(70);
        return spinner;
    }
    
    private Button createPresetButton(String name, double c1x, double c1y, double c2x, double c2y) {
        Button btn = new Button(name);
        btn.setStyle("-fx-font-size: 10px;");
        btn.setOnAction(e -> {
            cp1x = c1x; cp1y = c1y;
            cp2x = c2x; cp2y = c2y;
            updateSpinners();
            repaint();
        });
        return btn;
    }
    
    private void updateSpinners() {
        cp1xSpinner.getValueFactory().setValue(cp1x);
        cp1ySpinner.getValueFactory().setValue(cp1y);
        cp2xSpinner.getValueFactory().setValue(cp2x);
        cp2ySpinner.getValueFactory().setValue(cp2y);
    }
    
    private void onInterpolationTypeChanged() {
        updateControlsEnabled();
        repaint();
        
        if (onChangeCallback != null) {
            onChangeCallback.run();
        }
    }
    
    private void updateControlsEnabled() {
        boolean isBezier = "Bezier".equals(interpolationTypeCombo.getValue());
        cp1xSpinner.setDisable(!isBezier);
        cp1ySpinner.setDisable(!isBezier);
        cp2xSpinner.setDisable(!isBezier);
        cp2ySpinner.setDisable(!isBezier);
    }
    
    public void repaint() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        
        // Background
        gc.setFill(Color.rgb(30, 30, 32));
        gc.fillRect(0, 0, w, h);
        
        // Grid
        gc.setStroke(Color.rgb(50, 50, 55));
        gc.setLineWidth(1);
        
        for (int i = 0; i <= 4; i++) {
            double x = PADDING + (CANVAS_SIZE * i / 4.0);
            double y = PADDING + (CANVAS_SIZE * i / 4.0);
            gc.strokeLine(x, PADDING, x, PADDING + CANVAS_SIZE);
            gc.strokeLine(PADDING, y, PADDING + CANVAS_SIZE, y);
        }
        
        // Diagonal (linear reference)
        gc.setStroke(Color.rgb(80, 80, 85));
        gc.setLineDashes(4, 4);
        gc.strokeLine(PADDING, PADDING + CANVAS_SIZE, PADDING + CANVAS_SIZE, PADDING);
        gc.setLineDashes(null);
        
        // Draw the curve
        String type = interpolationTypeCombo.getValue();
        if ("Linear".equals(type)) {
            drawLinearCurve(gc);
        } else if ("Bezier".equals(type)) {
            drawBezierCurve(gc);
        } else if ("Stepped".equals(type)) {
            drawSteppedCurve(gc);
        }
        
        // Border
        gc.setStroke(Color.rgb(100, 100, 105));
        gc.setLineWidth(2);
        gc.strokeRect(PADDING, PADDING, CANVAS_SIZE, CANVAS_SIZE);
        
        // Labels
        gc.setFill(Color.rgb(150, 150, 150));
        gc.setFont(javafx.scene.text.Font.font(10));
        gc.fillText("0", PADDING - 12, PADDING + CANVAS_SIZE + 4);
        gc.fillText("1", PADDING + CANVAS_SIZE - 4, PADDING + CANVAS_SIZE + 12);
        gc.fillText("In", PADDING + CANVAS_SIZE / 2 - 6, PADDING + CANVAS_SIZE + 14);
        gc.fillText("Out", 2, PADDING + CANVAS_SIZE / 2 + 4);
    }
    
    private void drawLinearCurve(GraphicsContext gc) {
        gc.setStroke(Color.rgb(100, 200, 255));
        gc.setLineWidth(2);
        gc.strokeLine(PADDING, PADDING + CANVAS_SIZE, PADDING + CANVAS_SIZE, PADDING);
    }
    
    private void drawBezierCurve(GraphicsContext gc) {
        // Draw control point handles
        double p0x = PADDING, p0y = PADDING + CANVAS_SIZE;
        double p3x = PADDING + CANVAS_SIZE, p3y = PADDING;
        
        double c1x = PADDING + cp1x * CANVAS_SIZE;
        double c1y = PADDING + CANVAS_SIZE - cp1y * CANVAS_SIZE;
        double c2x = PADDING + cp2x * CANVAS_SIZE;
        double c2y = PADDING + CANVAS_SIZE - cp2y * CANVAS_SIZE;
        
        // Control point lines
        gc.setStroke(Color.rgb(150, 150, 150, 0.5));
        gc.setLineWidth(1);
        gc.strokeLine(p0x, p0y, c1x, c1y);
        gc.strokeLine(p3x, p3y, c2x, c2y);
        
        // Bezier curve
        gc.setStroke(Color.rgb(100, 200, 255));
        gc.setLineWidth(2);
        gc.beginPath();
        gc.moveTo(p0x, p0y);
        gc.bezierCurveTo(c1x, c1y, c2x, c2y, p3x, p3y);
        gc.stroke();
        
        // Control point handles
        gc.setFill(Color.rgb(255, 180, 100));
        gc.fillOval(c1x - HANDLE_RADIUS, c1y - HANDLE_RADIUS, HANDLE_RADIUS * 2, HANDLE_RADIUS * 2);
        gc.fillOval(c2x - HANDLE_RADIUS, c2y - HANDLE_RADIUS, HANDLE_RADIUS * 2, HANDLE_RADIUS * 2);
        
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(1);
        gc.strokeOval(c1x - HANDLE_RADIUS, c1y - HANDLE_RADIUS, HANDLE_RADIUS * 2, HANDLE_RADIUS * 2);
        gc.strokeOval(c2x - HANDLE_RADIUS, c2y - HANDLE_RADIUS, HANDLE_RADIUS * 2, HANDLE_RADIUS * 2);
    }
    
    private void drawSteppedCurve(GraphicsContext gc) {
        gc.setStroke(Color.rgb(255, 150, 150));
        gc.setLineWidth(2);
        gc.strokeLine(PADDING, PADDING + CANVAS_SIZE, PADDING + CANVAS_SIZE, PADDING + CANVAS_SIZE);
        gc.strokeLine(PADDING + CANVAS_SIZE, PADDING + CANVAS_SIZE, PADDING + CANVAS_SIZE, PADDING);
    }
    
    private void onMousePressed(MouseEvent e) {
        if (!"Bezier".equals(interpolationTypeCombo.getValue())) return;
        if (e.getButton() != MouseButton.PRIMARY) return;
        
        double mx = e.getX();
        double my = e.getY();
        
        double c1x = PADDING + cp1x * CANVAS_SIZE;
        double c1y = PADDING + CANVAS_SIZE - cp1y * CANVAS_SIZE;
        double c2x = PADDING + cp2x * CANVAS_SIZE;
        double c2y = PADDING + CANVAS_SIZE - cp2y * CANVAS_SIZE;
        
        // Check if clicking on a control point
        if (Math.hypot(mx - c1x, my - c1y) < HANDLE_RADIUS * 2) {
            dragging = DragHandle.CP1;
        } else if (Math.hypot(mx - c2x, my - c2y) < HANDLE_RADIUS * 2) {
            dragging = DragHandle.CP2;
        }
    }
    
    private void onMouseDragged(MouseEvent e) {
        if (dragging == DragHandle.NONE) return;
        
        double mx = e.getX();
        double my = e.getY();
        
        // Convert to normalized coordinates
        double nx = (mx - PADDING) / CANVAS_SIZE;
        double ny = 1.0 - (my - PADDING) / CANVAS_SIZE;
        
        // Clamp x to valid range
        nx = Math.max(0, Math.min(1, nx));
        // Allow y to go slightly out of bounds for overshoot effects
        ny = Math.max(-0.5, Math.min(1.5, ny));
        
        if (dragging == DragHandle.CP1) {
            cp1x = nx;
            cp1y = ny;
        } else if (dragging == DragHandle.CP2) {
            cp2x = nx;
            cp2y = ny;
        }
        
        updateSpinners();
        repaint();
    }
    
    /**
     * Set the keyframe to edit.
     */
    public void setTargetKeyframe(KeyframeTrack<?> track, Keyframe<?> keyframe) {
        this.targetTrack = track;
        this.targetKeyframe = keyframe;
        
        if (keyframe != null) {
            Interpolator interp = keyframe.getInterpolator();
            
            if (interp instanceof SteppedInterpolator) {
                interpolationTypeCombo.setValue("Stepped");
            } else if (interp instanceof BezierInterpolator) {
                interpolationTypeCombo.setValue("Bezier");
                BezierInterpolator bezier = (BezierInterpolator) interp;
                cp1x = bezier.getX1();
                cp1y = bezier.getY1();
                cp2x = bezier.getX2();
                cp2y = bezier.getY2();
                updateSpinners();
            } else {
                interpolationTypeCombo.setValue("Linear");
            }
        }
        
        updateControlsEnabled();
        repaint();
    }
    
    /**
     * Apply the current curve settings to the target keyframe.
     */
    @SuppressWarnings("unchecked")
    private void applyToKeyframe() {
        if (targetKeyframe == null || targetTrack == null) return;
        
        Interpolator newInterpolator;
        String type = interpolationTypeCombo.getValue();
        
        if ("Linear".equals(type)) {
            newInterpolator = LinearInterpolator.INSTANCE;
        } else if ("Bezier".equals(type)) {
            newInterpolator = new BezierInterpolator(cp1x, cp1y, cp2x, cp2y);
        } else {
            newInterpolator = SteppedInterpolator.INSTANCE;
        }
        
        // Update the keyframe's interpolator
        targetKeyframe.setInterpolator(newInterpolator);
        
        if (onChangeCallback != null) {
            onChangeCallback.run();
        }
    }
    
    /**
     * Get the current interpolator based on UI settings.
     */
    public Interpolator getCurrentInterpolator() {
        String type = interpolationTypeCombo.getValue();
        
        if ("Linear".equals(type)) {
            return LinearInterpolator.INSTANCE;
        } else if ("Bezier".equals(type)) {
            return new BezierInterpolator(cp1x, cp1y, cp2x, cp2y);
        } else {
            return SteppedInterpolator.INSTANCE;
        }
    }
    
    /**
     * Set callback to be called when curve changes.
     */
    public void setOnChangeCallback(Runnable callback) {
        this.onChangeCallback = callback;
    }
}
