package com.animstudio.editor.tools;

import com.animstudio.core.ik.IKConstraint;
import com.animstudio.core.ik.IKManager;
import com.animstudio.core.model.Bone;
import com.animstudio.core.model.Skeleton;
import com.animstudio.editor.commands.Command;
import com.animstudio.editor.EditorContext;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;

/**
 * Tool for creating and manipulating IK constraints.
 */
public class IKTool extends AbstractTool {
    
    private enum Mode {
        SELECT_START,      // Selecting the start bone of IK chain
        SELECT_END,        // Selecting the end bone of IK chain
        DRAG_TARGET,       // Dragging IK target
        CREATE_TARGET      // Creating new target bone
    }
    
    private Mode mode = Mode.SELECT_START;
    
    private Bone startBone;
    private Bone endBone;
    private Bone targetBone;
    private IKConstraint activeConstraint;
    
    private double dragStartX, dragStartY;
    private double targetStartX, targetStartY;
    
    private final List<Bone> selectedChain = new ArrayList<>();
    
    // IK target visual settings
    private static final double TARGET_SIZE = 15;
    private static final Color TARGET_COLOR = Color.ORANGE;
    private static final Color CHAIN_COLOR = Color.CYAN;
    
    public IKTool() {
        super("IK Tool", ToolCategory.RIGGING);
    }
    
    @Override
    public void activate(EditorContext context) {
        super.activate(context);
        resetState();
    }
    
    @Override
    public void deactivate() {
        resetState();
        super.deactivate();
    }
    
    private void resetState() {
        mode = Mode.SELECT_START;
        startBone = null;
        endBone = null;
        targetBone = null;
        selectedChain.clear();
    }
    
    @Override
    public void onMousePressed(MouseEvent event) {
        if (context == null) return;
        
        double x = event.getX();
        double y = event.getY();
        
        Bone clickedBone = findBoneAt(x, y);
        
        switch (mode) {
            case SELECT_START:
                if (clickedBone != null) {
                    startBone = clickedBone;
                    mode = Mode.SELECT_END;
                    context.setStatusMessage("IK: Select end bone of chain");
                }
                break;
                
            case SELECT_END:
                if (clickedBone != null && clickedBone != startBone) {
                    endBone = clickedBone;
                    buildChain();
                    if (!selectedChain.isEmpty()) {
                        mode = Mode.CREATE_TARGET;
                        context.setStatusMessage("IK: Click to place target");
                    } else {
                        context.setStatusMessage("IK: Invalid chain - bones must be connected");
                        mode = Mode.SELECT_START;
                    }
                }
                break;
                
            case CREATE_TARGET:
                createIKConstraint(x, y);
                mode = Mode.DRAG_TARGET;
                context.setStatusMessage("IK: Drag target to pose. Press Enter to confirm.");
                break;
                
            case DRAG_TARGET:
                if (isOverTarget(x, y)) {
                    dragStartX = x;
                    dragStartY = y;
                    if (targetBone != null) {
                        targetStartX = targetBone.getX();
                        targetStartY = targetBone.getY();
                    }
                } else {
                    // Clicked elsewhere - check if selecting different constraint
                    IKConstraint clicked = findConstraintAt(x, y);
                    if (clicked != null && clicked != activeConstraint) {
                        activeConstraint = clicked;
                        targetBone = clicked.getTarget();
                        context.setStatusMessage("IK: Selected constraint: " + clicked.getName());
                    }
                }
                break;
        }
    }
    
    @Override
    public void onMouseDragged(MouseEvent event) {
        if (mode == Mode.DRAG_TARGET && targetBone != null) {
            double dx = event.getX() - dragStartX;
            double dy = event.getY() - dragStartY;
            
            targetBone.setX(targetStartX + dx);
            targetBone.setY(targetStartY + dy);
            
            // Apply IK
            if (activeConstraint != null) {
                activeConstraint.apply();
            }
        }
    }
    
    @Override
    public void onMouseReleased(MouseEvent event) {
        if (mode == Mode.DRAG_TARGET && activeConstraint != null) {
            // Create command for undo
            // Command would capture bone rotations before and after
        }
    }
    
    @Override
    public void onKeyPressed(KeyEvent event) {
        switch (event.getCode()) {
            case ESCAPE:
                resetState();
                context.setStatusMessage("IK: Ready - select start bone");
                break;
                
            case ENTER:
                if (activeConstraint != null) {
                    // Finalize IK - could bake rotations or keep constraint
                    context.setStatusMessage("IK: Constraint applied");
                }
                break;
                
            case DELETE:
            case BACK_SPACE:
                if (activeConstraint != null) {
                    deleteActiveConstraint();
                }
                break;
        }
    }
    
    private void buildChain() {
        selectedChain.clear();
        
        if (startBone == null || endBone == null) return;
        
        // Build chain from end to start (child to parent)
        Bone current = endBone;
        while (current != null) {
            selectedChain.add(0, current);
            if (current == startBone) {
                break;
            }
            current = current.getParent();
        }
        
        // Verify we found the start bone
        if (selectedChain.isEmpty() || selectedChain.get(0) != startBone) {
            selectedChain.clear();
        }
    }
    
