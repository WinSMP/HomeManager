package org.winlogon.homemanager;

import com.github.walker84837.JResult.Result;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface DataHandler {
    Result<Void, DatabaseError> updateHome(Player player, String homeName, Location location);
    Result<Void, DatabaseError> deleteHome(Player player, String homeName);
    Optional<Boolean> createHome(Player player, String homeName, Location location);
    Optional<Location> getHomeLocation(Player player, String homeName);

    // Async versions
    CompletableFuture<Optional<Location>> getHomeLocationAsync(Player player, String homeName);
    CompletableFuture<List<String>> getHomesAsync(Player player);

    List<String> getHomes(Player player);

    /**
     * Returns a list of home names for the player.
     * Implementations may cache this list to avoid blocking the caller.
     */
    default List<String> getHomesCached(UUID playerUuid) {
        // Default implementation for non-caching handlers
        return getPlayerHomes(playerUuid).stream().map(Home::homeName).toList();
    }

    default List<String> getHomes(Player player, int offset, int limit) {
        return getHomes(player).subList(offset, Math.min(offset + limit, getHomes(player).size()));
    }

    List<Home> getAllHomes();
    List<Home> getPlayerHomes(UUID playerUuid);
    Result<Void, DatabaseError> importHome(Home home);
    Result<Void, DatabaseError> importHomes(List<Home> homes);
    Result<Void, DatabaseError> deleteHomeByUuid(UUID playerUuid, String homeName);
    boolean homeExists(UUID playerUuid, String homeName);

    default PaginatedResult getHomesPaginated(Player player, int page, int pageSize) {
        int totalHomes = getHomes(player).size();
        if (totalHomes == 0) {
            return new PaginatedResult(List.of(), 0);
        }
        int totalPages = (int) Math.ceil((double) totalHomes / pageSize);
        int actualPage = Math.max(1, Math.min(page, totalPages));
        var homes = getHomes(player, (actualPage - 1) * pageSize, pageSize);
        return new PaginatedResult(homes, totalPages);
    }
}
