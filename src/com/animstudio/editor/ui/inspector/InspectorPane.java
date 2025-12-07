package com.animstudio.editor.ui.inspector;

import com.animstudio.core.model.Bone;
import com.animstudio.editor.EditorContext;
import com.animstudio.editor.commands.MoveBoneCommand;
import com.animstudio.editor.commands.RotateBoneCommand;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Inspector panel for editing properties of selected bone.
 */
public class InspectorPane extends VBox {
    
    private Bone target;
    
    // Bone properties
    private final Label nameLabel;
    private final TextField nameField;
    private final Spinner<Double> xSpinner;
    private final Spinner<Double> ySpinner;
    private final Spinner<Double> rotationSpinner;
    private final Spinner<Double> scaleXSpinner;
    private final Spinner<Double> scaleYSpinner;
    private final Spinner<Double> lengthSpinner;
    private final ColorPicker colorPicker;
    
    private boolean updating = false;
    
    public InspectorPane() {
        setSpacing(10);
        setPadding(new Insets(10));
        
        // Title
        Label title = new Label("Inspector");
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        
        // Name section
        nameLabel = new Label("No selection");
        nameField = new TextField();
        nameField.setPromptText("Bone name");
        nameField.setOnAction(e -> applyName());
        
        // Transform section
        TitledPane transformPane = new TitledPane();
        transformPane.setText("Transform");
        transformPane.setCollapsible(true);
        transformPane.setExpanded(true);
        
        GridPane transformGrid = new GridPane();
        transformGrid.setHgap(10);
        transformGrid.setVgap(5);
        transformGrid.setPadding(new Insets(5));
        
        // Position
        xSpinner = createDoubleSpinner(-10000, 10000, 0, 1);
        ySpinner = createDoubleSpinner(-10000, 10000, 0, 1);
        
        transformGrid.add(new Label("X:"), 0, 0);
        transformGrid.add(xSpinner, 1, 0);
        transformGrid.add(new Label("Y:"), 0, 1);
        transformGrid.add(ySpinner, 1, 1);
        
        // Rotation
        rotationSpinner = createDoubleSpinner(-360, 360, 0, 1);
        transformGrid.add(new Label("Rotation:"), 0, 2);
        transformGrid.add(rotationSpinner, 1, 2);
        
        // Scale
        scaleXSpinner = createDoubleSpinner(0.01, 100, 1, 0.1);
        scaleYSpinner = createDoubleSpinner(0.01, 100, 1, 0.1);
        
        transformGrid.add(new Label("Scale X:"), 0, 3);
        transformGrid.add(scaleXSpinner, 1, 3);
        transformGrid.add(new Label("Scale Y:"), 0, 4);
        transformGrid.add(scaleYSpinner, 1, 4);
        
        // Length
        lengthSpinner = createDoubleSpinner(0, 1000, 50, 5);
        transformGrid.add(new Label("Length:"), 0, 5);
        transformGrid.add(lengthSpinner, 1, 5);
        
        GridPane.setHgrow(xSpinner, Priority.ALWAYS);
        GridPane.setHgrow(ySpinner, Priority.ALWAYS);
        
        transformPane.setContent(transformGrid);
        
        // Appearance section
        TitledPane appearancePane = new TitledPane();
        appearancePane.setText("Appearance");
        appearancePane.setCollapsible(true);
        appearancePane.setExpanded(false);
        
        GridPane appearanceGrid = new GridPane();
        appearanceGrid.setHgap(10);
        appearanceGrid.setVgap(5);
        appearanceGrid.setPadding(new Insets(5));
        
        colorPicker = new ColorPicker();
        appearanceGrid.add(new Label("Color:"), 0, 0);
        appearanceGrid.add(colorPicker, 1, 0);
        
        appearancePane.setContent(appearanceGrid);
        
        // Buttons
        Button resetButton = new Button("Reset to Setup Pose");
        resetButton.setOnAction(e -> resetToSetupPose());
        resetButton.setMaxWidth(Double.MAX_VALUE);
        
        Button setSetupButton = new Button("Set as Setup Pose");
        setSetupButton.setOnAction(e -> setAsSetupPose());
        setSetupButton.setMaxWidth(Double.MAX_VALUE);
        
        // Add all sections
        getChildren().addAll(
            title,
            nameLabel,
            nameField,
            new Separator(),
            transformPane,
            appearancePane,
            new Separator(),
            resetButton,
            setSetupButton
        );
        
        // Setup value change listeners
        setupListeners();
        
        // Initial state
        setTarget(null);
    }
    
