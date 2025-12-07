package com.animstudio.editor.ui.log;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Predicate;

/**
 * Log panel for displaying application messages, warnings, and errors.
 * Supports filtering by log level and searching.
 */
public class LogPanel extends VBox {
    
    public enum LogLevel {
        DEBUG("DEBUG", Color.GRAY),
        INFO("INFO", Color.LIGHTBLUE),
        WARNING("WARN", Color.ORANGE),
        ERROR("ERROR", Color.RED),
        SUCCESS("OK", Color.LIGHTGREEN);
        
        private final String label;
        private final Color color;
        
        LogLevel(String label, Color color) {
            this.label = label;
            this.color = color;
        }
        
        public String getLabel() { return label; }
        public Color getColor() { return color; }
    }
    
    public static class LogEntry {
        private final LocalDateTime timestamp;
        private final LogLevel level;
        private final String message;
        private final String source;
        
        public LogEntry(LogLevel level, String message, String source) {
            this.timestamp = LocalDateTime.now();
            this.level = level;
            this.message = message;
            this.source = source;
        }
        
        public LocalDateTime getTimestamp() { return timestamp; }
        public LogLevel getLevel() { return level; }
        public String getMessage() { return message; }
        public String getSource() { return source; }
        
        @Override
        public String toString() {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
            return String.format("[%s] [%s] %s%s", 
                timestamp.format(fmt),
                level.getLabel(),
                source != null ? "[" + source + "] " : "",
                message);
        }
    }
    
    private static LogPanel instance;
    
    private final ObservableList<LogEntry> allEntries;
    private final FilteredList<LogEntry> filteredEntries;
    private final ListView<LogEntry> logListView;
    private TextField searchField;
    private ComboBox<String> levelFilter;
    private CheckBox autoScrollCheckbox;
    private Label statusLabel;
    
    private LogLevel minimumLevel = LogLevel.DEBUG;
    private int maxEntries = 5000;
    
    public LogPanel() {
        instance = this;
        
        allEntries = FXCollections.observableArrayList();
        filteredEntries = new FilteredList<>(allEntries, p -> true);
        
        setSpacing(5);
        setPadding(new Insets(5));
        
        // Toolbar
        HBox toolbar = createToolbar();
        
        // Log list
        logListView = createLogListView();
        VBox.setVgrow(logListView, Priority.ALWAYS);
        
        // Status bar
        statusLabel = new Label("0 entries");
        statusLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: gray;");
        
        getChildren().addAll(toolbar, logListView, statusLabel);
        
        // Initial log message
        info("Log panel initialized", "LogPanel");
    }
    
    public static LogPanel getInstance() {
        return instance;
    }
    
    private HBox createToolbar() {
        HBox toolbar = new HBox(10);
        toolbar.setPadding(new Insets(2));
        toolbar.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        
        // Level filter
        levelFilter = new ComboBox<>();
        levelFilter.getItems().addAll("All", "Debug+", "Info+", "Warnings+", "Errors Only");
        levelFilter.setValue("All");
        levelFilter.setOnAction(e -> updateFilter());
        
        // Search field
        searchField = new TextField();
        searchField.setPromptText("Search logs...");
        searchField.setPrefWidth(200);
        searchField.textProperty().addListener((obs, old, val) -> updateFilter());
        
        // Auto-scroll checkbox
        autoScrollCheckbox = new CheckBox("Auto-scroll");
        autoScrollCheckbox.setSelected(true);
        
        // Clear button
        Button clearBtn = new Button("Clear");
        clearBtn.setOnAction(e -> clear());
        
        // Export button
        Button exportBtn = new Button("Export");
        exportBtn.setOnAction(e -> exportLogs());
        
        toolbar.getChildren().addAll(
            new Label("Filter:"), levelFilter,
            new Separator(javafx.geometry.Orientation.VERTICAL),
            searchField,
            new Separator(javafx.geometry.Orientation.VERTICAL),
            autoScrollCheckbox,
            new Region(), // Spacer
            clearBtn, exportBtn
        );
        HBox.setHgrow(toolbar.getChildren().get(toolbar.getChildren().size() - 3), Priority.ALWAYS);
        
        return toolbar;
    }
    
    private ListView<LogEntry> createLogListView() {
        ListView<LogEntry> listView = new ListView<>(filteredEntries);
        listView.setCellFactory(lv -> new LogEntryCell());
        listView.setStyle("-fx-background-color: #1e1e1e; -fx-control-inner-background: #1e1e1e;");
        return listView;
    }
    
