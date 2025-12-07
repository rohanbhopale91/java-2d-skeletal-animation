package com.animstudio.editor.preferences;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

/**
 * Dialog for editing application preferences.
 */
public class PreferencesDialog extends Dialog<Boolean> {
    
    private final EditorPreferences prefs;
    private final TabPane tabPane;
    
    // General settings
    private ComboBox<String> themeCombo;
    private ComboBox<String> languageCombo;
    private Spinner<Integer> recentFilesSpinner;
    private CheckBox autosaveCheckbox;
    private Spinner<Integer> autosaveIntervalSpinner;
    private Spinner<Integer> undoLevelsSpinner;
    
    // Canvas settings
    private Spinner<Integer> canvasWidthSpinner;
    private Spinner<Integer> canvasHeightSpinner;
    private Spinner<Integer> frameRateSpinner;
    private CheckBox antialiasingCheckbox;
    
    // Grid settings
    private CheckBox gridVisibleCheckbox;
    private Spinner<Integer> gridSizeSpinner;
    private CheckBox snapToGridCheckbox;
    
    // Display settings
    private ColorPicker boneColorPicker;
    private ColorPicker selectedBoneColorPicker;
    private ColorPicker ikTargetColorPicker;
    private CheckBox showBoneNamesCheckbox;
    private CheckBox showBoneLengthsCheckbox;
    
    public PreferencesDialog() {
        this.prefs = EditorPreferences.getInstance();
        this.tabPane = new TabPane();
        
        setTitle("Preferences");
        setHeaderText("Configure application settings");
        
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL, ButtonType.APPLY);
        
        Button resetButton = new Button("Reset to Defaults");
        resetButton.setOnAction(e -> resetToDefaults());
        
        createTabs();
        loadCurrentValues();
        
        VBox content = new VBox(10);
        content.getChildren().addAll(tabPane, resetButton);
        content.setPadding(new Insets(10));
        
        getDialogPane().setContent(content);
        getDialogPane().setPrefSize(500, 500);
        