    private Spinner<Double> createDoubleSpinner(double min, double max, double initial, double step) {
        SpinnerValueFactory<Double> factory = new SpinnerValueFactory.DoubleSpinnerValueFactory(min, max, initial, step);
        Spinner<Double> spinner = new Spinner<>(factory);
        spinner.setEditable(true);
        spinner.setPrefWidth(100);
        
        // Handle text input
        spinner.getEditor().setOnAction(e -> {
            try {
                double value = Double.parseDouble(spinner.getEditor().getText());
                spinner.getValueFactory().setValue(value);
            } catch (NumberFormatException ex) {
                spinner.getEditor().setText(String.valueOf(spinner.getValue()));
            }
        });
        
        return spinner;
    }
    
    private void setupListeners() {
        xSpinner.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (!updating && target != null) {
                applyPosition();
            }
        });
        
        ySpinner.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (!updating && target != null) {
                applyPosition();
            }
        });
        
        rotationSpinner.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (!updating && target != null) {
                applyRotation();
            }
        });
        
        scaleXSpinner.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (!updating && target != null) {
                target.setScaleX(newVal);
                updateSkeleton();
            }
        });
        
        scaleYSpinner.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (!updating && target != null) {
                target.setScaleY(newVal);
                updateSkeleton();
            }
        });
        
        lengthSpinner.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (!updating && target != null) {
                target.setLength(newVal);
                updateSkeleton();
            }
        });
    }
    
    public void setTarget(Bone bone) {
        this.target = bone;
        updating = true;
        
        if (bone == null) {
            nameLabel.setText("No selection");
            nameField.setText("");
            nameField.setDisable(true);
            xSpinner.setDisable(true);
            ySpinner.setDisable(true);
            rotationSpinner.setDisable(true);
            scaleXSpinner.setDisable(true);
            scaleYSpinner.setDisable(true);
            lengthSpinner.setDisable(true);
        } else {
            nameLabel.setText("Bone: " + bone.getName());
            nameField.setText(bone.getName());
            nameField.setDisable(false);
            
            xSpinner.setDisable(false);
            ySpinner.setDisable(false);
            rotationSpinner.setDisable(false);
            scaleXSpinner.setDisable(false);
            scaleYSpinner.setDisable(false);
            lengthSpinner.setDisable(false);
            
            xSpinner.getValueFactory().setValue(bone.getX());
            ySpinner.getValueFactory().setValue(bone.getY());
            rotationSpinner.getValueFactory().setValue(bone.getRotation());
            scaleXSpinner.getValueFactory().setValue(bone.getScaleX());
            scaleYSpinner.getValueFactory().setValue(bone.getScaleY());
            lengthSpinner.getValueFactory().setValue(bone.getLength());
        }
        
        updating = false;
    }
    
    public void refresh() {
        setTarget(target);
    }
    
    private void applyName() {
        if (target != null) {
            target.setName(nameField.getText());
            nameLabel.setText("Bone: " + target.getName());
        }
    }
    
    private void applyPosition() {
        if (target == null) return;
        
        EditorContext.getInstance().getCommandStack().execute(
            new MoveBoneCommand(target, xSpinner.getValue(), ySpinner.getValue())
        );
        updateSkeleton();
    }
    
    private void applyRotation() {
        if (target == null) return;
        
        EditorContext.getInstance().getCommandStack().execute(
            new RotateBoneCommand(target, rotationSpinner.getValue())
        );
        updateSkeleton();
    }
    
    private void resetToSetupPose() {
        if (target != null) {
            target.resetToSetupPose();
            updateSkeleton();
            refresh();
        }
    }
    
    private void setAsSetupPose() {
        if (target != null) {
            target.setToSetupPose();
        }
    }
    
    private void updateSkeleton() {
        EditorContext context = EditorContext.getInstance();
        if (context.getCurrentSkeleton() != null) {
            context.getCurrentSkeleton().updateWorldTransforms();
        }
    }
}
