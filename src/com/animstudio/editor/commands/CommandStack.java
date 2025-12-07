package com.animstudio.editor.commands;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.Consumer;

/**
 * Manages undo/redo command history.
 */
public class CommandStack {
    
    private final Deque<Command> undoStack;
    private final Deque<Command> redoStack;
    private final int maxSize;
    private final List<Consumer<Command>> listeners;
    private boolean executing;
    
    public CommandStack(int maxSize) {
        this.maxSize = maxSize;
        this.undoStack = new ArrayDeque<>();
        this.redoStack = new ArrayDeque<>();
        this.listeners = new ArrayList<>();
        this.executing = false;
    }
    
    /**
     * Execute a command and add it to the undo stack.
     */
    public void execute(Command command) {
        if (executing) return;
        
        executing = true;
        try {
            command.execute();
            
            // Try to merge with previous command
            if (!undoStack.isEmpty() && undoStack.peek().canMergeWith(command)) {
                undoStack.peek().mergeWith(command);
            } else {
                undoStack.push(command);
                
                // Limit stack size
                while (undoStack.size() > maxSize) {
                    ((ArrayDeque<Command>) undoStack).removeLast();
                }
            }
            
            // Clear redo stack when new command is executed
            redoStack.clear();
            
            // Notify listeners
            for (Consumer<Command> listener : listeners) {
                listener.accept(command);
            }
        } finally {
            executing = false;
        }
    }
    
    /**
     * Undo the last command.
     */
    public boolean undo() {
        if (undoStack.isEmpty() || executing) return false;
        
        executing = true;
        try {
            Command command = undoStack.pop();
            command.undo();
            redoStack.push(command);
            return true;
        } finally {
            executing = false;
        }
    }
    
    /**
     * Redo the last undone command.
     */
    public boolean redo() {
        if (redoStack.isEmpty() || executing) return false;
        
        executing = true;
        try {
            Command command = redoStack.pop();
            command.execute();
            undoStack.push(command);
            return true;
        } finally {
            executing = false;
        }
    }
    
    /**
     * Check if undo is available.
     */
    public boolean canUndo() {
        return !undoStack.isEmpty();
    }
    
    /**
     * Check if redo is available.
     */
    public boolean canRedo() {
        return !redoStack.isEmpty();
    }
    
    /**
     * Get the description of the next undo command.
     */
    public String getUndoDescription() {
        return undoStack.isEmpty() ? null : undoStack.peek().getDescription();
    }
    
    /**
     * Get the description of the next redo command.
     */
    public String getRedoDescription() {
        return redoStack.isEmpty() ? null : redoStack.peek().getDescription();
    }
    
    /**
     * Clear all history.
     */
    public void clear() {
        undoStack.clear();
        redoStack.clear();
    }
    
    /**
     * Add a listener for command execution.
     */
    public void addListener(Consumer<Command> listener) {
        listeners.add(listener);
    }
    
    /**
     * Remove a listener.
     */
    public void removeListener(Consumer<Command> listener) {
        listeners.remove(listener);
    }
    
    public int getUndoStackSize() { return undoStack.size(); }
    public int getRedoStackSize() { return redoStack.size(); }
}
