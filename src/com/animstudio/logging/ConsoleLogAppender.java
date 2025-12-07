package com.animstudio.logging;

import java.io.PrintStream;

/**
 * Log appender that writes to the console (System.out/System.err).
 */
public class ConsoleLogAppender implements LogAppender {
    
    private final String name;
    private final LogFormatter formatter;
    private LogLevel minLevel;
    private final boolean useStdErr;
    private boolean enabled;
    
    public ConsoleLogAppender() {
        this("Console", new TextLogFormatter(), LogLevel.DEBUG, true);
    }
    
    public ConsoleLogAppender(String name, LogFormatter formatter, LogLevel minLevel, boolean useStdErr) {
        this.name = name;
        this.formatter = formatter;
        this.minLevel = minLevel;
        this.useStdErr = useStdErr;
        this.enabled = true;
    }
    
    @Override
    public void append(LogEvent event) {
        if (!enabled || !shouldAppend(event)) {
            return;
        }
        
        String formatted = formatter.format(event);
        PrintStream stream = getStreamForLevel(event.level());
        stream.println(formatted);
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public LogLevel getMinLevel() {
        return minLevel;
    }
    
    @Override
    public void setMinLevel(LogLevel level) {
        this.minLevel = level;
    }
    
    @Override
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    @Override
    public void flush() {
        System.out.flush();
        System.err.flush();
    }
    
    private PrintStream getStreamForLevel(LogLevel level) {
        if (useStdErr && level.getPriority() >= LogLevel.ERROR.getPriority()) {
            return System.err;
        }
        return System.out;
    }
    
    /**
     * Create a console appender with default settings.
     */
    public static ConsoleLogAppender createDefault() {
        return new ConsoleLogAppender();
    }
    
    /**
     * Create a debug console appender (TRACE level, verbose format).
     */
    public static ConsoleLogAppender createDebug() {
        return new ConsoleLogAppender("DebugConsole", TextLogFormatter.verbose(), LogLevel.TRACE, true);
    }
    
    /**
     * Create a production console appender (INFO level, compact format).
     */
    public static ConsoleLogAppender createProduction() {
        return new ConsoleLogAppender("ProdConsole", TextLogFormatter.compact(), LogLevel.INFO, true);
    }
}
