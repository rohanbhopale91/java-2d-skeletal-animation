package com.animstudio.editor.ui.constraints;

import com.animstudio.core.ik.IKConstraint;
import com.animstudio.core.ik.IKManager;
import com.animstudio.core.model.Bone;
import com.animstudio.editor.EditorContext;
import com.animstudio.editor.commands.DeleteIKConstraintCommand;
import com.animstudio.editor.commands.ModifyIKConstraintCommand;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.stream.Collectors;

/**
 * Panel for viewing and editing IK constraints.
 */
public class IKConstraintsPane extends VBox {
    
    private final EditorContext context;
    private final ListView<IKConstraint> constraintListView;
    private final ObjectProperty<IKConstraint> selectedConstraint = new SimpleObjectProperty<>();
    
    // Property editors
    private final TextField nameField;
    private final Slider mixSlider;
    private final Label mixLabel;
    private final CheckBox bendPositiveCheckBox;
    private final CheckBox stretchCheckBox;
    private final CheckBox compressCheckBox;
    private final Spinner<Double> softnessSpinner;
    private final Spinner<Integer> iterationsSpinner;
    private final Label chainLabel;
    private final Label targetLabel;
    
    private final Button deleteButton;
    private final VBox propertiesBox;
    
    private boolean updating = false;
    
