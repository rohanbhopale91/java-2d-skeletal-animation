package com.animstudio.editor.ui.mesh;

import com.animstudio.core.mesh.DeformableMesh;
import com.animstudio.core.mesh.MeshVertex;
import com.animstudio.core.mesh.MeshTriangle;
import com.animstudio.core.model.Bone;
import com.animstudio.core.model.Skeleton;
import com.animstudio.editor.EditorContext;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.util.List;

/**
 * UI panel for editing deformable meshes.
 * Provides tools for vertex manipulation, weight painting, and mesh creation.
 */
public class MeshEditorPane extends BorderPane {
    
    private final TableView<MeshEntry> meshTable;
    private final ObservableList<MeshEntry> meshEntries;
    
    private final TableView<VertexEntry> vertexTable;
    private final ObservableList<VertexEntry> vertexEntries;
    
    // Weight painting controls
    private final ComboBox<String> weightBoneCombo;
    private final Slider weightSlider;
    private final Slider brushSizeSlider;
    private final ToggleGroup paintModeGroup;
    
    // Mesh properties
    private final TextField meshNameField;
    private final Label vertexCountLabel;
    private final Label triangleCountLabel;
    
    private DeformableMesh selectedMesh;
    private boolean updatingUI = false;
    
    public MeshEditorPane() {
        setPadding(new Insets(10));
        setStyle("-fx-background-color: #2d2d30;");
        
        // === Mesh List Section ===
        meshEntries = FXCollections.observableArrayList();
        meshTable = new TableView<>(meshEntries);
        meshTable.setPlaceholder(new Label("No meshes"));
        meshTable.setPrefHeight(120);
        
        TableColumn<MeshEntry, String> meshNameCol = new TableColumn<>("Name");
        meshNameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        meshNameCol.setPrefWidth(100);
        
        TableColumn<MeshEntry, Integer> vertCountCol = new TableColumn<>("Vertices");
        vertCountCol.setCellValueFactory(new PropertyValueFactory<>("vertexCount"));
        vertCountCol.setPrefWidth(60);
        
        TableColumn<MeshEntry, Integer> triCountCol = new TableColumn<>("Triangles");
        triCountCol.setCellValueFactory(new PropertyValueFactory<>("triangleCount"));
        triCountCol.setPrefWidth(60);
        
        meshTable.getColumns().addAll(meshNameCol, vertCountCol, triCountCol);
        
        meshTable.getSelectionModel().selectedItemProperty().addListener((obs, old, entry) -> {
            if (entry != null) {
                selectMesh(entry.getMesh());
            }
        });
        
        // Mesh list toolbar
        Button addMeshButton = new Button("+ Mesh");
        addMeshButton.setTooltip(new Tooltip("Create new mesh"));
        addMeshButton.setOnAction(e -> createNewMesh());
        
        Button removeMeshButton = new Button("- Mesh");
        removeMeshButton.setTooltip(new Tooltip("Remove selected mesh"));
        removeMeshButton.setOnAction(e -> removeSelectedMesh());
        
        Button duplicateMeshButton = new Button("Dup");
        duplicateMeshButton.setTooltip(new Tooltip("Duplicate mesh"));
        duplicateMeshButton.setOnAction(e -> duplicateSelectedMesh());
        
        HBox meshToolbar = new HBox(5, addMeshButton, removeMeshButton, duplicateMeshButton);
        meshToolbar.setPadding(new Insets(5, 0, 5, 0));
        
        VBox meshListSection = new VBox(5, new Label("Meshes:"), meshTable, meshToolbar);
        
        // === Mesh Properties Section ===
        GridPane propsGrid = new GridPane();
        propsGrid.setHgap(10);
        propsGrid.setVgap(8);
        
        int row = 0;
        
        meshNameField = new TextField();
        meshNameField.setPromptText("Mesh name");
        meshNameField.textProperty().addListener((obs, old, val) -> {
            if (!updatingUI && selectedMesh != null) {
                selectedMesh.setName(val);
                refreshMeshList();
            }
        });
        propsGrid.add(new Label("Name:"), 0, row);
        propsGrid.add(meshNameField, 1, row++);
        
        vertexCountLabel = new Label("0");
        propsGrid.add(new Label("Vertices:"), 0, row);
        propsGrid.add(vertexCountLabel, 1, row++);
        
        triangleCountLabel = new Label("0");
        propsGrid.add(new Label("Triangles:"), 0, row);
        propsGrid.add(triangleCountLabel, 1, row++);
        
        TitledPane propsPane = new TitledPane("Mesh Properties", propsGrid);
        propsPane.setExpanded(true);
        
        // === Vertex List Section ===
        vertexEntries = FXCollections.observableArrayList();
        vertexTable = new TableView<>(vertexEntries);
        vertexTable.setPlaceholder(new Label("No vertices"));
        vertexTable.setPrefHeight(100);
        
        TableColumn<VertexEntry, Integer> idxCol = new TableColumn<>("#");
        idxCol.setCellValueFactory(new PropertyValueFactory<>("index"));
        idxCol.setPrefWidth(30);
        
        TableColumn<VertexEntry, Double> xCol = new TableColumn<>("X");
        xCol.setCellValueFactory(new PropertyValueFactory<>("x"));
        xCol.setPrefWidth(50);
        
        TableColumn<VertexEntry, Double> yCol = new TableColumn<>("Y");
        yCol.setCellValueFactory(new PropertyValueFactory<>("y"));
        yCol.setPrefWidth(50);
        
        TableColumn<VertexEntry, String> weightsCol = new TableColumn<>("Weights");
        weightsCol.setCellValueFactory(new PropertyValueFactory<>("weights"));
        weightsCol.setPrefWidth(80);
        
        vertexTable.getColumns().addAll(idxCol, xCol, yCol, weightsCol);
        
        TitledPane vertexPane = new TitledPane("Vertices", vertexTable);
        vertexPane.setExpanded(false);
        
        // === Weight Painting Section ===
        GridPane weightGrid = new GridPane();
        weightGrid.setHgap(10);
        weightGrid.setVgap(8);
        
        row = 0;
        
        weightBoneCombo = new ComboBox<>();
        weightBoneCombo.setPromptText("Select bone");
        weightBoneCombo.setMaxWidth(Double.MAX_VALUE);
        weightGrid.add(new Label("Bone:"), 0, row);
        weightGrid.add(weightBoneCombo, 1, row++);
        
        weightSlider = new Slider(0, 1, 1);
        weightSlider.setShowTickLabels(true);
        weightSlider.setMajorTickUnit(0.25);
        Label weightValueLabel = new Label("1.00");
        weightSlider.valueProperty().addListener((obs, old, val) -> {
            weightValueLabel.setText(String.format("%.2f", val.doubleValue()));
        });
        HBox weightRow = new HBox(5, weightSlider, weightValueLabel);
        HBox.setHgrow(weightSlider, Priority.ALWAYS);
        weightGrid.add(new Label("Weight:"), 0, row);
        weightGrid.add(weightRow, 1, row++);
        
        brushSizeSlider = new Slider(5, 100, 30);
        brushSizeSlider.setShowTickLabels(true);
        brushSizeSlider.setMajorTickUnit(25);
        Label brushSizeLabel = new Label("30");
        brushSizeSlider.valueProperty().addListener((obs, old, val) -> {
            brushSizeLabel.setText(String.format("%.0f", val.doubleValue()));
        });
        HBox brushRow = new HBox(5, brushSizeSlider, brushSizeLabel);
        HBox.setHgrow(brushSizeSlider, Priority.ALWAYS);
        weightGrid.add(new Label("Brush:"), 0, row);
        weightGrid.add(brushRow, 1, row++);
        
        // Paint mode toggles
        paintModeGroup = new ToggleGroup();
        RadioButton addMode = new RadioButton("Add");
        addMode.setToggleGroup(paintModeGroup);
        addMode.setSelected(true);
        addMode.setUserData("add");
        
        RadioButton subtractMode = new RadioButton("Subtract");
        subtractMode.setToggleGroup(paintModeGroup);
        subtractMode.setUserData("subtract");
        
        RadioButton replaceMode = new RadioButton("Replace");
        replaceMode.setToggleGroup(paintModeGroup);
        replaceMode.setUserData("replace");
        
        HBox modeBox = new HBox(10, addMode, subtractMode, replaceMode);
        weightGrid.add(new Label("Mode:"), 0, row);
        weightGrid.add(modeBox, 1, row++);
        
        // Normalize button
        Button normalizeButton = new Button("Normalize Weights");
        normalizeButton.setTooltip(new Tooltip("Ensure all vertex weights sum to 1.0"));
        normalizeButton.setOnAction(e -> normalizeWeights());
        normalizeButton.setMaxWidth(Double.MAX_VALUE);
        weightGrid.add(normalizeButton, 0, row, 2, 1);
        row++;
        
        // Auto-weight button
        Button autoWeightButton = new Button("Auto-Generate Weights");
        autoWeightButton.setTooltip(new Tooltip("Automatically assign weights based on bone proximity"));
        autoWeightButton.setOnAction(e -> autoGenerateWeights());
        autoWeightButton.setMaxWidth(Double.MAX_VALUE);
        weightGrid.add(autoWeightButton, 0, row, 2, 1);
        
        TitledPane weightPane = new TitledPane("Weight Painting", weightGrid);
        weightPane.setExpanded(true);
        
        // === Mesh Tools Section ===
        VBox toolsBox = new VBox(5);
        
        Button addVertexButton = new Button("Add Vertex Mode (A)");
        addVertexButton.setMaxWidth(Double.MAX_VALUE);
        addVertexButton.setOnAction(e -> setMeshToolMode("add_vertex"));
        
        Button createTriButton = new Button("Create Triangle Mode (T)");
        createTriButton.setMaxWidth(Double.MAX_VALUE);
        createTriButton.setOnAction(e -> setMeshToolMode("create_triangle"));
        
        Button selectButton = new Button("Select Mode (S)");
        selectButton.setMaxWidth(Double.MAX_VALUE);
        selectButton.setOnAction(e -> setMeshToolMode("select"));
        
        Button moveButton = new Button("Move Vertex Mode (G)");
        moveButton.setMaxWidth(Double.MAX_VALUE);
        moveButton.setOnAction(e -> setMeshToolMode("move"));
        
        Button paintButton = new Button("Weight Paint Mode (W)");
        paintButton.setMaxWidth(Double.MAX_VALUE);
        paintButton.setOnAction(e -> setMeshToolMode("weight_paint"));
        
        toolsBox.getChildren().addAll(selectButton, addVertexButton, createTriButton, moveButton, paintButton);
        
        TitledPane toolsPane = new TitledPane("Mesh Tools", toolsBox);
        toolsPane.setExpanded(true);
        
        // === Layout ===
        VBox content = new VBox(10, meshListSection, propsPane, vertexPane, weightPane, toolsPane);
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        setCenter(scrollPane);
        
        // Initial state
        setPropertiesEnabled(false);
        
        // Listen for skeleton changes
        EditorContext.getInstance().currentSkeletonProperty().addListener((obs, old, skeleton) -> {
            refreshBoneList();
            refreshMeshList();
        });
    }
    
