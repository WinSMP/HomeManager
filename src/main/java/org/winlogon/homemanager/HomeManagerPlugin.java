package org.winlogon.homemanager;

import org.bukkit.plugin.java.JavaPlugin;
import org.winlogon.homemanager.database.SQLiteHandler;
import org.winlogon.homemanager.database.PostgresHandler;

record DatabaseConfig(
    boolean enabled,
    String host,
    int port,
    String database,
    String user,
    String password
) {}

public class HomeManagerPlugin extends JavaPlugin {
    private DataHandler databaseHandler;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        var config = getConfig();
        if (!config.getBoolean("postgres.enabled", false)) {
            databaseHandler = new SQLiteHandler(getDataFolder());
            getLogger().info("Using SQLite for home storage.");
        }

        var host = config.getString("postgres.host", "localhost");
        var port = config.getInt("postgres.port", 5432);
        var database = config.getString("postgres.database", "minecraft");
        var user = config.getString("postgres.user", "postgres");
        var password = config.getString("postgres.password", "");

        databaseHandler = new PostgresHandler(host, port, database, user, password);
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

