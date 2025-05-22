package org.winlogon.homemanager;

import org.bukkit.plugin.java.JavaPlugin;

import org.winlogon.homemanager.database.SQLiteHandler;
import org.winlogon.homemanager.database.PostgresHandler;
import org.winlogon.homemanager.database.AdvancedDatabaseConfig;

public class HomeManagerPlugin extends JavaPlugin {
    private DataHandler databaseHandler;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        // load postgresql jdbc driver
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException _) {
            throw new RuntimeException("PostgreSQL JDBC Driver not found.");
        }


        var config = getConfig();
        var logger = getLogger();
        var postgresEnabled = config.getBoolean("postgres.enabled", false);
        if (!postgresEnabled) {
            databaseHandler = new SQLiteHandler(getDataFolder());
            logger.info("Using SQLite for home storage.");
        }

        var databaseConfig = new AdvancedDatabaseConfig(
            postgresEnabled,
            config.getString("postgres.host", "localhost"),
            config.getInt("postgres.port", 5432),
            config.getString("postgres.database", "minecraft"),
            config.getString("postgres.user", "postgres"),
            config.getString("postgres.password", "")
        );

        databaseHandler = new PostgresHandler(databaseConfig, logger);
        getLogger().info("Using PostgreSQL for home storage.");

        CommandHandler<DataHandler> commandHandler = new CommandHandler<>(databaseHandler);
        commandHandler.registerCommands();
    }

    @Override
    public void onDisable() {
        if (databaseHandler != null) {
            databaseHandler.closeConnection();
        }
    }
}

