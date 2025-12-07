package com.animstudio.editor.ui.assets;

import com.animstudio.core.model.AssetEntry;
import com.animstudio.core.model.AssetLibrary;
import com.animstudio.core.model.Bone;
import com.animstudio.core.model.Slot;
import com.animstudio.core.model.Skeleton;
import com.animstudio.editor.EditorContext;
import com.animstudio.editor.commands.AttachSpriteCommand;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Panel for managing project assets (sprites/images).
 * Allows importing, previewing, and attaching assets to slots.
 */
public class AssetLibraryPane extends BorderPane {
    
    private final AssetLibrary library;
    private final ListView<AssetEntry> assetListView;
    private final ImageView previewImageView;
    private final Label infoLabel;
    private final Map<String, Image> imageCache = new HashMap<>();
    
    private static final int THUMBNAIL_SIZE = 48;
    private static final int PREVIEW_SIZE = 150;
    
    public AssetLibraryPane() {
        this.library = EditorContext.getInstance().getAssetLibrary();
        
        // Toolbar
        ToolBar toolbar = createToolbar();
        setTop(toolbar);
        
        // Asset list
        assetListView = new ListView<>();
        assetListView.setCellFactory(lv -> new AssetListCell());
        assetListView.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldVal, newVal) -> updatePreview(newVal)
        );
        
        // Enable drag from list
        assetListView.setOnDragDetected(event -> {
            AssetEntry selected = assetListView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                Dragboard db = assetListView.startDragAndDrop(TransferMode.COPY);
                ClipboardContent content = new ClipboardContent();
                content.putString(selected.getId());
                db.setContent(content);
                event.consume();
            }
        });
        
        // Preview panel
        VBox previewPanel = new VBox(10);
        previewPanel.setPadding(new Insets(10));
        previewPanel.setAlignment(Pos.TOP_CENTER);
        previewPanel.setStyle("-fx-background-color: #2d2d30;");
        previewPanel.setPrefWidth(180);
        
        previewImageView = new ImageView();
        previewImageView.setFitWidth(PREVIEW_SIZE);
        previewImageView.setFitHeight(PREVIEW_SIZE);
        previewImageView.setPreserveRatio(true);
        
        infoLabel = new Label("No asset selected");
        infoLabel.setWrapText(true);
        infoLabel.setStyle("-fx-text-fill: #cccccc;");
        
        Button attachButton = new Button("Attach to Slot");
        attachButton.setOnAction(e -> attachSelectedAsset());
        attachButton.setMaxWidth(Double.MAX_VALUE);
        
        Button deleteButton = new Button("Delete");
        deleteButton.setOnAction(e -> deleteSelectedAsset());
        deleteButton.setMaxWidth(Double.MAX_VALUE);
        
        previewPanel.getChildren().addAll(previewImageView, infoLabel, attachButton, deleteButton);
        
        // Layout
        setCenter(assetListView);
        setRight(previewPanel);
        
        // Listen for library changes
        library.addListener(new AssetLibrary.AssetLibraryListener() {
            @Override
            public void onAssetAdded(AssetEntry asset) {
                refreshList();
            }
            
            @Override
            public void onAssetRemoved(AssetEntry asset) {
                imageCache.remove(asset.getId());
                refreshList();
            }
        });
        
        refreshList();
    }
    
    private ToolBar createToolbar() {
        Button importBtn = new Button("Import");
        importBtn.setOnAction(e -> importAssets());
        
        Button refreshBtn = new Button("⟳");
        refreshBtn.setTooltip(new Tooltip("Refresh"));
        refreshBtn.setOnAction(e -> refreshList());
        
        TextField searchField = new TextField();
        searchField.setPromptText("Search...");
        searchField.setPrefWidth(120);
        searchField.textProperty().addListener((obs, oldVal, newVal) -> filterList(newVal));
        
        return new ToolBar(importBtn, new Separator(), searchField, refreshBtn);
    }
    
    private void importAssets() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Import Assets");
        chooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp"),
            new FileChooser.ExtensionFilter("All Files", "*.*")
        );
        
        List<File> files = chooser.showOpenMultipleDialog(getScene().getWindow());
        if (files != null) {
            for (File file : files) {
                importFile(file);
            }
        }
    }
    
    private void importFile(File file) {
        try {
            AssetEntry entry = AssetEntry.fromFile(file, null);
            
            // Load image to get dimensions
            Image image = new Image(new FileInputStream(file));
            entry.setWidth((int) image.getWidth());
            entry.setHeight((int) image.getHeight());
            
            // Cache the image
            imageCache.put(entry.getId(), image);
            
            library.addAsset(entry);
            
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Import Error");
            alert.setHeaderText("Failed to import: " + file.getName());
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }
    
    private void refreshList() {
        assetListView.getItems().setAll(library.getAssets());
    }
    
    private void filterList(String filter) {
        if (filter == null || filter.isEmpty()) {
            refreshList();
        } else {
            String lowerFilter = filter.toLowerCase();
            assetListView.getItems().setAll(
                library.getAssets().stream()
                    .filter(a -> a.getName().toLowerCase().contains(lowerFilter))
                    .toList()
            );
        }
    }
    
    private void updatePreview(AssetEntry asset) {
        if (asset == null) {
            previewImageView.setImage(null);
            infoLabel.setText("No asset selected");
            return;
        }
        
        Image image = getOrLoadImage(asset);
        previewImageView.setImage(image);
        
        infoLabel.setText(String.format(
            "%s\n%d × %d px\n%s",
            asset.getName(),
            asset.getWidth(),
            asset.getHeight(),
            asset.getExtension().toUpperCase()
        ));
    }
    
    private Image getOrLoadImage(AssetEntry asset) {
        Image cached = imageCache.get(asset.getId());
        if (cached != null) return cached;
        
        try {
            String path = asset.getAbsolutePath();
            if (path != null) {
                Image image = new Image(new FileInputStream(path));
                imageCache.put(asset.getId(), image);
                return image;
            }
        } catch (Exception e) {
            // Ignore, return null
        }
        return null;
    }
    
    private void attachSelectedAsset() {
        AssetEntry selected = assetListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("No asset selected", "Please select an asset to attach.");
            return;
        }
        
        Skeleton skeleton = EditorContext.getInstance().getCurrentSkeleton();
        if (skeleton == null) {
            showWarning("No skeleton", "Please create or load a skeleton first.");
            return;
        }
        
        Bone bone = EditorContext.getInstance().getSelectedBone();
        if (bone == null) {
            showWarning("No bone selected", "Please select a bone to attach the sprite to.");
            return;
        }
        
        // Find or create slot for this bone
        Slot slot = skeleton.getSlots().stream()
            .filter(s -> s.getBone() == bone)
            .findFirst()
            .orElseGet(() -> {
                Slot newSlot = new Slot(bone.getName() + "_slot", bone);
                skeleton.addSlot(newSlot);
                return newSlot;
            });
        
        // Execute attach command
        AttachSpriteCommand cmd = new AttachSpriteCommand(slot, selected);
        EditorContext.getInstance().getCommandStack().execute(cmd);
        
        // Trigger repaint
        EditorContext.getInstance().setModified(true);
    }
    
    private void deleteSelectedAsset() {
        AssetEntry selected = assetListView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Delete Asset");
            confirm.setHeaderText("Delete '" + selected.getName() + "'?");
            confirm.setContentText("This will remove the asset from the library. Existing attachments will be affected.");
            
            confirm.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    library.removeAsset(selected);
                }
            });
        }
    }
    
    private void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    /**
     * Gets the asset library.
     */
    public AssetLibrary getLibrary() {
        return library;
    }
    
    /**
     * Custom list cell with thumbnail.
     */
    private class AssetListCell extends ListCell<AssetEntry> {
        private final HBox content;
        private final ImageView thumbnail;
        private final Label nameLabel;
        private final Label sizeLabel;
        
        public AssetListCell() {
            content = new HBox(8);
            content.setAlignment(Pos.CENTER_LEFT);
            content.setPadding(new Insets(4));
            
            thumbnail = new ImageView();
            thumbnail.setFitWidth(THUMBNAIL_SIZE);
            thumbnail.setFitHeight(THUMBNAIL_SIZE);
            thumbnail.setPreserveRatio(true);
            
            VBox textBox = new VBox(2);
            nameLabel = new Label();
            nameLabel.setStyle("-fx-font-weight: bold;");
            sizeLabel = new Label();
            sizeLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #888888;");
            textBox.getChildren().addAll(nameLabel, sizeLabel);
            
            content.getChildren().addAll(thumbnail, textBox);
        }
        
        @Override
        protected void updateItem(AssetEntry item, boolean empty) {
            super.updateItem(item, empty);
            
            if (empty || item == null) {
                setGraphic(null);
            } else {
                nameLabel.setText(item.getName());
                sizeLabel.setText(item.getWidth() + " × " + item.getHeight());
                
                Image image = getOrLoadImage(item);
                thumbnail.setImage(image);
                
                setGraphic(content);
            }
        }
    }
}
