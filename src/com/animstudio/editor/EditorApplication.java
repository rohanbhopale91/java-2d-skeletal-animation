package com.animstudio.editor;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import com.animstudio.logging.LoggingSystem;
import com.animstudio.logging.Log;
import com.animstudio.logging.LogCategory;

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
        // Initialize logging system FIRST
        LoggingSystem.initialize();
        Log.info(LogCategory.STARTUP, "EditorApplication", "Starting AnimStudio Editor...");
        
        instance = this;
        // Initialize editor context
        EditorContext.getInstance();
        Log.info(LogCategory.STARTUP, "EditorApplication", "EditorContext initialized");
    }
    
    @Override
    public void start(Stage primaryStage) throws Exception {
        Log.info(LogCategory.STARTUP, "EditorApplication", "Starting UI...");
        this.primaryStage = primaryStage;
        
        // Load the main window FXML
        Log.debug(LogCategory.STARTUP, "EditorApplication", "Loading MainWindow.fxml");
        FXMLLoader loader = new FXMLLoader(getClass().getResource("MainWindow.fxml"));
        Parent root = loader.load();
        
        // Store controller reference
        MainWindowController controller = loader.getController();
        EditorContext.getInstance().setMainController(controller);
        Log.info(LogCategory.STARTUP, "EditorApplication", "MainWindowController initialized");
        
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
        
        Log.info(LogCategory.STARTUP, "EditorApplication", "Showing main window");
        primaryStage.show();
        
        // Initialize with empty project
        EditorContext.getInstance().newProject();
        Log.info(LogCategory.STARTUP, "EditorApplication", "AnimStudio Editor started successfully");
    }
    
    @Override
    public void stop() throws Exception {
        Log.info(LogCategory.SHUTDOWN, "EditorApplication", "Shutting down AnimStudio Editor...");
        EditorContext.getInstance().shutdown();
        LoggingSystem.shutdown();
    }
    
    public static EditorApplication getInstance() {
        return instance;
    }
    
    public Stage getPrimaryStage() {
        return primaryStage;
    }
}
