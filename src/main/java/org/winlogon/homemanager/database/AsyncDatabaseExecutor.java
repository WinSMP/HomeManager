package org.winlogon.homemanager.database;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

/**
 * Handles execution of database operations on virtual threads and returns
 * callbacks to the server's native async scheduler context.
 */
public final class AsyncDatabaseExecutor {
    private final Plugin plugin;
    private final ExecutorService dbExecutor;
    private final boolean isFolia;

    public AsyncDatabaseExecutor(Plugin plugin) {
        this.plugin = plugin;
        this.dbExecutor = Executors.newVirtualThreadPerTaskExecutor();
        this.isFolia = detectFolia();
    }

    /**
     * Executes a blocking database supplier on a virtual thread and returns a
     * CompletableFuture that completes in the server's async scheduler context.
     */
    public <T> CompletableFuture<T> supply(Supplier<T> supplier) {
        var future = new CompletableFuture<T>();

        dbExecutor.submit(() -> {
            try {
                T result = supplier.get();
                runOnNativeAsync(() -> future.complete(result));
            } catch (Exception e) {
                runOnNativeAsync(() -> future.completeExceptionally(e));
            }
        });

        return future;
    }

    /**
     * Executes a database task on a virtual thread.
     */
    public void execute(Runnable task) {
        dbExecutor.submit(task);
    }

    private void runOnNativeAsync(Runnable task) {
        if (isFolia) {
            Bukkit.getAsyncScheduler().runNow(plugin, _ -> task.run());
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
        }
    }

    private boolean detectFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public void shutdown() {
        dbExecutor.shutdown();
    }
}