    private void updateFilter() {
        String levelChoice = levelFilter.getValue();
        String searchText = searchField.getText().toLowerCase();
        
        // Determine minimum level
        switch (levelChoice) {
            case "Debug+": minimumLevel = LogLevel.DEBUG; break;
            case "Info+": minimumLevel = LogLevel.INFO; break;
            case "Warnings+": minimumLevel = LogLevel.WARNING; break;
            case "Errors Only": minimumLevel = LogLevel.ERROR; break;
            default: minimumLevel = LogLevel.DEBUG; break;
        }
        
        filteredEntries.setPredicate(entry -> {
            // Level filter
            if (entry.getLevel().ordinal() < minimumLevel.ordinal() && 
                entry.getLevel() != LogLevel.SUCCESS) {
                return false;
            }
            
            // Search filter
            if (!searchText.isEmpty()) {
                String text = entry.toString().toLowerCase();
                if (!text.contains(searchText)) {
                    return false;
                }
            }
            
            return true;
        });
        
        updateStatus();
    }
    
    private void updateStatus() {
        int total = allEntries.size();
        int filtered = filteredEntries.size();
        if (total == filtered) {
            statusLabel.setText(total + " entries");
        } else {
            statusLabel.setText(filtered + " / " + total + " entries");
        }
    }
    
    private void scrollToBottom() {
        if (autoScrollCheckbox.isSelected() && !filteredEntries.isEmpty()) {
            logListView.scrollTo(filteredEntries.size() - 1);
        }
    }
    
    // === Logging Methods ===
    
    public void log(LogLevel level, String message, String source) {
        LogEntry entry = new LogEntry(level, message, source);
        
        Platform.runLater(() -> {
            allEntries.add(entry);
            
            // Trim old entries
            while (allEntries.size() > maxEntries) {
                allEntries.remove(0);
            }
            
            updateStatus();
            scrollToBottom();
        });
    }
    
    public void log(LogLevel level, String message) {
        log(level, message, null);
    }
    
    public void debug(String message, String source) {
        log(LogLevel.DEBUG, message, source);
    }
    
    public void debug(String message) {
        log(LogLevel.DEBUG, message, null);
    }
    
    public void info(String message, String source) {
        log(LogLevel.INFO, message, source);
    }
    
    public void info(String message) {
        log(LogLevel.INFO, message, null);
    }
    
    public void warning(String message, String source) {
        log(LogLevel.WARNING, message, source);
    }
    
    public void warning(String message) {
        log(LogLevel.WARNING, message, null);
    }
    
    public void error(String message, String source) {
        log(LogLevel.ERROR, message, source);
    }
    
    public void error(String message) {
        log(LogLevel.ERROR, message, null);
    }
    
    public void error(String message, Throwable t) {
        log(LogLevel.ERROR, message + ": " + t.getMessage(), null);
    }
    
    public void success(String message, String source) {
        log(LogLevel.SUCCESS, message, source);
    }
    
    public void success(String message) {
        log(LogLevel.SUCCESS, message, null);
    }
    
    public void clear() {
        allEntries.clear();
        updateStatus();
    }
    
    private void exportLogs() {
        StringBuilder sb = new StringBuilder();
        sb.append("AnimStudio Log Export\n");
        sb.append("Exported: ").append(LocalDateTime.now()).append("\n");
        sb.append("=".repeat(80)).append("\n\n");
        
        for (LogEntry entry : allEntries) {
            sb.append(entry.toString()).append("\n");
        }
        
        // Copy to clipboard
        javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
        javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
        content.putString(sb.toString());
        clipboard.setContent(content);
        
        info("Log exported to clipboard (" + allEntries.size() + " entries)", "LogPanel");
    }
    
    public void setMaxEntries(int max) {
        this.maxEntries = max;
    }
    
    // === Custom Cell ===
    
    private static class LogEntryCell extends ListCell<LogEntry> {
        @Override
        protected void updateItem(LogEntry item, boolean empty) {
            super.updateItem(item, empty);
            
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
                setStyle("");
            } else {
                setText(item.toString());
                setFont(Font.font("Consolas", 11));
                
                Color color = item.getLevel().getColor();
                String colorStyle = String.format("-fx-text-fill: rgb(%d,%d,%d);",
                    (int)(color.getRed() * 255),
                    (int)(color.getGreen() * 255),
                    (int)(color.getBlue() * 255));
                setStyle(colorStyle + " -fx-background-color: transparent;");
            }
        }
    }
    
    // === Static Convenience Methods ===
    
    public static void logDebug(String message) {
        if (instance != null) instance.debug(message);
    }
    
    public static void logInfo(String message) {
        if (instance != null) instance.info(message);
    }
    
    public static void logWarning(String message) {
        if (instance != null) instance.warning(message);
    }
    
    public static void logError(String message) {
        if (instance != null) instance.error(message);
    }
    
    public static void logSuccess(String message) {
        if (instance != null) instance.success(message);
    }
}
