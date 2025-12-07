package com.animstudio.editor.help;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.web.WebView;

import java.util.*;

/**
 * Help system providing documentation and tutorials.
 */
public class HelpSystem {
    
    private static HelpSystem instance;
    private final Map<String, HelpTopic> topics;
    
    public static HelpSystem getInstance() {
        if (instance == null) {
            instance = new HelpSystem();
        }
        return instance;
    }
    
    private HelpSystem() {
        topics = new LinkedHashMap<>();
        initializeTopics();
    }
    
    private void initializeTopics() {
        // Getting Started
        addTopic("getting-started", "Getting Started", """
            <h1>Getting Started with AnimStudio</h1>
            <p>Welcome to AnimStudio, a professional 2D skeletal animation software.</p>
            
            <h2>Creating Your First Animation</h2>
            <ol>
                <li><strong>Create a Skeleton</strong> - Use the Bone Tool (B) to create bones</li>
                <li><strong>Build Hierarchy</strong> - Parent bones together to form a skeleton</li>
                <li><strong>Set Up Animation</strong> - Create an animation clip in the timeline</li>
                <li><strong>Add Keyframes</strong> - Press K to add keyframes at different positions</li>
                <li><strong>Preview</strong> - Press Space to play your animation</li>
            </ol>
            
            <h2>Interface Overview</h2>
            <ul>
                <li><strong>Canvas</strong> - Central area for viewing and editing</li>
                <li><strong>Timeline</strong> - Bottom panel for animation editing</li>
                <li><strong>Hierarchy</strong> - Left panel showing bone structure</li>
                <li><strong>Properties</strong> - Right panel for bone properties</li>
            </ul>
            """, "basics");
        
        addTopic("bones", "Working with Bones", """
            <h1>Working with Bones</h1>
            
            <h2>Creating Bones</h2>
            <p>Select the Bone Tool (B) and click-drag on the canvas to create bones.</p>
            <ul>
                <li>Click to set the bone's origin point</li>
                <li>Drag to set the bone's length and direction</li>
                <li>Release to complete the bone</li>
            </ul>
            
            <h2>Bone Hierarchy</h2>
            <p>Bones can be parented to create hierarchical structures:</p>
            <ul>
                <li>Select a bone and use Ctrl+P to set its parent</li>
                <li>Child bones move with their parents</li>
                <li>The root bone is typically at the character's center</li>
            </ul>
            
            <h2>Transforming Bones</h2>
            <ul>
                <li><strong>Move (M)</strong> - Translate bone position</li>
                <li><strong>Rotate (R)</strong> - Rotate bone around pivot</li>
                <li><strong>Scale (S)</strong> - Scale bone length</li>
            </ul>
            """, "basics");
        
        addTopic("animation", "Animation Basics", """
            <h1>Animation Basics</h1>
            
            <h2>Timeline Overview</h2>
            <p>The timeline shows your animation over time. Each row represents a bone,
            and keyframes appear as diamonds on the timeline.</p>
            
            <h2>Creating Keyframes</h2>
            <ol>
                <li>Select a bone</li>
                <li>Move the playhead to the desired frame</li>
                <li>Transform the bone (move, rotate, scale)</li>
                <li>Press K to add a keyframe</li>
            </ol>
            
            <h2>Easing Functions</h2>
            <p>Easing controls how values change between keyframes:</p>
            <ul>
                <li><strong>Linear</strong> - Constant speed</li>
                <li><strong>Ease In</strong> - Slow start</li>
                <li><strong>Ease Out</strong> - Slow end</li>
                <li><strong>Ease In-Out</strong> - Slow start and end</li>
            </ul>
            
            <h2>Playback Controls</h2>
            <ul>
                <li><strong>Space</strong> - Play/Pause</li>
                <li><strong>Home</strong> - Go to first frame</li>
                <li><strong>End</strong> - Go to last frame</li>
                <li><strong>,</strong> - Previous frame</li>
                <li><strong>.</strong> - Next frame</li>
            </ul>
            """, "animation");
        
        addTopic("ik", "Inverse Kinematics", """
            <h1>Inverse Kinematics (IK)</h1>
            
            <h2>What is IK?</h2>
            <p>Inverse Kinematics allows you to pose bone chains by moving the end effector.
            The system automatically calculates the rotation of intermediate bones.</p>
            
            <h2>Setting Up IK</h2>
            <ol>
                <li>Select the IK Tool (I)</li>
                <li>Click on the end bone of a chain</li>
                <li>Configure chain length and constraints</li>
                <li>Drag the IK target to pose the chain</li>
            </ol>
            
            <h2>IK Constraints</h2>
            <ul>
                <li><strong>Chain Length</strong> - Number of bones affected</li>
                <li><strong>Angle Limits</strong> - Restrict rotation range</li>
                <li><strong>Pole Target</strong> - Control bend direction</li>
            </ul>
            """, "advanced");
        
        addTopic("mesh", "Mesh Deformation", """
            <h1>Mesh Deformation</h1>
            
            <h2>Creating Meshes</h2>
            <p>Meshes allow you to deform images with bones:</p>
            <ol>
                <li>Import an image</li>
                <li>Use the Mesh Tool (E) to create vertices</li>
                <li>Connect vertices to form triangles</li>
                <li>Bind vertices to bones</li>
            </ol>
            
            <h2>Skinning</h2>
            <p>Skinning determines how bones influence mesh vertices:</p>
            <ul>
                <li><strong>Rigid</strong> - Vertex follows one bone</li>
                <li><strong>Smooth</strong> - Vertex blends between multiple bones</li>
            </ul>
            
            <h2>Weight Painting</h2>
            <p>Adjust bone influence by painting weights on vertices:</p>
            <ul>
                <li>Select a bone</li>
                <li>Enable weight painting mode</li>
                <li>Paint to increase/decrease influence</li>
            </ul>
            """, "advanced");
        
        addTopic("export", "Exporting Animations", """
            <h1>Exporting Animations</h1>
            
            <h2>Export Formats</h2>
            <ul>
                <li><strong>PNG Sequence</strong> - Individual frames as PNG files</li>
                <li><strong>GIF</strong> - Animated GIF (limited colors)</li>
                <li><strong>Video</strong> - MP4/WebM video file</li>
                <li><strong>Sprite Sheet</strong> - All frames in a single image</li>
            </ul>
            
            <h2>Export Settings</h2>
            <ul>
                <li><strong>Resolution</strong> - Output image size</li>
                <li><strong>Frame Rate</strong> - Frames per second</li>
                <li><strong>Quality</strong> - Compression quality</li>
                <li><strong>Background</strong> - Transparent or solid</li>
            </ul>
            
            <h2>Sprite Sheet Options</h2>
            <ul>
                <li><strong>Grid Layout</strong> - Frames in rows and columns</li>
                <li><strong>Strip Layout</strong> - Frames in a single row</li>
                <li><strong>JSON Data</strong> - Frame coordinates for game engines</li>
            </ul>
            """, "export");
        
        addTopic("shortcuts", "Keyboard Shortcuts", """
            <h1>Keyboard Shortcuts</h1>
            
            <h2>File Operations</h2>
            <ul>
                <li><strong>Ctrl+N</strong> - New Project</li>
                <li><strong>Ctrl+O</strong> - Open Project</li>
                <li><strong>Ctrl+S</strong> - Save Project</li>
                <li><strong>Ctrl+Shift+S</strong> - Save As</li>
                <li><strong>Ctrl+E</strong> - Export</li>
            </ul>
            
            <h2>Edit Operations</h2>
            <ul>
                <li><strong>Ctrl+Z</strong> - Undo</li>
                <li><strong>Ctrl+Y</strong> - Redo</li>
                <li><strong>Delete</strong> - Delete selected</li>
                <li><strong>Ctrl+A</strong> - Select All</li>
            </ul>
            
            <h2>Tools</h2>
            <ul>
                <li><strong>V</strong> - Select Tool</li>
                <li><strong>M</strong> - Move Tool</li>
                <li><strong>R</strong> - Rotate Tool</li>
                <li><strong>S</strong> - Scale Tool</li>
                <li><strong>B</strong> - Bone Tool</li>
                <li><strong>I</strong> - IK Tool</li>
            </ul>
            
            <h2>Animation</h2>
            <ul>
                <li><strong>Space</strong> - Play/Pause</li>
                <li><strong>K</strong> - Add Keyframe</li>
                <li><strong>Home</strong> - First Frame</li>
                <li><strong>End</strong> - Last Frame</li>
            </ul>
            """, "reference");
    }
    
