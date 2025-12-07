package com.animstudio.editor.help;

import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

import java.util.List;
import java.util.Map;

/**
 * Help browser dialog showing documentation.
 */
public class HelpDialog extends Dialog<Void> {
    
    private final HelpSystem helpSystem;
    private final WebView webView;
    private final WebEngine webEngine;
    private final ListView<HelpSystem.HelpTopic> topicList;
    
    public HelpDialog() {
        this.helpSystem = HelpSystem.getInstance();
        this.webView = new WebView();
        this.webEngine = webView.getEngine();
        this.topicList = new ListView<>();
        
        setTitle("Help");
        setHeaderText(null);
        
        getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        
        // Create split pane
        SplitPane splitPane = new SplitPane();
        splitPane.setOrientation(Orientation.HORIZONTAL);
        splitPane.setDividerPositions(0.3);
        
        // Left side - topic list
        VBox leftPane = createLeftPane();
        
        // Right side - content
        VBox rightPane = createRightPane();
        
        splitPane.getItems().addAll(leftPane, rightPane);
        
        getDialogPane().setContent(splitPane);
        getDialogPane().setPrefSize(900, 600);
        
        // Load initial topic
        if (!helpSystem.getAllTopics().isEmpty()) {
            HelpSystem.HelpTopic firstTopic = helpSystem.getAllTopics().iterator().next();
            showTopic(firstTopic);
            topicList.getSelectionModel().select(firstTopic);
        }
    }
    
    public HelpDialog(String topicId) {
        this();
        
        HelpSystem.HelpTopic topic = helpSystem.getTopic(topicId);
        if (topic != null) {
            showTopic(topic);
            topicList.getSelectionModel().select(topic);
        }
    }
    
    private VBox createLeftPane() {
        VBox leftPane = new VBox(10);
        leftPane.setPadding(new Insets(10));
        
        // Search field
        TextField searchField = new TextField();
        searchField.setPromptText("Search help...");
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.isEmpty()) {
                loadAllTopics();
            } else {
                List<HelpSystem.HelpTopic> results = helpSystem.search(newVal);
                topicList.getItems().setAll(results);
            }
        });
        
        // Topic list
        topicList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(HelpSystem.HelpTopic item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.title);
            }
        });
        
        topicList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                showTopic(newVal);
            }
        });
        
        loadAllTopics();
        
        VBox.setVgrow(topicList, Priority.ALWAYS);
        leftPane.getChildren().addAll(searchField, topicList);
        
        return leftPane;
    }
    
    private VBox createRightPane() {
        VBox rightPane = new VBox(10);
        rightPane.setPadding(new Insets(10));
        
        // Navigation toolbar
        HBox toolbar = new HBox(5);
        Button backBtn = new Button("◀");
        backBtn.setOnAction(e -> webEngine.executeScript("history.back()"));
        Button forwardBtn = new Button("▶");
        forwardBtn.setOnAction(e -> webEngine.executeScript("history.forward()"));
        Button homeBtn = new Button("🏠");
        homeBtn.setOnAction(e -> {
            HelpSystem.HelpTopic firstTopic = helpSystem.getAllTopics().iterator().next();
            showTopic(firstTopic);
        });
        
        toolbar.getChildren().addAll(backBtn, forwardBtn, homeBtn);
        
        // WebView for content
        VBox.setVgrow(webView, Priority.ALWAYS);
        
        rightPane.getChildren().addAll(toolbar, webView);
        return rightPane;
    }
    
    private void loadAllTopics() {
        topicList.getItems().clear();
        topicList.getItems().addAll(helpSystem.getAllTopics());
    }
    
    private void showTopic(HelpSystem.HelpTopic topic) {
        String html = """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                        padding: 20px;
                        line-height: 1.6;
                        color: #333;
                        max-width: 800px;
                    }
                    h1 { color: #2c3e50; border-bottom: 2px solid #3498db; padding-bottom: 10px; }
                    h2 { color: #34495e; margin-top: 30px; }
                    ul, ol { padding-left: 25px; }
                    li { margin: 5px 0; }
                    strong { color: #2980b9; }
                    code {
                        background: #f4f4f4;
                        padding: 2px 6px;
                        border-radius: 3px;
                        font-family: 'Consolas', monospace;
                    }
                    .category {
                        display: inline-block;
                        background: #3498db;
                        color: white;
                        padding: 3px 10px;
                        border-radius: 15px;
                        font-size: 12px;
                        margin-bottom: 15px;
                    }
                </style>
            </head>
            <body>
                <span class="category">%s</span>
                %s
            </body>
            </html>
            """.formatted(topic.getCategoryDisplayName(), topic.content);
        
        webEngine.loadContent(html);
    }
}
