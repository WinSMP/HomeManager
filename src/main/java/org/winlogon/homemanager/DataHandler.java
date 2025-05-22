package org.winlogon.homemanager;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;

interface DataHandler {
    boolean updateHome(Player player, String homeName, Location location);
    boolean deleteHome(Player player, String homeName);
    Optional<Boolean> createHome(Player player, String homeName, Location location);
    Optional<Location> getHomeLocation(Player player, String homeName);
    List<String> getHomes(Player player);
    void closeConnection();
}
