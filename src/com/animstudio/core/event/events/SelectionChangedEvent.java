package com.animstudio.core.event.events;

import com.animstudio.core.event.EngineEvent;
import com.animstudio.core.model.Bone;

/**
 * Event fired when the selection changes in the editor.
 */
public class SelectionChangedEvent extends EngineEvent {
    
    private final Object[] selectedItems;
    private final Object[] previousSelection;
    
    public SelectionChangedEvent(Object source, Object[] selectedItems, Object[] previousSelection) {
        super(source);
        this.selectedItems = selectedItems;
        this.previousSelection = previousSelection;
    }
    
    public Object[] getSelectedItems() { return selectedItems; }
    public Object[] getPreviousSelection() { return previousSelection; }
    
    /**
     * Check if the selection contains a specific item.
     */
    public boolean contains(Object item) {
        for (Object selected : selectedItems) {
            if (selected == item) return true;
        }
        return false;
    }
    
    /**
     * Get the first selected bone, or null if no bone is selected.
     */
    public Bone getSelectedBone() {
        for (Object item : selectedItems) {
            if (item instanceof Bone) return (Bone) item;
        }
        return null;
    }
    
    /**
     * Check if selection is empty.
     */
    public boolean isEmpty() {
        return selectedItems == null || selectedItems.length == 0;
    }
}
