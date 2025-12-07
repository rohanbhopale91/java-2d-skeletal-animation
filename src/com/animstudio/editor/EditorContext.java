package com.animstudio.editor;

import com.animstudio.core.animation.AnimationClip;
import com.animstudio.core.animation.AnimationState;
import com.animstudio.core.event.EngineEventBus;
import com.animstudio.core.event.events.AnimationChangedEvent;
import com.animstudio.core.event.events.SelectionChangedEvent;
import com.animstudio.core.event.events.SkeletonChangedEvent;
import com.animstudio.core.ik.IKManager;
import com.animstudio.core.mesh.DeformableMesh;
import com.animstudio.core.model.AssetLibrary;
import com.animstudio.core.model.Bone;
import com.animstudio.core.model.Skeleton;
import com.animstudio.core.util.TimeUtils;
import com.animstudio.editor.commands.CommandStack;
import com.animstudio.editor.project.AnimationProject;
import com.animstudio.editor.project.Project;
import javafx.beans.property.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Singleton holding all editor state and providing access to core systems.
 * 
 * TIME CONVENTION:
 * - currentTime is in SECONDS (double)
 * - frameRate defines frames per second (default 30 FPS)
 * - UI may display frames but internally all times are seconds
 * - Use TimeUtils for conversions between frames and seconds
 */
public class EditorContext {
    
    private static EditorContext instance;
    
    // Core systems
    private final EngineEventBus eventBus;
    private final CommandStack commandStack;
    
    // Current project
    private Project currentProject;
    private File projectFile;
    
    // Selection state
    private final List<Object> selection;
    
    // Observable properties for UI binding
    private final ObjectProperty<Skeleton> currentSkeleton;
    private final ObjectProperty<AnimationClip> currentAnimation;
    private final ObjectProperty<Bone> selectedBone;
    private final DoubleProperty currentTime;      // Time in SECONDS
    private final BooleanProperty playing;
    private final BooleanProperty modified;
    
    // Animation playback
    private AnimationState animationState;
    private long lastUpdateTime;
    private double frameRate = TimeUtils.DEFAULT_FRAME_RATE;  // Frames per second
    
    // Controller reference
    private MainWindowController mainController;
    
    // IK and Mesh support
    private IKManager ikManager;
    private final List<DeformableMesh> meshes = new ArrayList<>();
    private String statusMessage = "";
    
    // Asset library
    private final AssetLibrary assetLibrary = new AssetLibrary();
    
    private EditorContext() {
        this.eventBus = EngineEventBus.getInstance();
        this.commandStack = new CommandStack(100);
        this.selection = new ArrayList<>();
        
        this.currentSkeleton = new SimpleObjectProperty<>();
        this.currentAnimation = new SimpleObjectProperty<>();
        this.selectedBone = new SimpleObjectProperty<>();
        this.currentTime = new SimpleDoubleProperty(0);  // 0 seconds
        this.playing = new SimpleBooleanProperty(false);
        this.modified = new SimpleBooleanProperty(false);
        
        // Mark as modified when commands are executed
        commandStack.addListener(cmd -> modified.set(true));
    }
    
    public static synchronized EditorContext getInstance() {
        if (instance == null) {
            instance = new EditorContext();
        }
        return instance;
    }
    
    // === Project Management ===
    
    public void newProject() {
        currentProject = new Project("Untitled");
        projectFile = null;
        modified.set(false);
        commandStack.clear();
        
        // Create default skeleton
        Skeleton skeleton = createDefaultSkeleton();
        currentProject.setSkeleton(skeleton);
        currentSkeleton.set(skeleton);
        
        // Create default animation (2 seconds duration)
        AnimationClip idleAnim = new AnimationClip("idle");
        idleAnim.setDuration(2.0);  // 2 seconds (60 frames at 30 FPS)
        currentProject.addAnimation(idleAnim);
        setCurrentAnimation(idleAnim);
        
        // Setup animation state
        animationState = new AnimationState(skeleton);
        
        eventBus.publish(new SkeletonChangedEvent(skeleton, SkeletonChangedEvent.ChangeType.LOADED));
    }
    
