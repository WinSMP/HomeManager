package org.winlogon.homemanager.database;

import org.bukkit.plugin.java.JavaPlugin;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Level;

public final class DatabaseManager {

    private final JavaPlugin plugin;
    private final DataSource dataSource;
    private final ExecutorService executor;
    private final BlockingQueue<Connection> connectionPool;
    private final int poolSize;

    public DatabaseManager(JavaPlugin plugin, DataSource dataSource, int poolSize) throws SQLException {
        this.plugin = plugin;
        this.dataSource = dataSource;
        this.poolSize = poolSize;
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
        this.connectionPool = new LinkedBlockingQueue<>(poolSize);
        initializePool();
    }

    private void initializePool() throws SQLException {
        for (int i = 0; i < poolSize; i++) {
            try {
                connectionPool.add(dataSource.getConnection());
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to create initial database connection", e);
                throw e;
            }
        }
    }

    private Connection getConnection() throws SQLException, InterruptedException {
        Connection connection = connectionPool.take();
        try {
            if (!connection.isValid(1)) {
                plugin.getLogger().warning("Stale database connection detected. Reconnecting...");
                closeConnection(connection); // Close the invalid connection
                return dataSource.getConnection(); // Return a new, fresh connection
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to validate connection, creating a new one.", e);
            closeConnection(connection);
            return dataSource.getConnection();
        }
        return connection;
    }

    private void releaseConnection(Connection connection) {
        if (connection != null) {
            try {
                if (!connection.isClosed()) {
                    connectionPool.offer(connection);
                } else {
                    // If the connection was closed for some reason, add a new one to maintain pool size
                    connectionPool.offer(dataSource.getConnection());
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to check or replace a closed connection", e);
            }
        }
    }

    private void closeConnection(Connection connection) {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                // Ignore, we are discarding it anyway
            }
        }
    }

    public void execute(SQLConsumer<Connection> action) {
        executor.submit(() -> {
            Connection connection = null;
            try {
                connection = getConnection();
                action.accept(connection);
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "An error occurred during a database operation.", e);
            } finally {
                releaseConnection(connection);
            }
        });
    }

    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        for (Connection connection : connectionPool) {
            closeConnection(connection);
        }
        connectionPool.clear();
        plugin.getLogger().info("DatabaseManager shut down successfully.");
    }

    @FunctionalInterface
    public interface SQLConsumer<T> {
        void accept(T t) throws SQLException;
    }
}
