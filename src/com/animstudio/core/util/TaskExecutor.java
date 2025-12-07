package com.animstudio.core.util;

import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Task executor for background operations.
 */
public class TaskExecutor {
    
    private static final Logger LOGGER = Logger.getLogger(TaskExecutor.class.getName());
    private static TaskExecutor instance;
    
    private final ExecutorService executor;
    private final ScheduledExecutorService scheduler;
    
    public static TaskExecutor getInstance() {
        if (instance == null) {
            instance = new TaskExecutor();
        }
        return instance;
    }
    
    private TaskExecutor() {
        int processors = Runtime.getRuntime().availableProcessors();
        executor = Executors.newFixedThreadPool(Math.max(2, processors - 1), r -> {
            Thread t = new Thread(r, "AnimStudio-Worker");
            t.setDaemon(true);
            return t;
        });
        
        scheduler = Executors.newScheduledThreadPool(1, r -> {
            Thread t = new Thread(r, "AnimStudio-Scheduler");
            t.setDaemon(true);
            return t;
        });
    }
    
    /**
     * Submit a task for background execution.
     */
    public <T> Future<T> submit(Callable<T> task) {
        return executor.submit(task);
    }
    
    /**
     * Submit a runnable task.
     */
    public Future<?> submit(Runnable task) {
        return executor.submit(task);
    }
    
    /**
     * Run a task and handle result on completion.
     */
    public <T> void runAsync(Callable<T> task, Consumer<T> onSuccess, Consumer<Throwable> onError) {
        executor.submit(() -> {
            try {
                T result = task.call();
                if (onSuccess != null) {
                    javafx.application.Platform.runLater(() -> onSuccess.accept(result));
                }
            } catch (Exception e) {
                LOGGER.warning("Async task failed: " + e.getMessage());
                if (onError != null) {
                    javafx.application.Platform.runLater(() -> onError.accept(e));
                }
            }
        });
    }
    
    /**
     * Schedule a task to run after a delay.
     */
    public ScheduledFuture<?> schedule(Runnable task, long delay, TimeUnit unit) {
        return scheduler.schedule(task, delay, unit);
    }
    
    /**
     * Schedule a task to run periodically.
     */
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, long initialDelay, long period, TimeUnit unit) {
        return scheduler.scheduleAtFixedRate(task, initialDelay, period, unit);
    }
    
    /**
     * Shutdown the executor (call when application closes).
     */
    public void shutdown() {
        executor.shutdown();
        scheduler.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
