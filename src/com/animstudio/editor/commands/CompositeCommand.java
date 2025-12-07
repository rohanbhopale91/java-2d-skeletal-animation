package com.animstudio.editor.commands;

import java.util.ArrayList;
import java.util.List;

/**
 * A command that groups multiple commands together.
 */
public class CompositeCommand implements Command {
    
    private final String description;
    private final List<Command> commands;
    
    public CompositeCommand(String description) {
        this.description = description;
        this.commands = new ArrayList<>();
    }
    
    public void add(Command command) {
        commands.add(command);
    }
    
    public boolean isEmpty() {
        return commands.isEmpty();
    }
    
    @Override
    public void execute() {
        for (Command command : commands) {
            command.execute();
        }
    }
    
    @Override
    public void undo() {
        // Undo in reverse order
        for (int i = commands.size() - 1; i >= 0; i--) {
            commands.get(i).undo();
        }
    }
    
    @Override
    public String getDescription() {
        return description;
    }
}
