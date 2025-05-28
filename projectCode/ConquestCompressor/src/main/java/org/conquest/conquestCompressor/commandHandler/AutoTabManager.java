package org.conquest.conquestCompressor.commandHandler;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 🔄 AutoTabManager
 * Provides context-aware tab suggestions for all ConquestCompressor commands.
 */
public class AutoTabManager {

    private static final List<String> ROOT_COMMANDS = List.of(
            "admin"
    );

    private static final List<String> ADMIN_SUBCOMMANDS = Arrays.asList(
            "help",
            "reload"
    );

    public static List<String> getSuggestions(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!(sender instanceof Player)) return Collections.emptyList();

        if (args.length == 1) {
            return partialMatch(args[0], ROOT_COMMANDS);
        }

        if (args[0].equalsIgnoreCase("admin")) {
            if (args.length == 2) {
                return partialMatch(args[1], ADMIN_SUBCOMMANDS);
            }
        }

        return Collections.emptyList();
    }

    private static List<String> partialMatch(String input, List<String> options) {
        List<String> matches = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase().startsWith(input.toLowerCase())) {
                matches.add(option);
            }
        }
        return matches;
    }
}
