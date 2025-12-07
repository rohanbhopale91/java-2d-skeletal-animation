package com.animstudio.editor.ui.ik;

import com.animstudio.core.ik.IKConstraint;
import com.animstudio.core.ik.IKManager;
import com.animstudio.core.model.Bone;
import com.animstudio.core.model.Skeleton;
import com.animstudio.editor.EditorContext;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;

/**
 * UI panel for managing IK constraints.
 * Allows creating, editing, and removing IK chains.
 */
public class IKEditorPane extends BorderPane {
    
    private final TableView<IKConstraintEntry> constraintTable;
    private final ObservableList<IKConstraintEntry> constraintEntries;
    
    // Properties panel
    private final TextField nameField;
    private final ComboBox<String> startBoneCombo;
    private final ComboBox<String> endBoneCombo;
    private final ComboBox<String> targetBoneCombo;
    private final Slider mixSlider;
    private final CheckBox bendPositiveCheck;
    private final CheckBox stretchCheck;
    private final CheckBox compressCheck;
    private final Spinner<Double> softnessSpinner;
    private final Spinner<Integer> iterationsSpinner;
    
    private IKConstraint selectedConstraint;
    private boolean updatingUI = false;
    
    public IKEditorPane() {
        setPadding(new Insets(10));
        setStyle("-fx-background-color: #2d2d30;");
        
        // Constraint list table
        constraintEntries = FXCollections.observableArrayList();
        constraintTable = new TableView<>(constraintEntries);
        constraintTable.setPlaceholder(new Label("No IK constraints"));
        constraintTable.setPrefHeight(150);
        
        TableColumn<IKConstraintEntry, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameCol.setPrefWidth(100);
        
        TableColumn<IKConstraintEntry, String> chainCol = new TableColumn<>("Chain");
        chainCol.setCellValueFactory(new PropertyValueFactory<>("chain"));
        chainCol.setPrefWidth(120);
        
        TableColumn<IKConstraintEntry, Double> mixCol = new TableColumn<>("Mix");
        mixCol.setCellValueFactory(new PropertyValueFactory<>("mix"));
        mixCol.setPrefWidth(60);
        
        constraintTable.getColumns().addAll(nameCol, chainCol, mixCol);
        
        constraintTable.getSelectionModel().selectedItemProperty().addListener((obs, old, entry) -> {
            if (entry != null) {
                selectConstraint(entry.getConstraint());
            }
        });
        
        // Toolbar for constraint list
        Button addButton = new Button("+");
        addButton.setTooltip(new Tooltip("Create new IK constraint"));
        addButton.setOnAction(e -> createNewConstraint());
        
        Button removeButton = new Button("-");
        removeButton.setTooltip(new Tooltip("Remove selected constraint"));
        removeButton.setOnAction(e -> removeSelectedConstraint());
        
        Button refreshButton = new Button("↻");
        refreshButton.setTooltip(new Tooltip("Refresh constraint list"));
        refreshButton.setOnAction(e -> refreshConstraintList());
        
        HBox listToolbar = new HBox(5, addButton, removeButton, new Separator(), refreshButton);
        listToolbar.setPadding(new Insets(5, 0, 5, 0));
        
        VBox listSection = new VBox(5, new Label("IK Constraints:"), constraintTable, listToolbar);
        
        // Properties section
        GridPane propsGrid = new GridPane();
        propsGrid.setHgap(10);
        propsGrid.setVgap(8);
        propsGrid.setPadding(new Insets(10, 0, 0, 0));
        
        int row = 0;
        
        // Name
        nameField = new TextField();
        nameField.setPromptText("Constraint name");
        nameField.textProperty().addListener((obs, old, val) -> {
            if (!updatingUI && selectedConstraint != null) {
                selectedConstraint.setName(val);
                refreshConstraintList();
            }
        });
        propsGrid.add(new Label("Name:"), 0, row);
        propsGrid.add(nameField, 1, row++);
        
        // Start bone
        startBoneCombo = new ComboBox<>();
        startBoneCombo.setPromptText("Select start bone");
        startBoneCombo.setMaxWidth(Double.MAX_VALUE);
        propsGrid.add(new Label("Start Bone:"), 0, row);
        propsGrid.add(startBoneCombo, 1, row++);
        
        // End bone
        endBoneCombo = new ComboBox<>();
        endBoneCombo.setPromptText("Select end bone");
        endBoneCombo.setMaxWidth(Double.MAX_VALUE);
        endBoneCombo.setOnAction(e -> updateChainFromSelection());
        propsGrid.add(new Label("End Bone:"), 0, row);
        propsGrid.add(endBoneCombo, 1, row++);
        
        // Target bone
        targetBoneCombo = new ComboBox<>();
        targetBoneCombo.setPromptText("Select target");
        targetBoneCombo.setMaxWidth(Double.MAX_VALUE);
        targetBoneCombo.setOnAction(e -> updateTargetFromSelection());
        propsGrid.add(new Label("Target:"), 0, row);
        propsGrid.add(targetBoneCombo, 1, row++);
        
        // Mix slider
        mixSlider = new Slider(0, 1, 1);
        mixSlider.setShowTickLabels(true);
        mixSlider.setShowTickMarks(true);
        mixSlider.setMajorTickUnit(0.25);
        mixSlider.valueProperty().addListener((obs, old, val) -> {
            if (!updatingUI && selectedConstraint != null) {
                selectedConstraint.setMix(val.doubleValue());
                refreshConstraintList();
            }
        });
        Label mixLabel = new Label("Mix:");
        propsGrid.add(mixLabel, 0, row);
        propsGrid.add(mixSlider, 1, row++);
        
        // Bend direction
        bendPositiveCheck = new CheckBox("Bend Positive");
        bendPositiveCheck.setSelected(true);
        bendPositiveCheck.selectedProperty().addListener((obs, old, val) -> {
            if (!updatingUI && selectedConstraint != null) {
                selectedConstraint.setBendPositive(val);
            }
        });
        propsGrid.add(bendPositiveCheck, 1, row++);
        
        // Stretch
        stretchCheck = new CheckBox("Allow Stretch");
        stretchCheck.selectedProperty().addListener((obs, old, val) -> {
            if (!updatingUI && selectedConstraint != null) {
                selectedConstraint.setStretch(val);
            }
        });
        propsGrid.add(stretchCheck, 1, row++);
        
        // Compress
        compressCheck = new CheckBox("Allow Compress");
        compressCheck.selectedProperty().addListener((obs, old, val) -> {
            if (!updatingUI && selectedConstraint != null) {
                selectedConstraint.setCompress(val);
            }
        });
        propsGrid.add(compressCheck, 1, row++);
        
        // Softness
        softnessSpinner = new Spinner<>(0.0, 100.0, 0.0, 0.5);
        softnessSpinner.setEditable(true);
        softnessSpinner.setPrefWidth(80);
        softnessSpinner.valueProperty().addListener((obs, old, val) -> {
            if (!updatingUI && selectedConstraint != null) {
                selectedConstraint.setSoftness(val);
            }
        });
        propsGrid.add(new Label("Softness:"), 0, row);
        propsGrid.add(softnessSpinner, 1, row++);
        
        // Iterations (for CCD)
        iterationsSpinner = new Spinner<>(1, 50, 10);
        iterationsSpinner.setEditable(true);
        iterationsSpinner.setPrefWidth(80);
        iterationsSpinner.valueProperty().addListener((obs, old, val) -> {
            if (!updatingUI && selectedConstraint != null) {
                selectedConstraint.setMaxIterations(val);
            }
        });
        propsGrid.add(new Label("Iterations:"), 0, row);
        propsGrid.add(iterationsSpinner, 1, row++);
        
        // Apply button
        Button applyButton = new Button("Apply IK Now");
        applyButton.setOnAction(e -> applySelectedConstraint());
        applyButton.setMaxWidth(Double.MAX_VALUE);
        propsGrid.add(applyButton, 0, row, 2, 1);
        
        // Make column 1 grow
        ColumnConstraints col0 = new ColumnConstraints();
        col0.setHgrow(Priority.NEVER);
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setHgrow(Priority.ALWAYS);
        propsGrid.getColumnConstraints().addAll(col0, col1);
        
        TitledPane propsPane = new TitledPane("Constraint Properties", propsGrid);
        propsPane.setExpanded(true);
        propsPane.setCollapsible(true);
        
        VBox content = new VBox(10, listSection, propsPane);
        setCenter(new ScrollPane(content));
        
        // Initial state
        setPropertiesEnabled(false);
        
        // Listen for skeleton changes
        EditorContext.getInstance().currentSkeletonProperty().addListener((obs, old, skeleton) -> {
            refreshBoneLists();
            refreshConstraintList();
        });
    }
    
