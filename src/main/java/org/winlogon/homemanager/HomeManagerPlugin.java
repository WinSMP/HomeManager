package org.winlogon.homemanager;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPIPaperConfig;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.postgresql.ds.PGSimpleDataSource;
import org.sqlite.SQLiteDataSource;
import org.winlogon.homemanager.database.AsyncDatabaseExecutor;
import org.winlogon.homemanager.database.CachedDataHandler;
import org.winlogon.homemanager.database.QueryRunner;
import org.winlogon.homemanager.database.PostgresHandler;
import org.winlogon.homemanager.database.SQLiteHandler;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HomeManagerPlugin extends JavaPlugin implements Listener {
    private static final String POSTGRES = "postgres.";
    private final String IGNORE_EMPTY_PASSWORD = "homemanager.ignore-empty-password";

    private QueryRunner queryRunner;
    private AsyncDatabaseExecutor asyncDatabaseExecutor;
    private CachedDataHandler cachedDataHandler;
    private FileConfiguration config;
    private static Logger logger;

    @Override
    public void onLoad() {
        CommandAPI.onLoad(new CommandAPIPaperConfig(this).verboseOutput(true));
        this.asyncDatabaseExecutor = new AsyncDatabaseExecutor(this);
        saveDefaultConfig();
        this.config = getConfig();
        logger = getLogger();
    }

    @Override
    public void onEnable() {
        CommandAPI.onEnable();

        DataHandler databaseHandler;
        int pageSize;

        try {
            DataSource dataSource = setupDataSource();
            int poolSize = config.getInt("database.pool-size", 10);
            this.queryRunner = new QueryRunner(this, dataSource, poolSize);
            logger.info("DatabaseManager initialized successfully.");

            boolean isPostgres = config.getBoolean(POSTGRES + "enabled", false);
            if (isPostgres) {
                databaseHandler = new PostgresHandler(queryRunner, asyncDatabaseExecutor, logger);
                logger.info("Using PostgreSQL for home storage.");
            } else {
                databaseHandler = new SQLiteHandler(queryRunner, asyncDatabaseExecutor, logger);
                logger.info("Using SQLite for home storage.");
            }

            this.cachedDataHandler = new CachedDataHandler(databaseHandler);
            pageSize = config.getInt("page-size", 5);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to initialize database connection pool. Disabling plugin.", e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        var commandHandler = new CommandHandler<>(cachedDataHandler, this, pageSize);
        commandHandler.registerCommands();

        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        CommandAPI.onDisable();
        if (queryRunner != null) {
            queryRunner.shutdown();
        }
        if (asyncDatabaseExecutor != null) {
            asyncDatabaseExecutor.shutdown();
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (cachedDataHandler != null) {
            cachedDataHandler.invalidate(event.getPlayer().getUniqueId());
        }
    }

    private DataSource setupDataSource() {
        boolean isPostgresEnabled = config.getBoolean(POSTGRES + "enabled", false);
        if (isPostgresEnabled) {
            return getPostgresDataSource();
        } else {
            var ds = new SQLiteDataSource();
            ds.setUrl("jdbc:sqlite:" + getDataFolder().getAbsolutePath() + "/homes.db");
            return ds;
        }
    }

    private PGSimpleDataSource getPostgresDataSource() {
        var ds = new PGSimpleDataSource();
        var host = config.getString(POSTGRES + "host", "localhost");
        var port = config.getInt(POSTGRES + "port", 5432);
        var dbName = config.getString(POSTGRES + "database", "minecraft");
        var user = config.getString(POSTGRES + "username", "postgres");
        var password = config.getString(POSTGRES + "password");

        // Avoid admins from committing the silly mistake of letting the password empty
        if (password == null && !getPasswordOverride().orElse(false)) {
            throw new IllegalStateException(
                "PostgreSQL password is not set! If you want to allow empty password, run with -D%s=true"
                    .formatted(IGNORE_EMPTY_PASSWORD)
            );
        }

        ds.setServerNames(new String[] { host });
        ds.setPortNumbers(new int[] { port });
        ds.setDatabaseName(dbName);
        ds.setUser(user);
        ds.setPassword(password);
        return ds;
    }

    private Optional<Boolean> getPasswordOverride() {
        
        try {
            return Optional.ofNullable(System.getProperty(IGNORE_EMPTY_PASSWORD))
                           .map(String::toLowerCase)
                           .map(Boolean::parseBoolean);
        } catch (Exception e) {
            logger.warning("Plugin flag %s was found but didn't parse successfully.".formatted(IGNORE_EMPTY_PASSWORD));
            return Optional.empty();
        }
    }
}