    /**
     * Set the skeleton to display meshes for.
     * This will refresh the mesh list and bone combo.
     */
    public void setSkeleton(Skeleton skeleton) {
        refreshBoneList();
        refreshMeshList();
    }
    
    private void refreshBoneList() {
        weightBoneCombo.getItems().clear();
        
        Skeleton skeleton = EditorContext.getInstance().getCurrentSkeleton();
        if (skeleton == null) return;
        
        for (Bone bone : skeleton.getBones()) {
            weightBoneCombo.getItems().add(bone.getName());
        }
    }
    
    public void refreshMeshList() {
        meshEntries.clear();
        
        Skeleton skeleton = EditorContext.getInstance().getCurrentSkeleton();
        if (skeleton == null) return;
        
        // Get meshes from skeleton (assuming skeleton has mesh collection)
        List<DeformableMesh> meshes = skeleton.getMeshes();
        if (meshes != null) {
            for (DeformableMesh mesh : meshes) {
                meshEntries.add(new MeshEntry(mesh));
            }
        }
    }
    
    private void selectMesh(DeformableMesh mesh) {
        selectedMesh = mesh;
        setPropertiesEnabled(mesh != null);
        
        if (mesh != null) {
            updatingUI = true;
            try {
                meshNameField.setText(mesh.getName());
                vertexCountLabel.setText(String.valueOf(mesh.getVertexCount()));
                triangleCountLabel.setText(String.valueOf(mesh.getTriangleCount()));
                
                // Populate vertex table
                refreshVertexTable();
            } finally {
                updatingUI = false;
            }
        }
    }
    
