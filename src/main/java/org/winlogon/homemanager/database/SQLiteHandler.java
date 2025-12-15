package org.winlogon.homemanager.database;

import com.github.walker84837.JResult.Result;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.winlogon.homemanager.DataHandler;
import org.winlogon.homemanager.DatabaseError;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SQLiteHandler implements DataHandler {
    private final QueryRunner dbManager;
    private final Logger logger;

    public SQLiteHandler(QueryRunner dbManager, Logger logger) {
        this.dbManager = dbManager;
        this.logger = logger;
        init();
    }

    private void init() {
        String createTableSql = """
            CREATE TABLE IF NOT EXISTS homes (
                player_uuid TEXT,
                home_name TEXT,
                world_name TEXT,
                x REAL,
                y REAL,
                z REAL,
                PRIMARY KEY (player_uuid, home_name)
            );
            """;
        dbManager.execute(connection -> {
            try (var stmt = connection.createStatement()) {
                stmt.executeUpdate(createTableSql);
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Failed to create or verify 'homes' table in SQLite", e);
            }
        });
    }

    @Override
    public Result<Void, DatabaseError> updateHome(Player player, String homeName, Location location) {
        var sql = "UPDATE homes SET world_name = ?, x = ?, y = ?, z = ? WHERE player_uuid = ? AND home_name = ?";
        try {
            return dbManager.executeQuery(connection -> {
                try (var stmt = connection.prepareStatement(sql)) {
                    stmt.setString(1, location.getWorld().getName());
                    stmt.setDouble(2, location.getX());
                    stmt.setDouble(3, location.getY());
                    stmt.setDouble(4, location.getZ());
                    stmt.setString(5, player.getUniqueId().toString());
                    stmt.setString(6, homeName);
                    int rowsUpdated = stmt.executeUpdate();
                    if (rowsUpdated > 0) {
                        return Result.ok(null);
                    } else {
                        return Result.err(DatabaseError.HOME_NOT_FOUND);
                    }
                }
            });
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error updating home for " + player.getName(), e);
            return Result.err(DatabaseError.SQL_EXCEPTION);
        }
    }

    @Override
    public Result<Void, DatabaseError> deleteHome(Player player, String homeName) {
        String sql = "DELETE FROM homes WHERE player_uuid = ? AND home_name = ?";
        try {
            return dbManager.executeQuery(connection -> {
                try (var stmt = connection.prepareStatement(sql)) {
                    stmt.setString(1, player.getUniqueId().toString());
                    stmt.setString(2, homeName);
                    int rowsDeleted = stmt.executeUpdate();
                    if (rowsDeleted > 0) {
                        return Result.ok(null);
                    } else {
                        return Result.err(DatabaseError.HOME_NOT_FOUND);
                    }
                }
            });
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error deleting home for " + player.getName(), e);
            return Result.err(DatabaseError.SQL_EXCEPTION);
        }
    }

    @Override
    public Optional<Boolean> createHome(Player player, String homeName, Location location) {
        String sql = "INSERT INTO homes (player_uuid, home_name, world_name, x, y, z) VALUES (?, ?, ?, ?, ?, ?)";
        try {
            return dbManager.executeQuery(connection -> {
                try (var stmt = connection.prepareStatement(sql)) {
                    stmt.setString(1, player.getUniqueId().toString());
                    stmt.setString(2, homeName);
                    stmt.setString(3, location.getWorld().getName());
                    stmt.setDouble(4, location.getX());
                    stmt.setDouble(5, location.getY());
                    stmt.setDouble(6, location.getZ());
                    stmt.executeUpdate();
                    return Optional.of(true);
                } catch (SQLException e) {
                    if (e.getErrorCode() == 19) { // SQLite constraint violation
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
        String sql = "SELECT world_name, x, y, z FROM homes WHERE player_uuid = ? AND home_name = ?";
        try {
            return dbManager.executeQuery(connection -> {
                try (var stmt = connection.prepareStatement(sql)) {
                    stmt.setString(1, player.getUniqueId().toString());
                    stmt.setString(2, homeName);
                    try (var rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            var worldName = rs.getString("world_name");
                            var x = rs.getDouble("x");
                            var y = rs.getDouble("y");
                            var z = rs.getDouble("z");
                            var world = Bukkit.getWorld(worldName);
                            if (world == null) {
                                return Optional.empty();
                            } else {
                                return Optional.of(new Location(world, x, y, z));
                            }
                        } else {
                            return Optional.empty();
                        }
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
            return new ArrayList<>(); // Return empty list on error
        }
    }
}
