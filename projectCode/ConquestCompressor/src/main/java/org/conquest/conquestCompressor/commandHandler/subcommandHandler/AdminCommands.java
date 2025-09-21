package org.conquest.conquestCompressor.commandHandler.subcommandHandler;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.conquest.conquestCompressor.commandHandler.permissionHandler.PermissionManager;
import org.conquest.conquestCompressor.commandHandler.permissionHandler.PermissionModels;
import org.conquest.conquestCompressor.functionalHandler.ItemDataModel;
import org.conquest.conquestCompressor.functionalHandler.compressorHandler.CompressorManager;
import org.conquest.conquestCompressor.functionalHandler.compressorHandler.CompressorModel;
import org.conquest.conquestCompressor.functionalHandler.compressorHandler.CompressorItemManager;
import org.conquest.conquestCompressor.functionalHandler.compressorHandler.CompressorItemModel;
import org.conquest.conquestCompressor.guiHandler.GUIOpener;
import org.conquest.conquestCompressor.guiHandler.guiBuildingHandler.EditingSession;
import org.conquest.conquestCompressor.guiHandler.guiBuildingHandler.EditingSessionManager;
import org.conquest.conquestCompressor.guiHandler.guiBuildingHandler.guiMenuModels.GUIFileEnums;
import org.conquest.conquestCompressor.responseHandler.MessageResponseManager;
import org.conquest.conquestCompressor.responseHandler.messageModels.AdminMessageModels;
import org.conquest.conquestCompressor.ConquestCompressor;
import org.conquest.conquestCompressor.configurationHandler.configurationFiles.GameCompressorItemFile;

