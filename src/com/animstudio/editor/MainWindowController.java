package com.animstudio.editor;

import com.animstudio.core.animation.AnimationClip;
import com.animstudio.core.animation.BoneKeyframe;
import com.animstudio.core.event.EngineEventBus;
import com.animstudio.core.event.events.SelectionChangedEvent;
import com.animstudio.core.event.events.SkeletonChangedEvent;
import com.animstudio.core.model.Bone;
import com.animstudio.core.model.Skeleton;
import com.animstudio.core.util.TimeUtils;
import com.animstudio.editor.help.HelpDialog;
import com.animstudio.editor.help.ShortcutsDialog;
import com.animstudio.editor.help.AboutDialog;
import com.animstudio.editor.project.AnimationProject;
import com.animstudio.editor.tools.ToolManager;
import com.animstudio.editor.ui.canvas.CanvasPane;
import com.animstudio.editor.ui.export.ExportDialog;
import com.animstudio.editor.ui.hierarchy.BoneTreeView;
import com.animstudio.editor.ui.inspector.InspectorPane;
import com.animstudio.editor.ui.log.LogPanel;
import com.animstudio.editor.ui.timeline.TimelinePane;
import com.animstudio.io.ProjectSerializer;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Controller for the main editor window.
 */
public class MainWindowController implements Initializable {
    
    @FXML private BorderPane rootPane;
    @FXML private MenuBar menuBar;
    @FXML private ToolBar toolBar;
    @FXML private SplitPane mainSplitPane;
    @FXML private SplitPane leftSplitPane;
    @FXML private VBox hierarchyContainer;
    @FXML private BorderPane canvasContainer;
    @FXML private VBox inspectorContainer;
    @FXML private BorderPane timelineContainer;
    @FXML private BorderPane logContainer;
    @FXML private Label statusLabel;
    
    // View menu check items
    @FXML private CheckMenuItem showBonesMenuItem;
    @FXML private CheckMenuItem showGridMenuItem;
    @FXML private CheckMenuItem showAttachmentsMenuItem;
    @FXML private CheckMenuItem onionSkinningMenuItem;
    @FXML private CheckMenuItem showLogPanelMenuItem;
    
    // Toolbar tool buttons
    @FXML private ToggleButton selectToolBtn;
    @FXML private ToggleButton moveToolBtn;
    @FXML private ToggleButton rotateToolBtn;
    @FXML private ToggleButton scaleToolBtn;
    @FXML private Slider zoomSlider;
    @FXML private ToggleGroup toolGroup;
    
    // Custom UI components
    private CanvasPane canvasPane;
    private BoneTreeView boneTreeView;
    private InspectorPane inspectorPane;
    private TimelinePane timelinePane;
    private LogPanel logPanel;
    
    // Animation loop
    private AnimationTimer animationTimer;
    
    // File choosers
    private FileChooser openChooser;
    private FileChooser saveChooser;
    private File currentFile;
    
    // Clipboard
    private Bone clipboardBone;
    
    private EditorContext context;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        context = EditorContext.getInstance();
        
        // Setup file choosers
        setupFileChoosers();
        
        // Create custom components
        setupCanvasPane();
        setupBoneTreeView();
        setupInspectorPane();
        setupTimelinePane();
        setupLogPanel();
        
        // Setup event handlers
        setupEventHandlers();
        
        // Start animation loop
        startAnimationLoop();
        
