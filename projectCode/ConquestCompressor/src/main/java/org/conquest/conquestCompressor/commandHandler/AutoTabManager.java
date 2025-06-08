package org.conquest.conquestCompressor.commandHandler;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.conquest.conquestCompressor.functionalHandler.compressorHandler.CompressorManager;
import org.conquest.conquestCompressor.functionalHandler.compressorHandler.CompressorModel;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 🔄 AutoTabManager
 * Provides context-aware tab suggestions for all ConquestCompressor commands.
 */
public class AutoTabManager {

    private static final List<String> ROOT_COMMANDS = List.of("admin", "help", "toggle");

    private static final List<String> ADMIN_SUBCOMMANDS = List.of(
            "help",
            "reload",
            "compressor"
    );

    private static final List<String> COMPRESSOR_SUBCOMMANDS = List.of(
            "edit",
            "create",
            "delete"
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

            String sub = args[1].toLowerCase();

            // /compressor admin compressor ...
            if (sub.equals("compressor")) {
                if (args.length == 3) return partialMatch(args[2], COMPRESSOR_SUBCOMMANDS);

                if (args.length == 4 && args[2].equalsIgnoreCase("edit") || args[2].equalsIgnoreCase("delete")) {
                    return getCompressorNames();
                }
            }

        }

        return Collections.emptyList();
    }

    private static List<String> partialMatch(String input, List<String> options) {
        return options.stream()
                .filter(opt -> opt.toLowerCase().startsWith(input.toLowerCase()))
                .collect(Collectors.toList());
    }

    private static List<String> getCompressorNames() {
        return CompressorManager.getAllRecipes().stream()
                .map(CompressorModel::getKey)
                .collect(Collectors.toList());
    }
}