import java.util.*;

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
            case "item" -> handleItemSub(player, args);
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

                    EditingSession session = EditingSessionManager.getOrCreate(player, EditingSession.SessionMode.EDITING);
                    session.setEditingCompressor(model);
                    GUIOpener.open(player, GUIFileEnums.COMPRESSOR);
                } else {
                    MessageResponseManager.send(player, AdminMessageModels.EDIT_MISSING_KEY);
                }
                return true;
            }

            case "create" -> {
                if (args.length >= 4) {
                    String key = args[3].toLowerCase();

                    if (CompressorManager.getRecipe(key) != null) {
                        MessageResponseManager.send(player, AdminMessageModels.COMPRESSOR_ALREADY_EXISTS, Map.of("key", key));
                        return true;
                    }

                    CompressorModel model = new CompressorModel(key);
                    EditingSession session = EditingSessionManager.getOrCreate(player, EditingSession.SessionMode.CREATING);
                    session.setEditingCompressor(model);
                    GUIOpener.open(player, GUIFileEnums.COMPRESSOR);
                } else {
                    MessageResponseManager.send(player, AdminMessageModels.CREATE_MISSING_KEY);
                }
                return true;
            }

            case "delete" -> {
                if (args.length >= 4) {
                    String key = args[3].toLowerCase();
                    boolean success = CompressorManager.deleteRecipe(key);
                    if (success) {
                        MessageResponseManager.send(player, AdminMessageModels.COMPRESSOR_DELETE_SUCCESS, Map.of("key", key));
                    } else {
                        MessageResponseManager.send(player, AdminMessageModels.COMPRESSOR_DELETE_FAIL, Map.of("key", key));
                    }
                } else {
                    MessageResponseManager.send(player, AdminMessageModels.EDIT_MISSING_KEY);
                }
                return true;
            }

            default -> {
                MessageResponseManager.send(player, AdminMessageModels.ADMIN_USAGE_HINT);
                return true;
            }
        }
    }

    // ──────────────────────────────────────────────────────────────
    // NEW: /compressor admin item ...
    // ──────────────────────────────────────────────────────────────

    private static boolean handleItemSub(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage("§eUsage: /compressor admin item <give|create|delete> ...");
            return true;
        }
        if (!PermissionManager.has(player, PermissionModels.ADMIN_COMPRESSOR)) {
            MessageResponseManager.send(player, AdminMessageModels.NO_PERMISSION);
            return true;
        }

        String action = args[2].toLowerCase();
        switch (action) {
            case "give" -> {
                // /compressor admin item give <player> <itemKey> <amount>
                if (args.length < 6) {
                    player.sendMessage("§eUsage: /compressor admin item give <player> <itemKey> <1|2|4|8|16|32|64>");
                    return true;
                }

                String targetName = args[3];
                Player target = Bukkit.getPlayerExact(targetName);
                if (target == null) {
                    player.sendMessage("§cPlayer not found: §f" + targetName);
                    return true;
                }

                String key = args[4].toLowerCase();
                CompressorItemModel model = CompressorItemManager.get(key).orElse(null);
                if (model == null) {
                    player.sendMessage("§cItem key not found: §f" + key);
                    return true;
                }

                int amount = parseAmount(args[5]);
                if (amount <= 0) {
                    player.sendMessage("§cInvalid amount. Use: 1,2,4,8,16,32,64");
                    return true;
                }

                // Build stacks and give
                int remaining = amount;
                while (remaining > 0) {
                    int give = Math.min(64, remaining);
                    remaining -= give;

                    // build fresh stack; adjust amount per stack
                    var stack = model.buildItem();
                    stack.setAmount(give);
                    HashMap<Integer, ItemStack> overflow = target.getInventory().addItem(stack);
                    // drop overflow at feet
                    overflow.values().forEach(it -> target.getWorld().dropItemNaturally(target.getLocation(), it));
                }

                player.sendMessage("§aGave §f" + amount + "§a of §e" + key + " §ato §b" + target.getName() + "§a.");
                if (!player.equals(target)) {
                    target.sendMessage("§aYou received §f" + amount + "§a of §e" + key + "§a.");
                }
                return true;
            }

            case "delete" -> {
                // /compressor admin item delete <itemKey> [more keys...]
                if (args.length < 4) {
                    player.sendMessage("§eUsage: /compressor admin item delete <itemKey> [moreKeys...]");
                    return true;
                }

                List<String> keys = Arrays.stream(args).skip(3).map(String::toLowerCase).toList();
                int removed = 0;

                for (String key : keys) {
                    if (CompressorItemManager.get(key).isEmpty()) {
                        player.sendMessage("§7- §cNot found: §f" + key);
                        continue;
                    }

                    // Unregister from memory
                    CompressorItemManager.unregister(key);

                    // Remove from YAML
                    var yaml = GameCompressorItemFile.getConfig();
                    if (yaml != null) {
                        yaml.set("compressorItems." + key, null);
                        removed++;
                    }
                }

                GameCompressorItemFile.save();
                player.sendMessage("§aDeleted §f" + removed + " §aitem definition(s).");
                return true;
            }

            case "create" -> {
                // /compressor admin item create <key> <recipe1> [recipe2] ...
                if (args.length < 5) {
                    player.sendMessage("§eUsage: /compressor admin item create <key> <recipe1> [recipe2] [recipe3] ...");
                    return true;
                }

                String key = args[3].toLowerCase();

                // prevent duplicates (both in-memory and YAML)
                if (CompressorItemManager.get(key).isPresent() || GameCompressorItemFile.contains(key)) {
                    player.sendMessage("§cItem already exists: §f" + key);
                    return true;
                }

                // Grab held item (main-hand, then off-hand)
                ItemStack held = player.getInventory().getItemInMainHand();
                if (held.getType().isAir()) {
                    held = player.getInventory().getItemInOffHand();
                }
                if (held.getType().isAir()) {
                    player.sendMessage("§cYou must hold an item to create a compressor item from.");
                    return true;
                }

                // Collect recipe keys
                List<String> requested = new ArrayList<>();
                for (int i = 4; i < args.length; i++) requested.add(args[i].toLowerCase());

                // Validate recipes against existing auto-compressor recipes
                List<String> validRecipes = new ArrayList<>();
                List<String> unknown = new ArrayList<>();
                for (String r : requested) {
                    if (CompressorManager.getRecipe(r) != null) validRecipes.add(r);
                    else unknown.add(r);
                }
                if (validRecipes.isEmpty()) {
                    player.sendMessage("§cNo valid recipe keys provided. Ensure they exist in gameAutoCompressor.yml.");
                    if (!unknown.isEmpty()) player.sendMessage("§7Unknown: §f" + String.join(", ", unknown));
                    return true;
                }

                // Build ItemDataModel from the held item (may be null for plain items)
                ItemDataModel idm = ItemDataModel.fromItem(held);

                // Write to YAML (no amount saved; itemData only if present)
                var yaml = GameCompressorItemFile.getConfig();
                if (yaml == null) {
                    player.sendMessage("§cConfig not loaded. Try /compressor admin reload.");
                    return true;
                }

                String base = "compressorItems." + key;
                yaml.set(base + ".enabled", true);
                yaml.set(base + ".item.material", held.getType().name());
                if (idm != null) {
                    yaml.set(base + ".item.itemData", idm.serialize());
                }
                yaml.set(base + ".trigger.leftClick", true);
                yaml.set(base + ".trigger.rightClick", true);
                yaml.set(base + ".trigger.cooldownTicks", 10);
                yaml.set(base + ".trigger.consumeOnUse", false);
                yaml.set(base + ".recipes", validRecipes);

                GameCompressorItemFile.save();

                // Also register in-memory immediately so it works without a reload
                CompressorItemModel runtimeModel = new CompressorItemModel(
                        key,
                        true,
                        held.getType(),
                        idm,       // nullable is fine
                        true,      // leftClick
                        true,      // rightClick
                        10,        // cooldownTicks
                        false,     // consumeOnUse
                        validRecipes
                );
                CompressorItemManager.register(runtimeModel);

                if (!unknown.isEmpty()) {
                    player.sendMessage("§aCreated item §e" + key + " §awith recipes: §f" + String.join(", ", validRecipes));
                    player.sendMessage("§7Skipped unknown recipes: §f" + String.join(", ", unknown));
                } else {
                    player.sendMessage("§aCreated item §e" + key + " §awith recipes: §f" + String.join(", ", validRecipes));
                }
                return true;
            }

            default -> {
                player.sendMessage("§eUsage: /compressor admin item <give|create|delete> ...");
                return true;
            }
        }
    }

    // ──────────────────────────────────────────────────────────────
    // helpers
    // ──────────────────────────────────────────────────────────────

    private static int parseAmount(String s) {
        try {
            int v = Integer.parseInt(s);
            // enforce allowed powers-of-two up to 64
            if (v == 1 || v == 2 || v == 4 || v == 8 || v == 16 || v == 32 || v == 64) return v;
            return -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
