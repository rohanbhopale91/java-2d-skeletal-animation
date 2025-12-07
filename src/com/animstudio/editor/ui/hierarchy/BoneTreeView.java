package com.animstudio.editor.ui.hierarchy;

import com.animstudio.core.model.Bone;
import com.animstudio.core.model.Skeleton;

import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;

/**
 * Tree view displaying the bone hierarchy.
 */
public class BoneTreeView extends TreeView<Bone> {
    
    private Skeleton skeleton;
    
    public BoneTreeView() {
        setShowRoot(true);
        
        // Custom cell factory for bone display
        setCellFactory(tv -> new BoneTreeCell());
    }
    
    public void setSkeleton(Skeleton skeleton) {
        this.skeleton = skeleton;
        
        if (skeleton == null || skeleton.getRootBone() == null) {
            setRoot(null);
            return;
        }
        
        // Build tree from skeleton hierarchy
        TreeItem<Bone> rootItem = buildTreeItem(skeleton.getRootBone());
        rootItem.setExpanded(true);
        setRoot(rootItem);
    }
    
    private TreeItem<Bone> buildTreeItem(Bone bone) {
        TreeItem<Bone> item = new TreeItem<>(bone);
        item.setExpanded(true);
        
        for (Bone child : bone.getChildren()) {
            item.getChildren().add(buildTreeItem(child));
        }
        
        return item;
    }
    
    public void selectBone(Bone bone) {
        if (bone == null) {
            getSelectionModel().clearSelection();
            return;
        }
        
        TreeItem<Bone> item = findItem(getRoot(), bone);
        if (item != null) {
            getSelectionModel().select(item);
            scrollTo(getRow(item));
        }
    }
    
    private TreeItem<Bone> findItem(TreeItem<Bone> root, Bone bone) {
        if (root == null) return null;
        if (root.getValue() == bone) return root;
        
        for (TreeItem<Bone> child : root.getChildren()) {
            TreeItem<Bone> found = findItem(child, bone);
            if (found != null) return found;
        }
        
        return null;
    }
    
    public Skeleton getSkeleton() {
        return skeleton;
    }
    
    /**
     * Custom cell for displaying bones.
     */
    private static class BoneTreeCell extends TreeCell<Bone> {
        @Override
        protected void updateItem(Bone bone, boolean empty) {
            super.updateItem(bone, empty);
            
            if (empty || bone == null) {
                setText(null);
                setGraphic(null);
            } else {
                setText(bone.getName());
                // Could add icon based on bone type/state
            }
        }
    }
}
