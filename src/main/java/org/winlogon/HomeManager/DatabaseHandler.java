package org.winlogon.HomeManager;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHandler {

    private static Connection connection;

    public static void setupDatabase(File dataFolder) {
        try {
            File databaseFile = new File(dataFolder, "homes.db");
            connection = DriverManager.getConnection("jdbc:sqlite:" + databaseFile.getAbsolutePath());
            try (Statement stmt = connection.createStatement()) {
                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS homes (" +
                        "player_uuid TEXT, " +
                        "home_name TEXT, " +
                        "world_name TEXT, " +
                        "x REAL, " +
                        "y REAL, " +
                        "z REAL, " +
                        "PRIMARY KEY (player_uuid, home_name))");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void saveHome(Player player, String homeName, Location location) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(
                "REPLACE INTO homes (player_uuid, home_name, world_name, x, y, z) VALUES (?, ?, ?, ?, ?, ?)")) {
            stmt.setString(1, player.getUniqueId().toString());
            stmt.setString(2, homeName);
            stmt.setString(3, location.getWorld().getName());
            stmt.setDouble(4, location.getX());
            stmt.setDouble(5, location.getY());
            stmt.setDouble(6, location.getZ());
            stmt.executeUpdate();
        }
    }

    // New method to get the home location from the database
    public static Location getHomeLocation(Player player, String homeName) throws SQLException {
        Location location = null;
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT world_name, x, y, z FROM homes WHERE player_uuid = ? AND home_name = ?")) {
            stmt.setString(1, player.getUniqueId().toString());
            stmt.setString(2, homeName);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String worldName = rs.getString("world_name");
                    double x = rs.getDouble("x");
                    double y = rs.getDouble("y");
                    double z = rs.getDouble("z");

                    // Get the world object based on the world name from the database
                    location = new Location(Bukkit.getWorld(worldName), x, y, z);
                }
            }
        }
        return location;
    }

    public static List<String> getHomes(Player player) throws SQLException {
        List<String> homes = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT home_name FROM homes WHERE player_uuid = ?")) {
            stmt.setString(1, player.getUniqueId().toString());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    homes.add(rs.getString("home_name"));
                }
            }
        }
        return homes;
    }

    public static void deleteHome(Player player, String homeName) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(
                "DELETE FROM homes WHERE player_uuid = ? AND home_name = ?")) {
            stmt.setString(1, player.getUniqueId().toString());
            stmt.setString(2, homeName);
            stmt.executeUpdate();
        }
    }

    public static void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
