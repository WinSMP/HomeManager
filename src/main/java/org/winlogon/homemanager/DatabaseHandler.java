package org.winlogon.homemanager;

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

    /**
     * Update the home location in the database
     *
     * @param player The player who owns the home
     * @param homeName The name of the home
     * @param location The new location of the home
     */
    public static void updateHome(Player player, String homeName, Location location) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(
                "UPDATE homes SET world_name = ?, x = ?, y = ?, z = ? WHERE player_uuid = ? AND home_name = ?")) {
            stmt.setString(1, location.getWorld().getName());
            stmt.setDouble(2, location.getX());
            stmt.setDouble(3, location.getY());
            stmt.setDouble(4, location.getZ());
            stmt.setString(5, player.getUniqueId().toString());
            stmt.setString(6, homeName);
            int rowsUpdated = stmt.executeUpdate();
            if (rowsUpdated == 0) {
                throw new SQLException("Home does not exist");
            }
        }
    }

    /**
     * Create a new home in the database
     * 
     * @param player The player who owns the home
     * @param homeName The name of the home
     * @param location The location of the home
     * @throws SQLException If the home already exists
     */
    public static void createHome(Player player, String homeName, Location location) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO homes (player_uuid, home_name, world_name, x, y, z) VALUES (?, ?, ?, ?, ?, ?)")) {
            stmt.setString(1, player.getUniqueId().toString());
            stmt.setString(2, homeName);
            stmt.setString(3, location.getWorld().getName());
            stmt.setDouble(4, location.getX());
            stmt.setDouble(5, location.getY());
            stmt.setDouble(6, location.getZ());
            stmt.executeUpdate();
        } catch (SQLException e) {
            if (e.getErrorCode() == 19) { // SQLite constraint violation (primary key)
                throw new SQLException("Home already exists");
            } else {
                throw e;
            }
        }
    }

    /**
     * Get the location of a home
     *  
     * @param player The player who owns the home
     * @param homeName The name of the home
     * @return The location of the home
     * @throws SQLException If the home does not exist
     */
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

    /**
     * Get all homes owned by a player
     * 
     * @param player The player who owns the homes
     * @return A list of home names
     * @throws SQLException If there is a database error
     */
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

    /**
     * Delete a home from the database
     * 
     * @param player The player who owns the home
     * @param homeName The name of the home
     * @throws SQLException If the home does not exist
     */
    public static void deleteHome(Player player, String homeName) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(
                "DELETE FROM homes WHERE player_uuid = ? AND home_name = ?")) {
            stmt.setString(1, player.getUniqueId().toString());
            stmt.setString(2, homeName);
            stmt.executeUpdate();
        }
    }

    /**
     * Close the database connection
     */
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
