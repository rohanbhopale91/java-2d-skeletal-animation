package com.animstudio.editor.ui.automation;

import com.animstudio.automation.AbstractRule;
import com.animstudio.automation.AnimationRule;
import com.animstudio.automation.RuleEngine;
import com.animstudio.automation.rules.*;
import com.animstudio.core.model.Bone;
import com.animstudio.core.model.Skeleton;
import com.animstudio.editor.EditorContext;

import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;

/**
 * UI panel for managing procedural animation rules.
 */
public class RulesEditorPane extends BorderPane {
    
    private final TableView<RuleEntry> ruleTable;
    private final ObservableList<RuleEntry> ruleEntries;
    
    // Rule properties
    private final TextField nameField;
    private final ComboBox<String> typeCombo;
    private final ComboBox<String> targetBoneCombo;
    private final Slider intensitySlider;
    private final CheckBox enabledCheck;
    
    // Rule-specific properties container
    private final VBox rulePropertiesBox;
    
    private AnimationRule selectedRule;
    private boolean updatingUI = false;
    
    public RulesEditorPane() {
        setPadding(new Insets(10));
        setStyle("-fx-background-color: #2d2d30;");
        
        // Rule list table
        ruleEntries = FXCollections.observableArrayList();
        ruleTable = new TableView<>(ruleEntries);
        ruleTable.setPlaceholder(new Label("No rules defined"));
        ruleTable.setPrefHeight(150);
        
        // Columns
        TableColumn<RuleEntry, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(data -> data.getValue().nameProperty());
        nameCol.setPrefWidth(100);
        
        TableColumn<RuleEntry, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(data -> data.getValue().typeProperty());
        typeCol.setPrefWidth(80);
        
        TableColumn<RuleEntry, String> targetCol = new TableColumn<>("Target");
        targetCol.setCellValueFactory(data -> data.getValue().targetProperty());
        targetCol.setPrefWidth(80);
        
        TableColumn<RuleEntry, Boolean> enabledCol = new TableColumn<>("On");
        enabledCol.setCellValueFactory(data -> data.getValue().enabledProperty().asObject());
        enabledCol.setPrefWidth(40);
        enabledCol.setCellFactory(col -> new TableCell<>() {
            private final CheckBox checkBox = new CheckBox();
            {
                checkBox.setOnAction(e -> {
                    RuleEntry entry = getTableRow().getItem();
                    if (entry != null) {
                        entry.getRule().setEnabled(checkBox.isSelected());
                    }
                });
            }
            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    checkBox.setSelected(item);
                    setGraphic(checkBox);
                }
            }
        });
        
        ruleTable.getColumns().addAll(nameCol, typeCol, targetCol, enabledCol);
        
        // Selection handler
        ruleTable.getSelectionModel().selectedItemProperty().addListener((obs, old, entry) -> {
            selectRule(entry != null ? entry.getRule() : null);
        });
        
        // Buttons
        Button addBtn = new Button("Add Rule");
        addBtn.setOnAction(e -> createNewRule());
        
        Button removeBtn = new Button("Remove");
        removeBtn.setOnAction(e -> removeSelectedRule());
        
        HBox buttonBox = new HBox(5, addBtn, removeBtn);
        buttonBox.setPadding(new Insets(5, 0, 10, 0));
        
        // Rule list section
        VBox listSection = new VBox(5);
        listSection.getChildren().addAll(
            new Label("Automation Rules"),
            ruleTable,
            buttonBox
        );
        
        // Common properties
        nameField = new TextField();
        nameField.setPromptText("Rule name");
        nameField.setDisable(true);
        
        typeCombo = new ComboBox<>();
        typeCombo.getItems().addAll("Breathing", "Wiggle", "Blink", "LookAt", "WalkCycle");
        typeCombo.setDisable(true);
        
        targetBoneCombo = new ComboBox<>();
        targetBoneCombo.setDisable(true);
        targetBoneCombo.setOnAction(e -> {
            if (!updatingUI && selectedRule != null && targetBoneCombo.getValue() != null) {
                selectedRule.setTargetBone(targetBoneCombo.getValue());
                refreshRuleList();
            }
        });
        
        intensitySlider = new Slider(0, 1, 1);
        intensitySlider.setShowTickLabels(true);
        intensitySlider.setShowTickMarks(true);
        intensitySlider.setDisable(true);
        intensitySlider.valueProperty().addListener((obs, old, val) -> {
            if (!updatingUI && selectedRule instanceof AbstractRule ar) {
                ar.setIntensity(val.doubleValue());
            }
        });
        
        enabledCheck = new CheckBox("Enabled");
        enabledCheck.setDisable(true);
        enabledCheck.setOnAction(e -> {
            if (!updatingUI && selectedRule != null) {
                selectedRule.setEnabled(enabledCheck.isSelected());
                refreshRuleList();
            }
        });
        
        // Common properties grid
        GridPane commonGrid = new GridPane();
        commonGrid.setHgap(10);
        commonGrid.setVgap(5);
        commonGrid.setPadding(new Insets(5));
        
        commonGrid.add(new Label("Name:"), 0, 0);
        commonGrid.add(nameField, 1, 0);
        commonGrid.add(new Label("Type:"), 0, 1);
        commonGrid.add(typeCombo, 1, 1);
        commonGrid.add(new Label("Target Bone:"), 0, 2);
        commonGrid.add(targetBoneCombo, 1, 2);
        commonGrid.add(new Label("Intensity:"), 0, 3);
        commonGrid.add(intensitySlider, 1, 3);
        commonGrid.add(enabledCheck, 1, 4);
        
        GridPane.setHgrow(nameField, Priority.ALWAYS);
        GridPane.setHgrow(typeCombo, Priority.ALWAYS);
        GridPane.setHgrow(targetBoneCombo, Priority.ALWAYS);
        GridPane.setHgrow(intensitySlider, Priority.ALWAYS);
        
        TitledPane commonPane = new TitledPane("Common Properties", commonGrid);
        commonPane.setCollapsible(true);
        commonPane.setExpanded(true);
        
        // Rule-specific properties (dynamically populated)
        rulePropertiesBox = new VBox(5);
        rulePropertiesBox.setPadding(new Insets(5));
        
        TitledPane specificPane = new TitledPane("Rule Settings", rulePropertiesBox);
        specificPane.setCollapsible(true);
        specificPane.setExpanded(true);
        
        // Engine controls
        CheckBox engineEnabledCheck = new CheckBox("Engine Enabled");
        engineEnabledCheck.setSelected(true);
        engineEnabledCheck.setOnAction(e -> {
            RuleEngine engine = EditorContext.getInstance().getRuleEngine();
            if (engine != null) {
                engine.setEnabled(engineEnabledCheck.isSelected());
            }
        });
        
        Slider globalIntensitySlider = new Slider(0, 1, 1);
        globalIntensitySlider.setShowTickLabels(true);
        globalIntensitySlider.valueProperty().addListener((obs, old, val) -> {
            RuleEngine engine = EditorContext.getInstance().getRuleEngine();
            if (engine != null) {
                engine.setGlobalIntensity(val.doubleValue());
            }
        });
        
        GridPane engineGrid = new GridPane();
        engineGrid.setHgap(10);
        engineGrid.setVgap(5);
        engineGrid.add(engineEnabledCheck, 0, 0, 2, 1);
        engineGrid.add(new Label("Global Intensity:"), 0, 1);
        engineGrid.add(globalIntensitySlider, 1, 1);
        GridPane.setHgrow(globalIntensitySlider, Priority.ALWAYS);
        
        TitledPane enginePane = new TitledPane("Rule Engine", engineGrid);
        enginePane.setCollapsible(true);
        enginePane.setExpanded(false);
        
        // Layout
        VBox contentBox = new VBox(10);
        contentBox.getChildren().addAll(listSection, commonPane, specificPane, enginePane);
        
        ScrollPane scrollPane = new ScrollPane(contentBox);
        scrollPane.setFitToWidth(true);
        setCenter(scrollPane);
        
        // Listen for skeleton changes
        EditorContext.getInstance().currentSkeletonProperty().addListener((obs, old, skeleton) -> {
            refreshBoneList();
            refreshRuleList();
        });
    }
    
    /**
     * Set the skeleton to configure rules for.
     */
    public void setSkeleton(Skeleton skeleton) {
        refreshBoneList();
        refreshRuleList();
    }
    
    private void refreshBoneList() {
        targetBoneCombo.getItems().clear();
        
        Skeleton skeleton = EditorContext.getInstance().getCurrentSkeleton();
        if (skeleton == null) return;
        
        for (Bone bone : skeleton.getBones()) {
            targetBoneCombo.getItems().add(bone.getName());
        }
    }
    
    public void refreshRuleList() {
        ruleEntries.clear();
        
        RuleEngine engine = EditorContext.getInstance().getRuleEngine();
        if (engine == null) return;
        
        for (AnimationRule rule : engine.getRules()) {
            ruleEntries.add(new RuleEntry(rule));
        }
    }
    
    private void selectRule(AnimationRule rule) {
        selectedRule = rule;
        setPropertiesEnabled(rule != null);
        
        if (rule != null) {
            updatingUI = true;
            try {
                nameField.setText(rule.getName());
                targetBoneCombo.setValue(rule.getTargetBone());
                enabledCheck.setSelected(rule.isEnabled());
                
                // Set type
                String type = getRuleType(rule);
                typeCombo.setValue(type);
                
                // Set intensity if AbstractRule
                if (rule instanceof AbstractRule ar) {
                    intensitySlider.setValue(ar.getIntensity());
                }
                
                // Build rule-specific properties
                buildRuleSpecificUI(rule);
            } finally {
                updatingUI = false;
            }
        } else {
            rulePropertiesBox.getChildren().clear();
        }
    }
    
    private String getRuleType(AnimationRule rule) {
        if (rule instanceof BreathingRule) return "Breathing";
        if (rule instanceof WiggleRule) return "Wiggle";
        if (rule instanceof BlinkRule) return "Blink";
        if (rule instanceof LookAtRule) return "LookAt";
        if (rule instanceof WalkCycleRule) return "WalkCycle";
        return "Unknown";
    }
    
    private void buildRuleSpecificUI(AnimationRule rule) {
        rulePropertiesBox.getChildren().clear();
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(5);
        int row = 0;
        
        if (rule instanceof BreathingRule br) {
            // Breath rate
            Spinner<Double> rateSpinner = new Spinner<>(0.1, 2.0, br.getBreathRate(), 0.05);
            rateSpinner.setEditable(true);
            rateSpinner.valueProperty().addListener((obs, old, val) -> br.setBreathRate(val));
            grid.add(new Label("Breath Rate:"), 0, row);
            grid.add(rateSpinner, 1, row++);
            
            // Scale amount
            Spinner<Double> scaleSpinner = new Spinner<>(0.0, 0.2, br.getScaleAmount(), 0.01);
            scaleSpinner.setEditable(true);
            scaleSpinner.valueProperty().addListener((obs, old, val) -> br.setScaleAmount(val));
            grid.add(new Label("Scale Amount:"), 0, row);
            grid.add(scaleSpinner, 1, row++);
            
        } else if (rule instanceof WiggleRule wr) {
            // Frequency
            Spinner<Double> freqSpinner = new Spinner<>(0.1, 10.0, wr.getFrequency(), 0.1);
            freqSpinner.setEditable(true);
            freqSpinner.valueProperty().addListener((obs, old, val) -> wr.setFrequency(val));
            grid.add(new Label("Frequency:"), 0, row);
            grid.add(freqSpinner, 1, row++);
            
            // Rotation Amount
            Spinner<Double> rotSpinner = new Spinner<>(0.0, 45.0, wr.getRotationAmount(), 1.0);
            rotSpinner.setEditable(true);
            rotSpinner.valueProperty().addListener((obs, old, val) -> wr.setRotationAmount(val));
            grid.add(new Label("Rotation Amount:"), 0, row);
            grid.add(rotSpinner, 1, row++);
            
        } else if (rule instanceof BlinkRule blr) {
            // Blink interval
            Spinner<Double> intervalSpinner = new Spinner<>(0.5, 10.0, blr.getBlinkInterval(), 0.5);
            intervalSpinner.setEditable(true);
            intervalSpinner.valueProperty().addListener((obs, old, val) -> blr.setBlinkInterval(val));
            grid.add(new Label("Blink Interval:"), 0, row);
            grid.add(intervalSpinner, 1, row++);
            
            // Blink duration
            Spinner<Double> durSpinner = new Spinner<>(0.05, 0.5, blr.getBlinkDuration(), 0.05);
            durSpinner.setEditable(true);
            durSpinner.valueProperty().addListener((obs, old, val) -> blr.setBlinkDuration(val));
            grid.add(new Label("Blink Duration:"), 0, row);
            grid.add(durSpinner, 1, row++);
            
        } else if (rule instanceof LookAtRule lar) {
            // Follow speed (smoothing)
            Spinner<Double> speedSpinner = new Spinner<>(0.1, 20.0, lar.getSmoothing(), 0.5);
            speedSpinner.setEditable(true);
            speedSpinner.valueProperty().addListener((obs, old, val) -> lar.setSmoothing(val));
            grid.add(new Label("Smoothing:"), 0, row);
            grid.add(speedSpinner, 1, row++);
            
            // Max rotation
            Spinner<Double> maxAngleSpinner = new Spinner<>(0.0, 180.0, lar.getMaxRotation(), 5.0);
            maxAngleSpinner.setEditable(true);
            maxAngleSpinner.valueProperty().addListener((obs, old, val) -> lar.setMaxRotation(val));
            grid.add(new Label("Max Rotation:"), 0, row);
            grid.add(maxAngleSpinner, 1, row++);
        }
        
        rulePropertiesBox.getChildren().add(grid);
    }
    
    private void setPropertiesEnabled(boolean enabled) {
        nameField.setDisable(!enabled);
        typeCombo.setDisable(true); // Type cannot be changed after creation
        targetBoneCombo.setDisable(!enabled);
        intensitySlider.setDisable(!enabled);
        enabledCheck.setDisable(!enabled);
    }
    
    private void createNewRule() {
        // Show dialog to select rule type
        ChoiceDialog<String> dialog = new ChoiceDialog<>("Breathing", 
            "Breathing", "Wiggle", "Blink", "LookAt", "WalkCycle");
        dialog.setTitle("Add Rule");
        dialog.setHeaderText("Select rule type");
        dialog.setContentText("Type:");
        
        dialog.showAndWait().ifPresent(type -> {
            AnimationRule rule = createRuleOfType(type);
            if (rule != null) {
                RuleEngine engine = EditorContext.getInstance().getRuleEngine();
                if (engine != null) {
                    engine.addRule(rule);
                    refreshRuleList();
                    
                    // Select the new rule
                    for (RuleEntry entry : ruleEntries) {
                        if (entry.getRule() == rule) {
                            ruleTable.getSelectionModel().select(entry);
                            break;
                        }
                    }
                }
            }
        });
    }
    
    private AnimationRule createRuleOfType(String type) {
        return switch (type) {
            case "Breathing" -> new BreathingRule();
            case "Wiggle" -> new WiggleRule();
            case "Blink" -> new BlinkRule();
            case "LookAt" -> new LookAtRule();
            case "WalkCycle" -> new WalkCycleRule();
            default -> null;
        };
    }
    
    private void removeSelectedRule() {
        RuleEntry entry = ruleTable.getSelectionModel().getSelectedItem();
        if (entry == null) return;
        
        RuleEngine engine = EditorContext.getInstance().getRuleEngine();
        if (engine != null) {
            engine.removeRule(entry.getRule());
            refreshRuleList();
        }
    }
    
    // === Inner class for table entry ===
    
    public static class RuleEntry {
        private final AnimationRule rule;
        private final SimpleStringProperty name;
        private final SimpleStringProperty type;
        private final SimpleStringProperty target;
        private final SimpleBooleanProperty enabled;
        
        public RuleEntry(AnimationRule rule) {
            this.rule = rule;
            this.name = new SimpleStringProperty(rule.getName());
            this.type = new SimpleStringProperty(getTypeString(rule));
            this.target = new SimpleStringProperty(rule.getTargetBone() != null ? rule.getTargetBone() : "-");
            this.enabled = new SimpleBooleanProperty(rule.isEnabled());
        }
        
        private String getTypeString(AnimationRule rule) {
            if (rule instanceof BreathingRule) return "Breathing";
            if (rule instanceof WiggleRule) return "Wiggle";
            if (rule instanceof BlinkRule) return "Blink";
            if (rule instanceof LookAtRule) return "LookAt";
            if (rule instanceof WalkCycleRule) return "WalkCycle";
            return "Custom";
        }
        
        public AnimationRule getRule() { return rule; }
        public StringProperty nameProperty() { return name; }
        public StringProperty typeProperty() { return type; }
        public StringProperty targetProperty() { return target; }
        public BooleanProperty enabledProperty() { return enabled; }
    }
}