        // Handle apply button
        Button applyButton = (Button) getDialogPane().lookupButton(ButtonType.APPLY);
        applyButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            savePreferences();
            event.consume(); // Don't close dialog
        });
        
        setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                savePreferences();
                return true;
            }
            return false;
        });
        
        // Set owner stage for proper modality
        Stage stage = (Stage) getDialogPane().getScene().getWindow();
        stage.setResizable(true);
    }
    
    private void createTabs() {
        tabPane.getTabs().addAll(
            createGeneralTab(),
            createCanvasTab(),
            createGridTab(),
            createDisplayTab()
        );
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
    }
    
    private Tab createGeneralTab() {
        Tab tab = new Tab("General");
        
        GridPane grid = createGrid();
        int row = 0;
        
        // Theme
        themeCombo = new ComboBox<>();
        themeCombo.getItems().addAll("dark", "light", "system");
        addRow(grid, row++, "Theme:", themeCombo);
        
        // Language
        languageCombo = new ComboBox<>();
        languageCombo.getItems().addAll("en", "es", "fr", "de", "ja", "zh");
        addRow(grid, row++, "Language:", languageCombo);
        
        // Recent files count
        recentFilesSpinner = new Spinner<>(1, 50, 10);
        recentFilesSpinner.setEditable(true);
        addRow(grid, row++, "Recent Files:", recentFilesSpinner);
        
        // Autosave
        autosaveCheckbox = new CheckBox("Enable Autosave");
        grid.add(autosaveCheckbox, 0, row++, 2, 1);
        
        // Autosave interval
        autosaveIntervalSpinner = new Spinner<>(1, 60, 5);
        autosaveIntervalSpinner.setEditable(true);
        HBox intervalBox = new HBox(5, autosaveIntervalSpinner, new Label("minutes"));
        addRow(grid, row++, "Autosave Interval:", intervalBox);
        
        // Undo levels
        undoLevelsSpinner = new Spinner<>(10, 500, 100);
        undoLevelsSpinner.setEditable(true);
        addRow(grid, row++, "Undo Levels:", undoLevelsSpinner);
        
        // Bind autosave interval to autosave enabled
        autosaveIntervalSpinner.disableProperty().bind(autosaveCheckbox.selectedProperty().not());
        
        ScrollPane scrollPane = new ScrollPane(grid);
        scrollPane.setFitToWidth(true);
        tab.setContent(scrollPane);
        
        return tab;
    }
    
    private Tab createCanvasTab() {
        Tab tab = new Tab("Canvas");
        
        GridPane grid = createGrid();
        int row = 0;
        
        // Canvas width
        canvasWidthSpinner = new Spinner<>(320, 4096, 1280);
        canvasWidthSpinner.setEditable(true);
        addRow(grid, row++, "Default Width:", canvasWidthSpinner);
        
        // Canvas height
        canvasHeightSpinner = new Spinner<>(240, 4096, 720);
        canvasHeightSpinner.setEditable(true);
        addRow(grid, row++, "Default Height:", canvasHeightSpinner);
        
        // Preset buttons
        HBox presets = new HBox(5);
        presets.getChildren().addAll(
            createPresetButton("720p", 1280, 720),
            createPresetButton("1080p", 1920, 1080),
            createPresetButton("4K", 3840, 2160),
            createPresetButton("Square", 1024, 1024)
        );
        grid.add(new Label("Presets:"), 0, row);
        grid.add(presets, 1, row++);
        
        // Frame rate
        frameRateSpinner = new Spinner<>(1, 120, 30);
        frameRateSpinner.setEditable(true);
        HBox fpsBox = new HBox(5, frameRateSpinner, new Label("FPS"));
        addRow(grid, row++, "Frame Rate:", fpsBox);
        
        // Antialiasing
        antialiasingCheckbox = new CheckBox("Enable Antialiasing");
        grid.add(antialiasingCheckbox, 0, row++, 2, 1);
        
        ScrollPane scrollPane = new ScrollPane(grid);
        scrollPane.setFitToWidth(true);
        tab.setContent(scrollPane);
        
        return tab;
    }
    
    private Tab createGridTab() {
        Tab tab = new Tab("Grid");
        
        GridPane grid = createGrid();
        int row = 0;
        
        // Grid visible
        gridVisibleCheckbox = new CheckBox("Show Grid");
        grid.add(gridVisibleCheckbox, 0, row++, 2, 1);
        
        // Grid size
        gridSizeSpinner = new Spinner<>(5, 100, 20);
        gridSizeSpinner.setEditable(true);
        HBox sizeBox = new HBox(5, gridSizeSpinner, new Label("pixels"));
        addRow(grid, row++, "Grid Size:", sizeBox);
        
        // Snap to grid
        snapToGridCheckbox = new CheckBox("Snap to Grid");
        grid.add(snapToGridCheckbox, 0, row++, 2, 1);
        
        // Bind grid size to grid visible
        gridSizeSpinner.disableProperty().bind(gridVisibleCheckbox.selectedProperty().not());
        snapToGridCheckbox.disableProperty().bind(gridVisibleCheckbox.selectedProperty().not());
        
        ScrollPane scrollPane = new ScrollPane(grid);
        scrollPane.setFitToWidth(true);
        tab.setContent(scrollPane);
        
        return tab;
    }
    
    private Tab createDisplayTab() {
        Tab tab = new Tab("Display");
        
        GridPane grid = createGrid();
        int row = 0;
        
        // Bone color
        boneColorPicker = new ColorPicker();
        addRow(grid, row++, "Bone Color:", boneColorPicker);
        
        // Selected bone color
        selectedBoneColorPicker = new ColorPicker();
        addRow(grid, row++, "Selected Bone Color:", selectedBoneColorPicker);
        
        // IK target color
        ikTargetColorPicker = new ColorPicker();
        addRow(grid, row++, "IK Target Color:", ikTargetColorPicker);
        
        // Show bone names
        showBoneNamesCheckbox = new CheckBox("Show Bone Names");
        grid.add(showBoneNamesCheckbox, 0, row++, 2, 1);
        
        // Show bone lengths
        showBoneLengthsCheckbox = new CheckBox("Show Bone Lengths");
        grid.add(showBoneLengthsCheckbox, 0, row++, 2, 1);
        
        ScrollPane scrollPane = new ScrollPane(grid);
        scrollPane.setFitToWidth(true);
        tab.setContent(scrollPane);
        
        return tab;
    }
    
    private GridPane createGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        return grid;
    }
    
    private void addRow(GridPane grid, int row, String label, javafx.scene.Node control) {
        grid.add(new Label(label), 0, row);
        grid.add(control, 1, row);
    }
    
    private Button createPresetButton(String name, int width, int height) {
        Button button = new Button(name);
        button.setOnAction(e -> {
            canvasWidthSpinner.getValueFactory().setValue(width);
            canvasHeightSpinner.getValueFactory().setValue(height);
        });
        return button;
    }
    
    private void loadCurrentValues() {
        // General
        themeCombo.setValue(prefs.getTheme());
        languageCombo.setValue(prefs.getLanguage());
        recentFilesSpinner.getValueFactory().setValue(prefs.getRecentFilesCount());
        autosaveCheckbox.setSelected(prefs.isAutosaveEnabled());
        autosaveIntervalSpinner.getValueFactory().setValue(prefs.getAutosaveInterval());
        undoLevelsSpinner.getValueFactory().setValue(prefs.getUndoLevels());
        
        // Canvas
        canvasWidthSpinner.getValueFactory().setValue(prefs.getCanvasWidth());
        canvasHeightSpinner.getValueFactory().setValue(prefs.getCanvasHeight());
        frameRateSpinner.getValueFactory().setValue(prefs.getFrameRate());
        antialiasingCheckbox.setSelected(prefs.isAntialiasing());
        
        // Grid
        gridVisibleCheckbox.setSelected(prefs.isGridVisible());
        gridSizeSpinner.getValueFactory().setValue(prefs.getGridSize());
        snapToGridCheckbox.setSelected(prefs.isSnapToGrid());
        
        // Display
        boneColorPicker.setValue(Color.web(prefs.getBoneColor()));
        selectedBoneColorPicker.setValue(Color.web(prefs.getSelectedBoneColor()));
        ikTargetColorPicker.setValue(Color.web(prefs.getIkTargetColor()));
        showBoneNamesCheckbox.setSelected(prefs.isShowBoneNames());
        showBoneLengthsCheckbox.setSelected(prefs.isShowBoneLengths());
    }
    
    private void savePreferences() {
        // General
        prefs.setTheme(themeCombo.getValue());
        prefs.setLanguage(languageCombo.getValue());
        prefs.setRecentFilesCount(recentFilesSpinner.getValue());
        prefs.setAutosaveEnabled(autosaveCheckbox.isSelected());
        prefs.setAutosaveInterval(autosaveIntervalSpinner.getValue());
        prefs.setUndoLevels(undoLevelsSpinner.getValue());
        
        // Canvas
        prefs.setCanvasWidth(canvasWidthSpinner.getValue());
        prefs.setCanvasHeight(canvasHeightSpinner.getValue());
        prefs.setFrameRate(frameRateSpinner.getValue());
        prefs.setAntialiasing(antialiasingCheckbox.isSelected());
        
        // Grid
        prefs.setGridVisible(gridVisibleCheckbox.isSelected());
        prefs.setGridSize(gridSizeSpinner.getValue());
        prefs.setSnapToGrid(snapToGridCheckbox.isSelected());
        
        // Display
        prefs.setBoneColor(toHexString(boneColorPicker.getValue()));
        prefs.setSelectedBoneColor(toHexString(selectedBoneColorPicker.getValue()));
        prefs.setIkTargetColor(toHexString(ikTargetColorPicker.getValue()));
        prefs.setShowBoneNames(showBoneNamesCheckbox.isSelected());
        prefs.setShowBoneLengths(showBoneLengthsCheckbox.isSelected());
        
        prefs.flush();
    }
    
    private void resetToDefaults() {
        // Reset preferences
        prefs.resetToDefaults();
        
        // Reload UI values
        loadCurrentValues();
    }
    
    private String toHexString(Color color) {
        return String.format("#%02X%02X%02X",
            (int) (color.getRed() * 255),
            (int) (color.getGreen() * 255),
            (int) (color.getBlue() * 255));
    }
}
