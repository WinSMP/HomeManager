package org.winlogon.homemanager;

import com.github.walker84837.JResult.Result;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface DataHandler {
    CompletableFuture<Result<Void, DatabaseError>> updateHome(Player player, String homeName, Location location);
    CompletableFuture<Result<Void, DatabaseError>> deleteHome(Player player, String homeName);
    CompletableFuture<Optional<Boolean>> createHome(Player player, String homeName, Location location);
    CompletableFuture<Optional<Location>> getHomeLocation(Player player, String homeName);
    CompletableFuture<List<String>> getHomes(Player player);
}
