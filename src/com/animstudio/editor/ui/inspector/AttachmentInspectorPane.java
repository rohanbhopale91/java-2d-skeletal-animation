package com.animstudio.editor.ui.inspector;

import com.animstudio.core.model.AssetEntry;
import com.animstudio.core.model.Bone;
import com.animstudio.core.model.RegionAttachment;
import com.animstudio.core.model.Skeleton;
import com.animstudio.core.model.Slot;
import com.animstudio.editor.EditorContext;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.io.FileInputStream;

/**
 * Inspector panel for editing slot and attachment properties.
 * Allows viewing/editing draw order, attachment offsets, and visibility.
 */
public class AttachmentInspectorPane extends VBox {
    
    private Slot targetSlot;
    private boolean updating = false;
    
    // Slot properties
    private final Label slotNameLabel;
    private final Spinner<Integer> drawOrderSpinner;
    private final Slider alphaSlider;
    private final Label alphaValueLabel;
    private final CheckBox visibleCheckBox;
    
    // Attachment properties
    private final TitledPane attachmentPane;
    private final Label attachmentNameLabel;
    private final ImageView attachmentPreview;
    private final Spinner<Double> offsetXSpinner;
    private final Spinner<Double> offsetYSpinner;
    private final Spinner<Double> rotationSpinner;
    private final Spinner<Double> scaleXSpinner;
    private final Spinner<Double> scaleYSpinner;
    private final Spinner<Double> pivotXSpinner;
    private final Spinner<Double> pivotYSpinner;
    
    private final Button detachButton;
    
