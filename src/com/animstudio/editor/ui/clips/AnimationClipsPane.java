package com.animstudio.editor.ui.clips;

import com.animstudio.core.animation.AnimationClip;
import com.animstudio.editor.EditorContext;
import com.animstudio.editor.commands.CreateClipCommand;
import com.animstudio.editor.commands.DeleteClipCommand;
import com.animstudio.editor.commands.DuplicateClipCommand;
import com.animstudio.editor.commands.RenameClipCommand;
import com.animstudio.editor.project.Project;

import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;

import java.util.Optional;

/**
 * Panel for managing animation clips (create, rename, duplicate, delete).
 * Includes playback controls like loop mode and speed.
 */
public class AnimationClipsPane extends VBox {
    
    private final EditorContext context;
    private final ListView<AnimationClip> clipListView;
    private final CheckBox loopCheckBox;
    private final Slider speedSlider;
    private final Label speedLabel;
    private final DoubleProperty playbackSpeed = new SimpleDoubleProperty(1.0);
    
    // Buttons
    private final Button createButton;
    private final Button duplicateButton;
    private final Button renameButton;
    private final Button deleteButton;
    
    public AnimationClipsPane(EditorContext context) {
        this.context = context;
        
        setSpacing(8);
        setPadding(new Insets(8));
        getStyleClass().add("clips-pane");
        
        // Title
        Label titleLabel = new Label("Animation Clips");
        titleLabel.getStyleClass().add("pane-title");
        
        // Create list view
        clipListView = new ListView<>();
        clipListView.setPlaceholder(new Label("No animations\nClick '+' to create"));
        clipListView.setCellFactory(lv -> new ClipListCell());
        clipListView.setPrefHeight(200);
        VBox.setVgrow(clipListView, Priority.ALWAYS);
        
        // Selection handler
        clipListView.getSelectionModel().selectedItemProperty().addListener((obs, oldClip, newClip) -> {
            if (newClip != null) {
                context.setCurrentAnimation(newClip);
            }
            updateButtonStates();
        });
        
        // Double-click to rename
        clipListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && event.getButton() == MouseButton.PRIMARY) {
                AnimationClip selected = clipListView.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    showRenameDialog(selected);
                }
            }
        });
        
        // Delete key
        clipListView.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.DELETE) {
                AnimationClip selected = clipListView.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    deleteClip(selected);
                }
            }
        });
        
        // Toolbar buttons
        createButton = new Button("+");
        createButton.setTooltip(new Tooltip("Create New Animation (Ctrl+Shift+N)"));
        createButton.setOnAction(e -> createNewClip());
        
        duplicateButton = new Button("⧉");
        duplicateButton.setTooltip(new Tooltip("Duplicate Selected Animation"));
        duplicateButton.setOnAction(e -> duplicateSelectedClip());
        
        renameButton = new Button("✎");
        renameButton.setTooltip(new Tooltip("Rename Selected Animation (F2)"));
        renameButton.setOnAction(e -> {
            AnimationClip selected = clipListView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                showRenameDialog(selected);
            }
        });
        
        deleteButton = new Button("−");
        deleteButton.setTooltip(new Tooltip("Delete Selected Animation (Delete)"));
        deleteButton.setOnAction(e -> {
            AnimationClip selected = clipListView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                deleteClip(selected);
            }
        });
        
        HBox toolbar = new HBox(4, createButton, duplicateButton, renameButton, deleteButton);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        
        // Loop checkbox
        loopCheckBox = new CheckBox("Loop");
        loopCheckBox.setSelected(true);
        loopCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            AnimationClip clip = clipListView.getSelectionModel().getSelectedItem();
            if (clip != null) {
                clip.setLooping(newVal);
            }
        });
        
        // Speed slider
        Label speedTitleLabel = new Label("Speed:");
        speedSlider = new Slider(0.1, 3.0, 1.0);
        speedSlider.setMajorTickUnit(0.5);
        speedSlider.setShowTickLabels(true);
        speedSlider.setPrefWidth(120);
        speedSlider.valueProperty().bindBidirectional(playbackSpeed);
        
        speedLabel = new Label("1.00x");
        speedLabel.setMinWidth(45);
        playbackSpeed.addListener((obs, oldVal, newVal) -> {
            speedLabel.setText(String.format("%.2fx", newVal.doubleValue()));
        });
        
        // Reset speed button
        Button resetSpeedButton = new Button("⟲");
        resetSpeedButton.setTooltip(new Tooltip("Reset Speed to 1.0x"));
        resetSpeedButton.setOnAction(e -> playbackSpeed.set(1.0));
        
        HBox speedBox = new HBox(4, speedTitleLabel, speedSlider, speedLabel, resetSpeedButton);
        speedBox.setAlignment(Pos.CENTER_LEFT);
        
        // Playback controls section
        VBox playbackBox = new VBox(4, loopCheckBox, speedBox);
        playbackBox.getStyleClass().add("playback-controls");
        
        Separator separator = new Separator();
        
        getChildren().addAll(
            titleLabel,
            toolbar,
            clipListView,
            separator,
            playbackBox
        );
        
        // Initial state
        updateButtonStates();
        
        // Listen for animation changes
        context.currentAnimationProperty().addListener((obs, oldAnim, newAnim) -> {
            if (newAnim != null && !newAnim.equals(clipListView.getSelectionModel().getSelectedItem())) {
                clipListView.getSelectionModel().select(newAnim);
            }
            if (newAnim != null) {
                loopCheckBox.setSelected(newAnim.isLooping());
            }
        });
        
        // Initial refresh
        Platform.runLater(this::refreshClipList);
    }
    
    /**
     * Refresh the clip list from the current project.
     */
    public void refreshClipList() {
        clipListView.getItems().clear();
        
        Project project = context.getCurrentProject();
        if (project != null) {
            clipListView.getItems().addAll(project.getAnimations());
            
            // Select current animation if any
            AnimationClip current = context.getCurrentAnimation();
            if (current != null && clipListView.getItems().contains(current)) {
                clipListView.getSelectionModel().select(current);
            } else if (!clipListView.getItems().isEmpty()) {
                clipListView.getSelectionModel().selectFirst();
            }
        }
        
        updateButtonStates();
    }
    
    /**
     * Create a new animation clip.
     */
    private void createNewClip() {
        Project project = context.getCurrentProject();
        if (project == null) {
            showAlert("No Project", "Please create or open a project first.");
            return;
        }
        
        TextInputDialog dialog = new TextInputDialog("new_animation");
        dialog.setTitle("Create Animation");
        dialog.setHeaderText("Enter animation name:");
        dialog.setContentText("Name:");
        
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(name -> {
            if (name.trim().isEmpty()) {
                showAlert("Invalid Name", "Animation name cannot be empty.");
                return;
            }
            
            if (project.getAnimation(name.trim()) != null) {
                showAlert("Duplicate Name", "An animation with this name already exists.");
                return;
            }
            
            CreateClipCommand cmd = new CreateClipCommand(project, name.trim());
            context.getCommandStack().execute(cmd);
            
            refreshClipList();
            clipListView.getSelectionModel().select(cmd.getCreatedClip());
        });
    }
    
    /**
     * Duplicate the selected clip.
     */
    private void duplicateSelectedClip() {
        Project project = context.getCurrentProject();
        AnimationClip selected = clipListView.getSelectionModel().getSelectedItem();
        
        if (project == null || selected == null) {
            return;
        }
        
        DuplicateClipCommand cmd = new DuplicateClipCommand(project, selected);
        context.getCommandStack().execute(cmd);
        
        refreshClipList();
        clipListView.getSelectionModel().select(cmd.getDuplicatedClip());
    }
    
    /**
     * Show rename dialog for a clip.
     */
    private void showRenameDialog(AnimationClip clip) {
        Project project = context.getCurrentProject();
        if (project == null) return;
        
        TextInputDialog dialog = new TextInputDialog(clip.getName());
        dialog.setTitle("Rename Animation");
        dialog.setHeaderText("Enter new name for '" + clip.getName() + "':");
        dialog.setContentText("Name:");
        
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(newName -> {
            if (newName.trim().isEmpty()) {
                showAlert("Invalid Name", "Animation name cannot be empty.");
                return;
            }
            
            if (newName.equals(clip.getName())) {
                return; // No change
            }
            
            if (project.getAnimation(newName.trim()) != null) {
                showAlert("Duplicate Name", "An animation with this name already exists.");
                return;
            }
            
            RenameClipCommand cmd = new RenameClipCommand(project, clip, newName.trim());
            context.getCommandStack().execute(cmd);
            
            refreshClipList();
        });
    }
    
    /**
     * Delete a clip with confirmation.
     */
    private void deleteClip(AnimationClip clip) {
        Project project = context.getCurrentProject();
        if (project == null) return;
        
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Animation");
        confirm.setHeaderText("Delete '" + clip.getName() + "'?");
        confirm.setContentText("This action can be undone with Ctrl+Z.");
        
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            DeleteClipCommand cmd = new DeleteClipCommand(project, clip);
            context.getCommandStack().execute(cmd);
            
            refreshClipList();
        }
    }
    
    /**
     * Update button enabled states based on selection.
     */
    private void updateButtonStates() {
        boolean hasProject = context.getCurrentProject() != null;
        boolean hasSelection = clipListView.getSelectionModel().getSelectedItem() != null;
        
        createButton.setDisable(!hasProject);
        duplicateButton.setDisable(!hasSelection);
        renameButton.setDisable(!hasSelection);
        deleteButton.setDisable(!hasSelection);
        loopCheckBox.setDisable(!hasSelection);
        speedSlider.setDisable(!hasSelection);
    }
    
    /**
     * Show an alert dialog.
     */
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    /**
     * Get the playback speed property for binding.
     */
    public DoubleProperty playbackSpeedProperty() {
        return playbackSpeed;
    }
    
    /**
     * Custom list cell for animation clips.
     */
    private class ClipListCell extends ListCell<AnimationClip> {
        @Override
        protected void updateItem(AnimationClip clip, boolean empty) {
            super.updateItem(clip, empty);
            
            if (empty || clip == null) {
                setText(null);
                setGraphic(null);
            } else {
                // Show name and duration
                double durationFrames = clip.getDuration() * context.getFrameRate();
                String text = String.format("%s (%.0f frames)", clip.getName(), durationFrames);
                setText(text);
                
                // Show loop indicator
                if (clip.isLooping()) {
                    setStyle("-fx-font-weight: normal;");
                } else {
                    setStyle("-fx-font-style: italic;");
                }
            }
        }
    }
}