    private void createIKConstraint(double targetX, double targetY) {
        if (selectedChain.isEmpty() || context == null) return;
        
        Skeleton skeleton = context.getActiveSkeleton();
        if (skeleton == null) return;
        
        // Create target bone
        String targetName = "ik_target_" + System.currentTimeMillis();
        targetBone = new Bone(targetName);
        targetBone.setX(targetX);
        targetBone.setY(targetY);
        targetBone.setLength(10);
        skeleton.addBone(targetBone);
        
        // Create IK constraint
        String constraintName = startBone.getName() + "_to_" + endBone.getName() + "_ik";
        activeConstraint = new IKConstraint(constraintName, selectedChain, targetBone);
        
        // Get or create IK manager
        IKManager ikManager = context.getIKManager();
        if (ikManager == null) {
            ikManager = new IKManager(skeleton);
            context.setIKManager(ikManager);
        }
        
        ikManager.addConstraint(activeConstraint);
        
        // Apply constraint immediately
        activeConstraint.apply();
    }
    
    private void deleteActiveConstraint() {
        if (activeConstraint == null || context == null) return;
        
        IKManager ikManager = context.getIKManager();
        if (ikManager != null) {
            ikManager.removeConstraint(activeConstraint.getName());
        }
        
        // Optionally remove target bone
        if (targetBone != null) {
            Skeleton skeleton = context.getActiveSkeleton();
            if (skeleton != null) {
                skeleton.removeBone(targetBone);
            }
        }
        
        activeConstraint = null;
        targetBone = null;
        context.setStatusMessage("IK: Constraint deleted");
    }
    
    private Bone findBoneAt(double x, double y) {
        if (context == null) return null;
        
        Skeleton skeleton = context.getActiveSkeleton();
        if (skeleton == null) return null;
        
        double threshold = 20;
        
        for (Bone bone : skeleton.getBones()) {
            double bx = bone.getWorldTransform().getX();
            double by = bone.getWorldTransform().getY();
            
            double dist = Math.sqrt((x - bx) * (x - bx) + (y - by) * (y - by));
            if (dist < threshold) {
                return bone;
            }
        }
        
        return null;
    }
    
    private boolean isOverTarget(double x, double y) {
        if (targetBone == null) return false;
        
        double tx = targetBone.getX();
        double ty = targetBone.getY();
        
        return Math.sqrt((x - tx) * (x - tx) + (y - ty) * (y - ty)) < TARGET_SIZE;
    }
    
    private IKConstraint findConstraintAt(double x, double y) {
        if (context == null) return null;
        
        IKManager ikManager = context.getIKManager();
        if (ikManager == null) return null;
        
        for (IKConstraint constraint : ikManager.getConstraints()) {
            Bone target = constraint.getTarget();
            if (target != null) {
                double tx = target.getX();
                double ty = target.getY();
                
                if (Math.sqrt((x - tx) * (x - tx) + (y - ty) * (y - ty)) < TARGET_SIZE) {
                    return constraint;
                }
            }
        }
        
        return null;
    }
    
    @Override
    public void render(GraphicsContext gc) {
        // Draw selected chain
        if (!selectedChain.isEmpty()) {
            gc.setStroke(CHAIN_COLOR);
            gc.setLineWidth(3);
            
            for (Bone bone : selectedChain) {
                double x1 = bone.getWorldTransform().getX();
                double y1 = bone.getWorldTransform().getY();
                double angle = Math.toRadians(bone.getWorldTransform().getRotation());
                double x2 = x1 + Math.cos(angle) * bone.getLength();
                double y2 = y1 + Math.sin(angle) * bone.getLength();
                
                gc.strokeLine(x1, y1, x2, y2);
            }
        }
        
        // Draw IK targets
        if (context != null) {
            IKManager ikManager = context.getIKManager();
            if (ikManager != null) {
                for (IKConstraint constraint : ikManager.getConstraints()) {
                    Bone target = constraint.getTarget();
                    if (target != null) {
                        drawTarget(gc, target, constraint == activeConstraint);
                    }
                }
            }
        }
        
        // Draw current target if creating
        if (targetBone != null && activeConstraint != null) {
            drawTarget(gc, targetBone, true);
        }
    }
    
    private void drawTarget(GraphicsContext gc, Bone target, boolean selected) {
        double x = target.getX();
        double y = target.getY();
        
        gc.setFill(selected ? TARGET_COLOR : TARGET_COLOR.deriveColor(0, 0.7, 0.8, 1));
        gc.setStroke(selected ? Color.WHITE : Color.GRAY);
        gc.setLineWidth(2);
        
        // Draw diamond shape
        double s = TARGET_SIZE / 2;
        double[] xPoints = {x, x + s, x, x - s};
        double[] yPoints = {y - s, y, y + s, y};
        
        gc.fillPolygon(xPoints, yPoints, 4);
        gc.strokePolygon(xPoints, yPoints, 4);
        
        // Draw crosshair
        gc.strokeLine(x - s * 0.5, y, x + s * 0.5, y);
        gc.strokeLine(x, y - s * 0.5, x, y + s * 0.5);
    }
    
    @Override
    public String getStatusMessage() {
        switch (mode) {
            case SELECT_START:
                return "IK: Click to select start bone of chain";
            case SELECT_END:
                return "IK: Click to select end bone of chain";
            case CREATE_TARGET:
                return "IK: Click to place IK target";
            case DRAG_TARGET:
                return "IK: Drag target to pose. ESC to cancel.";
            default:
                return "IK Tool";
        }
    }
}