    private void refreshVertexTable() {
        vertexEntries.clear();
        
        if (selectedMesh == null) return;
        
        List<MeshVertex> vertices = selectedMesh.getVertices();
        for (int i = 0; i < vertices.size(); i++) {
            vertexEntries.add(new VertexEntry(i, vertices.get(i)));
        }
    }
    
    private void setPropertiesEnabled(boolean enabled) {
        meshNameField.setDisable(!enabled);
        weightBoneCombo.setDisable(!enabled);
        weightSlider.setDisable(!enabled);
        brushSizeSlider.setDisable(!enabled);
    }
    
    private void createNewMesh() {
        Skeleton skeleton = EditorContext.getInstance().getCurrentSkeleton();
        if (skeleton == null) {
            showAlert("No skeleton loaded");
            return;
        }
        
        String name = "Mesh_" + (skeleton.getMeshes().size() + 1);
        DeformableMesh mesh = new DeformableMesh(name);
        skeleton.addMesh(mesh);
        
        refreshMeshList();
        
        // Select the new mesh
        for (MeshEntry entry : meshEntries) {
            if (entry.getMesh() == mesh) {
                meshTable.getSelectionModel().select(entry);
                break;
            }
        }
    }
    
    private void removeSelectedMesh() {
        if (selectedMesh == null) return;
        
        Skeleton skeleton = EditorContext.getInstance().getCurrentSkeleton();
        if (skeleton == null) return;
        
        skeleton.removeMesh(selectedMesh);
        selectedMesh = null;
        refreshMeshList();
        setPropertiesEnabled(false);
    }
    
