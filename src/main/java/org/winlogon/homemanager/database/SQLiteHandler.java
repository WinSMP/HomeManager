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
import java.util.concurrent.CompletableFuture;
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
    public CompletableFuture<Result<Void, DatabaseError>> updateHome(Player player, String homeName, Location location) {
        CompletableFuture<Result<Void, DatabaseError>> future = new CompletableFuture<>();
        var sql = "UPDATE homes SET world_name = ?, x = ?, y = ?, z = ? WHERE player_uuid = ? AND home_name = ?";
        dbManager.execute(connection -> {
            try (var stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, location.getWorld().getName());
                stmt.setDouble(2, location.getX());
                stmt.setDouble(3, location.getY());
                stmt.setDouble(4, location.getZ());
                stmt.setString(5, player.getUniqueId().toString());
                stmt.setString(6, homeName);
                int rowsUpdated = stmt.executeUpdate();
                if (rowsUpdated > 0) {
                    future.complete(Result.ok(null));
                } else {
                    future.complete(Result.err(DatabaseError.HOME_NOT_FOUND));
                }
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Error updating home for " + player.getName(), e);
                future.complete(Result.err(DatabaseError.SQL_EXCEPTION));
            }
        });
        return future;
    }

    @Override
    public CompletableFuture<Result<Void, DatabaseError>> deleteHome(Player player, String homeName) {
        CompletableFuture<Result<Void, DatabaseError>> future = new CompletableFuture<>();
        String sql = "DELETE FROM homes WHERE player_uuid = ? AND home_name = ?";
        dbManager.execute(connection -> {
            try (var stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, player.getUniqueId().toString());
                stmt.setString(2, homeName);
                int rowsDeleted = stmt.executeUpdate();
                if (rowsDeleted > 0) {
                    future.complete(Result.ok(null));
                } else {
                    future.complete(Result.err(DatabaseError.HOME_NOT_FOUND));
                }
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Error deleting home for " + player.getName(), e);
                future.complete(Result.err(DatabaseError.SQL_EXCEPTION));
            }
        });
        return future;
    }

    @Override
    public CompletableFuture<Optional<Boolean>> createHome(Player player, String homeName, Location location) {
        CompletableFuture<Optional<Boolean>> future = new CompletableFuture<>();
        String sql = "INSERT INTO homes (player_uuid, home_name, world_name, x, y, z) VALUES (?, ?, ?, ?, ?, ?)";
        dbManager.execute(connection -> {
            try (var stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, player.getUniqueId().toString());
                stmt.setString(2, homeName);
                stmt.setString(3, location.getWorld().getName());
                stmt.setDouble(4, location.getX());
                stmt.setDouble(5, location.getY());
                stmt.setDouble(6, location.getZ());
                stmt.executeUpdate();
                future.complete(Optional.of(true));
            } catch (SQLException e) {
                if (e.getErrorCode() == 19) { // SQLite constraint violation
                    future.complete(Optional.of(false));
                } else {
                    logger.log(Level.SEVERE, "Error creating home for " + player.getName(), e);
                    future.complete(Optional.empty());
                }
            }
        });
        return future;
    }

    @Override
    public CompletableFuture<Optional<Location>> getHomeLocation(Player player, String homeName) {
        CompletableFuture<Optional<Location>> future = new CompletableFuture<>();
        String sql = "SELECT world_name, x, y, z FROM homes WHERE player_uuid = ? AND home_name = ?";
        dbManager.execute(connection -> {
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
                            future.complete(Optional.empty());
                        } else {
                            future.complete(Optional.of(new Location(world, x, y, z)));
                        }
                    } else {
                        future.complete(Optional.empty());
                    }
                }
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Error getting home location for " + player.getName(), e);
                future.complete(Optional.empty());
            }
        });
        return future;
    }

    @Override
    public CompletableFuture<List<String>> getHomes(Player player) {
        CompletableFuture<List<String>> future = new CompletableFuture<>();
        List<String> homes = new ArrayList<>();
        String sql = "SELECT home_name FROM homes WHERE player_uuid = ?";
        dbManager.execute(connection -> {
            try (var stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, player.getUniqueId().toString());
                try (var rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        homes.add(rs.getString("home_name"));
                    }
                    future.complete(homes);
                }
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Error getting homes for " + player.getName(), e);
                future.complete(new ArrayList<>()); // Return empty list on error
            }
        });
        return future;
    }
}