    public IKConstraintsPane(EditorContext context) {
        this.context = context;
        
        setSpacing(8);
        setPadding(new Insets(8));
        getStyleClass().add("ik-constraints-pane");
        
        // Title
        Label titleLabel = new Label("IK Constraints");
        titleLabel.getStyleClass().add("pane-title");
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        
        // Constraint list
        constraintListView = new ListView<>();
        constraintListView.setPlaceholder(new Label("No IK constraints\nUse IK Tool to create"));
        constraintListView.setCellFactory(lv -> new ConstraintListCell());
        constraintListView.setPrefHeight(120);
        VBox.setVgrow(constraintListView, Priority.NEVER);
        
        constraintListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            selectedConstraint.set(newVal);
            updatePropertiesPane();
        });
        
        // Toolbar
        Button refreshButton = new Button("↻");
        refreshButton.setTooltip(new Tooltip("Refresh constraint list"));
        refreshButton.setOnAction(e -> refreshConstraintList());
        
        deleteButton = new Button("−");
        deleteButton.setTooltip(new Tooltip("Delete selected constraint"));
        deleteButton.setDisable(true);
        deleteButton.setOnAction(e -> deleteSelectedConstraint());
        
        HBox toolbar = new HBox(4, refreshButton, deleteButton);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        
        // Properties section
        propertiesBox = new VBox(8);
        propertiesBox.setPadding(new Insets(8));
        propertiesBox.setStyle("-fx-background-color: #2d2d30; -fx-background-radius: 4;");
        
        Label propsTitle = new Label("Properties");
        propsTitle.setStyle("-fx-font-weight: bold;");
        
        // Name field
        HBox nameBox = new HBox(8);
        nameBox.setAlignment(Pos.CENTER_LEFT);
        nameField = new TextField();
        nameField.setPromptText("Constraint name");
        nameField.setPrefWidth(150);
        nameField.setOnAction(e -> applyNameChange());
        nameBox.getChildren().addAll(new Label("Name:"), nameField);
        
        // Chain info
        chainLabel = new Label("Chain: (none)");
        chainLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #888;");
        
        targetLabel = new Label("Target: (none)");
        targetLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #888;");
        
        // Mix slider
        HBox mixBox = new HBox(8);
        mixBox.setAlignment(Pos.CENTER_LEFT);
        mixSlider = new Slider(0, 1, 1);
        mixSlider.setPrefWidth(120);
        mixSlider.setShowTickMarks(true);
        mixSlider.setMajorTickUnit(0.25);
        mixLabel = new Label("1.00");
        mixLabel.setMinWidth(35);
        mixSlider.valueProperty().addListener((obs, old, val) -> {
            mixLabel.setText(String.format("%.2f", val.doubleValue()));
            if (!updating) {
                applyMixChange(old.doubleValue(), val.doubleValue());
            }
        });
        mixBox.getChildren().addAll(new Label("Mix:"), mixSlider, mixLabel);
        
        // Checkboxes
        bendPositiveCheckBox = new CheckBox("Bend Positive");
        bendPositiveCheckBox.setTooltip(new Tooltip("Direction of bend for 2-bone IK"));
        bendPositiveCheckBox.selectedProperty().addListener((obs, old, val) -> {
            if (!updating) applyBendPositiveChange(old, val);
        });
        
        stretchCheckBox = new CheckBox("Stretch");
        stretchCheckBox.setTooltip(new Tooltip("Allow bones to stretch beyond length"));
        stretchCheckBox.selectedProperty().addListener((obs, old, val) -> {
            if (!updating) applyStretchChange(old, val);
        });
        
        compressCheckBox = new CheckBox("Compress");
        compressCheckBox.setTooltip(new Tooltip("Allow bones to compress below length"));
        compressCheckBox.selectedProperty().addListener((obs, old, val) -> {
            if (!updating) applyCompressChange(old, val);
        });
        
        HBox checkboxRow = new HBox(12, bendPositiveCheckBox, stretchCheckBox, compressCheckBox);
        
        // Softness spinner
        HBox softnessBox = new HBox(8);
        softnessBox.setAlignment(Pos.CENTER_LEFT);
        softnessSpinner = new Spinner<>(0.0, 100.0, 0.0, 0.5);
        softnessSpinner.setEditable(true);
        softnessSpinner.setPrefWidth(80);
        softnessSpinner.valueProperty().addListener((obs, old, val) -> {
            if (!updating && old != null && val != null) {
                applySoftnessChange(old, val);
            }
        });
        softnessBox.getChildren().addAll(new Label("Softness:"), softnessSpinner);
        
        // Iterations spinner (for CCD solver)
        HBox iterationsBox = new HBox(8);
        iterationsBox.setAlignment(Pos.CENTER_LEFT);
        iterationsSpinner = new Spinner<>(1, 50, 10, 1);
        iterationsSpinner.setEditable(true);
        iterationsSpinner.setPrefWidth(80);
        iterationsSpinner.valueProperty().addListener((obs, old, val) -> {
            if (!updating && old != null && val != null) {
                applyIterationsChange(old, val);
            }
        });
        iterationsBox.getChildren().addAll(new Label("Iterations:"), iterationsSpinner);
        
        // Apply button
        Button applyButton = new Button("Apply IK");
        applyButton.setTooltip(new Tooltip("Apply IK constraint to update bone rotations"));
        applyButton.setOnAction(e -> applySelectedConstraint());
        
        propertiesBox.getChildren().addAll(
            propsTitle,
            nameBox,
            chainLabel,
            targetLabel,
            new Separator(),
            mixBox,
            checkboxRow,
            softnessBox,
            iterationsBox,
            new Separator(),
            applyButton
        );
        
        // Initially hide properties until a constraint is selected
        propertiesBox.setVisible(false);
        propertiesBox.setManaged(false);
        
        getChildren().addAll(titleLabel, toolbar, constraintListView, propertiesBox);
        
        // Listen for selection changes
        selectedConstraint.addListener((obs, old, val) -> {
            deleteButton.setDisable(val == null);
            propertiesBox.setVisible(val != null);
            propertiesBox.setManaged(val != null);
        });
        
        // Initial refresh
        Platform.runLater(this::refreshConstraintList);
    }
    
    /**
     * Refresh the constraint list from IKManager.
     */
    public void refreshConstraintList() {
        constraintListView.getItems().clear();
        
        IKManager ikManager = context.getIKManager();
        if (ikManager != null) {
            constraintListView.getItems().addAll(ikManager.getConstraints());
            
            // Reselect if still exists
            IKConstraint selected = selectedConstraint.get();
            if (selected != null && constraintListView.getItems().contains(selected)) {
                constraintListView.getSelectionModel().select(selected);
            }
        }
    }
    
    /**
     * Update the properties pane with selected constraint values.
     */
    private void updatePropertiesPane() {
        IKConstraint constraint = selectedConstraint.get();
        if (constraint == null) return;
        
        updating = true;
        try {
            nameField.setText(constraint.getName());
            mixSlider.setValue(constraint.getMix());
            bendPositiveCheckBox.setSelected(constraint.isBendPositive());
            stretchCheckBox.setSelected(constraint.isStretch());
            compressCheckBox.setSelected(constraint.isCompress());
            softnessSpinner.getValueFactory().setValue(constraint.getSoftness());
            iterationsSpinner.getValueFactory().setValue(constraint.getMaxIterations());
            
            // Update chain info
            String chainText = constraint.getBones().stream()
                .map(Bone::getName)
                .collect(Collectors.joining(" → "));
            chainLabel.setText("Chain: " + (chainText.isEmpty() ? "(none)" : chainText));
            
            // Update target info
            Bone target = constraint.getTarget();
            targetLabel.setText("Target: " + (target != null ? target.getName() : "(none)"));
            
        } finally {
            updating = false;
        }
    }
    
    private void deleteSelectedConstraint() {
        IKConstraint constraint = selectedConstraint.get();
        IKManager ikManager = context.getIKManager();
        
        if (constraint == null || ikManager == null) return;
        
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete IK Constraint");
        confirm.setHeaderText("Delete '" + constraint.getName() + "'?");
        confirm.setContentText("This action can be undone with Ctrl+Z.");
        
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                DeleteIKConstraintCommand cmd = new DeleteIKConstraintCommand(ikManager, constraint);
                context.getCommandStack().execute(cmd);
                refreshConstraintList();
            }
        });
    }
    
    private void applyNameChange() {
        IKConstraint constraint = selectedConstraint.get();
        if (constraint == null) return;
        
        String oldName = constraint.getName();
        String newName = nameField.getText().trim();
        
        if (!newName.isEmpty() && !newName.equals(oldName)) {
            ModifyIKConstraintCommand cmd = new ModifyIKConstraintCommand(
                constraint, "name", oldName, newName);
            context.getCommandStack().execute(cmd);
            refreshConstraintList();
        }
    }
    
    private void applyMixChange(double oldValue, double newValue) {
        IKConstraint constraint = selectedConstraint.get();
        if (constraint == null) return;
        
        ModifyIKConstraintCommand cmd = new ModifyIKConstraintCommand(
            constraint, "mix", oldValue, newValue);
        context.getCommandStack().execute(cmd);
    }
    
    private void applyBendPositiveChange(boolean oldValue, boolean newValue) {
        IKConstraint constraint = selectedConstraint.get();
        if (constraint == null) return;
        
        ModifyIKConstraintCommand cmd = new ModifyIKConstraintCommand(
            constraint, "bendPositive", oldValue, newValue);
        context.getCommandStack().execute(cmd);
    }
    
    private void applyStretchChange(boolean oldValue, boolean newValue) {
        IKConstraint constraint = selectedConstraint.get();
        if (constraint == null) return;
        
        ModifyIKConstraintCommand cmd = new ModifyIKConstraintCommand(
            constraint, "stretch", oldValue, newValue);
        context.getCommandStack().execute(cmd);
    }
    
    private void applyCompressChange(boolean oldValue, boolean newValue) {
        IKConstraint constraint = selectedConstraint.get();
        if (constraint == null) return;
        
        ModifyIKConstraintCommand cmd = new ModifyIKConstraintCommand(
            constraint, "compress", oldValue, newValue);
        context.getCommandStack().execute(cmd);
    }
    
    private void applySoftnessChange(double oldValue, double newValue) {
        IKConstraint constraint = selectedConstraint.get();
        if (constraint == null) return;
        
        ModifyIKConstraintCommand cmd = new ModifyIKConstraintCommand(
            constraint, "softness", oldValue, newValue);
        context.getCommandStack().execute(cmd);
    }
    
    private void applyIterationsChange(int oldValue, int newValue) {
        IKConstraint constraint = selectedConstraint.get();
        if (constraint == null) return;
        
        ModifyIKConstraintCommand cmd = new ModifyIKConstraintCommand(
            constraint, "maxIterations", oldValue, newValue);
        context.getCommandStack().execute(cmd);
    }
    
    private void applySelectedConstraint() {
        IKConstraint constraint = selectedConstraint.get();
        if (constraint != null) {
            constraint.apply();
            // Request canvas repaint
            if (context.getMainController() != null) {
                context.getMainController().requestCanvasRepaint();
            }
        }
    }
    
    /**
     * Get the selected constraint property for binding.
     */
    public ObjectProperty<IKConstraint> selectedConstraintProperty() {
        return selectedConstraint;
    }
    
    /**
     * Custom list cell for IK constraints.
     */
    private class ConstraintListCell extends ListCell<IKConstraint> {
        @Override
        protected void updateItem(IKConstraint constraint, boolean empty) {
            super.updateItem(constraint, empty);
            
            if (empty || constraint == null) {
                setText(null);
                setGraphic(null);
            } else {
                String text = constraint.getName();
                int boneCount = constraint.getBones().size();
                text += " (" + boneCount + " bone" + (boneCount != 1 ? "s" : "") + ")";
                setText(text);
                
                // Show mix level visually
                double mix = constraint.getMix();
                if (mix < 1.0) {
                    setStyle("-fx-opacity: " + (0.5 + mix * 0.5) + ";");
                } else {
                    setStyle("");
                }
            }
        }
    }
}
