package org.winlogon.HomeManager;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class HomeTabCompleter implements TabCompleter {

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player)) return Collections.emptyList();
        final List<String> subcommands = Arrays.asList(
            "create", "list", "delete", "teleport", "set", "help"
        );
        final List<String> completeSubcommands = Arrays.asList(
            "list", "delete", "teleport", "set"
        );
        Player player = (Player) sender;

        if (args.length == 1) {
            return subcommands;
        }

        if (args.length == 2 && completeSubcommands.contains(args[0])) {
            try {
                return DatabaseHandler.getHomes(player);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        return Collections.emptyList();
    }
}
