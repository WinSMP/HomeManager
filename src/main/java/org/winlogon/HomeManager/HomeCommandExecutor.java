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
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
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
                        player.sendMessage(ChatColor.YELLOW + "Usage: /home create <house name>");
                        return true;
                    }
                    String homeName = args[1];
                    DatabaseHandler.saveHome(player, homeName, player.getLocation());
                    player.sendMessage(ChatColor.GREEN + "Home " + ChatColor.DARK_AQUA + homeName + ChatColor.GREEN + " created.");
                    break;

                case "list":
                    List<String> homes = DatabaseHandler.getHomes(player);
                    player.sendMessage(ChatColor.AQUA + "Your homes: " + 
                        (homes.isEmpty() ? ChatColor.GRAY + "None" : ChatColor.GRAY + String.join(", ", homes)));
                    break;

                case "delete":
                    if (args.length != 2) {
                        player.sendMessage(ChatColor.YELLOW + "Usage: /home delete <house name>");
                        return true;
                    }
                    String homeToDelete = args[1];
                    DatabaseHandler.deleteHome(player, homeToDelete);
                    player.sendMessage(ChatColor.RED + "Home " + homeToDelete + " deleted.");
                    break;

                case "set":
                    if (args.length != 2) {
                        player.sendMessage(ChatColor.YELLOW + "Usage: /home set <house name>");
                        return true;
                    }
                    String homeToUpdate = args[1];
                    DatabaseHandler.saveHome(player, homeToUpdate, player.getLocation());
                    player.sendMessage(ChatColor.GREEN + "Home " + homeToUpdate + " updated!");
                    break;

                case "teleport":
                    if (args.length != 2) {
                        player.sendMessage(ChatColor.YELLOW + "Usage: /home teleport <house name>");
                        return true;
                    }
                    String homeToTeleport = args[1];
                    Location homeLocation = DatabaseHandler.getHomeLocation(player, homeToTeleport);
                    if (homeLocation == null) {
                        player.sendMessage(ChatColor.RED + "Home " + homeToTeleport + " does not exist.");
                    } else {
                        player.teleport(homeLocation);
                        player.sendMessage(ChatColor.GREEN + "Teleported to home: " + homeToTeleport);
                    }
                    break;

                case "help":
                default:
                    showHelp(player);
                    break;
            }
        } catch (SQLException e) {
            player.sendMessage(ChatColor.RED + "An error occurred while processing your command.");
            e.printStackTrace();
        }

        return true;
    }

    private void showHelp(Player player) {
        player.sendMessage(ChatColor.GOLD + "Home Commands:");
        player.sendMessage("- " + ChatColor.YELLOW + "/home create <house name> " + ChatColor.WHITE + ":: Create a new home at your current position.");
        player.sendMessage("- " + ChatColor.YELLOW + "/home list " + ChatColor.WHITE + ":: List all your homes.");
        player.sendMessage("- " + ChatColor.YELLOW + "/home delete <house name> " + ChatColor.WHITE + ":: Delete a home.");
        player.sendMessage("- " + ChatColor.YELLOW + "/home set <house name> " + ChatColor.WHITE + ":: Update a home location to be at your position.");
        player.sendMessage("- " + ChatColor.YELLOW + "/home teleport <house name> " + ChatColor.WHITE + ":: Teleport to a saved home.");
        player.sendMessage("- " + ChatColor.YELLOW + "/home help " + ChatColor.WHITE + ":: Show this help menu.");
    }
}
