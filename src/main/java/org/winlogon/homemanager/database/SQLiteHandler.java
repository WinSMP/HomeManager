package org.winlogon.homemanager.database;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import com.github.walker84837.JResult.Result;

import org.winlogon.homemanager.DataHandler;
import org.winlogon.homemanager.DatabaseError;

import java.io.File;
import java.sql.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;

public class SQLiteHandler implements DataHandler {

    private Connection connection;

    public SQLiteHandler(File dataFolder) {
        setupDatabase(dataFolder);
    }

    private void setupDatabase(File dataFolder) {
        try {
            var databaseFile = new File(dataFolder, "homes.db");
            connection = DriverManager.getConnection("jdbc:sqlite:" + databaseFile.getAbsolutePath());
            try (Statement stmt = connection.createStatement()) {
                stmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS homes (
                            player_uuid TEXT,
                            home_name TEXT,
                            world_name TEXT,
                            x REAL,
                            y REAL,
                            z REAL,
                            PRIMARY KEY (player_uuid, home_name))
                        """);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Result<Void, DatabaseError> updateHome(Player player, String homeName, Location location) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "UPDATE homes SET world_name = ?, x = ?, y = ?, z = ? WHERE player_uuid = ? AND home_name = ?")) {
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
        try (PreparedStatement stmt = connection.prepareStatement(
                "DELETE FROM homes WHERE player_uuid = ? AND home_name = ?")) {
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
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO homes (player_uuid, home_name, world_name, x, y, z) VALUES (?, ?, ?, ?, ?, ?)")) {
            stmt.setString(1, player.getUniqueId().toString());
            stmt.setString(2, homeName);
            stmt.setString(3, location.getWorld().getName());
            stmt.setDouble(4, location.getX());
            stmt.setDouble(5, location.getY());
            stmt.setDouble(6, location.getZ());
            stmt.executeUpdate();
            return Optional.of(true);
        } catch (SQLException e) {
            if (e.getErrorCode() == 19) { // SQLite constraint violation (primary key)
                return Optional.of(false);
            } else {
                return Optional.empty();
            }
        }
    }

    @Override
    public Optional<Location> getHomeLocation(Player player, String homeName) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT world_name, x, y, z FROM homes WHERE player_uuid = ? AND home_name = ?")) {
            stmt.setString(1, player.getUniqueId().toString());
            stmt.setString(2, homeName);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    var worldName = rs.getString("world_name");
                    var x = rs.getDouble("x");
                    var y = rs.getDouble("y");
                    var z = rs.getDouble("z");
                    var world = Bukkit.getWorld(worldName);
                    if (world == null) {
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
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT home_name FROM homes WHERE player_uuid = ?")) {
            stmt.setString(1, player.getUniqueId().toString());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    homes.add(rs.getString("home_name"));
                }
            }
        } catch (SQLException e) {
            return homes;
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