    /**
     * Load a project from AnimationProject data structure.
     */
    public void loadProject(AnimationProject project) {
        currentProject = new Project(project.getName());
        projectFile = null;
        modified.set(false);
        commandStack.clear();
        
        // Load skeleton
        Skeleton skeleton = project.getSkeleton();
        if (skeleton == null) {
            skeleton = createDefaultSkeleton();
        }
        currentProject.setSkeleton(skeleton);
        currentSkeleton.set(skeleton);
        
        // Load animations
        for (AnimationClip clip : project.getAnimations()) {
            currentProject.addAnimation(clip);
        }
        if (!project.getAnimations().isEmpty()) {
            setCurrentAnimation(project.getAnimations().get(0));
        }
        
        // Setup animation state
        animationState = new AnimationState(skeleton);
        
        eventBus.publish(new SkeletonChangedEvent(skeleton, SkeletonChangedEvent.ChangeType.LOADED));
    }
    
    /**
     * Create an AnimationProject from current state for saving.
     */
    public AnimationProject createProject() {
        AnimationProject project = new AnimationProject();
        project.setName(currentProject != null ? currentProject.getName() : "Untitled");
        project.setSkeleton(currentSkeleton.get());
        if (currentProject != null) {
            for (AnimationClip clip : currentProject.getAnimations()) {
                project.addAnimation(clip);
            }
        }
        return project;
    }
    
    /**
     * Set the modified/dirty flag.
     */
    public void setModified(boolean mod) {
        modified.set(mod);
    }
    
    private Skeleton createDefaultSkeleton() {
        Skeleton skeleton = new Skeleton("Character");
        
        Bone root = new Bone("root");
        root.setY(200);
        root.setToSetupPose();
        skeleton.addBone(root);
        
        Bone spine = new Bone("spine");
        spine.setParent(root);
        spine.setY(-50);
        spine.setLength(60);
        spine.setToSetupPose();
        skeleton.addBone(spine);
        
        Bone head = new Bone("head");
        head.setParent(spine);
        head.setY(-60);
        head.setLength(40);
        head.setToSetupPose();
        skeleton.addBone(head);
        
        Bone armL = new Bone("arm_L");
        armL.setParent(spine);
        armL.setX(-30);
        armL.setRotation(-45);
        armL.setLength(50);
        armL.setToSetupPose();
        skeleton.addBone(armL);
        
        Bone armR = new Bone("arm_R");
        armR.setParent(spine);
        armR.setX(30);
        armR.setRotation(45);
        armR.setLength(50);
        armR.setToSetupPose();
        skeleton.addBone(armR);
        
        Bone legL = new Bone("leg_L");
        legL.setParent(root);
        legL.setX(-15);
        legL.setY(10);
        legL.setRotation(170);
        legL.setLength(60);
        legL.setToSetupPose();
        skeleton.addBone(legL);
        
        Bone legR = new Bone("leg_R");
        legR.setParent(root);
        legR.setX(15);
        legR.setY(10);
        legR.setRotation(190);
        legR.setLength(60);
        legR.setToSetupPose();
        skeleton.addBone(legR);
        
        skeleton.updateWorldTransforms();
        return skeleton;
    }
    
    public Project getCurrentProject() {
        return currentProject;
    }
    
    public boolean confirmClose() {
        if (!modified.get()) return true;
        // TODO: Show save dialog
        return true;
    }
    
    public void shutdown() {
        playing.set(false);
    }
    
    // === Selection ===
    
    public void select(Object... items) {
        Object[] previous = selection.toArray();
        selection.clear();
        for (Object item : items) {
            if (item != null) selection.add(item);
        }
        
        // Update selected bone property
        Bone bone = null;
        for (Object item : selection) {
            if (item instanceof Bone) {
                bone = (Bone) item;
                break;
            }
        }
        selectedBone.set(bone);
        
        eventBus.publish(new SelectionChangedEvent(this, selection.toArray(), previous));
    }
    
    public void clearSelection() {
        select();
    }
    
    public List<Object> getSelection() {
        return new ArrayList<>(selection);
    }
    
    public boolean isSelected(Object item) {
        return selection.contains(item);
    }
    
    // === Animation ===
    
    public void setCurrentAnimation(AnimationClip clip) {
        currentAnimation.set(clip);
        currentTime.set(0);  // Reset to 0 seconds
        if (animationState != null && clip != null) {
            animationState.setAnimation(0, clip, clip.isLooping());
        }
        eventBus.publish(new AnimationChangedEvent(clip, AnimationChangedEvent.ChangeType.SELECTED));
    }
    
    /**
     * Set the current playback time.
     * @param timeSeconds Time in seconds
     */
    public void setCurrentTime(double timeSeconds) {
        currentTime.set(timeSeconds);
        if (animationState != null) {
            animationState.setTime(timeSeconds);
            animationState.apply();
        }
        eventBus.publish(new AnimationChangedEvent(currentAnimation.get(), 
            AnimationChangedEvent.ChangeType.TIME_CHANGED, timeSeconds));
    }
    
