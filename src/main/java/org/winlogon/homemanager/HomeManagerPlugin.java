package org.winlogon.homemanager;

import org.bukkit.configuration.file.FileConfiguration;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPIPaperConfig;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPIPaperConfig;
import org.bukkit.plugin.java.JavaPlugin;
import org.postgresql.ds.PGSimpleDataSource;
import org.sqlite.SQLiteDataSource;
import org.winlogon.homemanager.database.QueryRunner;
import org.winlogon.homemanager.database.PostgresHandler;
import org.winlogon.homemanager.database.SQLiteHandler;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HomeManagerPlugin extends JavaPlugin {
    private DataHandler databaseHandler;
    private QueryRunner queryRunner;
    private FileConfiguration config;
    private Logger logger;

    @Override
    public void onLoad() {
        CommandAPI.onLoad(new CommandAPIPaperConfig(this).verboseOutput(true));
    }

    @Override
    public void onEnable() {
        CommandAPI.onEnable();
        saveDefaultConfig();
        this.config = getConfig();
        this.logger = getLogger();

        try {
            DataSource dataSource = setupDataSource();
            int poolSize = config.getInt("database.pool-size", 10);
            this.queryRunner = new QueryRunner(this, dataSource, poolSize);
            logger.info("DatabaseManager initialized successfully.");

            boolean isPostgres = config.getBoolean("postgres.enabled", false);
            if (isPostgres) {
                databaseHandler = new PostgresHandler(queryRunner, logger);
                logger.info("Using PostgreSQL for home storage.");
            } else {
                databaseHandler = new SQLiteHandler(queryRunner, logger);
                logger.info("Using SQLite for home storage.");
            }

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to initialize database connection pool. Disabling plugin.", e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        var commandHandler = new CommandHandler<DataHandler>(databaseHandler, this);
        commandHandler.registerCommands();
    }

    @Override
    public void onDisable() {
        CommandAPI.onDisable();
        if (queryRunner != null) {
            queryRunner.shutdown();
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

