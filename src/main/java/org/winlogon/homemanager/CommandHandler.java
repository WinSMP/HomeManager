package org.winlogon.homemanager;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.executors.CommandArguments;
import dev.jorel.commandapi.arguments.StringArgument;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.sql.SQLException;

public class CommandHandler {
    private DatabaseHandler databaseHandler;

    public CommandHandler(DatabaseHandler databaseHandler) {
        this.databaseHandler = databaseHandler;
    }
    
    public void registerCommands() {
        new CommandAPICommand("home")
            .withShortDescription("Manage your homes")
            .withSubcommand(new CommandAPICommand("create")
                .withArguments(new StringArgument("name"))
                .executesPlayer((player, args) -> {
                    createHome(args, player);
                })
            )
            .withSubcommand(new CommandAPICommand("list")
                .executesPlayer((player, args) -> {
                    listHomes(args, player);
                })
            )
            .withSubcommand(new CommandAPICommand("delete")
                .withArguments(new StringArgument("home-name").replaceSuggestions(ArgumentSuggestions.strings((info) -> {
                    CommandSender sender = info.sender();
                    return getHomesOfSender(sender);
                })))
                .executesPlayer((player, args) -> {
                    deleteHome(args, player);
                })
            )
            .withSubcommand(new CommandAPICommand("set")
                .withArguments(new StringArgument("home-name").replaceSuggestions(ArgumentSuggestions.strings((info) -> {
                    CommandSender sender = info.sender();
                    return getHomesOfSender(sender);
                })))
                .executesPlayer((player, args) -> {
                    setHome(args, player);
                })
            )
            .withSubcommand(new CommandAPICommand("teleport")
                .withArguments(new StringArgument("home-name").replaceSuggestions(ArgumentSuggestions.strings((info) -> {
                    CommandSender sender = info.sender();
                    return getHomesOfSender(sender);
                })))
                .executesPlayer((player, args) -> {
                    teleportHome(args, player);
                })
            )
            .register();
    }

    private void setHome(CommandArguments args, Player player) {
        var homeName = (String) args.get("home-name");
        try {
            if (databaseHandler.getHomeLocation(player, homeName) == null) {
                player.sendMessage("§cHome " + homeName + " does not exist.");
            } else {
                databaseHandler.updateHome(player, homeName, player.getLocation());
                player.sendMessage("§7Home §3" + homeName + "§7 updated!");
            }
        } catch (SQLException e) {
            player.sendMessage("§cAn error occurred while updating your home.");
        }
    }

    private void teleportHome(CommandArguments args, Player player) {
        var homeName = (String) args.get("home-name");
        try {
            Location homeLocation = databaseHandler.getHomeLocation(player, homeName);
            if (homeLocation == null) {
                player.sendMessage("§cHome " + homeName + " does not exist.");
            } else {
                teleportPlayer(player, homeLocation);
                player.sendMessage("§7Teleported to home: §3" + homeName);
            }
        } catch (SQLException e) {
            player.sendMessage("§cAn error occurred while teleporting.");
        }
    }

    private void deleteHome(CommandArguments args, Player player) {
        var homeName = (String) args.get("home-name");
        try {
            databaseHandler.deleteHome(player, homeName);
            player.sendMessage("§cHome " + homeName + " deleted.");
        } catch (SQLException e) {
            player.sendMessage("§cThat home does not exist.");
        }
    }

    private void createHome(CommandArguments args, Player player) {
        var homeName = (String) args.get("name");
        try {
            databaseHandler.createHome(player, homeName, player.getLocation());
            player.sendMessage("§7Home §3" + homeName + "§7 created.");
        } catch (SQLException e) {
            player.sendMessage("§cA home with that name already exists.");
        }
    }

    private void listHomes(CommandArguments args, Player player) {
        try {
            var homes = databaseHandler.getHomes(player);
            var homeList = homes.isEmpty() ? "None" : String.join(", ", homes);
            player.sendMessage("§7Your homes: §3" + homeList);
        } catch (SQLException e) {
            player.sendMessage("§cAn error occurred while fetching your homes.");
        }
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

    private String[] getHomesOfSender(CommandSender sender) {
        if (sender instanceof Player player) {
            try {
                return databaseHandler.getHomes(player).toArray(new String[0]);
            } catch (SQLException e) {
                return new String[0];
            }
        }
        return new String[0];
    }
}
