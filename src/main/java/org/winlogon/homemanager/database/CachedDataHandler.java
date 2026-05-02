package org.winlogon.homemanager.database;

import com.github.walker84837.JResult.Result;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.winlogon.homemanager.DataHandler;
import org.winlogon.homemanager.DatabaseError;
import org.winlogon.homemanager.Home;
import org.winlogon.homemanager.PaginatedResult;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class CachedDataHandler implements DataHandler {
    private final DataHandler delegate;
    private final Map<UUID, List<String>> homeNamesCache = new ConcurrentHashMap<>();

    public CachedDataHandler(DataHandler delegate) {
        this.delegate = delegate;
    }

    @Override
    public List<String> getHomesCached(UUID playerUuid) {
        return homeNamesCache.computeIfAbsent(playerUuid, uuid ->
            delegate.getPlayerHomes(uuid).stream().map(Home::homeName).toList()
        );
    }

    public void invalidate(UUID playerUuid) {
        homeNamesCache.remove(playerUuid);
    }

    @Override
    public Result<Void, DatabaseError> updateHome(Player player, String homeName, Location location) {
        var result = delegate.updateHome(player, homeName, location);
        if (result.isOk()) {
            invalidate(player.getUniqueId());
        }
        return result;
    }

    @Override
    public Result<Void, DatabaseError> deleteHome(Player player, String homeName) {
        var result = delegate.deleteHome(player, homeName);
        if (result.isOk()) {
            invalidate(player.getUniqueId());
        }
        return result;
    }

    @Override
    public Optional<Boolean> createHome(Player player, String homeName, Location location) {
        var result = delegate.createHome(player, homeName, location);
        result.ifPresent(created -> {
            if (created) invalidate(player.getUniqueId());
        });
        return result;
    }

    @Override
    public Optional<Location> getHomeLocation(Player player, String homeName) {
        return delegate.getHomeLocation(player, homeName);
    }

    @Override
    public CompletableFuture<Optional<Location>> getHomeLocationAsync(Player player, String homeName) {
        return delegate.getHomeLocationAsync(player, homeName);
    }

    @Override
    public CompletableFuture<List<String>> getHomesAsync(Player player) {
        return delegate.getHomesAsync(player);
    }

    @Override
    public List<String> getHomes(Player player) {
        return delegate.getHomes(player);
    }

    @Override
    public List<Home> getAllHomes() {
        return delegate.getAllHomes();
    }

    @Override
    public List<Home> getPlayerHomes(UUID playerUuid) {
        return delegate.getPlayerHomes(playerUuid);
    }

    @Override
    public Result<Void, DatabaseError> importHome(Home home) {
        var result = delegate.importHome(home);
        if (result.isOk()) {
            invalidate(home.playerUuid());
        }
        return result;
    }

    @Override
    public Result<Void, DatabaseError> importHomes(List<Home> homes) {
        var result = delegate.importHomes(homes);
        if (result.isOk()) {
            homes.stream().map(Home::playerUuid).distinct().forEach(this::invalidate);
        }
        return result;
    }

    @Override
    public Result<Void, DatabaseError> deleteHomeByUuid(UUID playerUuid, String homeName) {
        var result = delegate.deleteHomeByUuid(playerUuid, homeName);
        if (result.isOk()) {
            invalidate(playerUuid);
        }
        return result;
    }

    @Override
    public boolean homeExists(UUID playerUuid, String homeName) {
        return delegate.homeExists(playerUuid, homeName);
    }

    @Override
    public PaginatedResult getHomesPaginated(Player player, int page, int pageSize) {
        return delegate.getHomesPaginated(player, page, pageSize);
    }
}
