package org.winlogon.homemanager;

import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.util.UUID;

public record Home(
    UUID playerUuid,
    String homeName,
    String worldName,
    double x,
    double y,
    double z,
    Float yaw,
    Float pitch
) {
    public Location toLocation() {
        var world = Bukkit.getWorld(worldName);
        if (world == null) {
            return null;
        }
        float actualYaw = yaw != null ? yaw : 0f;
        float actualPitch = pitch != null ? pitch : 0f;
        return new Location(world, x, y, z, actualYaw, actualPitch);
    }

    public static Home fromLocation(UUID playerUuid, String homeName, Location location) {
        return new Home(
            playerUuid,
            homeName,
            location.getWorld().getName(),
            location.getX(),
            location.getY(),
            location.getZ(),
            location.getYaw(),
            location.getPitch()
        );
    }

    public Home withName(String newName) {
        return new Home(playerUuid, newName, worldName, x, y, z, yaw, pitch);
    }

    public Home withPlayerUuid(UUID newPlayerUuid) {
        return new Home(newPlayerUuid, homeName, worldName, x, y, z, yaw, pitch);
    }
}