    private void refreshBoneLists() {
        startBoneCombo.getItems().clear();
        endBoneCombo.getItems().clear();
        targetBoneCombo.getItems().clear();
        
        Skeleton skeleton = EditorContext.getInstance().getCurrentSkeleton();
        if (skeleton == null) return;
        
        for (Bone bone : skeleton.getBones()) {
            String name = bone.getName();
            startBoneCombo.getItems().add(name);
            endBoneCombo.getItems().add(name);
            targetBoneCombo.getItems().add(name);
        }
    }
    
    /**
     * Set the skeleton to display IK constraints for.
     * This will refresh the constraint list and bone combos.
     */
    public void setSkeleton(Skeleton skeleton) {
        refreshBoneLists();
        refreshConstraintList();
    }
    
    public void refreshConstraintList() {
        constraintEntries.clear();
        
        Skeleton skeleton = EditorContext.getInstance().getCurrentSkeleton();
        if (skeleton == null) return;
        
        IKManager manager = skeleton.getIKManager();
        if (manager == null) return;
        
        for (IKConstraint constraint : manager.getConstraints()) {
            constraintEntries.add(new IKConstraintEntry(constraint));
        }
    }
    
    private void selectConstraint(IKConstraint constraint) {
        selectedConstraint = constraint;
        setPropertiesEnabled(constraint != null);
        
        if (constraint != null) {
            updatingUI = true;
            try {
                nameField.setText(constraint.getName());
                mixSlider.setValue(constraint.getMix());
                bendPositiveCheck.setSelected(constraint.isBendPositive());
                stretchCheck.setSelected(constraint.isStretch());
                compressCheck.setSelected(constraint.isCompress());
                softnessSpinner.getValueFactory().setValue(constraint.getSoftness());
                iterationsSpinner.getValueFactory().setValue(constraint.getMaxIterations());
                
                // Set bone selections
                if (!constraint.getBones().isEmpty()) {
                    startBoneCombo.setValue(constraint.getBones().get(0).getName());
                    endBoneCombo.setValue(constraint.getBones().get(constraint.getBones().size() - 1).getName());
                }
                if (constraint.getTarget() != null) {
                    targetBoneCombo.setValue(constraint.getTarget().getName());
                }
            } finally {
                updatingUI = false;
            }
        }
    }
    
