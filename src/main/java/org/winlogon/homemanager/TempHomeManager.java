package org.winlogon.homemanager;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class TempHomeManager {
    private final ConcurrentHashMap<UUID, Map<Integer, TempHome>> playerTempHomes = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, AtomicInteger> playerNextId = new ConcurrentHashMap<>();

    public TempHomeManager(JavaPlugin plugin, boolean isFolia) {
        final long interval = 20 * 60 * 5; // 30 minutes
        if (isFolia) {
            return;
        } else {
            Bukkit.getScheduler().runTaskTimer(plugin, this::cleanupExpiredHomes, interval, interval);
        }
    }

    public int createTempHome(Player player) {
        var uuid = player.getUniqueId();
        var id = playerNextId.computeIfAbsent(uuid, k -> new AtomicInteger(0)).getAndIncrement();
        Map<Integer, TempHome> homes = playerTempHomes.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>());
        homes.put(id, new TempHome(player.getLocation()));
        return id;
    }

    public List<Integer> getTempHomeIds(UUID playerId) {
        var homes = playerTempHomes.get(playerId);
        if (homes == null) return Collections.emptyList();

        long now = System.currentTimeMillis();
        List<Integer> validIds = new ArrayList<>();

        var iterator = homes.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (entry.getValue().isExpired(now)) {
                iterator.remove();
            } else {
                validIds.add(entry.getKey());
            }
        }
        return validIds;
    }

    public TempHome getTempHome(UUID playerId, int id) {
        var homes = playerTempHomes.get(playerId);
        if (homes == null) return null;

        TempHome tempHome = homes.get(id);
        if (tempHome == null) return null;

        long now = System.currentTimeMillis();
        if (tempHome.isExpired(now)) {
            homes.remove(id);
            return null;
        }
        return tempHome;
    }

    public boolean removeTempHome(UUID playerId, int id) {
        var homes = playerTempHomes.get(playerId);
        return homes != null && homes.remove(id) != null;
    }

    private void cleanupExpiredHomes() {
        long now = System.currentTimeMillis();
        playerTempHomes.forEach((uuid, homes) -> {
            homes.entrySet().removeIf(entry -> entry.getValue().isExpired(now));
        });
    }
}
