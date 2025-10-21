package org.winlogon.homemanager;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPIPaperConfig;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.postgresql.ds.PGSimpleDataSource;
import org.sqlite.SQLiteDataSource;
import org.winlogon.homemanager.database.QueryRunner;
import org.winlogon.homemanager.database.PostgresHandler;
import org.winlogon.homemanager.database.SQLiteHandler;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HomeManagerPlugin extends JavaPlugin {
    private static final String POSTGRES = "postgres.";
    private final String IGNORE_EMPTY_PASSWORD = "homemanager.ignore-empty-password";

    private DataHandler databaseHandler;
    private QueryRunner queryRunner;
    private FileConfiguration config;
    private static Logger logger;

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

            boolean isPostgres = config.getBoolean(POSTGRES + "enabled", false);
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

        // Avoid admins from doing the silly mistake of letting the password empty
        if (password == null && getPasswordOverride().orElse(false)) {
            Objects.requireNonNull(password, "The password must be set! If you want to override this, "
                    + STR."run this with -D\{IGNORE_EMPTY_PASSWORD}=true");
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
            logger.warning(STR."Plugin flag \{IGNORE_EMPTY_PASSWORD} was found but didn't parse successfully.");
            return Optional.empty();
        }
    }
}
