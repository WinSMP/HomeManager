package org.winlogon.homemanager;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import com.github.walker84837.JResult.Result;

import java.util.List;
import java.util.Optional;

public interface DataHandler {
    Result<Void, DatabaseError> updateHome(Player player, String homeName, Location location);
    Result<Void, DatabaseError> deleteHome(Player player, String homeName);
    /**
     * @return true if home was created, false if it already existed, or an empty optional if there was an error
     */
    Optional<Boolean> createHome(Player player, String homeName, Location location);
    Optional<Location> getHomeLocation(Player player, String homeName);
    List<String> getHomes(Player player);
    void closeConnection();
}
