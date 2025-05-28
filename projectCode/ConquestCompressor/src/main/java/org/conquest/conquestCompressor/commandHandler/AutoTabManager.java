package org.conquest.conquestCompressor.commandHandler;

import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.conquest.conquestCompressor.functionalHandler.compressorHandler.CompressorManager;
import org.conquest.conquestCompressor.functionalHandler.compressorHandler.CompressorModel;
import org.conquest.conquestCompressor.functionalHandler.recipeHandler.RecipeManager;
import org.conquest.conquestCompressor.functionalHandler.recipeHandler.RecipeModel;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 🔄 AutoTabManager
 * Provides context-aware tab suggestions for all ConquestCompressor commands.
 */
public class AutoTabManager {

    private static final List<String> ROOT_COMMANDS = List.of("admin");

    private static final List<String> ADMIN_SUBCOMMANDS = List.of(
            "help",
            "reload",
            "recipe",
            "compressor",
            "setvoucher"
    );

    private static final List<String> RECIPE_SUBCOMMANDS = List.of(
            "editrecipe",
            "createrecipe",
            "deleterecipe"
    );

    private static final List<String> COMPRESSOR_SUBCOMMANDS = List.of(
            "editcompressor",
            "createcompressor",
            "deletecompressor"
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

            // /compressor admin recipe ...
            if (sub.equals("recipe")) {
                if (args.length == 3) return partialMatch(args[2], RECIPE_SUBCOMMANDS);

                if (args.length == 4 && args[2].equalsIgnoreCase("editrecipe") || args[2].equalsIgnoreCase("deleterecipe")) {
                    return getRecipeNames();
                }
            }

            // /compressor admin compressor ...
            if (sub.equals("compressor")) {
                if (args.length == 3) return partialMatch(args[2], COMPRESSOR_SUBCOMMANDS);

                if (args.length == 4 && args[2].equalsIgnoreCase("editcompressor") || args[2].equalsIgnoreCase("deletecompressor")) {
                    return getCompressorNames();
                }
            }

            // /compressor admin setvoucher <material|HAND> [amount]
            if (sub.equals("setvoucher")) {
                if (args.length == 3) {
                    List<String> materials = Arrays.stream(Material.values())
                            .map(Enum::name)
                            .filter(name -> name.equalsIgnoreCase("hand") || Material.matchMaterial(name) != null)
                            .collect(Collectors.toList());
                    materials.add("HAND");
                    return partialMatch(args[2], materials);
                }
                if (args.length == 4 && !args[2].equalsIgnoreCase("HAND")) {
                    return List.of("1", "8", "16", "64");
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

    private static List<String> getRecipeNames() {
        return RecipeManager.getAllRecipes().stream()
                .map(RecipeModel::getKey)
                .collect(Collectors.toList());
    }

    private static List<String> getCompressorNames() {
        return CompressorManager.getAllRecipes().stream()
                .map(CompressorModel::getKey)
                .collect(Collectors.toList());
    }
}
