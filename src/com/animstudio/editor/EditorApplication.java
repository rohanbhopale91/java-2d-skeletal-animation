package com.animstudio.editor;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

/**
 * Main entry point for the AnimStudio editor application.
 */
public class EditorApplication extends Application {
    
    private static EditorApplication instance;
    private Stage primaryStage;
    
    public static void main(String[] args) {
        launch(args);
    }
    
    @Override
    public void init() throws Exception {
        instance = this;
        // Initialize editor context
        EditorContext.getInstance();
    }
    
    @Override
    public void start(Stage primaryStage) throws Exception {
        this.primaryStage = primaryStage;
        
        // Load the main window FXML
        FXMLLoader loader = new FXMLLoader(getClass().getResource("MainWindow.fxml"));
        Parent root = loader.load();
        
        // Store controller reference
        MainWindowController controller = loader.getController();
        EditorContext.getInstance().setMainController(controller);
        
        // Setup scene
        Scene scene = new Scene(root, 1400, 900);
        scene.getStylesheets().add(getClass().getResource("editor.css").toExternalForm());
        
        // Configure stage
        primaryStage.setTitle("AnimStudio - 2D Skeletal Animation Editor");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(1000);
        primaryStage.setMinHeight(700);
        
        // Handle close request
        primaryStage.setOnCloseRequest(event -> {
            if (!EditorContext.getInstance().confirmClose()) {
                event.consume();
            }
        });
        
        primaryStage.show();
        
        // Initialize with empty project
        EditorContext.getInstance().newProject();
    }
    
    @Override
    public void stop() throws Exception {
        EditorContext.getInstance().shutdown();
    }
    
    public static EditorApplication getInstance() {
        return instance;
    }
    
    public Stage getPrimaryStage() {
        return primaryStage;
    }
}
