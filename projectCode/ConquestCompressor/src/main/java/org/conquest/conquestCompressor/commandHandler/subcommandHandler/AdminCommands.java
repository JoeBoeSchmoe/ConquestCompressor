package org.conquest.conquestCompressor.commandHandler.subcommandHandler;

import org.bukkit.entity.Player;
import org.conquest.conquestCompressor.commandHandler.permissionHandler.PermissionManager;
import org.conquest.conquestCompressor.commandHandler.permissionHandler.PermissionModels;
import org.conquest.conquestCompressor.functionalHandler.compressorHandler.CompressorManager;
import org.conquest.conquestCompressor.functionalHandler.compressorHandler.CompressorModel;
import org.conquest.conquestCompressor.functionalHandler.recipeHandler.RecipeManager;
import org.conquest.conquestCompressor.functionalHandler.recipeHandler.RecipeModel;
import org.conquest.conquestCompressor.guiHandler.GUIOpener;
import org.conquest.conquestCompressor.guiHandler.guiBuildingHandler.EditingSession;
import org.conquest.conquestCompressor.guiHandler.guiBuildingHandler.EditingSessionManager;
import org.conquest.conquestCompressor.guiHandler.guiBuildingHandler.guiMenuManagers.CompressorGUIManager;
import org.conquest.conquestCompressor.guiHandler.guiBuildingHandler.guiMenuManagers.RecipesGUIManager;
import org.conquest.conquestCompressor.guiHandler.guiBuildingHandler.guiMenuModels.GUIFileEnums;
import org.conquest.conquestCompressor.responseHandler.MessageResponseManager;
import org.conquest.conquestCompressor.responseHandler.messageModels.AdminMessageModels;
import org.conquest.conquestCompressor.ConquestCompressor;

import java.util.Map;

/**
 * 🛠 AdminCommands
 * Handles all /compressor admin subcommands.
 */
public class AdminCommands {

    public static boolean handle(Player player, String[] args) {
        String sub = args[1].toLowerCase();

        return switch (sub) {
            case "reload" -> handleReload(player);
            case "help" -> {
                MessageResponseManager.sendHelpPage(player, "admin-help", 1);
                yield true;
            }
            case "compressor" -> handleCompressorSub(player, args);
            case "recipe" -> handleRecipeSub(player, args);
            default -> {
                MessageResponseManager.send(player, AdminMessageModels.ADMIN_USAGE_HINT);
                yield true;
            }
        };
    }

    private static boolean handleReload(Player player) {
        if (!PermissionManager.has(player, PermissionModels.ADMIN_RELOAD)) {
            MessageResponseManager.send(player, AdminMessageModels.NO_PERMISSION);
            return true;
        }

        ConquestCompressor.getInstance().reload();
        MessageResponseManager.send(player, AdminMessageModels.CONFIG_RELOADED);
        return true;
    }