    public AttachmentInspectorPane() {
        setSpacing(10);
        setPadding(new Insets(10));
        
        // Title
        Label title = new Label("Slot / Attachment");
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        
        slotNameLabel = new Label("No slot selected");
        
        // Slot properties section
        TitledPane slotPane = new TitledPane();
        slotPane.setText("Slot Properties");
        slotPane.setCollapsible(true);
        slotPane.setExpanded(true);
        
        GridPane slotGrid = new GridPane();
        slotGrid.setHgap(10);
        slotGrid.setVgap(5);
        slotGrid.setPadding(new Insets(5));
        
        // Draw order
        drawOrderSpinner = new Spinner<>(new SpinnerValueFactory.IntegerSpinnerValueFactory(-1000, 1000, 0, 1));
        drawOrderSpinner.setEditable(true);
        drawOrderSpinner.setPrefWidth(80);
        drawOrderSpinner.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (!updating && targetSlot != null) {
                targetSlot.setDrawOrder(newVal);
                repaintCanvas();
            }
        });
        
        slotGrid.add(new Label("Draw Order:"), 0, 0);
        slotGrid.add(drawOrderSpinner, 1, 0);
        
        // Alpha/opacity
        alphaSlider = new Slider(0, 1, 1);
        alphaSlider.setPrefWidth(100);
        alphaValueLabel = new Label("100%");
        alphaSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (!updating && targetSlot != null) {
                targetSlot.setAlpha(newVal.floatValue());
                alphaValueLabel.setText(String.format("%.0f%%", newVal.doubleValue() * 100));
                repaintCanvas();
            }
        });
        
        HBox alphaBox = new HBox(5, alphaSlider, alphaValueLabel);
        slotGrid.add(new Label("Opacity:"), 0, 1);
        slotGrid.add(alphaBox, 1, 1);
        
        // Visibility toggle
        visibleCheckBox = new CheckBox("Visible");
        visibleCheckBox.setSelected(true);
        visibleCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (!updating && targetSlot != null) {
                targetSlot.setVisible(newVal);
                repaintCanvas();
            }
        });
        slotGrid.add(visibleCheckBox, 0, 2, 2, 1);
        
        slotPane.setContent(slotGrid);
        
        // Attachment section
        attachmentPane = new TitledPane();
        attachmentPane.setText("Attachment");
        attachmentPane.setCollapsible(true);
        attachmentPane.setExpanded(true);
        
        VBox attachmentContent = new VBox(10);
        attachmentContent.setPadding(new Insets(5));
        
        // Preview
        attachmentPreview = new ImageView();
        attachmentPreview.setFitWidth(80);
        attachmentPreview.setFitHeight(80);
        attachmentPreview.setPreserveRatio(true);
        attachmentPreview.setStyle("-fx-background-color: #333;");
        
        attachmentNameLabel = new Label("No attachment");
        attachmentNameLabel.setStyle("-fx-font-style: italic; -fx-text-fill: #888;");
        
        HBox previewBox = new HBox(10, attachmentPreview, attachmentNameLabel);
        
        // Attachment transform
        GridPane attachGrid = new GridPane();
        attachGrid.setHgap(10);
        attachGrid.setVgap(5);
        
        offsetXSpinner = createDoubleSpinner(-1000, 1000, 0, 1);
        offsetYSpinner = createDoubleSpinner(-1000, 1000, 0, 1);
        rotationSpinner = createDoubleSpinner(-360, 360, 0, 1);
        scaleXSpinner = createDoubleSpinner(0.01, 10, 1, 0.1);
        scaleYSpinner = createDoubleSpinner(0.01, 10, 1, 0.1);
        pivotXSpinner = createDoubleSpinner(0, 1, 0.5, 0.1);
        pivotYSpinner = createDoubleSpinner(0, 1, 0.5, 0.1);
        
        attachGrid.add(new Label("Offset X:"), 0, 0);
        attachGrid.add(offsetXSpinner, 1, 0);
        attachGrid.add(new Label("Offset Y:"), 0, 1);
        attachGrid.add(offsetYSpinner, 1, 1);
        attachGrid.add(new Label("Rotation:"), 0, 2);
        attachGrid.add(rotationSpinner, 1, 2);
        attachGrid.add(new Label("Scale X:"), 0, 3);
        attachGrid.add(scaleXSpinner, 1, 3);
        attachGrid.add(new Label("Scale Y:"), 0, 4);
        attachGrid.add(scaleYSpinner, 1, 4);
        attachGrid.add(new Label("Pivot X:"), 0, 5);
        attachGrid.add(pivotXSpinner, 1, 5);
        attachGrid.add(new Label("Pivot Y:"), 0, 6);
        attachGrid.add(pivotYSpinner, 1, 6);
        
        // Detach button
        detachButton = new Button("Detach");
        detachButton.setMaxWidth(Double.MAX_VALUE);
        detachButton.setOnAction(e -> detachAttachment());
        
        attachmentContent.getChildren().addAll(previewBox, attachGrid, detachButton);
        attachmentPane.setContent(attachmentContent);
        
        // Setup spinner listeners
        setupAttachmentListeners();
        
        // Add all sections
        getChildren().addAll(title, slotNameLabel, new Separator(), slotPane, attachmentPane);
        
        // Initial state
        setTargetSlot(null);
    }
    
    private Spinner<Double> createDoubleSpinner(double min, double max, double initial, double step) {
        SpinnerValueFactory<Double> factory = new SpinnerValueFactory.DoubleSpinnerValueFactory(min, max, initial, step);
        Spinner<Double> spinner = new Spinner<>(factory);
        spinner.setEditable(true);
        spinner.setPrefWidth(80);
        
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
    
    private void setupAttachmentListeners() {
        offsetXSpinner.valueProperty().addListener((obs, oldVal, newVal) -> applyAttachmentChanges());
        offsetYSpinner.valueProperty().addListener((obs, oldVal, newVal) -> applyAttachmentChanges());
        rotationSpinner.valueProperty().addListener((obs, oldVal, newVal) -> applyAttachmentChanges());
        scaleXSpinner.valueProperty().addListener((obs, oldVal, newVal) -> applyAttachmentChanges());
        scaleYSpinner.valueProperty().addListener((obs, oldVal, newVal) -> applyAttachmentChanges());
        pivotXSpinner.valueProperty().addListener((obs, oldVal, newVal) -> applyAttachmentChanges());
        pivotYSpinner.valueProperty().addListener((obs, oldVal, newVal) -> applyAttachmentChanges());
    }
    
    private void applyAttachmentChanges() {
        if (updating || targetSlot == null || targetSlot.getAttachment() == null) return;
        if (!(targetSlot.getAttachment() instanceof RegionAttachment)) return;
        
        RegionAttachment attachment = (RegionAttachment) targetSlot.getAttachment();
        attachment.setX(offsetXSpinner.getValue());
        attachment.setY(offsetYSpinner.getValue());
        attachment.setRotation(rotationSpinner.getValue());
        attachment.setScaleX(scaleXSpinner.getValue());
        attachment.setScaleY(scaleYSpinner.getValue());
        attachment.setPivotX(pivotXSpinner.getValue());
        attachment.setPivotY(pivotYSpinner.getValue());
        
        repaintCanvas();
    }
    
    /**
     * Set the slot to inspect.
     */
    public void setTargetSlot(Slot slot) {
        this.targetSlot = slot;
        updating = true;
        
        if (slot == null) {
            slotNameLabel.setText("No slot selected");
            disableSlotControls(true);
            disableAttachmentControls(true);
            attachmentPreview.setImage(null);
            attachmentNameLabel.setText("No attachment");
        } else {
            slotNameLabel.setText("Slot: " + slot.getName());
            disableSlotControls(false);
            
            // Update slot properties
            drawOrderSpinner.getValueFactory().setValue(slot.getDrawOrder());
            alphaSlider.setValue(slot.getAlpha());
            alphaValueLabel.setText(String.format("%.0f%%", slot.getAlpha() * 100));
            visibleCheckBox.setSelected(slot.isVisible());
            
            // Update attachment properties
            if (slot.getAttachment() != null && slot.getAttachment() instanceof RegionAttachment) {
                RegionAttachment attachment = (RegionAttachment) slot.getAttachment();
                disableAttachmentControls(false);
                
                attachmentNameLabel.setText(attachment.getName());
                
                // Load preview image
                loadAttachmentPreview(attachment);
                
                // Update transform spinners
                offsetXSpinner.getValueFactory().setValue(attachment.getX());
                offsetYSpinner.getValueFactory().setValue(attachment.getY());
                rotationSpinner.getValueFactory().setValue(attachment.getRotation());
                scaleXSpinner.getValueFactory().setValue(attachment.getScaleX());
                scaleYSpinner.getValueFactory().setValue(attachment.getScaleY());
                pivotXSpinner.getValueFactory().setValue(attachment.getPivotX());
                pivotYSpinner.getValueFactory().setValue(attachment.getPivotY());
            } else {
                disableAttachmentControls(true);
                attachmentPreview.setImage(null);
                attachmentNameLabel.setText("No attachment");
            }
        }
        
        updating = false;
    }
    
    /**
     * Set target slot from a bone by finding/creating its slot.
     */
    public void setTargetBone(Bone bone) {
        if (bone == null) {
            setTargetSlot(null);
            return;
        }
        
        Skeleton skeleton = EditorContext.getInstance().getCurrentSkeleton();
        if (skeleton == null) {
            setTargetSlot(null);
            return;
        }
        
        // Find slot for this bone
        Slot slot = skeleton.findSlotForBone(bone);
        if (slot == null) {
            // Create a new slot for this bone
            String slotName = bone.getName() + "_slot";
            slot = new Slot(slotName, bone);
            skeleton.addSlot(slot);
        }
        
        setTargetSlot(slot);
    }
    
    private void loadAttachmentPreview(RegionAttachment attachment) {
        String imagePath = attachment.getImagePath();
        if (imagePath != null && !imagePath.isEmpty()) {
            try {
                Image image = new Image(new FileInputStream(imagePath), 80, 80, true, true);
                attachmentPreview.setImage(image);
            } catch (Exception e) {
                attachmentPreview.setImage(null);
            }
        } else {
            attachmentPreview.setImage(null);
        }
    }
    
    private void detachAttachment() {
        if (targetSlot != null && targetSlot.getAttachment() != null) {
            targetSlot.setAttachment(null);
            setTargetSlot(targetSlot); // Refresh UI
            repaintCanvas();
        }
    }
    
    private void disableSlotControls(boolean disabled) {
        drawOrderSpinner.setDisable(disabled);
        alphaSlider.setDisable(disabled);
        visibleCheckBox.setDisable(disabled);
    }
    
    private void disableAttachmentControls(boolean disabled) {
        offsetXSpinner.setDisable(disabled);
        offsetYSpinner.setDisable(disabled);
        rotationSpinner.setDisable(disabled);
        scaleXSpinner.setDisable(disabled);
        scaleYSpinner.setDisable(disabled);
        pivotXSpinner.setDisable(disabled);
        pivotYSpinner.setDisable(disabled);
        detachButton.setDisable(disabled);
    }
    
    private void repaintCanvas() {
        if (EditorContext.getInstance().getMainController() != null) {
            EditorContext.getInstance().getMainController().getCanvasPane().repaint();
        }
    }
    
    /**
     * Refresh the inspector to reflect current slot state.
     */
    public void refresh() {
        setTargetSlot(targetSlot);
    }
    
    public Slot getTargetSlot() {
        return targetSlot;
    }
}
