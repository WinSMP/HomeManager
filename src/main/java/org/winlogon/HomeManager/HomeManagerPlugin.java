package org.winlogon.HomeManager;

import org.bukkit.plugin.java.JavaPlugin;

public class HomeManagerPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        saveDefaultConfig();
        DatabaseHandler.setupDatabase(getDataFolder());
        
        getCommand("home").setExecutor(new HomeCommandExecutor());
        getCommand("home").setTabCompleter(new HomeTabCompleter());
    }

    @Override
    public void onDisable() {
        DatabaseHandler.closeConnection();
    }
}
