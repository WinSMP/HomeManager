package org.winlogon.homemanager.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.plugin.Plugin;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public final class QueryRunner {
    private final Plugin plugin;
    private final HikariDataSource hikariDataSource;
    private final ExecutorService executor;

    public QueryRunner(Plugin plugin, DataSource dataSource, int poolSize) {
        this.plugin = plugin;
        this.executor = Executors.newVirtualThreadPerTaskExecutor();

        var config = new HikariConfig();
        config.setDataSource(dataSource);
        config.setMaximumPoolSize(poolSize);
        config.setPoolName("HomeManagerPool");

        // Performance and stability optimizations
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");

        this.hikariDataSource = new HikariDataSource(config);
    }

    public void execute(SQLConsumer<Connection> action) {
        executor.submit(() -> {
            try (var connection = hikariDataSource.getConnection()) {
                action.accept(connection);
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "An error occurred during a database operation.", e);
            }
        });
    }

    public <R> R executeQuery(SQLFunction<Connection, R> action) throws SQLException {
        try (var connection = hikariDataSource.getConnection()) {
            return action.apply(connection);
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "An error occurred during a database operation.", e);
            throw e;
        }
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

        if (hikariDataSource != null) {
            hikariDataSource.close();
        }
        plugin.getLogger().info("DatabaseManager shut down successfully.");
    }

    @FunctionalInterface
    public interface SQLConsumer<T> {
        void accept(T t) throws SQLException;
    }

    @FunctionalInterface
    public interface SQLFunction<T, R> {
        R apply(T t) throws SQLException;
    }
}
