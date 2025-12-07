package com.animstudio.editor.ui.export;

import com.animstudio.core.animation.AnimationClip;
import com.animstudio.core.model.Skeleton;
import com.animstudio.editor.EditorContext;
import com.animstudio.editor.ui.canvas.CanvasPane;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Dialog for exporting animations as image sequences, GIFs, or videos.
 */
public class ExportDialog extends Dialog<Void> {
    
    public enum ExportFormat {
        PNG_SEQUENCE("PNG Sequence", "png"),
        JPEG_SEQUENCE("JPEG Sequence", "jpg"),
        GIF("Animated GIF", "gif"),
        SPRITE_SHEET("Sprite Sheet", "png");
        
        private final String label;
        private final String extension;
        
        ExportFormat(String label, String extension) {
            this.label = label;
            this.extension = extension;
        }
        
        public String getLabel() { return label; }
        public String getExtension() { return extension; }
        
        @Override
        public String toString() { return label; }
    }
    
    private final EditorContext context;
    
    // Format settings
    private ComboBox<ExportFormat> formatCombo;
    private ComboBox<String> animationCombo;
    
    // Frame range
    private Spinner<Integer> startFrameSpinner;
    private Spinner<Integer> endFrameSpinner;
    private Spinner<Integer> frameRateSpinner;
    private CheckBox useAnimationRangeCheckbox;
    
    // Size settings
    private Spinner<Integer> widthSpinner;
    private Spinner<Integer> heightSpinner;
    private CheckBox maintainAspectCheckbox;
    private Spinner<Integer> scaleSpinner;
    
    // Output settings
    private TextField outputPathField;
    private TextField filenamePatternField;
    
    // Sprite sheet settings
    private Spinner<Integer> columnsSpinner;
    
    // Background
    private CheckBox transparentBgCheckbox;
    private ColorPicker bgColorPicker;
    
    // Progress
    private ProgressBar progressBar;
    private Label progressLabel;
    private Button exportButton;
    private Button cancelButton;
    
    private AtomicBoolean exportCancelled = new AtomicBoolean(false);
    
    public ExportDialog() {
        this.context = EditorContext.getInstance();
        
        setTitle("Export Animation");
        setHeaderText("Export your animation");
        
        getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        
        // Tabs for different settings
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        
        tabPane.getTabs().addAll(
            createGeneralTab(),
            createSizeTab(),
            createOutputTab(),
            createAdvancedTab()
        );
        
        // Progress section
        VBox progressSection = createProgressSection();
        
        content.getChildren().addAll(tabPane, progressSection);
        
        getDialogPane().setContent(content);
        getDialogPane().setPrefSize(550, 500);
        
        // Initialize values
        loadAnimations();
        updateUIState();
    }
    
    private Tab createGeneralTab() {
        Tab tab = new Tab("General");
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(15));
        
        int row = 0;
        
        // Format
        grid.add(new Label("Format:"), 0, row);
        formatCombo = new ComboBox<>();
        formatCombo.getItems().addAll(ExportFormat.values());
        formatCombo.setValue(ExportFormat.PNG_SEQUENCE);
        formatCombo.setOnAction(e -> updateUIState());
        grid.add(formatCombo, 1, row++);
        
        // Animation
        grid.add(new Label("Animation:"), 0, row);
        animationCombo = new ComboBox<>();
        animationCombo.setOnAction(e -> loadAnimationRange());
        grid.add(animationCombo, 1, row++);
        
        // Separator
        grid.add(new Separator(), 0, row++, 2, 1);
        
        // Frame range
        grid.add(new Label("Start Frame:"), 0, row);
        startFrameSpinner = new Spinner<>(0, 9999, 0);
        startFrameSpinner.setEditable(true);
        grid.add(startFrameSpinner, 1, row++);
        