    private void duplicateSelectedMesh() {
        if (selectedMesh == null) return;
        
        Skeleton skeleton = EditorContext.getInstance().getCurrentSkeleton();
        if (skeleton == null) return;
        
        DeformableMesh duplicate = selectedMesh.duplicate();
        duplicate.setName(selectedMesh.getName() + "_copy");
        skeleton.addMesh(duplicate);
        
        refreshMeshList();
    }
    
    private void normalizeWeights() {
        if (selectedMesh == null) return;
        
        selectedMesh.normalizeWeights();
        refreshVertexTable();
    }
    
    private void autoGenerateWeights() {
        if (selectedMesh == null) return;
        
        Skeleton skeleton = EditorContext.getInstance().getCurrentSkeleton();
        if (skeleton == null) return;
        
        selectedMesh.autoGenerateWeights(skeleton);
        refreshVertexTable();
    }
    
    private void setMeshToolMode(String mode) {
        // This would communicate with ToolManager to set the mesh tool mode
        EditorContext.getInstance().setStatusMessage("Mesh tool mode: " + mode);
    }
    
    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING, message, ButtonType.OK);
        alert.showAndWait();
    }
    
    // === Accessor Methods ===
    
    public String getSelectedBoneName() {
        return weightBoneCombo.getValue();
    }
    
    public double getPaintWeight() {
        return weightSlider.getValue();
    }
    
    public double getBrushSize() {
        return brushSizeSlider.getValue();
    }
    
    public String getPaintMode() {
        Toggle selected = paintModeGroup.getSelectedToggle();
        return selected != null ? (String) selected.getUserData() : "add";
    }
    
    /**
     * Table entry for meshes.
     */
    public static class MeshEntry {
        private final DeformableMesh mesh;
        private final SimpleStringProperty name;
        private final SimpleIntegerProperty vertexCount;
        private final SimpleIntegerProperty triangleCount;
        
        public MeshEntry(DeformableMesh mesh) {
            this.mesh = mesh;
            this.name = new SimpleStringProperty(mesh.getName());
            this.vertexCount = new SimpleIntegerProperty(mesh.getVertexCount());
            this.triangleCount = new SimpleIntegerProperty(mesh.getTriangleCount());
        }
        
        public DeformableMesh getMesh() { return mesh; }
        public String getName() { return name.get(); }
        public int getVertexCount() { return vertexCount.get(); }
        public int getTriangleCount() { return triangleCount.get(); }
    }
    
    /**
     * Table entry for vertices.
     */
    public static class VertexEntry {
        private final SimpleIntegerProperty index;
        private final SimpleDoubleProperty x;
        private final SimpleDoubleProperty y;
        private final SimpleStringProperty weights;
        
        public VertexEntry(int index, MeshVertex vertex) {
            this.index = new SimpleIntegerProperty(index);
            this.x = new SimpleDoubleProperty(vertex.getX());
            this.y = new SimpleDoubleProperty(vertex.getY());
            
            // Format weights as string
            StringBuilder sb = new StringBuilder();
            float[] w = vertex.getBoneWeights();
            int[] bi = vertex.getBoneIndices();
            for (int i = 0; i < w.length && i < bi.length; i++) {
                if (w[i] > 0.01f) {
                    if (sb.length() > 0) sb.append(", ");
                    sb.append(bi[i]).append(":").append(String.format("%.2f", w[i]));
                }
            }
            this.weights = new SimpleStringProperty(sb.toString());
        }
        
        public int getIndex() { return index.get(); }
        public double getX() { return x.get(); }
        public double getY() { return y.get(); }
        public String getWeights() { return weights.get(); }
    }
}
