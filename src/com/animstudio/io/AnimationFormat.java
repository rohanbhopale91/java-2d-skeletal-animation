package com.animstudio.io;

/**
 * Defines the animation file format structure for JSON serialization.
 * This class contains nested record/class definitions that map to JSON structure.
 */
public class AnimationFormat {
    
    /**
     * Root structure of the animation project file.
     */
    public static class ProjectData {
        public MetaData meta;
        public SettingsData settings;
        public SkeletonData skeleton;
        public AnimationData[] animations;
        public MeshData[] meshes;
        public TextureData[] textures;
        public java.util.Map<String, Object> customProperties;
    }
    
    /**
     * Project metadata.
     */
    public static class MetaData {
        public int formatVersion;
        public String name;
        public String author;
        public String description;
        public String createdAt;
        public String modifiedAt;
    }
    
    /**
     * Project settings.
     */
    public static class SettingsData {
        public int frameRate;
        public double defaultDuration;
        public int canvasWidth;
        public int canvasHeight;
    }
    
    /**
     * Skeleton structure.
     */
    public static class SkeletonData {
        public String name;
        public BoneData[] bones;
    }
    
    /**
     * Individual bone data.
     */
    public static class BoneData {
        public String name;
        public String parent; // parent bone name, null for root
        public double localX;
        public double localY;
        public double length;
        public double rotation;
        public double scaleX;
        public double scaleY;
        public int zOrder;
        public String textureName;
        public double[] textureRegion; // [u, v, width, height]
    }
    
    /**
     * Animation clip data.
     */
    public static class AnimationData {
        public String name;
        public double duration;
        public boolean looping;
        public TrackData[] tracks;
        public EventData[] events;
    }
    
    /**
     * Animation track data.
     */
    public static class TrackData {
        public String targetPath;
        public String propertyType; // POSITION_X, POSITION_Y, ROTATION, SCALE_X, SCALE_Y
        public KeyframeData[] keyframes;
    }
    
    /**
     * Individual keyframe data.
     */
    public static class KeyframeData {
        public double time;
        public double value;
        public String interpolation; // LINEAR, EASE_IN, EASE_OUT, EASE_IN_OUT, BEZIER, STEP
        public double[] bezierControl; // [cx1, cy1, cx2, cy2] for bezier curves
    }
    
    /**
     * Animation event data.
     */
    public static class EventData {
        public double time;
        public String name;
        public String stringValue;
        public double numericValue;
    }
    
    /**
     * Deformable mesh data.
     */
    public static class MeshData {
        public String name;
        public String attachedBone;
        public VertexData[] vertices;
        public int[] triangles; // triplets of vertex indices
    }
    
    /**
     * Mesh vertex data.
     */
    public static class VertexData {
        public double x;
        public double y;
        public double u;
        public double v;
        public BoneWeightData[] weights;
    }
    
    /**
     * Bone weight for vertex skinning.
     */
    public static class BoneWeightData {
        public String boneName;
        public double weight;
    }
    
    /**
     * Texture reference data.
     */
    public static class TextureData {
        public String name;
        public String path;
        public int width;
        public int height;
    }
}
