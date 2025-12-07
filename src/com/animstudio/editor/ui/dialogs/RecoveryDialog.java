package com.animstudio.editor.ui.dialogs;

import com.animstudio.io.AutosaveManager;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Window;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;

/**
 * Dialog for recovering projects from autosave files.
 * Shown on startup when autosave files are detected.
 */
public class RecoveryDialog extends Dialog<File> {
    
    private final ListView<File> autosaveList;
    private final AutosaveManager autosaveManager;
    
    public RecoveryDialog(Window owner) {
        setTitle("Recover Project");
        setHeaderText("Unsaved work was found from a previous session.\nWould you like to recover it?");
        initModality(Modality.APPLICATION_MODAL);
        initOwner(owner);
        
        autosaveManager = AutosaveManager.getInstance();
        
        // Create content
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        
        Label infoLabel = new Label("Select an autosave file to recover:");
        
        autosaveList = new ListView<>();
        autosaveList.setPrefHeight(200);
        autosaveList.setPrefWidth(400);
        
        // Populate list
        File[] autosaves = autosaveManager.getAutosaveFiles();
        autosaveList.getItems().addAll(autosaves);
        
        // Custom cell factory for better display
        autosaveList.setCellFactory(lv -> new ListCell<>() {
            private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            
            @Override
            protected void updateItem(File file, boolean empty) {
                super.updateItem(file, empty);
                if (empty || file == null) {
                    setText(null);
                } else {
                    String name = file.getName();
                    Date modified = new Date(file.lastModified());
                    long sizeKb = file.length() / 1024;
                    setText(String.format("%s (%s, %d KB)", name, dateFormat.format(modified), sizeKb));
                }
            }
        });
        
        // Select first item
        if (!autosaves.equals(null) && autosaves.length > 0) {
            autosaveList.getSelectionModel().selectFirst();
        }
        
        Label warningLabel = new Label("⚠ Choosing 'Discard All' will permanently delete all autosave files.");
        warningLabel.setStyle("-fx-text-fill: #cc7700;");
        
        content.getChildren().addAll(infoLabel, autosaveList, warningLabel);
        getDialogPane().setContent(content);
        
        // Buttons
        ButtonType recoverButton = new ButtonType("Recover", ButtonBar.ButtonData.OK_DONE);
        ButtonType discardButton = new ButtonType("Discard All", ButtonBar.ButtonData.NO);
        ButtonType skipButton = new ButtonType("Skip", ButtonBar.ButtonData.CANCEL_CLOSE);
        
        getDialogPane().getButtonTypes().addAll(recoverButton, discardButton, skipButton);
        
        // Disable recover button if no selection
        getDialogPane().lookupButton(recoverButton).disableProperty().bind(
            autosaveList.getSelectionModel().selectedItemProperty().isNull()
        );
        
        // Result converter
        setResultConverter(buttonType -> {
            if (buttonType == recoverButton) {
                return autosaveList.getSelectionModel().getSelectedItem();
            } else if (buttonType == discardButton) {
                // Clear all autosaves
                autosaveManager.clearAutosaves();
            }
            return null;
        });
    }
    
    /**
     * Show recovery dialog if there are autosaves available.
     * @return The selected file to recover, or null if user chose to skip/discard
     */
    public static Optional<File> showIfNeeded(Window owner) {
        AutosaveManager manager = AutosaveManager.getInstance();
        if (!manager.hasAutosaves()) {
            return Optional.empty();
        }
        
        RecoveryDialog dialog = new RecoveryDialog(owner);
        return dialog.showAndWait();
    }
}
