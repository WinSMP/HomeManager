package org.winlogon.homemanager.database;

import com.github.walker84837.JResult.Result;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.winlogon.homemanager.DataHandler;
import org.winlogon.homemanager.DatabaseError;
import org.winlogon.homemanager.Home;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Abstract base class for database handlers that manage home storage.
 * <p>
 * Subclasses must implement the abstract methods to provide database-specific behavior
 * for SQL dialect differences between SQLite and PostgreSQL.
 *
 * @see SQLiteHandler
 * @see PostgresHandler
 */
public abstract class AbstractDatabaseHandler implements DataHandler {
    protected final QueryRunner dbManager;
    protected final Logger logger;

    protected AbstractDatabaseHandler(QueryRunner dbManager, Logger logger) {
        this.dbManager = dbManager;
        this.logger = logger;
        init();
    }

    /**
     * Returns the SQL type constant used for NULL float values (yaw/pitch).
     * <p>
     * SQLite uses {@code Types.REAL}, PostgreSQL uses {@code Types.DOUBLE}.
     *
     * @return {@link java.sql.Types} constant for float/double
     */
    protected abstract int getNullType();

    /**
     * Returns the database-specific error code for duplicate key violations.
     * <p>
     * SQLite returns "19", PostgreSQL returns "23505".
     *
     * @return SQL state or error code for uniqueness violations
     */
    protected abstract String getDuplicateErrorCode();

    /**
     * Returns the SQL for inserting/updating a home with conflict resolution.
     * <p>
     * SQLite uses {@code INSERT OR REPLACE}, PostgreSQL uses
     * {@code INSERT ... ON CONFLICT ... DO UPDATE SET}.
     *
     * @return parameterized INSERT/UPSERT SQL statement
     */
    protected abstract String getUpsertSql();

    private void init() {
        String createTableSql = getCreateTableSql();
        dbManager.execute(connection -> {
            try (var stmt = connection.createStatement()) {
                stmt.executeUpdate(createTableSql);
                migrateSchema(connection);
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Failed to create or verify 'homes' table", e);
            }
        });
    }

    protected String getCreateTableSql() {
        return """
            CREATE TABLE IF NOT EXISTS homes (
                player_uuid TEXT,
                home_name TEXT,
                world_name TEXT,
                x REAL,
                y REAL,
                z REAL,
                yaw REAL,
                pitch REAL,
                PRIMARY KEY (player_uuid, home_name)
            );
            """;
    }

    private void migrateSchema(java.sql.Connection connection) throws SQLException {
        var meta = connection.getMetaData();
        
        try (var rs = meta.getColumns(null, null, "homes", "yaw")) {
            if (!rs.next()) {
                try (var stmt = connection.createStatement()) {
                    stmt.executeUpdate("ALTER TABLE homes ADD COLUMN yaw REAL");
                    logger.info("Migrated homes table: added yaw column");
                }
            }
        }
        
        try (var rs = meta.getColumns(null, null, "homes", "pitch")) {
            if (!rs.next()) {
                try (var stmt = connection.createStatement()) {
                    stmt.executeUpdate("ALTER TABLE homes ADD COLUMN pitch REAL");
                    logger.info("Migrated homes table: added pitch column");
                }
            }
        }
    }

    @Override
    public Result<Void, DatabaseError> updateHome(Player player, String homeName, Location location) {
        var sql = "UPDATE homes SET world_name = ?, x = ?, y = ?, z = ?, yaw = ?, pitch = ? WHERE player_uuid = ? AND home_name = ?";
        try {
            return dbManager.executeQuery(connection -> {
                try (var stmt = connection.prepareStatement(sql)) {
                    stmt.setString(1, location.getWorld().getName());
                    stmt.setDouble(2, location.getX());
                    stmt.setDouble(3, location.getY());
                    stmt.setDouble(4, location.getZ());
                    stmt.setFloat(5, location.getYaw());
                    stmt.setFloat(6, location.getPitch());
                    stmt.setString(7, player.getUniqueId().toString());
                    stmt.setString(8, homeName);
                    int rowsUpdated = stmt.executeUpdate();
                    return rowsUpdated > 0 ? Result.ok(null) : Result.err(DatabaseError.HOME_NOT_FOUND);
                }
            });
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error updating home for " + player.getName(), e);
            return Result.err(DatabaseError.SQL_EXCEPTION);
        }
    }

