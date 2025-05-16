package org.winlogon.homemanager;

import org.bukkit.plugin.java.JavaPlugin;

public class HomeManagerPlugin extends JavaPlugin {
    private DatabaseHandler databaseHandler;
    private TempHomeManager tempHomeManager;
    private boolean isFolia;

    @Override
    public void onEnable() {
        this.isFolia = isFolia();
        saveDefaultConfig();
        databaseHandler = new DatabaseHandler(getDataFolder());
        tempHomeManager = new TempHomeManager(this, this.isFolia);
        var commandHandler = new CommandHandler(databaseHandler, tempHomeManager, this.isFolia);
        commandHandler.registerCommands();
    }

    @Override
    public void onDisable() {
        databaseHandler.closeConnection();
    }

    private boolean isFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException _) {
            return false;
        }
    }
}
