package org.winlogon.homemanager;

import com.github.walker84837.JResult.Result;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface DataHandler {
    Result<Void, DatabaseError> updateHome(Player player, String homeName, Location location);
    Result<Void, DatabaseError> deleteHome(Player player, String homeName);
    Optional<Boolean> createHome(Player player, String homeName, Location location);
    Optional<Location> getHomeLocation(Player player, String homeName);
    List<String> getHomes(Player player);

    default PaginatedResult getHomesPaginated(Player player, int page, int pageSize) {
        List<String> allHomes = getHomes(player);

        if (allHomes.isEmpty()) {
            return new PaginatedResult(new ArrayList<>(), 0);
        }

        int totalHomes = allHomes.size();
        int totalPages = (int) Math.ceil((double) totalHomes / pageSize);

        // Clamp the page number
        int actualPage = Math.max(1, Math.min(page, totalPages));

        int startIndex = (actualPage - 1) * pageSize;
        int endIndex = Math.min(startIndex + pageSize, totalHomes);

        List<String> paginatedHomes = allHomes.subList(startIndex, endIndex);
        return new PaginatedResult(paginatedHomes, totalPages);
    }
}
