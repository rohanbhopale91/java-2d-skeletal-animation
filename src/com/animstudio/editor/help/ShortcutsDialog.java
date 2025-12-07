package com.animstudio.editor.help;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.List;
import java.util.Map;

/**
 * Dialog showing keyboard shortcuts reference.
 */
public class ShortcutsDialog extends Dialog<Void> {
    
    public ShortcutsDialog() {
        setTitle("Keyboard Shortcuts");
        setHeaderText("Quick Reference");
        
        getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        
        KeyboardShortcuts shortcuts = KeyboardShortcuts.getInstance();
        Map<String, List<KeyboardShortcuts.ShortcutInfo>> byCategory = shortcuts.getShortcutsByCategory();
        
        for (Map.Entry<String, List<KeyboardShortcuts.ShortcutInfo>> entry : byCategory.entrySet()) {
            Tab tab = new Tab(entry.getKey());
            tab.setContent(createCategoryContent(entry.getValue()));
            tabPane.getTabs().add(tab);
        }
        
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        content.getChildren().add(tabPane);
        
        // Search field
        TextField searchField = new TextField();
        searchField.setPromptText("Search shortcuts...");
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            // Could implement search filtering here
        });
        
        VBox mainContent = new VBox(10, searchField, tabPane);
        mainContent.setPadding(new Insets(10));
        
        getDialogPane().setContent(mainContent);
        getDialogPane().setPrefSize(500, 500);
    }
    
    private ScrollPane createCategoryContent(List<KeyboardShortcuts.ShortcutInfo> shortcuts) {
        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(8);
        grid.setPadding(new Insets(15));
        
        // Header
        Label actionHeader = new Label("Action");
        actionHeader.setFont(Font.font("System", FontWeight.BOLD, 12));
        Label shortcutHeader = new Label("Shortcut");
        shortcutHeader.setFont(Font.font("System", FontWeight.BOLD, 12));
        
        grid.add(actionHeader, 0, 0);
        grid.add(shortcutHeader, 1, 0);
        
        // Separator
        Separator separator = new Separator();
        grid.add(separator, 0, 1, 2, 1);
        
        int row = 2;
        for (KeyboardShortcuts.ShortcutInfo info : shortcuts) {
            Label actionLabel = new Label(info.description);
            Label shortcutLabel = new Label(info.getDisplayKey());
            shortcutLabel.setStyle("-fx-font-family: 'Consolas', 'Courier New', monospace; " +
                                   "-fx-background-color: #e0e0e0; " +
                                   "-fx-padding: 2 6 2 6; " +
                                   "-fx-background-radius: 3;");
            
            grid.add(actionLabel, 0, row);
            grid.add(shortcutLabel, 1, row);
            row++;
        }
        
        // Column constraints
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setHgrow(Priority.ALWAYS);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setMinWidth(120);
        grid.getColumnConstraints().addAll(col1, col2);
        
        ScrollPane scrollPane = new ScrollPane(grid);
        scrollPane.setFitToWidth(true);
        return scrollPane;
    }
}
