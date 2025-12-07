package com.animstudio.editor.commands;

import com.animstudio.core.model.AssetEntry;
import com.animstudio.core.model.Slot;
import com.animstudio.core.model.Attachment;
import com.animstudio.core.model.RegionAttachment;

/**
 * Command to attach a sprite/asset to a slot.
 * Supports undo/redo.
 */
public class AttachSpriteCommand implements Command {
    
    private final Slot slot;
    private final AssetEntry newAsset;
    private final Attachment previousAttachment;
    
    /**
     * Creates a command to attach an asset to a slot.
     * 
     * @param slot The slot to attach to
     * @param asset The asset to attach (null to clear)
     */
    public AttachSpriteCommand(Slot slot, AssetEntry asset) {
        this.slot = slot;
        this.newAsset = asset;
        
        // Store previous state for undo
        this.previousAttachment = slot.getAttachment();
    }
    
    @Override
    public void execute() {
        if (newAsset != null) {
            // Create a RegionAttachment from the asset
            RegionAttachment attachment = new RegionAttachment(newAsset.getName());
            attachment.setImagePath(newAsset.getAbsolutePath());
            attachment.setWidth(newAsset.getWidth());
            attachment.setHeight(newAsset.getHeight());
            attachment.setRegionWidth(newAsset.getWidth());
            attachment.setRegionHeight(newAsset.getHeight());
            attachment.setPivotX(newAsset.getPivotX());
            attachment.setPivotY(newAsset.getPivotY());
            
            slot.setAttachment(attachment);
        } else {
            slot.setAttachment(null);
        }
    }
    
    @Override
    public void undo() {
        slot.setAttachment(previousAttachment);
    }
    
    @Override
    public String getDescription() {
        if (newAsset != null) {
            return "Attach '" + newAsset.getName() + "' to slot '" + slot.getName() + "'";
        } else {
            return "Clear attachment from slot '" + slot.getName() + "'";
        }
    }
    
    @Override
    public boolean canMergeWith(Command other) {
        return false;
    }
    
    @Override
    public void mergeWith(Command other) {
        // No merge support
    }
}
