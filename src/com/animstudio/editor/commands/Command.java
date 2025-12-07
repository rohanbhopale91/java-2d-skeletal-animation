package com.animstudio.editor.commands;

/**
 * Interface for undoable commands.
 */
public interface Command {
    
    /**
     * Execute the command.
     */
    void execute();
    
    /**
     * Undo the command.
     */
    void undo();
    
    /**
     * Get a description of this command for UI display.
     */
    String getDescription();
    
    /**
     * Check if this command can be merged with another (for grouping small edits).
     */
    default boolean canMergeWith(Command other) {
        return false;
    }
    
    /**
     * Merge another command into this one.
     */
    default void mergeWith(Command other) {
        // Default: no merge
    }
}
