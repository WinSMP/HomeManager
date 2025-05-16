package org.winlogon.homemanager;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.executors.CommandArguments;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;

public class CommandHandler {
    private DatabaseHandler databaseHandler;
    private TempHomeManager tempHomeManager;
    private boolean isFolia;

    public CommandHandler(DatabaseHandler databaseHandler, TempHomeManager tempHomeManager, final boolean isFolia) {
        this.databaseHandler = databaseHandler;
        this.tempHomeManager = tempHomeManager;
        this.isFolia = isFolia;
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
            .withSubcommand(new CommandAPICommand("temp")
                .withShortDescription("Manage temporary homes")
                .withSubcommand(new CommandAPICommand("create")
                    .executesPlayer((player, args) -> {
                        int id = tempHomeManager.createTempHome(player);
                        player.sendRichMessage(
                            "<gray>Temporary home created with ID <dark_aqua>" + id + "</dark_aqua>.</gray>"
                        );
                    }))
                .withSubcommand(new CommandAPICommand("list")
                    .executesPlayer((player, args) -> {
                        var playerId = player.getUniqueId();
                        var ids = tempHomeManager.getTempHomeIds(playerId);
                        if (ids.isEmpty()) {
                            player.sendRichMessage("<red>You have no temporary homes.</red>");
                            return;
                        }
                        List<Component> lines = new ArrayList<>();
                        lines.add(Component.text("Your temporary homes:", NamedTextColor.GRAY));
                        for (int id : ids) {
                            var tempHome = tempHomeManager.getTempHome(playerId, id);
                            if (tempHome == null) continue;
                            var worldName = tempHome.getWorldName();
                            var x = tempHome.getX();
                            var y = tempHome.getY();
                            var z = tempHome.getZ();
                            var line = Component.text()
                                .append(Component.text("ID: ", NamedTextColor.GRAY))
                                .append(Component.text(id, NamedTextColor.DARK_AQUA))
                                .append(Component.text(" - ", NamedTextColor.GRAY))
                                .append(Component.text(
                                    String.format("%s, %.1f, %.1f, %.1f", worldName, x, y, z),
                                    NamedTextColor.WHITE
                                ))
                                .build();
                            lines.add(line);
                        }
                        Component message = Component.join(JoinConfiguration.newlines(), lines);
                        player.sendMessage(message);
                    }))
                .withSubcommand(new CommandAPICommand("finalize")
                    .withArguments(new IntegerArgument("id")
                        .replaceSuggestions(ArgumentSuggestions.strings(info -> {
                            Player player = (Player) info.sender();
                            return tempHomeManager.getTempHomeIds(player.getUniqueId()).stream()
                                .map(String::valueOf)
                                .toArray(String[]::new);
                        }))
                    .withArguments(new StringArgument("name"))
                    .executesPlayer((player, args) -> {
                        var tempId = (int) args.get("temp_house_id");
                        var name = (String) args.get("name");
                        var playerId = player.getUniqueId();
                        var tempHome = tempHomeManager.getTempHome(playerId, tempId);
                        if (tempHome == null) {
                            player.sendRichMessage("<red>Invalid temporary home ID or it has expired.</red>");
                            return;
                        }
                        var location = tempHome.getLocation();
                        if (location == null || location.getWorld() == null) {
                            player.sendRichMessage("<red>The world for this temporary home is not loaded.</red>");
                            tempHomeManager.removeTempHome(playerId, tempId);
                            return;
                        }
                        try {
                            if (databaseHandler.getHomeLocation(player, name) != null) {
                                player.sendRichMessage(
                                    "<red>A home with that name already exists.</red>",
                                    Placeholder.component("home-name", Component.text(name, NamedTextColor.DARK_AQUA))
                                );
                                return;
                            }
                            databaseHandler.createHome(player, name, location);
                            tempHomeManager.removeTempHome(playerId, tempId);
                            player.sendRichMessage(STR."<gray>Temporary home <dark_aqua>\{tempId}</dark_aqua> finalized as <dark_aqua>\{name}</dark_aqua>.</gray>");
                        } catch (SQLException e) {
                            player.sendRichMessage("<red>An error occurred while finalizing the temporary home.</red>");
                        }
                    }))
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
        if (this.isFolia) {
            player.teleportAsync(location);
        } else {
            player.teleport(location);
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
