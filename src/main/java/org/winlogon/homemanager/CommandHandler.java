package org.winlogon.homemanager;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.SuggestionInfo;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.executors.CommandArguments;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class CommandHandler<Handler extends DataHandler> {
    private final Handler databaseHandler;
    private final JavaPlugin plugin;
    private final int pageSize;

    public CommandHandler(Handler databaseHandler, JavaPlugin plugin, int pageSize) {
        this.databaseHandler = databaseHandler;
        this.plugin = plugin;
        this.pageSize = pageSize;
    }

    public void registerCommands() {
        new CommandAPICommand("home")
            .withShortDescription("Manage your homes")
            .withSubcommand(new CommandAPICommand("create")
                .withArguments(new StringArgument("name"))
                .executesPlayer(this::createHome))
            .withSubcommand(new CommandAPICommand("list")
                .withArguments(new IntegerArgument("page").setOptional(true))
                .executesPlayer(this::listHomes))
            .withSubcommand(new CommandAPICommand("delete")
                .withArguments(new StringArgument("home-name").replaceSuggestions(ArgumentSuggestions.strings(this::getHomes)))
                .executesPlayer(this::deleteHome))
            .withSubcommand(new CommandAPICommand("update")
                .withArguments(new StringArgument("home-name").replaceSuggestions(ArgumentSuggestions.strings(this::getHomes)))
                .executesPlayer(this::updateHome))
            .withSubcommand(new CommandAPICommand("teleport")
                .withArguments(new StringArgument("home-name").replaceSuggestions(ArgumentSuggestions.strings(this::getHomes)))
                .executesPlayer(this::teleportHome))
            .register();
    }

    private void updateHome(Player player, CommandArguments args) {
        var homeName = (String) args.get("home-name");
        var result = databaseHandler.updateHome(player, homeName, player.getLocation());
        result.match(
            ok -> player.sendRichMessage(
                "<gray>Home <home-name> updated!</gray>",
                Placeholder.component("home-name", Component.text(homeName, NamedTextColor.DARK_AQUA))
            ),
            err -> player.sendRichMessage(
                "<red>Home <home-name> does not exist.</red>",
                Placeholder.component("home-name", Component.text(homeName, NamedTextColor.DARK_AQUA))
            )
        );
    }

    private void teleportHome(Player player, CommandArguments args) {
        String homeName = (String) args.get("home-name");
        var homeLocationOpt = databaseHandler.getHomeLocation(player, homeName);
        if (homeLocationOpt.isPresent()) {
            teleportPlayer(player, homeLocationOpt.get());
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
    }

    private void deleteHome(Player player, CommandArguments args) {
        var homeName = (String) args.get("home-name");
        var result = databaseHandler.deleteHome(player, homeName);
        result.match(
            ok -> player.sendRichMessage(
                "<red>Home <home-name> deleted.</red>",
                Placeholder.component("home-name", Component.text(homeName, NamedTextColor.DARK_AQUA))
            ),
            err -> player.sendRichMessage("<red>That home does not exist.</red>")
        );
    }

    private void createHome(Player player, CommandArguments args) {
        var homeName = (String) args.get("name");
        var resultOpt = databaseHandler.createHome(player, homeName, player.getLocation());
        resultOpt.ifPresentOrElse(
            created -> {
                if (created) {
                    player.sendRichMessage("<gray>Home <home-name> created.</gray>", Placeholder.component("home-name", Component.text(homeName, NamedTextColor.DARK_AQUA)));
                } else {
                    player.sendRichMessage("<red>A home with that name already exists.</red>");
                }
            },
            () -> player.sendRichMessage("<red>An error occurred while creating your home.</red>")
        );
    }

    private String[] getHomes(SuggestionInfo<CommandSender> info) {
        if (info.sender() instanceof Player player) {
            return databaseHandler.getHomes(player).toArray(new String[0]);
        }
        return new String[0];
    }

    private void listHomes(Player player, CommandArguments args) {
        int page = (Integer) args.getOrDefault("page", 1); // Get page number, default to 1

        PaginatedResult paginatedResult = databaseHandler.getHomesPaginated(player, page, this.pageSize);
        var homes = paginatedResult.homes();
        int totalPages = paginatedResult.totalPages();

        if (homes.isEmpty()) {
            player.sendMessage(Component.text("You have no homes.", NamedTextColor.GRAY));
            return;
        }

        // Clamp page to be within valid range (already handled in DataHandler but good for display consistency)
        int actualPage = Math.max(1, Math.min(page, totalPages));

        Component homeListComponent = Component.join(
            JoinConfiguration.separator(Component.text(", ", NamedTextColor.GRAY)),
            homes.stream()
                .map(home -> Component.text(home, NamedTextColor.DARK_AQUA))
                .toArray(Component[]::new)
        );

        player.sendMessage(
            Component.text("Your homes (Page ", NamedTextColor.GRAY)
                .append(Component.text(actualPage, NamedTextColor.GREEN))
                .append(Component.text("/", NamedTextColor.GRAY))
                .append(Component.text(totalPages, NamedTextColor.GREEN))
                .append(Component.text("): ", NamedTextColor.GRAY))
                .append(homeListComponent)
        );
    }

    private void teleportPlayer(Player player, Location location) {
        if (isFolia()) {
            player.teleportAsync(location);
        } else {
            plugin.getServer().getScheduler().runTask(plugin, () -> player.teleport(location));
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