    private static boolean handleCompressorSub(Player player, String[] args) {
        if (args.length < 3) {
            MessageResponseManager.send(player, AdminMessageModels.COMPRESSOR_USAGE_HINT);
            return true;
        }
        if (!PermissionManager.has(player, PermissionModels.ADMIN_COMPRESSOR)) {
            MessageResponseManager.send(player, AdminMessageModels.NO_PERMISSION);
            return true;
        }

        String action = args[2].toLowerCase();
        switch (action) {
            case "edit" -> {
                if (args.length >= 4) {
                    String key = args[3].toLowerCase();
                    CompressorModel model = CompressorManager.getRecipe(key);
                    if (model == null) {
                        MessageResponseManager.send(player, AdminMessageModels.COMPRESSOR_NOT_FOUND, Map.of("key", key));
                        return true;
                    }

                    EditingSession session = EditingSessionManager.getOrCreate(player);
                    session.setEditingCompressor(model);
                    GUIOpener.open(player, GUIFileEnums.COMPRESSOR);
                } else {
                    MessageResponseManager.send(player, AdminMessageModels.EDIT_MISSING_KEY);
                }
            }
            case "create" -> {
                if (args.length >= 4) {
                    String key = args[3].toLowerCase();

                    if (CompressorManager.getRecipe(key) != null) {
                        // Key already in use, warn the player
                        MessageResponseManager.send(player, AdminMessageModels.COMPRESSOR_ALREADY_EXISTS, Map.of("key", key));
                        return true;
                    }

                    CompressorModel model = new CompressorModel(key);
                    EditingSession session = EditingSessionManager.getOrCreate(player);
                    session.setEditingCompressor(model);
                    GUIOpener.open(player, GUIFileEnums.COMPRESSOR);
                } else {
                    MessageResponseManager.send(player, AdminMessageModels.CREATE_MISSING_KEY);
                }
            }

            case "delete" -> {
                if (args.length >= 4) {
                    String key = args[3].toLowerCase();
                    boolean success = CompressorManager.deleteRecipe(key); // ← Actual deletion logic
                    if (success) {
                        MessageResponseManager.send(player, AdminMessageModels.COMPRESSOR_DELETE_SUCCESS, Map.of("key", key));
                    } else {
                        MessageResponseManager.send(player, AdminMessageModels.COMPRESSOR_DELETE_FAIL, Map.of("key", key));
                    }
                } else {
                    MessageResponseManager.send(player, AdminMessageModels.EDIT_MISSING_KEY); // Reuse "edit-missing-key" for missing key
                }
            }

            default -> MessageResponseManager.send(player, AdminMessageModels.ADMIN_USAGE_HINT);
        }

        return true;
    }

    private static boolean handleRecipeSub(Player player, String[] args) {
        if (args.length < 3) {
            MessageResponseManager.send(player, AdminMessageModels.RECIPE_USAGE_HINT);
            return true;
        }
        if (!PermissionManager.has(player, PermissionModels.ADMIN_RECIPE)) {
            MessageResponseManager.send(player, AdminMessageModels.NO_PERMISSION);
            return true;
        }
        String action = args[2].toLowerCase();
        switch (action) {
            case "edit" -> {
                if (args.length >= 4) {
                    String key = args[3].toLowerCase();
                    RecipeModel model = RecipeManager.getRecipe(key);
                    if (model == null) {
                        MessageResponseManager.send(player, AdminMessageModels.RECIPE_NOT_FOUND, Map.of("key", key));
                        return true;
                    }

                    EditingSession session = EditingSessionManager.getOrCreate(player);
                    session.setEditingRecipe(model);
                    GUIOpener.open(player, GUIFileEnums.RECIPES);
                } else {
                    MessageResponseManager.send(player, AdminMessageModels.EDIT_RECIPE_MISSING_KEY);
                }
            }
            case "create" -> {
                if (args.length >= 4) {
                    String key = args[3].toLowerCase();
                    RecipeModel model = new RecipeModel(key);
                    EditingSession session = EditingSessionManager.getOrCreate(player);
                    session.setEditingRecipe(model);
                    GUIOpener.open(player, GUIFileEnums.RECIPES);
                } else {
                    MessageResponseManager.send(player, AdminMessageModels.CREATE_RECIPE_MISSING_KEY);
                }
            }
            case "delete" -> {
                if (args.length >= 4) {
                    String key = args[3].toLowerCase();
                    boolean success = true; //RecipeManager.deleteRecipe(key);
                    if (success) {
                        MessageResponseManager.send(player, AdminMessageModels.RECIPE_DELETE_SUCCESS, Map.of("key", key));
                    } else {
                        MessageResponseManager.send(player, AdminMessageModels.RECIPE_DELETE_FAIL, Map.of("key", key));
                    }
                } else {
                    MessageResponseManager.send(player, AdminMessageModels.EDIT_RECIPE_MISSING_KEY); // same message for missing key
                }
            }
            default -> MessageResponseManager.send(player, AdminMessageModels.ADMIN_USAGE_HINT);
        }

        return true;
    }

}
