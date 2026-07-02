package org.winlogon.homemanager.database;

import org.bukkit.plugin.java.JavaPlugin;

/**
 * Minimal plugin subclass that avoids calling CommandAPI.onLoad(),
 * since the shaded CommandAPIVersionHandler interacts poorly with MockBukkit v26.1.2.
 */
public class TestPlugin extends JavaPlugin {
    @Override
    public void onLoad() {
    }
}
