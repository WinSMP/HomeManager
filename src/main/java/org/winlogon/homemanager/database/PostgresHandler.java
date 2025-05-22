package org.winlogon.homemanager.database;

import org.winlogon.homemanager.DataHandler;
import org.winlogon.homemanager.DatabaseError;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import com.github.walker84837.JResult.Result;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

public class PostgresHandler implements DataHandler {

    private Connection connection;

    public PostgresHandler(AdvancedDatabaseConfig config, Logger logger) {
        var jdbcUrl = STR."jdbc:postgresql://\{config.host()}:\{config.port()}/\{config.database()}";
        try {
            this.connection = DriverManager.getConnection(jdbcUrl, config.user(), config.password());
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to connect to PostgreSQL at " + jdbcUrl, e);
        }

        // Create the "homes" table if it doesn't exist (same schema as SQLite version, but using PostgreSQL types)
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS homes (
                    player_uuid VARCHAR(36) NOT NULL,
                    home_name VARCHAR(255) NOT NULL,
                    world_name VARCHAR(255),
                    x DOUBLE PRECISION,
                    y DOUBLE PRECISION,
                    z DOUBLE PRECISION,
                    PRIMARY KEY (player_uuid, home_name)
                );
                """);
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to create or verify 'homes' table in PostgreSQL", e);
        }
    }

    @Override
    public Result<Void, DatabaseError> updateHome(Player player, String homeName, Location location) {
        String sql = "UPDATE homes SET world_name = ?, x = ?, y = ?, z = ? WHERE player_uuid = ? AND home_name = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
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
        } catch (SQLException e) {
            return Result.err(DatabaseError.SQL_EXCEPTION);
        }
    }

    @Override
    public Result<Void, DatabaseError> deleteHome(Player player, String homeName) {
        String sql = "DELETE FROM homes WHERE player_uuid = ? AND home_name = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, player.getUniqueId().toString());
            stmt.setString(2, homeName);
            int rowsDeleted = stmt.executeUpdate();
            if (rowsDeleted > 0) {
                return Result.ok(null);
            } else {
                return Result.err(DatabaseError.HOME_NOT_FOUND);
            }
        } catch (SQLException e) {
            return Result.err(DatabaseError.SQL_EXCEPTION);
        }
    }

    @Override
    public Optional<Boolean> createHome(Player player, String homeName, Location location) {
        String sql = "INSERT INTO homes (player_uuid, home_name, world_name, x, y, z) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, player.getUniqueId().toString());
            stmt.setString(2, homeName);
            stmt.setString(3, location.getWorld().getName());
            stmt.setDouble(4, location.getX());
            stmt.setDouble(5, location.getY());
            stmt.setDouble(6, location.getZ());
            stmt.executeUpdate();
            return Optional.of(true);
        } catch (SQLException e) {
            // PostgreSQL uses SQLState "23505" for unique violation on a primary key
            String sqlState = e.getSQLState();
            if ("23505".equals(sqlState)) {
                // Home already exists for this player + name
                return Optional.of(false);
            } else {
                e.printStackTrace();
                return Optional.empty();
            }
        }
    }

    @Override
    public Optional<Location> getHomeLocation(Player player, String homeName) {
        String sql = "SELECT world_name, x, y, z FROM homes WHERE player_uuid = ? AND home_name = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, player.getUniqueId().toString());
            stmt.setString(2, homeName);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String worldName = rs.getString("world_name");
                    double x = rs.getDouble("x");
                    double y = rs.getDouble("y");
                    double z = rs.getDouble("z");
                    var world = Bukkit.getWorld(worldName);
                    if (world == null) {
                        // The world is not loaded
                        return Optional.empty();
                    }
                    return Optional.of(new Location(world, x, y, z));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    @Override
    public List<String> getHomes(Player player) {
        List<String> homes = new ArrayList<>();
        String sql = "SELECT home_name FROM homes WHERE player_uuid = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, player.getUniqueId().toString());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    homes.add(rs.getString("home_name"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return homes;
    }

    @Override
    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
