package com.animstudio.editor.ui.dialogs;

import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

import java.util.function.Consumer;

/**
 * A dialog that shows progress while saving/loading files.
 */
public class ProgressDialog extends Stage {
    
    private final ProgressBar progressBar;
    private final ProgressIndicator spinner;
    private final Label messageLabel;
    private boolean completed = false;
    
    public ProgressDialog(Window owner, String title, String message) {
        initOwner(owner);
        initModality(Modality.APPLICATION_MODAL);
        initStyle(StageStyle.UNDECORATED);
        setTitle(title);
        
        VBox content = new VBox(15);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(30));
        content.setStyle("-fx-background-color: #2d2d2d; -fx-background-radius: 8;");
        content.setMinWidth(300);
        
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: white;");
        
        messageLabel = new Label(message);
        messageLabel.setStyle("-fx-text-fill: #aaa;");
        
        progressBar = new ProgressBar();
        progressBar.setPrefWidth(250);
        progressBar.setProgress(-1); // Indeterminate
        
        spinner = new ProgressIndicator();
        spinner.setPrefSize(32, 32);
        spinner.setProgress(-1);
        
        content.getChildren().addAll(titleLabel, spinner, messageLabel, progressBar);
        
        Scene scene = new Scene(content);
        scene.setFill(null);
        setScene(scene);
        
        // Center on owner
        if (owner != null) {
            setX(owner.getX() + (owner.getWidth() - 300) / 2);
            setY(owner.getY() + (owner.getHeight() - 150) / 2);
        }
    }
    
    /**
     * Update the progress (0.0 to 1.0, or -1 for indeterminate).
     */
    public void setProgress(double progress) {
        progressBar.setProgress(progress);
        if (progress >= 0) {
            spinner.setProgress(progress);
        }
    }
    
    /**
     * Update the message text.
     */
    public void setMessage(String message) {
        messageLabel.setText(message);
    }
    
    /**
     * Mark as completed and close.
     */
    public void complete() {
        completed = true;
        close();
    }
    
    /**
     * Check if the operation completed successfully.
     */
    public boolean isCompleted() {
        return completed;
    }
    
    /**
     * Run a task with progress dialog.
     */
    public static <T> T runWithProgress(Window owner, String title, String message, 
                                        Task<T> task, Consumer<Exception> onError) {
        ProgressDialog dialog = new ProgressDialog(owner, title, message);
        
        // Bind progress
        dialog.progressBar.progressProperty().bind(task.progressProperty());
        dialog.messageLabel.textProperty().bind(task.messageProperty());
        
        task.setOnSucceeded(e -> {
            dialog.complete();
        });
        
        task.setOnFailed(e -> {
            dialog.close();
            if (onError != null && task.getException() != null) {
                onError.accept((Exception) task.getException());
            }
        });
        
        task.setOnCancelled(e -> {
            dialog.close();
        });
        
        // Start task
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
        
        // Show dialog (blocks until closed)
        dialog.showAndWait();
        
        if (task.isDone() && task.getValue() != null) {
            return task.getValue();
        }
        return null;
    }
    
    /**
     * Show a simple indeterminate progress dialog while a runnable executes.
     */
    public static void showWhile(Window owner, String title, String message, Runnable action) {
        ProgressDialog dialog = new ProgressDialog(owner, title, message);
        
        Thread thread = new Thread(() -> {
            try {
                action.run();
            } finally {
                javafx.application.Platform.runLater(dialog::complete);
            }
        });
        thread.setDaemon(true);
        thread.start();
        
        dialog.showAndWait();
    }
}