        updateStatus("Ready");
    }
    
    private void setupCanvasPane() {
        canvasPane = new CanvasPane();
        canvasContainer.setCenter(canvasPane);
    }
    
    private void setupBoneTreeView() {
        boneTreeView = new BoneTreeView();
        hierarchyContainer.getChildren().add(boneTreeView);
        boneTreeView.prefHeightProperty().bind(hierarchyContainer.heightProperty());
        
        // Handle selection changes
        boneTreeView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.getValue() != null) {
                context.select(newVal.getValue());
            }
        });
    }
    
    private void setupInspectorPane() {
        inspectorPane = new InspectorPane();
        inspectorContainer.getChildren().add(inspectorPane);
    }
    
    private void setupTimelinePane() {
        timelinePane = new TimelinePane();
        timelineContainer.setCenter(timelinePane);
    }
    
    private void setupLogPanel() {
        logPanel = new LogPanel();
        if (logContainer != null) {
            logContainer.setCenter(logPanel);
        }
        logPanel.info("AnimStudio initialized", "System");
    }
    
    private void setupFileChoosers() {
        // Open chooser
        openChooser = new FileChooser();
        openChooser.setTitle("Open Project");
        openChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Animation Projects", "*.animproj", "*.json"),
            new FileChooser.ExtensionFilter("All Files", "*.*")
        );
        
        // Save chooser
        saveChooser = new FileChooser();
        saveChooser.setTitle("Save Project");
        saveChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Animation Projects", "*.animproj"),
            new FileChooser.ExtensionFilter("JSON Files", "*.json")
        );
        
        // Setup zoom slider
        if (zoomSlider != null) {
            zoomSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
                canvasPane.setZoom(newVal.doubleValue());
            });
        }
    }
    
    private void setupEventHandlers() {
        EngineEventBus eventBus = context.getEventBus();
        
        // Skeleton loaded
        eventBus.subscribe(SkeletonChangedEvent.class, event -> {
            Platform.runLater(() -> {
                boneTreeView.setSkeleton(event.getSkeleton());
                canvasPane.repaint();
            });
        });
        
        // Selection changed
        eventBus.subscribe(SelectionChangedEvent.class, event -> {
            Platform.runLater(() -> {
                Bone selectedBone = event.getSelectedBone();
                if (selectedBone != null) {
                    boneTreeView.selectBone(selectedBone);
                }
                inspectorPane.setTarget(selectedBone);
                canvasPane.repaint();
            });
        });
    }
    
    private void startAnimationLoop() {
        animationTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                // Update playback
                context.updatePlayback();
                
                // Repaint canvas
                canvasPane.repaint();
            }
        };
        animationTimer.start();
    }
    
    // === Menu Actions ===
    
    @FXML
    private void onNewProject() {
        if (!confirmDiscardChanges()) return;
        context.newProject();
        currentFile = null;
        updateStatus("New project created");
    }
    
    @FXML
    private void onOpen() {
        if (!confirmDiscardChanges()) return;
        
        File file = openChooser.showOpenDialog(rootPane.getScene().getWindow());
        if (file != null) {
            try {
                ProjectSerializer serializer = new ProjectSerializer();
                AnimationProject project = serializer.load(file.toPath());
                context.loadProject(project);
                currentFile = file;
                updateStatus("Opened: " + file.getName());
            } catch (Exception e) {
                showError("Open Error", "Failed to open project: " + e.getMessage());
            }
        }
    }
    
    @FXML
    private void onSave() {
        if (currentFile == null) {
            onSaveAs();
        } else {
            saveToFile(currentFile);
        }
    }
    
    @FXML
    private void onSaveAs() {
        File file = saveChooser.showSaveDialog(rootPane.getScene().getWindow());
        if (file != null) {
            // Add extension if not present
            if (!file.getName().contains(".")) {
                file = new File(file.getAbsolutePath() + ".animproj");
            }
            saveToFile(file);
            currentFile = file;
        }
    }
    
    @FXML
    private void onExport() {
        ExportDialog dialog = new ExportDialog();
        dialog.showAndWait();
    }
    
    private void saveToFile(File file) {
        try {
            ProjectSerializer serializer = new ProjectSerializer();
            AnimationProject project = context.createProject();
            serializer.save(project, file.toPath());
            context.setModified(false);
            updateStatus("Saved: " + file.getName());
        } catch (Exception e) {
            showError("Save Error", "Failed to save project: " + e.getMessage());
        }
    }
    
    private boolean confirmDiscardChanges() {
        if (!context.isModified()) return true;
        
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Unsaved Changes");
        alert.setHeaderText("You have unsaved changes");
        alert.setContentText("Do you want to save before continuing?");
        
        ButtonType saveBtn = new ButtonType("Save");
        ButtonType discardBtn = new ButtonType("Discard");
        ButtonType cancelBtn = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        
        alert.getButtonTypes().setAll(saveBtn, discardBtn, cancelBtn);
        
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent()) {
            if (result.get() == saveBtn) {
                onSave();
                return !context.isModified();
            } else if (result.get() == discardBtn) {
                return true;
            }
        }
        return false;
    }
    
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    @FXML
    private void onExit() {
        if (confirmDiscardChanges()) {
            Platform.exit();
        }
    }
    
    @FXML
    private void onUndo() {
        if (context.getCommandStack().undo()) {
            context.getCurrentSkeleton().updateWorldTransforms();
            canvasPane.repaint();
            updateStatus("Undo: " + context.getCommandStack().getRedoDescription());
        }
    }
    
    @FXML
    private void onRedo() {
        if (context.getCommandStack().redo()) {
            context.getCurrentSkeleton().updateWorldTransforms();
            canvasPane.repaint();
            updateStatus("Redo: " + context.getCommandStack().getUndoDescription());
        }
    }
    
    @FXML
    private void onPlay() {
        context.togglePlayback();
        updateStatus(context.isPlaying() ? "Playing" : "Stopped");
    }
    
    @FXML
    private void onStop() {
        context.playingProperty().set(false);
        context.setCurrentTime(0);
        updateStatus("Stopped");
    }
    
    @FXML
    private void onAddBone() {
        Skeleton skeleton = context.getCurrentSkeleton();
        if (skeleton == null) return;
        
        Bone parent = context.getSelectedBone();
        if (parent == null) parent = skeleton.getRootBone();
        
        String name = "bone_" + (skeleton.getBoneCount() + 1);
        Bone newBone = new Bone(name);
        newBone.setParent(parent);
        newBone.setY(-40);
        newBone.setLength(40);
        newBone.setToSetupPose();
        skeleton.addBone(newBone);
        skeleton.updateWorldTransforms();
        
        boneTreeView.setSkeleton(skeleton);
        context.select(newBone);
        
        updateStatus("Added bone: " + name);
    }
    
    @FXML
    private void onDeleteBone() {
        Bone bone = context.getSelectedBone();
        if (bone == null || bone.getParent() == null) {
            updateStatus("Cannot delete root bone");
            return;
        }
        
        Skeleton skeleton = context.getCurrentSkeleton();
        skeleton.removeBone(bone);
        skeleton.updateWorldTransforms();
        
        boneTreeView.setSkeleton(skeleton);
        context.clearSelection();
        
        updateStatus("Deleted bone: " + bone.getName());
    }
    
    @FXML
    private void onResetPose() {
        Skeleton skeleton = context.getCurrentSkeleton();
        if (skeleton != null) {
            skeleton.resetToSetupPose();
            canvasPane.repaint();
            updateStatus("Reset to setup pose");
        }
    }
    
    @FXML
    private void onSetSetupPose() {
        Skeleton skeleton = context.getCurrentSkeleton();
        if (skeleton != null) {
            for (Bone bone : skeleton.getBones()) {
                bone.setToSetupPose();
            }
            updateStatus("Current pose saved as setup pose");
        }
    }
    
    @FXML
    private void onAbout() {
        new AboutDialog().showAndWait();
    }
    
    // === Edit Menu Actions ===
    
    @FXML
    private void onCut() {
        onCopy();
        onDelete();
    }
    
    @FXML
    private void onCopy() {
        Bone bone = context.getSelectedBone();
        if (bone != null) {
            clipboardBone = bone;
            updateStatus("Copied: " + bone.getName());
        }
    }
    
    @FXML
    private void onPaste() {
        if (clipboardBone == null) {
            updateStatus("Nothing to paste");
            return;
        }
        
        Skeleton skeleton = context.getCurrentSkeleton();
        if (skeleton == null) return;
        
        Bone parent = context.getSelectedBone();
        if (parent == null) parent = skeleton.getRootBone();
        
        // Create copy of bone
        String name = clipboardBone.getName() + "_copy";
        int counter = 1;
        while (skeleton.getBone(name) != null) {
            name = clipboardBone.getName() + "_copy" + counter++;
        }
        
        Bone newBone = new Bone(name);
        newBone.setParent(parent);
        newBone.setX(clipboardBone.getX());
        newBone.setY(clipboardBone.getY());
        newBone.setRotation(clipboardBone.getRotation());
        newBone.setLength(clipboardBone.getLength());
        newBone.setToSetupPose();
        skeleton.addBone(newBone);
        skeleton.updateWorldTransforms();
        
        boneTreeView.setSkeleton(skeleton);
        context.select(newBone);
        updateStatus("Pasted: " + name);
    }
    
    @FXML
    private void onDelete() {
        onDeleteBone();
    }
    
    // === Animation Menu Actions ===
    
    @FXML
    private void onAddKeyframe() {
        Bone bone = context.getSelectedBone();
        AnimationClip clip = context.getCurrentAnimation();
        if (bone == null || clip == null) {
            updateStatus("Select a bone first");
            return;
        }
        
        double time = context.getCurrentTime(); // Time in seconds
        int frame = TimeUtils.secondsToNearestFrame(time);
        
        BoneKeyframe kf = new BoneKeyframe(time);
        kf.setX(bone.getX());
        kf.setY(bone.getY());
        kf.setRotation(bone.getRotation());
        kf.setScaleX(bone.getScaleX());
        kf.setScaleY(bone.getScaleY());
        
        clip.addKeyframe(bone.getName(), kf);
        context.setModified(true);
        
        timelinePane.refresh();
        updateStatus(String.format("Added keyframe at frame %d (%.2fs)", frame, time));
    }
    
    @FXML
    private void onRemoveKeyframe() {
        Bone bone = context.getSelectedBone();
        AnimationClip clip = context.getCurrentAnimation();
        if (bone == null || clip == null) {
            updateStatus("Select a bone first");
            return;
        }
        
        AnimationClip.BoneKeyframes timeline = clip.getTimeline(bone.getName());
        if (timeline == null) {
            updateStatus("No timeline for this bone");
            return;
        }
        
        double time = context.getCurrentTime(); // Time in seconds
        int frame = TimeUtils.secondsToNearestFrame(time);
        
        BoneKeyframe kf = timeline.getKeyframeAt(time);
        if (kf != null) {
            clip.removeKeyframe(bone.getName(), time);
            context.setModified(true);
            timelinePane.refresh();
            updateStatus(String.format("Removed keyframe at frame %d (%.2fs)", frame, time));
        } else {
            updateStatus("No keyframe at this frame");
        }
    }
    
    @FXML
    private void onNewAnimation() {
        TextInputDialog dialog = new TextInputDialog("new_animation");
        dialog.setTitle("New Animation");
        dialog.setHeaderText("Create a new animation");
        dialog.setContentText("Animation name:");
        
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(name -> {
            AnimationClip clip = new AnimationClip(name);
            clip.setDuration(60);
            context.getCurrentProject().addAnimation(clip);
            context.setCurrentAnimation(clip);
            updateStatus("Created animation: " + name);
        });
    }
    
    @FXML
    private void onDuplicateAnimation() {
        AnimationClip current = context.getCurrentAnimation();
        if (current == null) {
            updateStatus("No animation to duplicate");
            return;
        }
        
        String newName = current.getName() + "_copy";
        AnimationClip copy = current.copy();
        copy.setName(newName);
        context.getCurrentProject().addAnimation(copy);
        context.setCurrentAnimation(copy);
        updateStatus("Duplicated animation: " + newName);
    }
    
    @FXML
    private void onDeleteAnimation() {
        AnimationClip current = context.getCurrentAnimation();
        if (current == null) {
            updateStatus("No animation to delete");
            return;
        }
        
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Animation");
        alert.setHeaderText("Delete animation: " + current.getName());
        alert.setContentText("Are you sure?");
        
        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            context.getCurrentProject().removeAnimation(current.getName());
            // Select first remaining animation
            var anims = context.getCurrentProject().getAnimations();
            if (!anims.isEmpty()) {
                context.setCurrentAnimation(anims.iterator().next());
            } else {
                context.setCurrentAnimation(null);
            }
            updateStatus("Deleted animation: " + current.getName());
        }
    }
    
    // === View Menu Actions ===
    
    @FXML
    private void onToggleShowBones() {
        boolean show = showBonesMenuItem.isSelected();
        canvasPane.setShowBones(show);
        canvasPane.repaint();
    }
    
    @FXML
    private void onToggleShowGrid() {
        boolean show = showGridMenuItem.isSelected();
        canvasPane.setShowGrid(show);
        canvasPane.repaint();
    }
    
    @FXML
    private void onToggleShowAttachments() {
        boolean show = showAttachmentsMenuItem.isSelected();
        canvasPane.setShowAttachments(show);
        canvasPane.repaint();
    }
    
    @FXML
    private void onToggleOnionSkinning() {
        boolean enabled = onionSkinningMenuItem.isSelected();
        canvasPane.setOnionSkinning(enabled);
        canvasPane.repaint();
        updateStatus("Onion skinning: " + (enabled ? "ON" : "OFF"));
    }
    
    @FXML
    private void onResetView() {
        canvasPane.resetView();
        if (zoomSlider != null) {
            zoomSlider.setValue(1.0);
        }
        updateStatus("View reset");
    }
    
    @FXML
    private void onZoomToFit() {
        canvasPane.zoomToFit();
        if (zoomSlider != null) {
            zoomSlider.setValue(canvasPane.getZoom());
        }
        updateStatus("Zoom to fit");
    }
    
    @FXML
    private void onToggleLogPanel() {
        boolean show = showLogPanelMenuItem.isSelected();
        if (logContainer != null) {
            logContainer.setVisible(show);
            logContainer.setManaged(show);
        }
        updateStatus("Log panel: " + (show ? "visible" : "hidden"));
    }
    
    // === Help Menu Actions ===
    
    @FXML
    private void onDocumentation() {
        new HelpDialog().showAndWait();
    }
    
    @FXML
    private void onKeyboardShortcuts() {
        new ShortcutsDialog().showAndWait();
    }
    
    // === Tool Selection ===
    
    @FXML
    private void onSelectTool() {
        ToolManager.getInstance().setActiveTool("select");
        updateStatus("Select tool");
    }
    
    @FXML
    private void onMoveTool() {
        ToolManager.getInstance().setActiveTool("move");
        updateStatus("Move tool");
    }
    
    @FXML
    private void onRotateTool() {
        ToolManager.getInstance().setActiveTool("rotate");
        updateStatus("Rotate tool");
    }
    
    @FXML
    private void onScaleTool() {
        ToolManager.getInstance().setActiveTool("scale");
        updateStatus("Scale tool");
    }
    
    private void updateStatus(String message) {
        if (statusLabel != null) {
            statusLabel.setText(message);
        }
    }
    
    /**
     * Public method for setting status text from external components.
     */
    public void setStatusText(String message) {
        updateStatus(message);
    }
    
    public CanvasPane getCanvasPane() { return canvasPane; }
    public BoneTreeView getBoneTreeView() { return boneTreeView; }
    public InspectorPane getInspectorPane() { return inspectorPane; }
    public TimelinePane getTimelinePane() { return timelinePane; }
}
