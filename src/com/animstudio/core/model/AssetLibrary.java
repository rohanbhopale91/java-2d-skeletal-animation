package com.animstudio.core.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Manages the collection of assets (sprites/images) for a project.
 * Provides lookup, add, remove operations.
 */
public class AssetLibrary {
    
    private final List<AssetEntry> assets = new ArrayList<>();
    private final List<AssetLibraryListener> listeners = new ArrayList<>();
    
    /**
     * Adds an asset to the library.
     */
    public void addAsset(AssetEntry asset) {
        if (asset != null && !assets.contains(asset)) {
            assets.add(asset);
            fireAssetAdded(asset);
        }
    }
    
    /**
     * Removes an asset from the library.
     */
    public boolean removeAsset(AssetEntry asset) {
        boolean removed = assets.remove(asset);
        if (removed) {
            fireAssetRemoved(asset);
        }
        return removed;
    }
    
    /**
     * Removes an asset by ID.
     */
    public boolean removeAssetById(String id) {
        Optional<AssetEntry> asset = findById(id);
        return asset.map(this::removeAsset).orElse(false);
    }
    
    /**
     * Finds an asset by ID.
     */
    public Optional<AssetEntry> findById(String id) {
        return assets.stream()
            .filter(a -> a.getId().equals(id))
            .findFirst();
    }
    
    /**
     * Finds an asset by name (case-insensitive).
     */
    public Optional<AssetEntry> findByName(String name) {
        return assets.stream()
            .filter(a -> a.getName().equalsIgnoreCase(name))
            .findFirst();
    }
    
    /**
     * Gets all assets (unmodifiable).
     */
    public List<AssetEntry> getAssets() {
        return Collections.unmodifiableList(assets);
    }
    
    /**
     * Gets only image assets.
     */
    public List<AssetEntry> getImageAssets() {
        return assets.stream()
            .filter(AssetEntry::isImage)
            .toList();
    }
    
    /**
     * Gets the number of assets.
     */
    public int size() {
        return assets.size();
    }
    
    /**
     * Clears all assets.
     */
    public void clear() {
        List<AssetEntry> copy = new ArrayList<>(assets);
        assets.clear();
        copy.forEach(this::fireAssetRemoved);
    }
    
    // === Listeners ===
    
    public void addListener(AssetLibraryListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }
    
    public void removeListener(AssetLibraryListener listener) {
        listeners.remove(listener);
    }
    
    private void fireAssetAdded(AssetEntry asset) {
        for (AssetLibraryListener l : listeners) {
            l.onAssetAdded(asset);
        }
    }
    
    private void fireAssetRemoved(AssetEntry asset) {
        for (AssetLibraryListener l : listeners) {
            l.onAssetRemoved(asset);
        }
    }
    
    /**
     * Listener interface for asset library changes.
     */
    public interface AssetLibraryListener {
        void onAssetAdded(AssetEntry asset);
        void onAssetRemoved(AssetEntry asset);
    }
}
