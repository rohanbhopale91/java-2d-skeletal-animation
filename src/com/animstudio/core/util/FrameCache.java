package com.animstudio.core.util;

import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cache for rendered frames to improve playback performance.
 */
public class FrameCache {
    
    private static FrameCache instance;
    
    private final Map<String, CachedFrame> cache;
    private final int maxFrames;
    private long cacheHits;
    private long cacheMisses;
    
    public static FrameCache getInstance() {
        if (instance == null) {
            instance = new FrameCache(100); // Default 100 frames
        }
        return instance;
    }
    
    public static FrameCache getInstance(int maxFrames) {
        if (instance == null) {
            instance = new FrameCache(maxFrames);
        }
        return instance;
    }
    
    private FrameCache(int maxFrames) {
        this.maxFrames = maxFrames;
        this.cache = new ConcurrentHashMap<>();
    }
    
    /**
     * Generate a cache key for a frame.
     */
    public String generateKey(String animationId, int frame, int width, int height) {
        return animationId + "_" + frame + "_" + width + "x" + height;
    }
    
    /**
     * Get a cached frame.
     */
    public WritableImage get(String key) {
        CachedFrame cached = cache.get(key);
        if (cached != null && !cached.isExpired()) {
            cacheHits++;
            return cached.image;
        }
        cacheMisses++;
        return null;
    }
    
    /**
     * Store a frame in cache.
     */
    public void put(String key, WritableImage image) {
        // Evict old entries if needed
        if (cache.size() >= maxFrames) {
            evictOldest();
        }
        cache.put(key, new CachedFrame(image));
    }
    
    /**
     * Check if a frame is cached.
     */
    public boolean contains(String key) {
        CachedFrame cached = cache.get(key);
        return cached != null && !cached.isExpired();
    }
    
    /**
     * Invalidate a specific frame.
     */
    public void invalidate(String key) {
        cache.remove(key);
    }
    
    /**
     * Invalidate all frames for an animation.
     */
    public void invalidateAnimation(String animationId) {
        cache.keySet().removeIf(key -> key.startsWith(animationId + "_"));
    }
    
    /**
     * Clear the entire cache.
     */
    public void clear() {
        cache.clear();
        cacheHits = 0;
        cacheMisses = 0;
    }
    
    /**
     * Get cache statistics.
     */
    public CacheStatistics getStatistics() {
        return new CacheStatistics(
            cache.size(),
            maxFrames,
            cacheHits,
            cacheMisses
        );
    }
    
    private void evictOldest() {
        String oldest = null;
        long oldestTime = Long.MAX_VALUE;
        
        for (Map.Entry<String, CachedFrame> entry : cache.entrySet()) {
            if (entry.getValue().timestamp < oldestTime) {
                oldestTime = entry.getValue().timestamp;
                oldest = entry.getKey();
            }
        }
        
        if (oldest != null) {
            cache.remove(oldest);
        }
    }
    
    private static class CachedFrame {
        final WritableImage image;
        final long timestamp;
        
        CachedFrame(WritableImage image) {
            this.image = image;
            this.timestamp = System.currentTimeMillis();
        }
        
        boolean isExpired() {
            // Frames expire after 30 seconds
            return System.currentTimeMillis() - timestamp > 30_000;
        }
    }
    
    public static class CacheStatistics {
        public final int currentSize;
        public final int maxSize;
        public final long hits;
        public final long misses;
        public final double hitRate;
        
        CacheStatistics(int currentSize, int maxSize, long hits, long misses) {
            this.currentSize = currentSize;
            this.maxSize = maxSize;
            this.hits = hits;
            this.misses = misses;
            long total = hits + misses;
            this.hitRate = total > 0 ? (double) hits / total : 0;
        }
    }
}