    private void setPropertiesEnabled(boolean enabled) {
        nameField.setDisable(!enabled);
        startBoneCombo.setDisable(!enabled);
        endBoneCombo.setDisable(!enabled);
        targetBoneCombo.setDisable(!enabled);
        mixSlider.setDisable(!enabled);
        bendPositiveCheck.setDisable(!enabled);
        stretchCheck.setDisable(!enabled);
        compressCheck.setDisable(!enabled);
        softnessSpinner.setDisable(!enabled);
        iterationsSpinner.setDisable(!enabled);
    }
    
    private void createNewConstraint() {
        Skeleton skeleton = EditorContext.getInstance().getCurrentSkeleton();
        if (skeleton == null) {
            showAlert("No skeleton loaded");
            return;
        }
        
        IKManager manager = skeleton.getIKManager();
        if (manager == null) {
            showAlert("Skeleton has no IK manager");
            return;
        }
        
        String name = "IK_" + (manager.getConstraintCount() + 1);
        IKConstraint constraint = new IKConstraint(name);
        manager.addConstraint(constraint);
        
        refreshConstraintList();
        
        // Select the new constraint
        for (IKConstraintEntry entry : constraintEntries) {
            if (entry.getConstraint() == constraint) {
                constraintTable.getSelectionModel().select(entry);
                break;
            }
        }
    }
    
