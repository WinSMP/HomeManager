package org.winlogon.HomeManager;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.List;

public class HomeCommandExecutor implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return true;
        }

        Player player = (Player) sender;

        try {
            if (args.length < 1) {
                showHelp(player);
                return true;
            }

            switch (args[0].toLowerCase()) {
                case "create":
                    if (args.length != 2) {
                        player.sendMessage("§eUsage: /home create <house name>");
                        return true;
                    }
                    String homeName = args[1];
                    DatabaseHandler.createHome(player, homeName, player.getLocation());
                    player.sendMessage("§7Home §3" + homeName + "§7 created.");
                    break;

                case "list":
                    List<String> homes = DatabaseHandler.getHomes(player);
                    String homeList = homes.isEmpty() ? "None" : String.join(", ", homes);
                    player.sendMessage("§7Your homes: §3" + homeList);
                    break;

                case "delete":
                    if (args.length != 2) {
                        player.sendMessage("§eUsage: /home delete <house name>");
                        return true;
                    }
                    String homeToDelete = args[1];
                    DatabaseHandler.deleteHome(player, homeToDelete);
                    player.sendMessage("§cHome " + homeToDelete + " deleted.");
                    break;

                case "set":
                    if (args.length != 2) {
                        player.sendMessage("§eUsage: /home set <house name>");
                        return true;
                    }
                    String homeToUpdate = args[1];
                    // Check if the home exists before updating
                    if (DatabaseHandler.getHomeLocation(player, homeToUpdate) == null) {
                        player.sendMessage("§cHome " + homeToUpdate + " does not exist. Use /home create to make a new home.");
                    } else {
                        DatabaseHandler.updateHome(player, homeToUpdate, player.getLocation());
                        player.sendMessage("§7Home §3" + homeToUpdate + "§7 updated!");
                    }
                    break;

                case "teleport":
                    if (args.length != 2) {
                        player.sendMessage("§eUsage: /home teleport <house name>");
                        return true;
                    }
                    String homeToTeleport = args[1];
                    Location homeLocation = DatabaseHandler.getHomeLocation(player, homeToTeleport);
                    if (homeLocation == null) {
                        player.sendMessage("§cHome " + homeToTeleport + " does not exist.");
                    } else {
                        teleportPlayer(player, homeLocation);
                        player.sendMessage("§7Teleported to home: §3" + homeToTeleport);
                    }
                    break;

                case "help":
                default:
                    showHelp(player);
                    break;
            }
        } catch (SQLException e) {
            if (e.getMessage().equals("Home already exists")) {
                player.sendMessage("§cA home with that name already exists.");
            } else if (e.getMessage().equals("Home does not exist")) {
                player.sendMessage("§cThat home does not exist. Use /home create to make a new home.");
            } else {
                player.sendMessage("§cAn error occurred while processing your command.");
                e.printStackTrace();
            }
        }

        return true;
    }

    private void showHelp(Player player) {
        player.sendMessage("§6Home Commands:");
        player.sendMessage("- §e/home create <house name> §f:: Create a new home at your current position.");
        player.sendMessage("- §e/home list §f:: List all your homes.");
        player.sendMessage("- §e/home delete <house name> §f:: Delete a home.");
        player.sendMessage("- §e/home set <house name> §f:: Update a home location to be at your position.");
        player.sendMessage("- §e/home teleport <house name> §f:: Teleport to a saved home.");
        player.sendMessage("- §e/home help §f:: Show this help menu.");
    }

    private void teleportPlayer(Player player, Location location) {
        if (isFolia()) {
            player.teleportAsync(location);
        } else {
            player.teleport(location);
        }
    }

    private boolean isFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
