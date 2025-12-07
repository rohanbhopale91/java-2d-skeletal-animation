package com.animstudio.core.util;

import java.lang.ref.SoftReference;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Memory-sensitive cache using soft references.
 * Entries can be garbage collected under memory pressure.
 */
public class SoftCache<K, V> {
    
    private final Map<K, SoftReference<V>> cache;
    private final Function<K, V> loader;
    
    public SoftCache() {
        this(null);
    }
    
    public SoftCache(Function<K, V> loader) {
        this.cache = new ConcurrentHashMap<>();
        this.loader = loader;
    }
    
    /**
     * Get a value from cache.
     */
    public V get(K key) {
        SoftReference<V> ref = cache.get(key);
        return ref != null ? ref.get() : null;
    }
    
    /**
     * Put a value in cache.
     */
    public void put(K key, V value) {
        cache.put(key, new SoftReference<>(value));
    }
    
    /**
     * Get a value, loading it if not present.
     */
    public V getOrLoad(K key) {
        SoftReference<V> ref = cache.get(key);
        V value = ref != null ? ref.get() : null;
        
        if (value == null && loader != null) {
            value = loader.apply(key);
            if (value != null) {
                put(key, value);
            }
        }
        
        return value;
    }
    
    /**
     * Remove an entry.
     */
    public void remove(K key) {
        cache.remove(key);
    }
    
    /**
     * Clear the cache.
     */
    public void clear() {
        cache.clear();
    }
    
    /**
     * Get current cache size (including potentially collected entries).
     */
    public int size() {
        return cache.size();
    }
    
    /**
     * Clean up null references.
     */
    public void cleanup() {
        cache.entrySet().removeIf(entry -> entry.getValue().get() == null);
    }
}
