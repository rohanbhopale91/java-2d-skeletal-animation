package com.animstudio.editor.ui.dialogs;

import com.animstudio.io.RecentFiles;
import com.animstudio.io.RecentFiles.RecentFileEntry;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Window;

import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Dialog for browsing and opening recent files.
 */
public class RecentFilesDialog extends Dialog<File> {
    
    private final ListView<RecentFileEntry> fileList;
    private final Label pathLabel;
    private final RecentFiles recentFiles;
    
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    
    public RecentFilesDialog(Window owner) {
        setTitle("Recent Projects");
        setHeaderText("Select a recent project to open");
        initModality(Modality.APPLICATION_MODAL);
        initOwner(owner);
        
        recentFiles = RecentFiles.getInstance();
        
        // Create content
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        content.setPrefWidth(500);
        content.setPrefHeight(350);
        
        fileList = new ListView<>();
        fileList.setPrefHeight(250);
        VBox.setVgrow(fileList, Priority.ALWAYS);
        
        // Populate list
        List<RecentFileEntry> entries = recentFiles.getRecentFiles();
        fileList.getItems().addAll(entries);
        
        // Custom cell factory
        fileList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(RecentFileEntry entry, boolean empty) {
                super.updateItem(entry, empty);
                if (empty || entry == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    VBox cell = new VBox(2);
                    Label nameLabel = new Label(entry.getFileName());
                    nameLabel.setStyle("-fx-font-weight: bold;");
                    
                    String dateStr = entry.timestamp.format(DATE_FORMAT);
                    Label dateLabel = new Label("Last opened: " + dateStr);
                    dateLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #888;");
                    
                    cell.getChildren().addAll(nameLabel, dateLabel);
                    setGraphic(cell);
                    
                    // Strikethrough if file doesn't exist
                    if (!entry.exists()) {
                        nameLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #999; -fx-strikethrough: true;");
                    }
                }
            }
        });
        
        // Path display
        pathLabel = new Label("");
        pathLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #666;");
        pathLabel.setWrapText(true);
        
        // Update path on selection
        fileList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                pathLabel.setText(newVal.path);
            } else {
                pathLabel.setText("");
            }
        });
        
        // Double-click to open
        fileList.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                RecentFileEntry selected = fileList.getSelectionModel().getSelectedItem();
                if (selected != null && selected.exists()) {
                    setResult(selected.getFile());
                    close();
                }
            }
        });
        
        // Buttons bar
        HBox buttonBar = new HBox(10);
        Button clearButton = new Button("Clear All");
        clearButton.setOnAction(e -> {
            recentFiles.clear();
            fileList.getItems().clear();
        });
        
        Button removeButton = new Button("Remove Selected");
        removeButton.setOnAction(e -> {
            RecentFileEntry selected = fileList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                recentFiles.removeFile(selected.getFile());
                fileList.getItems().remove(selected);
            }
        });
        removeButton.disableProperty().bind(fileList.getSelectionModel().selectedItemProperty().isNull());
        
        buttonBar.getChildren().addAll(clearButton, removeButton);
        
        content.getChildren().addAll(fileList, pathLabel, buttonBar);
        getDialogPane().setContent(content);
        
        // Dialog buttons
        ButtonType openButton = new ButtonType("Open", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        getDialogPane().getButtonTypes().addAll(openButton, cancelButton);
        
        // Disable open if no valid selection
        getDialogPane().lookupButton(openButton).disableProperty().bind(
            fileList.getSelectionModel().selectedItemProperty().isNull()
        );
        
        // Select first item
        if (!entries.isEmpty()) {
            fileList.getSelectionModel().selectFirst();
        }
        
        // Result converter
        setResultConverter(buttonType -> {
            if (buttonType == openButton) {
                RecentFileEntry selected = fileList.getSelectionModel().getSelectedItem();
                if (selected != null && selected.exists()) {
                    return selected.getFile();
                }
            }
            return null;
        });
    }
    
    /**
     * Show the dialog and return the selected file.
     */
    public static Optional<File> show(Window owner) {
        RecentFilesDialog dialog = new RecentFilesDialog(owner);
        return dialog.showAndWait();
    }
}