        grid.add(new Label("End Frame:"), 0, row);
        endFrameSpinner = new Spinner<>(0, 9999, 60);
        endFrameSpinner.setEditable(true);
        grid.add(endFrameSpinner, 1, row++);
        
        grid.add(new Label("Frame Rate:"), 0, row);
        frameRateSpinner = new Spinner<>(1, 120, 30);
        frameRateSpinner.setEditable(true);
        grid.add(frameRateSpinner, 1, row++);
        
        useAnimationRangeCheckbox = new CheckBox("Use animation's frame range");
        useAnimationRangeCheckbox.setSelected(true);
        useAnimationRangeCheckbox.setOnAction(e -> {
            if (useAnimationRangeCheckbox.isSelected()) {
                loadAnimationRange();
            }
        });
        grid.add(useAnimationRangeCheckbox, 0, row++, 2, 1);
        
        tab.setContent(grid);
        return tab;
    }
    
    private Tab createSizeTab() {
        Tab tab = new Tab("Size");
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(15));
        
        int row = 0;
        
        // Width
        grid.add(new Label("Width:"), 0, row);
        widthSpinner = new Spinner<>(1, 4096, 800);
        widthSpinner.setEditable(true);
        grid.add(widthSpinner, 1, row++);
        
        // Height
        grid.add(new Label("Height:"), 0, row);
        heightSpinner = new Spinner<>(1, 4096, 600);
        heightSpinner.setEditable(true);
        grid.add(heightSpinner, 1, row++);
        
        // Maintain aspect
        maintainAspectCheckbox = new CheckBox("Maintain aspect ratio");
        maintainAspectCheckbox.setSelected(true);
        grid.add(maintainAspectCheckbox, 0, row++, 2, 1);
        
        // Scale
        grid.add(new Label("Scale (%):"), 0, row);
        scaleSpinner = new Spinner<>(10, 400, 100);
        scaleSpinner.setEditable(true);
        grid.add(scaleSpinner, 1, row++);
        
        // Separator for sprite sheet
        grid.add(new Separator(), 0, row++, 2, 1);
        grid.add(new Label("Sprite Sheet Settings:"), 0, row++, 2, 1);
        
        // Columns (for sprite sheet)
        grid.add(new Label("Columns:"), 0, row);
        columnsSpinner = new Spinner<>(1, 100, 8);
        columnsSpinner.setEditable(true);
        grid.add(columnsSpinner, 1, row++);
        
        tab.setContent(grid);
        return tab;
    }
    
    private Tab createOutputTab() {
        Tab tab = new Tab("Output");
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(15));
        
        int row = 0;
        
        // Output path
        grid.add(new Label("Output Folder:"), 0, row);
        HBox pathBox = new HBox(5);
        outputPathField = new TextField();
        outputPathField.setPrefWidth(300);
        outputPathField.setText(System.getProperty("user.home") + File.separator + "AnimStudioExport");
        Button browseBtn = new Button("Browse...");
        browseBtn.setOnAction(e -> browseOutputPath());
        pathBox.getChildren().addAll(outputPathField, browseBtn);
        grid.add(pathBox, 1, row++);
        
        // Filename pattern
        grid.add(new Label("Filename Pattern:"), 0, row);
        filenamePatternField = new TextField("frame_{0000}");
        filenamePatternField.setTooltip(new Tooltip("Use {0000} for frame number with padding"));
        grid.add(filenamePatternField, 1, row++);
        
        // Preview
        grid.add(new Label("Preview:"), 0, row);
        Label previewLabel = new Label();
        filenamePatternField.textProperty().addListener((obs, old, val) -> {
            previewLabel.setText(formatFilename(val, 1));
        });
        previewLabel.setText(formatFilename(filenamePatternField.getText(), 1));
        grid.add(previewLabel, 1, row++);
        
        tab.setContent(grid);
        return tab;
    }
    
    private Tab createAdvancedTab() {
        Tab tab = new Tab("Advanced");
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(15));
        
        int row = 0;
        
        // Background
        grid.add(new Label("Background:"), 0, row);
        transparentBgCheckbox = new CheckBox("Transparent");
        transparentBgCheckbox.setSelected(true);
        transparentBgCheckbox.setOnAction(e -> bgColorPicker.setDisable(transparentBgCheckbox.isSelected()));
        grid.add(transparentBgCheckbox, 1, row++);
        
        grid.add(new Label("Background Color:"), 0, row);
        bgColorPicker = new ColorPicker(Color.WHITE);
        bgColorPicker.setDisable(true);
        grid.add(bgColorPicker, 1, row++);
        
        tab.setContent(grid);
        return tab;
    }
    
    private VBox createProgressSection() {
        VBox section = new VBox(10);
        section.setPadding(new Insets(10, 0, 0, 0));
        
        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(Double.MAX_VALUE);
        progressBar.setVisible(false);
        
        progressLabel = new Label("");
        progressLabel.setVisible(false);
        
        HBox buttons = new HBox(10);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        
        exportButton = new Button("Export");
        exportButton.setDefaultButton(true);
        exportButton.setOnAction(e -> startExport());
        
        cancelButton = new Button("Cancel Export");
        cancelButton.setDisable(true);
        cancelButton.setOnAction(e -> exportCancelled.set(true));
        
        buttons.getChildren().addAll(cancelButton, exportButton);
        
        section.getChildren().addAll(progressBar, progressLabel, buttons);
        return section;
    }
    
    private void loadAnimations() {
        animationCombo.getItems().clear();
        if (context.getCurrentProject() != null) {
            for (AnimationClip clip : context.getCurrentProject().getAnimations()) {
                animationCombo.getItems().add(clip.getName());
            }
            if (!animationCombo.getItems().isEmpty()) {
                animationCombo.setValue(animationCombo.getItems().get(0));
                loadAnimationRange();
            }
        }
    }
    
    private void loadAnimationRange() {
        if (!useAnimationRangeCheckbox.isSelected()) return;
        
        String animName = animationCombo.getValue();
        if (animName == null || context.getCurrentProject() == null) return;
        
        AnimationClip clip = context.getCurrentProject().getAnimation(animName);
        if (clip != null) {
            startFrameSpinner.getValueFactory().setValue(0);
            endFrameSpinner.getValueFactory().setValue((int) clip.getDuration());
        }
    }
    
    private void updateUIState() {
        ExportFormat format = formatCombo.getValue();
        boolean isSpriteSheet = format == ExportFormat.SPRITE_SHEET;
        columnsSpinner.setDisable(!isSpriteSheet);
    }
    
    private void browseOutputPath() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Output Folder");
        
        File current = new File(outputPathField.getText());
        if (current.exists() && current.isDirectory()) {
            chooser.setInitialDirectory(current);
        }
        
        Window window = getDialogPane().getScene().getWindow();
        File selected = chooser.showDialog(window);
        if (selected != null) {
            outputPathField.setText(selected.getAbsolutePath());
        }
    }
    
    private String formatFilename(String pattern, int frameNumber) {
        // Replace {0000} style patterns with padded frame number
        String result = pattern;
        if (result.contains("{")) {
            int start = result.indexOf("{");
            int end = result.indexOf("}");
            if (start >= 0 && end > start) {
                String format = result.substring(start + 1, end);
                int padding = format.length();
                String paddedNum = String.format("%0" + padding + "d", frameNumber);
                result = result.substring(0, start) + paddedNum + result.substring(end + 1);
            }
        }
        return result;
    }
    
    private void startExport() {
        exportCancelled.set(false);
        
        // Validate
        String animName = animationCombo.getValue();
        if (animName == null) {
            showError("Please select an animation to export.");
            return;
        }
        
        AnimationClip clip = context.getCurrentProject().getAnimation(animName);
        Skeleton skeleton = context.getCurrentSkeleton();
        if (clip == null || skeleton == null) {
            showError("No animation or skeleton available.");
            return;
        }
        
        // Create output directory
        Path outputDir = Path.of(outputPathField.getText());
        try {
            Files.createDirectories(outputDir);
        } catch (IOException e) {
            showError("Could not create output directory: " + e.getMessage());
            return;
        }
        
        // UI state
        progressBar.setVisible(true);
        progressLabel.setVisible(true);
        exportButton.setDisable(true);
        cancelButton.setDisable(false);
        
        // Export in background thread
        Thread exportThread = new Thread(() -> {
            try {
                ExportFormat format = formatCombo.getValue();
                
                switch (format) {
                    case PNG_SEQUENCE:
                    case JPEG_SEQUENCE:
                        exportSequence(clip, skeleton, outputDir, format);
                        break;
                    case GIF:
                        exportGif(clip, skeleton, outputDir);
                        break;
                    case SPRITE_SHEET:
                        exportSpriteSheet(clip, skeleton, outputDir);
                        break;
                }
                
                if (!exportCancelled.get()) {
                    javafx.application.Platform.runLater(() -> {
                        progressLabel.setText("Export complete!");
                        showInfo("Export completed successfully to:\n" + outputDir);
                    });
                }
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    showError("Export failed: " + e.getMessage());
                });
            } finally {
                javafx.application.Platform.runLater(() -> {
                    exportButton.setDisable(false);
                    cancelButton.setDisable(true);
                });
            }
        });
        
        exportThread.setDaemon(true);
        exportThread.start();
    }
    
    private void exportSequence(AnimationClip clip, Skeleton skeleton, Path outputDir, ExportFormat format) throws IOException {
        int startFrame = startFrameSpinner.getValue();
        int endFrame = endFrameSpinner.getValue();
        int width = widthSpinner.getValue();
        int height = heightSpinner.getValue();
        int scale = scaleSpinner.getValue();
        
        width = width * scale / 100;
        height = height * scale / 100;
        
        int totalFrames = endFrame - startFrame + 1;
        
        for (int frame = startFrame; frame <= endFrame && !exportCancelled.get(); frame++) {
            int currentFrame = frame;
            int progress = frame - startFrame;
            
            javafx.application.Platform.runLater(() -> {
                progressBar.setProgress((double) progress / totalFrames);
                progressLabel.setText("Exporting frame " + currentFrame + " of " + endFrame);
            });
            
            // Render frame
            WritableImage image = renderFrame(clip, skeleton, frame, width, height);
            
            // Save image
            String filename = formatFilename(filenamePatternField.getText(), frame) + "." + format.getExtension();
            Path filePath = outputDir.resolve(filename);
            
            saveImage(image, filePath, format);
        }
    }
    
    private void exportGif(AnimationClip clip, Skeleton skeleton, Path outputDir) throws IOException {
        // GIF export would require a GIF encoder library
        // For now, just export as PNG sequence with a note
        javafx.application.Platform.runLater(() -> {
            showInfo("GIF export requires additional libraries.\nExporting as PNG sequence instead.");
        });
        exportSequence(clip, skeleton, outputDir, ExportFormat.PNG_SEQUENCE);
    }
    
    private void exportSpriteSheet(AnimationClip clip, Skeleton skeleton, Path outputDir) throws IOException {
        int startFrame = startFrameSpinner.getValue();
        int endFrame = endFrameSpinner.getValue();
        int frameWidth = widthSpinner.getValue();
        int frameHeight = heightSpinner.getValue();
        int scale = scaleSpinner.getValue();
        int columns = columnsSpinner.getValue();
        
        frameWidth = frameWidth * scale / 100;
        frameHeight = frameHeight * scale / 100;
        
        int totalFrames = endFrame - startFrame + 1;
        int rows = (totalFrames + columns - 1) / columns;
        
        int sheetWidth = frameWidth * columns;
        int sheetHeight = frameHeight * rows;
        
        // Create sprite sheet canvas
        Canvas sheetCanvas = new Canvas(sheetWidth, sheetHeight);
        GraphicsContext gc = sheetCanvas.getGraphicsContext2D();
        
        // Fill background
        if (transparentBgCheckbox.isSelected()) {
            gc.clearRect(0, 0, sheetWidth, sheetHeight);
        } else {
            gc.setFill(bgColorPicker.getValue());
            gc.fillRect(0, 0, sheetWidth, sheetHeight);
        }
        
        // Render each frame
        for (int i = 0; i < totalFrames && !exportCancelled.get(); i++) {
            int frame = startFrame + i;
            int col = i % columns;
            int row = i / columns;
            
            final int progressI = i;
            final int progressFrame = frame;
            javafx.application.Platform.runLater(() -> {
                progressBar.setProgress((double) progressI / totalFrames);
                progressLabel.setText("Rendering frame " + progressFrame);
            });
            
            // Render frame to temp image
            WritableImage frameImage = renderFrame(clip, skeleton, frame, frameWidth, frameHeight);
            
            // Draw onto sprite sheet
            gc.drawImage(frameImage, col * frameWidth, row * frameHeight);
        }
        
        // Save sprite sheet
        WritableImage sheetImage = new WritableImage(sheetWidth, sheetHeight);
        sheetCanvas.snapshot(null, sheetImage);
        
        String filename = "spritesheet." + ExportFormat.SPRITE_SHEET.getExtension();
        Path filePath = outputDir.resolve(filename);
        saveImage(sheetImage, filePath, ExportFormat.PNG_SEQUENCE);
    }
    
    private WritableImage renderFrame(AnimationClip clip, Skeleton skeleton, int frame, int width, int height) {
        // Create a temporary canvas for rendering
        Canvas tempCanvas = new Canvas(width, height);
        GraphicsContext gc = tempCanvas.getGraphicsContext2D();
        
        // Background
        if (transparentBgCheckbox.isSelected()) {
            gc.clearRect(0, 0, width, height);
        } else {
            gc.setFill(bgColorPicker.getValue());
            gc.fillRect(0, 0, width, height);
        }
        
        // Apply animation
        skeleton.resetToSetupPose();
        clip.apply(skeleton, frame);
        skeleton.updateWorldTransforms();
        
        // Render skeleton (simplified - just bones)
        gc.save();
        gc.translate(width / 2.0, height / 2.0);
        
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(2);
        
        for (var bone : skeleton.getBones()) {
            double x = bone.getWorldPosition().x;
            double y = bone.getWorldPosition().y;
            double rot = Math.toRadians(bone.getWorldRotation());
            double len = bone.getLength();
            
            double endX = x + Math.cos(rot) * len;
            double endY = y + Math.sin(rot) * len;
            
            gc.strokeLine(x, y, endX, endY);
            gc.fillOval(x - 3, y - 3, 6, 6);
        }
        
        gc.restore();
        
        // Snapshot to image
        WritableImage image = new WritableImage(width, height);
        tempCanvas.snapshot(null, image);
        
        return image;
    }
    
    private void saveImage(WritableImage fxImage, Path filePath, ExportFormat format) throws IOException {
        // Convert JavaFX image to BufferedImage
        int width = (int) fxImage.getWidth();
        int height = (int) fxImage.getHeight();
        
        BufferedImage bImage;
        if (transparentBgCheckbox.isSelected() && format != ExportFormat.JPEG_SEQUENCE) {
            bImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        } else {
            bImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        }
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = fxImage.getPixelReader().getArgb(x, y);
                bImage.setRGB(x, y, argb);
            }
        }
        
        String formatName = format == ExportFormat.JPEG_SEQUENCE ? "jpg" : "png";
        ImageIO.write(bImage, formatName, filePath.toFile());
    }
    
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Export Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Export");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
