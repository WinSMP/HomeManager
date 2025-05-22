package org.winlogon.homemanager;

import org.bukkit.plugin.java.JavaPlugin;

public class HomeManagerPlugin extends JavaPlugin {
    private SQLiteHandler databaseHandler;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        databaseHandler = new SQLiteHandler(getDataFolder());
        var commandHandler = new CommandHandler(databaseHandler);
        commandHandler.registerCommands();
    }

    @Override
    public void onDisable() {
        databaseHandler.closeConnection();
    }
}
