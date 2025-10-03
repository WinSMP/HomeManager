package org.winlogon.homemanager;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.postgresql.ds.PGSimpleDataSource;
import org.sqlite.SQLiteDataSource;
import org.winlogon.homemanager.database.DatabaseManager;
import org.winlogon.homemanager.database.PostgresHandler;
import org.winlogon.homemanager.database.SQLiteHandler;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HomeManagerPlugin extends JavaPlugin {
    private DataHandler databaseHandler;
    private DatabaseManager databaseManager;
    private FileConfiguration config;
    private Logger logger;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.config = getConfig();
        this.logger = getLogger();

        try {
            DataSource dataSource = setupDataSource();
            int poolSize = config.getInt("database.pool-size", 10);
            this.databaseManager = new DatabaseManager(this, dataSource, poolSize);
            logger.info("DatabaseManager initialized successfully.");

            boolean isPostgres = config.getBoolean("postgres.enabled", false);
            if (isPostgres) {
                databaseHandler = new PostgresHandler(databaseManager, logger);
                logger.info("Using PostgreSQL for home storage.");
            } else {
                databaseHandler = new SQLiteHandler(databaseManager, logger);
                logger.info("Using SQLite for home storage.");
            }

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to initialize database connection pool. Disabling plugin.", e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        var commandHandler = new CommandHandler<DataHandler>(databaseHandler);
        commandHandler.registerCommands();
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) {
            databaseManager.shutdown();
        }
    }

    private DataSource setupDataSource() {
        if (config.getBoolean("postgres.enabled", false)) {
            var ds = new PGSimpleDataSource();
            ds.setServerNames(new String[] { config.getString("postgres.host", "localhost") } );
            ds.setPortNumbers(new int[] { config.getInt("postgres.port", 5432) } );
            ds.setDatabaseName(config.getString("postgres.database", "minecraft"));
            ds.setUser(config.getString("postgres.username", "postgres"));
            ds.setPassword(config.getString("postgres.password", ""));
            return ds;
        } else {
            var ds = new SQLiteDataSource();
            ds.setUrl("jdbc:sqlite:" + getDataFolder().getAbsolutePath() + "/homes.db");
            return ds;
        }
    }
}