    private void removeSelectedConstraint() {
        if (selectedConstraint == null) return;
        
        Skeleton skeleton = EditorContext.getInstance().getCurrentSkeleton();
        if (skeleton == null) return;
        
        IKManager manager = skeleton.getIKManager();
        if (manager == null) return;
        
        manager.removeConstraint(selectedConstraint.getName());
        selectedConstraint = null;
        refreshConstraintList();
        setPropertiesEnabled(false);
    }
    
    private void updateChainFromSelection() {
        if (updatingUI || selectedConstraint == null) return;
        
        Skeleton skeleton = EditorContext.getInstance().getCurrentSkeleton();
        if (skeleton == null) return;
        
        String startName = startBoneCombo.getValue();
        String endName = endBoneCombo.getValue();
        
        if (startName == null || endName == null) return;
        
        Bone startBone = skeleton.findBone(startName);
        Bone endBone = skeleton.findBone(endName);
        
        if (startBone == null || endBone == null) return;
        
        // Build chain from end to start
        List<Bone> chain = buildChain(startBone, endBone);
        if (!chain.isEmpty()) {
            selectedConstraint.getBones().clear();
            for (Bone bone : chain) {
                selectedConstraint.addBone(bone);
            }
            refreshConstraintList();
        }
    }
    
    private List<Bone> buildChain(Bone start, Bone end) {
        List<Bone> chain = new ArrayList<>();
        Bone current = end;
        
        while (current != null) {
            chain.add(0, current);
            if (current == start) {
                return chain;
            }
            current = current.getParent();
        }
        
        // No valid chain found
        return new ArrayList<>();
    }
    
    private void updateTargetFromSelection() {
        if (updatingUI || selectedConstraint == null) return;
        
        Skeleton skeleton = EditorContext.getInstance().getCurrentSkeleton();
        if (skeleton == null) return;
        
        String targetName = targetBoneCombo.getValue();
        if (targetName == null) return;
        
        Bone target = skeleton.findBone(targetName);
        selectedConstraint.setTarget(target);
    }
    
    private void applySelectedConstraint() {
        if (selectedConstraint != null) {
            selectedConstraint.apply();
            EditorContext.getInstance().getMainController().getCanvasPane().repaint();
        }
    }
    
    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING, message, ButtonType.OK);
        alert.showAndWait();
    }
    
    /**
     * Table entry for IK constraints.
     */
    public static class IKConstraintEntry {
        private final IKConstraint constraint;
        private final SimpleStringProperty name;
        private final SimpleStringProperty chain;
        private final SimpleDoubleProperty mix;
        
        public IKConstraintEntry(IKConstraint constraint) {
            this.constraint = constraint;
            this.name = new SimpleStringProperty(constraint.getName());
            
            StringBuilder chainStr = new StringBuilder();
            List<Bone> bones = constraint.getBones();
            if (!bones.isEmpty()) {
                chainStr.append(bones.get(0).getName());
                if (bones.size() > 1) {
                    chainStr.append(" → ");
                    chainStr.append(bones.get(bones.size() - 1).getName());
                }
            }
            this.chain = new SimpleStringProperty(chainStr.toString());
            this.mix = new SimpleDoubleProperty(constraint.getMix());
        }
        
        public IKConstraint getConstraint() { return constraint; }
        public String getName() { return name.get(); }
        public String getChain() { return chain.get(); }
        public double getMix() { return mix.get(); }
    }
}
