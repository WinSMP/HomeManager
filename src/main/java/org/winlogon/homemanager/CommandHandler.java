package org.winlogon.homemanager;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.executors.CommandArguments;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import dev.jorel.commandapi.arguments.StringArgument;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.Optional;

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
                    return getHomesOfSender(info.sender());
                })))
                .executesPlayer((player, args) -> {
                    deleteHome(args, player);
                })
            )
            .withSubcommand(new CommandAPICommand("set")
                .withArguments(new StringArgument("home-name").replaceSuggestions(ArgumentSuggestions.strings((info) -> {
                    return getHomesOfSender(info.sender());
                })))
                .executesPlayer((player, args) -> {
                    setHome(args, player);
                })
            )
            .withSubcommand(new CommandAPICommand("teleport")
                .withArguments(new StringArgument("home-name").replaceSuggestions(ArgumentSuggestions.strings((info) -> {
                    return getHomesOfSender(info.sender());
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
                player.sendRichMessage(
                    "<red>Home <home-name> does not exist.</red>", 
                    Placeholder.component("home-name", Component.text(homeName, NamedTextColor.DARK_AQUA))
                );
            } else {
                databaseHandler.updateHome(player, homeName, player.getLocation());
                player.sendRichMessage(
                    "<gray>Home <home-name> updated!</gray>",
                    Placeholder.component("home-name", Component.text(homeName, NamedTextColor.DARK_AQUA))
                );
            }
        } catch (SQLException e) {
            player.sendRichMessage("<red>An error occurred while updating your home.</red>");
        }
    }

    private void teleportHome(CommandArguments args, Player player) {
        var homeName = (String) args.get("home-name");
        try {
            var homeLocation = Optional.ofNullable(databaseHandler.getHomeLocation(player, homeName));
            if (homeLocation.isPresent()) {
                teleportPlayer(player, homeLocation.get());
                player.sendRichMessage(
                    "<gray>Teleported to home: <home-name></gray>",
                    Placeholder.component("home-name", Component.text(homeName, NamedTextColor.DARK_AQUA))
                );
            } else {
                player.sendRichMessage(
                    "<red>Home <home-name> does not exist.</red>",
                    Placeholder.component("home-name", Component.text(homeName, NamedTextColor.DARK_AQUA))
                );
            }
        } catch (SQLException e) {
            player.sendRichMessage("<red>An error occurred while teleporting.</red>");
        }
    }

    private void deleteHome(CommandArguments args, Player player) {
        var homeName = (String) args.get("home-name");
        try {
            databaseHandler.deleteHome(player, homeName);
            player.sendRichMessage(
                "<red>Home <home-name> deleted.</red>",
                Placeholder.component("home-name", Component.text(homeName, NamedTextColor.DARK_AQUA))
            );
        } catch (SQLException e) {
            player.sendRichMessage("<red>That home does not exist.</red>");
        }
    }

    private void createHome(CommandArguments args, Player player) {
        var homeName = (String) args.get("name");
        try {
            databaseHandler.createHome(player, homeName, player.getLocation());
            player.sendRichMessage(
                "<gray>Home <home-name> created.</gray>",
                Placeholder.component("home-name", Component.text(homeName, NamedTextColor.DARK_AQUA))
            );
        } catch (SQLException e) {
            player.sendRichMessage("<red>A home with that name already exists.</red>");
        }
    }

    private void listHomes(CommandArguments args, Player player) {
        try {
            var homes = databaseHandler.getHomes(player);
            var homeList = homes.isEmpty() ? "None" : String.join(", ", homes);
            player.sendRichMessage("""
                <gray>Your homes: <homes></gray>
                """,
                Placeholder.component("homes", Component.text(homeList, NamedTextColor.DARK_AQUA))
            );

        } catch (SQLException e) {
            player.sendRichMessage("<red>An error occurred while fetching your homes.</red>");
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
        } catch (ClassNotFoundException _) {
            return false;
        }
    }

    private String[] getHomesOfSender(CommandSender sender) {
        if (sender instanceof Player player) {
            try {
                return databaseHandler.getHomes(player).toArray(String[]::new);
            } catch (SQLException e) {
                return new String[0];
            }
        }
        return new String[0];
    }
}
