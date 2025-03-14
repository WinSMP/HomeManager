package org.winlogon.homemanager;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.StringArgument;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.List;

public class CommandHandler {
    
    public static void registerCommands() {
        new CommandAPICommand("home")
            .withPermission(CommandPermission.fromString("homemanager.use"))
            .withSubcommand(new CommandAPICommand("create")
                .withArguments(new StringArgument("name"))
                .executesPlayer((player, args) -> {
                    String homeName = (String) args.get("name");
                    try {
                        DatabaseHandler.createHome(player, homeName, player.getLocation());
                        player.sendMessage("§7Home §3" + homeName + "§7 created.");
                    } catch (SQLException e) {
                        player.sendMessage("§cA home with that name already exists.");
                    }
                })
            )
            .withSubcommand(new CommandAPICommand("list")
                .executesPlayer((player, args) -> {
                    try {
                        List<String> homes = DatabaseHandler.getHomes(player);
                        String homeList = homes.isEmpty() ? "None" : String.join(", ", homes);
                        player.sendMessage("§7Your homes: §3" + homeList);
                    } catch (SQLException e) {
                        player.sendMessage("§cAn error occurred while fetching your homes.");
                    }
                })
            )
            .withSubcommand(new CommandAPICommand("delete")
                .withArguments(new StringArgument("home-name").replaceSuggestions(ArgumentSuggestions.strings((info) -> {
                    CommandSender sender = info.sender();
                    if (sender instanceof Player player) {
                        try {
                            return DatabaseHandler.getHomes(player).toArray(new String[0]);
                        } catch (SQLException e) {
                            return new String[0];
                        }
                    }
                    return new String[0];
                })))
                .executesPlayer((player, args) -> {
                    String homeName = (String) args.get("home-name");
                    try {
                        DatabaseHandler.deleteHome(player, homeName);
                        player.sendMessage("§cHome " + homeName + " deleted.");
                    } catch (SQLException e) {
                        player.sendMessage("§cThat home does not exist.");
                    }
                })
            )
                .withSubcommand(new CommandAPICommand("set")
                    .withArguments(new StringArgument("home-name").replaceSuggestions(ArgumentSuggestions.strings((info) -> {
                        CommandSender sender = info.sender();
                        if (sender instanceof Player player) {
                            try {
                                return DatabaseHandler.getHomes(player).toArray(new String[0]);
                            } catch (SQLException e) {
                                return new String[0];
                            }
                        }
                        return new String[0];
                    }))) // Added an extra closing parenthesis here
                    .executesPlayer((player, args) -> {
                        String homeName = (String) args.get("home-name");
                        try {
                            if (DatabaseHandler.getHomeLocation(player, homeName) == null) {
                                player.sendMessage("§cHome " + homeName + " does not exist.");
                            } else {
                                DatabaseHandler.updateHome(player, homeName, player.getLocation());
                                player.sendMessage("§7Home §3" + homeName + "§7 updated!");
                            }
                        } catch (SQLException e) {
                            player.sendMessage("§cAn error occurred while updating your home.");
                        }
                    })
                )
            .withSubcommand(new CommandAPICommand("teleport")
                .withArguments(new StringArgument("home-name").replaceSuggestions(ArgumentSuggestions.strings((info) -> {
                    CommandSender sender = info.sender();
                    if (sender instanceof Player player) {
                        try {
                            return DatabaseHandler.getHomes(player).toArray(new String[0]);
                        } catch (SQLException e) {
                            return new String[0];
                        }
                    }
                    return new String[0];
                })))
                .executesPlayer((player, args) -> {
                    String homeName = (String) args.get("home-name");
                    try {
                        Location homeLocation = DatabaseHandler.getHomeLocation(player, homeName);
                        if (homeLocation == null) {
                            player.sendMessage("§cHome " + homeName + " does not exist.");
                        } else {
                            teleportPlayer(player, homeLocation);
                            player.sendMessage("§7Teleported to home: §3" + homeName);
                        }
                    } catch (SQLException e) {
                        player.sendMessage("§cAn error occurred while teleporting.");
                    }
                })
            )
            .register();
    }
    
    private static void teleportPlayer(Player player, Location location) {
        if (isFolia()) {
            player.teleportAsync(location);
        } else {
            player.teleport(location);
        }
    }

    private static boolean isFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
