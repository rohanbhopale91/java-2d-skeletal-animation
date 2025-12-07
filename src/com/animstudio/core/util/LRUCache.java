package com.animstudio.core.util;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Generic LRU cache for performance optimization.
 */
public class LRUCache<K, V> extends LinkedHashMap<K, V> {
    
    private final int maxSize;
    private Function<K, V> loader;
    
    public LRUCache(int maxSize) {
        super(maxSize, 0.75f, true);
        this.maxSize = maxSize;
    }
    
    public LRUCache(int maxSize, Function<K, V> loader) {
        this(maxSize);
        this.loader = loader;
    }
    
    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > maxSize;
    }
    
    /**
     * Get a value, loading it if not present and a loader is configured.
     */
    public V getOrLoad(K key) {
        V value = get(key);
        if (value == null && loader != null) {
            value = loader.apply(key);
            if (value != null) {
                put(key, value);
            }
        }
        return value;
    }
    
    /**
     * Get cache statistics.
     */
    public CacheStats getStats() {
        return new CacheStats(size(), maxSize);
    }
    
    public static class CacheStats {
        public final int currentSize;
        public final int maxSize;
        public final double fillRatio;
        
        public CacheStats(int currentSize, int maxSize) {
            this.currentSize = currentSize;
            this.maxSize = maxSize;
            this.fillRatio = maxSize > 0 ? (double) currentSize / maxSize : 0;
        }
    }
}