    private void addTopic(String id, String title, String content, String category) {
        topics.put(id, new HelpTopic(id, title, content, category));
    }
    
    public HelpTopic getTopic(String id) {
        return topics.get(id);
    }
    
    public Collection<HelpTopic> getAllTopics() {
        return topics.values();
    }
    
    public Map<String, List<HelpTopic>> getTopicsByCategory() {
        Map<String, List<HelpTopic>> byCategory = new LinkedHashMap<>();
        
        for (HelpTopic topic : topics.values()) {
            byCategory.computeIfAbsent(topic.category, k -> new ArrayList<>()).add(topic);
        }
        
        return byCategory;
    }
    
    /**
     * Search topics for matching content.
     */
    public List<HelpTopic> search(String query) {
        String lowerQuery = query.toLowerCase();
        List<HelpTopic> results = new ArrayList<>();
        
        for (HelpTopic topic : topics.values()) {
            if (topic.title.toLowerCase().contains(lowerQuery) ||
                topic.content.toLowerCase().contains(lowerQuery)) {
                results.add(topic);
            }
        }
        
        return results;
    }
    
    /**
     * Represents a help topic.
     */
    public static class HelpTopic {
        public final String id;
        public final String title;
        public final String content;
        public final String category;
        
        public HelpTopic(String id, String title, String content, String category) {
            this.id = id;
            this.title = title;
            this.content = content;
            this.category = category;
        }
        
        public String getCategoryDisplayName() {
            return switch (category) {
                case "basics" -> "Basics";
                case "animation" -> "Animation";
                case "advanced" -> "Advanced";
                case "export" -> "Export";
                case "reference" -> "Reference";
                default -> "Other";
            };
        }
    }
}
