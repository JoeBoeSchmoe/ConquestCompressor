package org.conquest.conquestCompressor.commandHandler;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.conquest.conquestCompressor.functionalHandler.compressorHandler.CompressorManager;
import org.conquest.conquestCompressor.functionalHandler.compressorHandler.CompressorModel;
import org.conquest.conquestCompressor.functionalHandler.compressorHandler.CompressorItemManager;

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
            "compressor",
            "item"
    );

    private static final List<String> COMPRESSOR_SUBCOMMANDS = List.of(
            "edit",
            "create",
            "delete"
    );

    private static final List<String> ITEM_SUBCOMMANDS = List.of(
            "give",
            "create",
            "delete"
    );

    private static final List<String> GIVE_AMOUNTS = List.of("1", "2", "4", "8", "16", "32", "64");

    public static List<String> getSuggestions(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!(sender instanceof Player)) return Collections.emptyList();

        if (args.length == 1) {
            return partialMatch(args[0], ROOT_COMMANDS);
        }

        if (!args[0].equalsIgnoreCase("admin")) {
            return Collections.emptyList();
        }

        // /compressor admin ...
        if (args.length == 2) {
            return partialMatch(args[1], ADMIN_SUBCOMMANDS);
        }

        String adminSub = args[1].toLowerCase();

        // /compressor admin compressor ...
        if (adminSub.equals("compressor")) {
            if (args.length == 3) {
                return partialMatch(args[2], COMPRESSOR_SUBCOMMANDS);
            }

            // /compressor admin compressor edit <key>
            // /compressor admin compressor delete <key>
            if (args.length == 4 && (args[2].equalsIgnoreCase("edit") || args[2].equalsIgnoreCase("delete"))) {
                return partialMatch(args[3], getCompressorNames());
            }
            return Collections.emptyList();
        }

        // /compressor admin item ...
        if (adminSub.equals("item")) {
            if (args.length == 3) {
                return partialMatch(args[2], ITEM_SUBCOMMANDS);
            }

            String itemSub = args[2].toLowerCase();

            // /compressor admin item give <player> <itemKey> <amount>
            if (itemSub.equals("give")) {
                if (args.length == 4) return partialMatch(args[3], getOnlinePlayerNames());
                if (args.length == 5) return partialMatch(args[4], getItemKeys());
                if (args.length == 6) return partialMatch(args[5], GIVE_AMOUNTS);
                return Collections.emptyList();
            }

            // /compressor admin item delete <itemKey> [more keys...]
            if (itemSub.equals("delete")) {
                List<String> alreadyChosen = Arrays.stream(args).skip(3).map(String::toLowerCase).toList();
                List<String> candidates = getItemKeys().stream()
                        .filter(k -> !alreadyChosen.contains(k.toLowerCase()))
                        .collect(Collectors.toList());
                String current = args[args.length - 1];
                return partialMatch(current, candidates);
            }

            // /compressor admin item create <key> <recipe1> [recipe2] ...
            if (itemSub.equals("create")) {
                if (args.length == 4) {
                    // suggest a placeholder for the new item key
                    return partialMatch(args[3], List.of("example_key"));
                }
                // suggest recipe keys, excluding ones already provided
                List<String> alreadyChosen = Arrays.stream(args)
                        .skip(4) // everything after <key>
                        .map(String::toLowerCase)
                        .toList();
                List<String> candidates = getCompressorNames().stream()
                        .filter(r -> !alreadyChosen.contains(r.toLowerCase()))
                        .collect(Collectors.toList());
                String current = args[args.length - 1];
                return partialMatch(current, candidates);
            }
        }

        return Collections.emptyList();
    }

    // ---------- helpers ----------

    private static List<String> partialMatch(String input, List<String> options) {
        String needle = input == null ? "" : input.toLowerCase();
        return options.stream()
                .filter(opt -> opt.toLowerCase().startsWith(needle))
                .sorted()
                .collect(Collectors.toList());
    }

    private static List<String> getCompressorNames() {
        return CompressorManager.getAllRecipes().stream()
                .map(CompressorModel::getKey)
                .sorted()
                .collect(Collectors.toList());
    }

    private static List<String> getItemKeys() {
        return CompressorItemManager.all().keySet().stream()
                .sorted()
                .collect(Collectors.toList());
    }

    private static List<String> getOnlinePlayerNames() {
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .sorted()
                .collect(Collectors.toList());
    }
}