    @Override
    public Result<Void, DatabaseError> deleteHome(Player player, String homeName) {
        return deleteHomeByUuid(player.getUniqueId(), homeName);
    }

    @Override
    public Result<Void, DatabaseError> deleteHomeByUuid(UUID playerUuid, String homeName) {
        String sql = "DELETE FROM homes WHERE player_uuid = ? AND home_name = ?";
        try {
            return dbManager.executeQuery(connection -> {
                try (var stmt = connection.prepareStatement(sql)) {
                    stmt.setString(1, playerUuid.toString());
                    stmt.setString(2, homeName);
                    int rowsDeleted = stmt.executeUpdate();
                    return rowsDeleted > 0 ? Result.ok(null) : Result.err(DatabaseError.HOME_NOT_FOUND);
                }
            });
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error deleting home for " + playerUuid, e);
            return Result.err(DatabaseError.SQL_EXCEPTION);
        }
    }

    @Override
    public Optional<Boolean> createHome(Player player, String homeName, Location location) {
        String sql = "INSERT INTO homes (player_uuid, home_name, world_name, x, y, z, yaw, pitch) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try {
            return dbManager.executeQuery(connection -> {
                try (var stmt = connection.prepareStatement(sql)) {
                    stmt.setString(1, player.getUniqueId().toString());
                    stmt.setString(2, homeName);
                    stmt.setString(3, location.getWorld().getName());
                    stmt.setDouble(4, location.getX());
                    stmt.setDouble(5, location.getY());
                    stmt.setDouble(6, location.getZ());
                    
                    float yaw = location.getYaw();
                    float pitch = location.getPitch();
                    if (yaw != 0f || pitch != 0f) {
                        stmt.setFloat(7, yaw);
                        stmt.setFloat(8, pitch);
                    } else {
                        stmt.setNull(7, getNullType());
                        stmt.setNull(8, getNullType());
                    }
                    
                    stmt.executeUpdate();
                    return Optional.of(true);
                } catch (SQLException e) {
                    if (getDuplicateErrorCode().equals(e.getSQLState())) {
                        return Optional.of(false);
                    } else {
                        throw e;
                    }
                }
            });
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error creating home for " + player.getName(), e);
            return Optional.empty();
        }
    }

    @Override
    public Optional<Location> getHomeLocation(Player player, String homeName) {
        String sql = "SELECT world_name, x, y, z, yaw, pitch FROM homes WHERE player_uuid = ? AND home_name = ?";
        try {
            return dbManager.executeQuery(connection -> {
                try (var stmt = connection.prepareStatement(sql)) {
                    stmt.setString(1, player.getUniqueId().toString());
                    stmt.setString(2, homeName);
                    try (var rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            String worldName = rs.getString("world_name");
                            double x = rs.getDouble("x");
                            double y = rs.getDouble("y");
                            double z = rs.getDouble("z");
                            float yaw = rs.getFloat("yaw");
                            if (rs.wasNull()) yaw = 0f;
                            float pitch = rs.getFloat("pitch");
                            if (rs.wasNull()) pitch = 0f;
                            
                            var world = Bukkit.getWorld(worldName);
                            if (world == null) {
                                return Optional.empty();
                            }
                            return Optional.of(new Location(world, x, y, z, yaw, pitch));
                        }
                        return Optional.empty();
                    }
                }
            });
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error getting home location for " + player.getName(), e);
            return Optional.empty();
        }
    }

    @Override
    public List<String> getHomes(Player player) {
        String sql = "SELECT home_name FROM homes WHERE player_uuid = ?";
        try {
            return dbManager.executeQuery(connection -> {
                List<String> homes = new ArrayList<>();
                try (var stmt = connection.prepareStatement(sql)) {
                    stmt.setString(1, player.getUniqueId().toString());
                    try (var rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            homes.add(rs.getString("home_name"));
                        }
                        return homes;
                    }
                }
            });
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error getting homes for " + player.getName(), e);
            return new ArrayList<>();
        }
    }

    @Override
    public List<Home> getAllHomes() {
        String sql = "SELECT player_uuid, home_name, world_name, x, y, z, yaw, pitch FROM homes";
        try {
            return dbManager.executeQuery(connection -> {
                List<Home> homes = new ArrayList<>();
                try (var stmt = connection.createStatement()) {
                    try (var rs = stmt.executeQuery(sql)) {
                        while (rs.next()) {
                            Home home = readHomeFromResultSet(rs);
                            if (home != null) {
                                homes.add(home);
                            }
                        }
                        return homes;
                    }
                }
            });
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error getting all homes", e);
            return new ArrayList<>();
        }
    }

    @Override
    public List<Home> getPlayerHomes(UUID playerUuid) {
        String sql = "SELECT player_uuid, home_name, world_name, x, y, z, yaw, pitch FROM homes WHERE player_uuid = ?";
        try {
            return dbManager.executeQuery(connection -> {
                List<Home> homes = new ArrayList<>();
                try (var stmt = connection.prepareStatement(sql)) {
                    stmt.setString(1, playerUuid.toString());
                    try (var rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            Home home = readHomeFromResultSet(rs);
                            if (home != null) {
                                homes.add(home);
                            }
                        }
                        return homes;
                    }
                }
            });
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error getting homes for player " + playerUuid, e);
            return new ArrayList<>();
        }
    }

    @Override
    public boolean homeExists(UUID playerUuid, String homeName) {
        String sql = "SELECT 1 FROM homes WHERE player_uuid = ? AND home_name = ?";
        try {
            return dbManager.executeQuery(connection -> {
                try (var stmt = connection.prepareStatement(sql)) {
                    stmt.setString(1, playerUuid.toString());
                    stmt.setString(2, homeName);
                    try (var rs = stmt.executeQuery()) {
                        return rs.next();
                    }
                }
            });
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error checking if home exists for " + playerUuid, e);
            return false;
        }
    }

    @Override
    public Result<Void, DatabaseError> importHome(Home home) {
        String sql = getUpsertSql();
        try {
            return dbManager.executeQuery(connection -> {
                try (var stmt = connection.prepareStatement(sql)) {
                    stmt.setString(1, home.playerUuid().toString());
                    stmt.setString(2, home.homeName());
                    stmt.setString(3, home.worldName());
                    stmt.setDouble(4, home.x());
                    stmt.setDouble(5, home.y());
                    stmt.setDouble(6, home.z());
                    
                    Float yaw = home.yaw();
                    Float pitch = home.pitch();
                    if (yaw != null && (yaw != 0f || (pitch != null && pitch != 0f))) {
                        stmt.setFloat(7, yaw);
                        stmt.setFloat(8, pitch != null ? pitch : 0f);
                    } else {
                        stmt.setNull(7, getNullType());
                        stmt.setNull(8, getNullType());
                    }

                    stmt.executeUpdate();
                    return Result.ok(null);
                }
            });
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error importing home for " + home.playerUuid(), e);
            return Result.err(DatabaseError.SQL_EXCEPTION);
        }
    }

    @Override
    public Result<Void, DatabaseError> importHomes(List<Home> homes) {
        for (Home home : homes) {
            var result = importHome(home);
            if (result.isErr()) {
                return result;
            }
        }
        return Result.ok(null);
    }

    protected Home readHomeFromResultSet(java.sql.ResultSet rs) throws SQLException {
        UUID playerUuid = UUID.fromString(rs.getString("player_uuid"));
        String homeName = rs.getString("home_name");
        String worldName = rs.getString("world_name");
        double x = rs.getDouble("x");
        double y = rs.getDouble("y");
        double z = rs.getDouble("z");
        
        float yaw = rs.getFloat("yaw");
        boolean yawNull = rs.wasNull();
        float pitch = rs.getFloat("pitch");
        boolean pitchNull = rs.wasNull();
        
        return new Home(
            playerUuid,
            homeName,
            worldName,
            x, y, z,
            yawNull ? null : yaw,
            pitchNull ? null : pitch
        );
    }
}