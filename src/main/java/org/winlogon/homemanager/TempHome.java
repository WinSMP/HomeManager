package org.winlogon.homemanager;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

public class TempHome {
    private final String worldName;
    private final double x;
    private final double y;
    private final double z;
    private final long creationTime;

    public TempHome(Location location) {
        this.worldName = location.getWorld().getName();
        this.x = location.getX();
        this.y = location.getY();
        this.z = location.getZ();
        this.creationTime = System.currentTimeMillis();
    }

    public Location getLocation() {
        World world = Bukkit.getWorld(worldName);
        if (world == null) return null;
        return new Location(world, x, y, z);
    }

    public boolean isExpired(long currentTime) {
        return currentTime - creationTime > 30 * 60 * 1000; // 30 minutes
    }

    public String getWorldName() { return worldName; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
}