    /**
     * Set the current time from a frame number.
     * @param frame Frame number (converted to seconds using frameRate)
     */
    public void setCurrentFrame(int frame) {
        setCurrentTime(TimeUtils.framesToSeconds(frame, frameRate));
    }
    
    /**
     * Get current time as a frame number.
     * @return Current frame (rounded to nearest integer)
     */
    public int getCurrentFrame() {
        return TimeUtils.secondsToNearestFrame(currentTime.get(), frameRate);
    }
    
    public void togglePlayback() {
        playing.set(!playing.get());
        if (playing.get()) {
            lastUpdateTime = System.nanoTime();
        }
    }
    
    /**
     * Update animation playback by advancing time.
     * Called from the animation timer in MainWindowController.
     * Time is advanced in SECONDS based on real elapsed time.
     */
    public void updatePlayback() {
        if (!playing.get() || animationState == null) return;
        
        long now = System.nanoTime();
        double deltaSeconds = (now - lastUpdateTime) / 1_000_000_000.0;
        lastUpdateTime = now;
        
        // Advance time in seconds (not frames!)
        double newTime = currentTime.get() + deltaSeconds;
        
        AnimationClip clip = currentAnimation.get();
        if (clip != null) {
            // clip.getDuration() is now in seconds
            if (clip.isLooping()) {
                newTime = newTime % clip.getDuration();
            } else if (newTime >= clip.getDuration()) {
                newTime = clip.getDuration();
                playing.set(false);
            }
        }
        
        setCurrentTime(newTime);
    }
    
    // === Getters for Properties ===
    
    // === Getters for Properties ===
    
    public EngineEventBus getEventBus() { return eventBus; }
    public CommandStack getCommandStack() { return commandStack; }
    
    public ObjectProperty<Skeleton> currentSkeletonProperty() { return currentSkeleton; }
    public Skeleton getCurrentSkeleton() { return currentSkeleton.get(); }
    
    public ObjectProperty<AnimationClip> currentAnimationProperty() { return currentAnimation; }
    public AnimationClip getCurrentAnimation() { return currentAnimation.get(); }
    
    public ObjectProperty<Bone> selectedBoneProperty() { return selectedBone; }
    public Bone getSelectedBone() { return selectedBone.get(); }
    
    /** Current time property (in seconds) for UI binding */
    public DoubleProperty currentTimeProperty() { return currentTime; }
    
    /** Get current playback time in seconds */
    public double getCurrentTime() { return currentTime.get(); }
    
    /** Get current playback time in seconds (alias for clarity) */
    public double getCurrentTimeSeconds() { return currentTime.get(); }
    
    public BooleanProperty playingProperty() { return playing; }
    public boolean isPlaying() { return playing.get(); }
    
    public BooleanProperty modifiedProperty() { return modified; }
    public boolean isModified() { return modified.get(); }
    
    /** Get frame rate in frames per second */
    public double getFrameRate() { return frameRate; }
    
    /** Set frame rate in frames per second */
    public void setFrameRate(double fps) { this.frameRate = fps; }
    
    public MainWindowController getMainController() { return mainController; }
    public void setMainController(MainWindowController controller) { this.mainController = controller; }
    
    public AnimationState getAnimationState() { return animationState; }
    
    // === IK Support ===
    
    public IKManager getIKManager() { return ikManager; }
    public void setIKManager(IKManager manager) { this.ikManager = manager; }
    
    // === Mesh Support ===
    
    public List<DeformableMesh> getMeshes() { return meshes; }
    
    public void addMesh(DeformableMesh mesh) {
        if (mesh != null && !meshes.contains(mesh)) {
            meshes.add(mesh);
        }
    }
    
    public void removeMesh(DeformableMesh mesh) {
        meshes.remove(mesh);
    }
    
    // === Status ===
    
    public void setStatusMessage(String message) { 
        this.statusMessage = message;
        if (mainController != null) {
            mainController.setStatusText(message);
        }
    }
    
    public String getStatusMessage() { return statusMessage; }
    
    // === Asset Library ===
    
    public AssetLibrary getAssetLibrary() { return assetLibrary; }
    
    // === Tool Support ===
    
    public Skeleton getActiveSkeleton() { return currentSkeleton.get(); }
}
