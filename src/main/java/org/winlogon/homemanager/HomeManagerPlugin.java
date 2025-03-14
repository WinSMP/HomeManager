package org.winlogon.homemanager;

import org.bukkit.plugin.java.JavaPlugin;

public class HomeManagerPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        saveDefaultConfig();
        DatabaseHandler.setupDatabase(getDataFolder());
        CommandHandler.registerCommands();
    }

    @Override
    public void onDisable() {
        DatabaseHandler.closeConnection();
    }
}
