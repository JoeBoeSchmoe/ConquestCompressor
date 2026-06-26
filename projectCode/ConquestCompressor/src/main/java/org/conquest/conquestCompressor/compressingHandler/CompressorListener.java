package org.conquest.conquestCompressor.compressingHandler;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.conquest.conquestCompressor.ConquestCompressor;
import org.conquest.conquestCompressor.commandHandler.permissionHandler.PermissionManager;
import org.conquest.conquestCompressor.commandHandler.permissionHandler.PermissionModels;
import org.conquest.conquestCompressor.commandHandler.subcommandHandler.UserCommands;
import org.conquest.conquestCompressor.configurationHandler.configurationFiles.ConfigFile;
import org.conquest.conquestCompressor.functionalHandler.ItemDataModel;
import org.conquest.conquestCompressor.functionalHandler.compressorHandler.CompressorItemManager;
import org.conquest.conquestCompressor.functionalHandler.compressorHandler.CompressorItemModel;
import org.conquest.conquestCompressor.functionalHandler.compressorHandler.CompressorManager;
import org.conquest.conquestCompressor.functionalHandler.compressorHandler.CompressorModel;

import java.util.*;

/**
 * 🧠 CompressorListener
 *
 * INTERVALS:
 *  1) baseIntervalTask (respects /compressor toggle; mode-aware)
 *  2) itemHeldIntervalTask (ignores toggle; runs if holding a compressor item; uses held item recipes)
 *  3) itemAnywhereIntervalTask (ignores toggle; runs if ANY compressor item exists anywhere in inventory;
 *     uses ONLY those items’ recipe sets). Runs ONLY when the user's /compressor auto toggle is OFF.
 *
 * EVENTS (pickup/open/close/click/shift-click):
 *  - Independent of toggle; require base permission; compress with ALL recipes.
 */
public class CompressorListener implements Listener {

    private static final Plugin plugin = ConquestCompressor.getInstance();

    // Three timers
    private static BukkitTask baseIntervalTask = null;          // respects toggle
    private static BukkitTask itemHeldIntervalTask = null;      // ignores toggle (hands)
    private static BukkitTask itemAnywhereIntervalTask = null;  // ignores toggle (anywhere) when user toggle is OFF

    private enum TargetMode {
        INVENTORY,
        HOLDING,
        HOLD_IN_MAIN_HAND,
        HOLD_IN_OFF_HAND
    }

    public static void initializeAutoCompression() {
        AutoCompressTrigger trigger = ConfigFile.getCompressTrigger();
        if (!trigger.isInterval()) {
            cancelTimers();
            return;
        }

        final long delayTicks = Math.max(1L, trigger.getIntervalMillis() / 50L);
        final TargetMode mode = readTargetMode();

        // 1) Base interval (respects toggle; mode-aware)
        if (baseIntervalTask == null) {
            baseIntervalTask = new BukkitRunnable() {
                @Override
                public void run() {
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        if (!shouldCompressInterval(player)) continue;
                        if (!isWorldAllowed(player)) continue;

                        switch (mode) {
                            case INVENTORY -> {
                                // Full inventory (and hands are part of the inventory anyway)
                                compress(player);
                            }
                            case HOLDING -> {
                                compressHandUsingAllRecipes(player, EquipmentSlot.HAND);
                                compressHandUsingAllRecipes(player, EquipmentSlot.OFF_HAND);
                            }
                            case HOLD_IN_MAIN_HAND -> {
                                compressHandUsingAllRecipes(player, EquipmentSlot.HAND);
                            }
                            case HOLD_IN_OFF_HAND -> {
                                compressHandUsingAllRecipes(player, EquipmentSlot.OFF_HAND);
                            }
                        }
                    }
                }
            }.runTaskTimer(plugin, delayTicks, delayTicks);
        }

        // 2) Item-held interval (ignores toggle; held items only)
        if (itemHeldIntervalTask == null) {
            itemHeldIntervalTask = new BukkitRunnable() {
                @Override
                public void run() {
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        if (!isWorldAllowed(player)) continue;
                        if (!PermissionManager.has(player, PermissionModels.USER_AUTO)) continue;
                        if (!isHoldingCompressorItem(player)) continue;
                        compressItemHandsInterval(player);
                    }
                }
            }.runTaskTimer(plugin, delayTicks, delayTicks);
        }

        // 3) Item-anywhere interval (ignores toggle; runs ONLY when user toggle is OFF)
        if (itemAnywhereIntervalTask == null) {
            itemAnywhereIntervalTask = new BukkitRunnable() {
                @Override
                public void run() {
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        if (!isWorldAllowed(player)) continue;
                        if (!PermissionManager.has(player, PermissionModels.USER_AUTO)) continue;

                        // Only fire when the user's generic auto toggle is OFF
                        if (UserCommands.isAutoCompressEnabled(player)) continue;

                        Set<String> recipeKeys = collectCompressorRecipeKeysAnywhere(player);
                        if (recipeKeys.isEmpty()) continue;

                        compressOnlyTheseRecipes(player, recipeKeys);
                    }
                }
            }.runTaskTimer(plugin, delayTicks, delayTicks);
        }
    }

    public static void resetAutoCompression() {
        cancelTimers();
        initializeAutoCompression();
    }

    private static void cancelTimers() {
        if (baseIntervalTask != null) { baseIntervalTask.cancel(); baseIntervalTask = null; }
        if (itemHeldIntervalTask != null) { itemHeldIntervalTask.cancel(); itemHeldIntervalTask = null; }
        if (itemAnywhereIntervalTask != null) { itemAnywhereIntervalTask.cancel(); itemAnywhereIntervalTask = null; }
    }

    // ───────────────────────────────────────────
    // Events (independent of toggle)
    // ───────────────────────────────────────────

    @EventHandler
    public void onClick(PlayerInteractEvent event) {
        if (!isEnabled("ON_LEFT_CLICK") && !isEnabled("ON_RIGHT_CLICK")) return;

        switch (event.getAction()) {
            case LEFT_CLICK_AIR, LEFT_CLICK_BLOCK -> {
                if (isEnabled("ON_LEFT_CLICK") && shouldCompressEvent(event.getPlayer())) {
                    compress(event.getPlayer());
                }
            }
            case RIGHT_CLICK_AIR, RIGHT_CLICK_BLOCK -> {
                if (isEnabled("ON_RIGHT_CLICK") && shouldCompressEvent(event.getPlayer())) {
                    compress(event.getPlayer());
                }
            }
            default -> { /* ignore */ }
        }
    }

    @EventHandler
    public void onItemPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!isEnabled("ON_ITEM_PICKUP") || !shouldCompressEvent(player)) return;
        compress(player);
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (!isEnabled("ON_CONTAINER_INVENTORY_OPEN") || !shouldCompressEvent(player)) return;
        compress(player);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (!isEnabled("ON_CONTAINER_INVENTORY_CLOSE") || !shouldCompressEvent(player)) return;
        compress(player);
    }

    @EventHandler
    public void onShiftClick(PlayerInteractEvent event) {
        if (!event.getPlayer().isSneaking()) return;
        if (!isEnabled("ON_SHIFT_LEFT_CLICK") && !isEnabled("ON_SHIFT_RIGHT_CLICK")) return;

        switch (event.getAction()) {
            case LEFT_CLICK_AIR, LEFT_CLICK_BLOCK -> {
                if (isEnabled("ON_SHIFT_LEFT_CLICK") && shouldCompressEvent(event.getPlayer())) {
                    compress(event.getPlayer());
                }
            }
            case RIGHT_CLICK_AIR, RIGHT_CLICK_BLOCK -> {
                if (isEnabled("ON_SHIFT_RIGHT_CLICK") && shouldCompressEvent(event.getPlayer())) {
                    compress(event.getPlayer());
                }
            }
            default -> { /* ignore */ }
        }
    }

    // ───────────────────────────────────────────
    // Toggle semantics
    // ───────────────────────────────────────────

    private static boolean shouldCompressEvent(Player player) {
        return PermissionManager.has(player, PermissionModels.USER_AUTO);
    }

    private static boolean shouldCompressInterval(Player player) {
        return PermissionManager.has(player, PermissionModels.USER_AUTO)
                && UserCommands.isAutoCompressEnabled(player);
    }

    // ───────────────────────────────────────────
    // Trigger key resolver (legacy single-event mode)
    // ───────────────────────────────────────────

    private boolean isEnabled(String key) {
        AutoCompressTrigger trigger = ConfigFile.getCompressTrigger();
        return trigger.isEvent() && key.equalsIgnoreCase(trigger.getEventKey());
    }

    // ───────────────────────────────────────────
    // Core compression (ALL recipes)
    // ───────────────────────────────────────────

    public static void compress(Player player) {
        if (!isWorldAllowed(player)) return;

        Inventory inv = player.getInventory();
        Collection<CompressorModel> recipes = CompressorManager.getAllRecipes();

        boolean compressedAny;
        do {
            compressedAny = false;

            for (CompressorModel recipe : recipes) {
                if (recipe == null || !recipe.isEnabled()) continue;

                String key = recipe.getKey();
                if (key == null || key.isEmpty()) continue;
                if (!PermissionManager.hasRecipe(player, key)) continue;

                int matched = ItemDataModel.countMatching(inv, recipe.getInputMaterial(), recipe.getInputItemData());
                int sets = matched / recipe.getInputAmount();
                if (sets <= 0) continue;

                int removeAmount = sets * recipe.getInputAmount();
                int totalOutput = sets * recipe.getOutputAmount();
                ItemStack outputItem = recipe.buildOutputItem();
                outputItem.setAmount(totalOutput);

                ItemStack[] beforeState = inv.getContents().clone();
                ItemDataModel.removeMatching(inv, recipe.getInputMaterial(), recipe.getInputItemData(), removeAmount);

                HashMap<Integer, ItemStack> leftovers = inv.addItem(outputItem);

                if (!leftovers.isEmpty()) {
                    inv.setContents(beforeState);
                    continue;
                }

                compressedAny = true;
            }

        } while (compressedAny);
    }

    // ───────────────────────────────────────────
    // INVENTORY-anywhere recipes only (used by itemAnywhereIntervalTask)
    // ───────────────────────────────────────────

    private static void compressOnlyTheseRecipes(Player player, Set<String> recipeKeys) {
        if (!isWorldAllowed(player)) return;
        if (recipeKeys == null || recipeKeys.isEmpty()) return;

        Inventory inv = player.getInventory();
        List<CompressorModel> recipes = new ArrayList<>();

        for (String key : recipeKeys) {
            if (key == null || key.isEmpty()) continue;
            CompressorModel r = CompressorManager.getRecipe(key);
            if (r == null || !r.isEnabled()) continue;
            if (!PermissionManager.hasRecipe(player, key)) continue;
            recipes.add(r);
        }
        if (recipes.isEmpty()) return;

        boolean changed;
        do {
            changed = false;

            for (CompressorModel recipe : recipes) {
                int matched = ItemDataModel.countMatching(inv, recipe.getInputMaterial(), recipe.getInputItemData());
                int sets = matched / recipe.getInputAmount();
                if (sets <= 0) continue;

                int removeAmount = sets * recipe.getInputAmount();
                int totalOutput = sets * recipe.getOutputAmount();

                ItemStack out = recipe.buildOutputItem();
                out.setAmount(totalOutput);

                ItemStack[] before = inv.getContents().clone();
                ItemDataModel.removeMatching(inv, recipe.getInputMaterial(), recipe.getInputItemData(), removeAmount);
                Map<Integer, ItemStack> leftovers = inv.addItem(out);

                if (!leftovers.isEmpty()) {
                    inv.setContents(before); // rollback
                    continue;
                }

                changed = true;
            }
        } while (changed);
    }

    private static Set<String> collectCompressorRecipeKeysAnywhere(Player player) {
        Set<String> keys = new HashSet<>();

        // Storage (hotbar included)
        ItemStack[] storage = safeStorageContents(player);
        if (storage != null) {
            for (ItemStack s : storage) {
                addRecipeKeysForStack(keys, s);
            }
        }

        // Main hand + off hand (explicit)
        addRecipeKeysForStack(keys, safeMainHand(player));
        addRecipeKeysForStack(keys, safeOffHand(player));

        return keys;
    }

    private static ItemStack[] safeStorageContents(Player p) {
        try { return p.getInventory().getStorageContents(); }
        catch (Throwable ignored) { return p.getInventory().getContents(); }
    }

    private static void addRecipeKeysForStack(Set<String> acc, ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR) return;
        CompressorItemManager.match(stack).ifPresent(model -> {
            if (!model.enabled()) return;
            for (String k : model.recipeKeys()) {
                if (k != null && !k.isEmpty()) acc.add(k);
            }
        });
    }

    // ───────────────────────────────────────────
    // Hand-only compression (ALL recipes) — NO custom match helper needed
    // ───────────────────────────────────────────

    private static void compressHandUsingAllRecipes(Player player, EquipmentSlot slot) {
        if (!isWorldAllowed(player)) return;

        ItemStack inHand = (slot == EquipmentSlot.HAND)
                ? safeMainHand(player)
                : safeOffHand(player);

        if (inHand == null || inHand.getType() == Material.AIR) return;

        // Build a temporary 9-slot inventory with ONLY the hand item to reuse existing matching logic.
        Inventory handOnly = Bukkit.createInventory(null, 9);
        handOnly.setItem(0, inHand.clone());

        Collection<CompressorModel> recipes = CompressorManager.getAllRecipes();
        Inventory inv = player.getInventory();

        boolean changed;
        do {
            changed = false;
            for (CompressorModel recipe : recipes) {
                if (recipe == null || !recipe.isEnabled()) continue;
                String key = recipe.getKey();
                if (key == null || key.isEmpty()) continue;
                if (!PermissionManager.hasRecipe(player, key)) continue;

                // Ask ItemDataModel to count matches against the HAND-ONLY temp inventory
                int matchedInHand = ItemDataModel.countMatching(handOnly, recipe.getInputMaterial(), recipe.getInputItemData());
                int sets = matchedInHand / recipe.getInputAmount();
                if (sets <= 0) continue;

                int toRemove = sets * recipe.getInputAmount();
                int totalOut = sets * recipe.getOutputAmount();

                ItemStack out = recipe.buildOutputItem();
                out.setAmount(totalOut);

                // Snapshot before mutating
                ItemStack[] before = inv.getContents().clone();

                // Remove ONLY from the hand stack
                int newAmt = inHand.getAmount() - toRemove;
                if (newAmt <= 0) {
                    if (slot == EquipmentSlot.HAND) player.getInventory().setItemInMainHand(null);
                    else player.getInventory().setItemInOffHand(null);
                } else {
                    inHand.setAmount(newAmt);
                    if (slot == EquipmentSlot.HAND) player.getInventory().setItemInMainHand(inHand);
                    else player.getInventory().setItemInOffHand(inHand);
                }

                // Also update the temp handOnly inventory to keep loops consistent
                ItemStack tmp = inHand.clone();
                if (newAmt <= 0) tmp = null;
                handOnly.setItem(0, tmp);

                // Try to add outputs
                Map<Integer, ItemStack> leftovers = inv.addItem(out);
                if (!leftovers.isEmpty()) {
                    // rollback
                    inv.setContents(before);
                    // restore hand
                    if (slot == EquipmentSlot.HAND) player.getInventory().setItemInMainHand(inHand);
                    else player.getInventory().setItemInOffHand(inHand);
                    // restore temp hand view
                    handOnly.setItem(0, inHand.clone());
                    continue;
                }

                changed = true;
            }
        } while (changed);
    }

    // ───────────────────────────────────────────
    // Item-held interval (ONLY item’s recipes)
    // ───────────────────────────────────────────

    private static boolean isHoldingCompressorItem(Player player) {
        ItemStack main = safeMainHand(player);
        if (isCompressorItem(main)) return true;

        ItemStack off = safeOffHand(player);
        return isCompressorItem(off);
    }

    private static ItemStack safeMainHand(Player p) {
        try { return p.getInventory().getItemInMainHand(); }
        catch (Throwable ignored) { return null; }
    }

    private static ItemStack safeOffHand(Player p) {
        try { return p.getInventory().getItemInOffHand(); }
        catch (Throwable ignored) { return null; }
    }

    private static boolean isCompressorItem(ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR) return false;
        return CompressorItemManager.match(stack).isPresent();
    }

    private static void compressItemHandsInterval(Player player) {
        if (!isWorldAllowed(player)) return;

        ItemStack main = safeMainHand(player);
        if (isCompressorItem(main)) compressForItemHand(player, main);

        ItemStack off = safeOffHand(player);
        if (isCompressorItem(off)) compressForItemHand(player, off);
    }

    private static void compressForItemHand(Player player, ItemStack stack) {
        CompressorItemModel itemModel = CompressorItemManager.match(stack).orElse(null);
        if (itemModel == null || !itemModel.enabled()) return;

        Inventory inv = player.getInventory();

        List<CompressorModel> recipes = new ArrayList<>();
        for (String key : itemModel.recipeKeys()) {
            CompressorModel r = CompressorManager.getRecipe(key);
            if (r == null || !r.isEnabled()) continue;
            if (!PermissionManager.hasRecipe(player, key)) continue;
            recipes.add(r);
        }
        if (recipes.isEmpty()) return;

        boolean changed;
        do {
            changed = false;

            for (CompressorModel recipe : recipes) {
                int matched = ItemDataModel.countMatching(inv, recipe.getInputMaterial(), recipe.getInputItemData());
                int sets = matched / recipe.getInputAmount();
                if (sets <= 0) continue;

                int removeAmount = sets * recipe.getInputAmount();
                int totalOutput = sets * recipe.getOutputAmount();

                ItemStack output = recipe.buildOutputItem();
                output.setAmount(totalOutput);

                ItemStack[] before = inv.getContents().clone();
                ItemDataModel.removeMatching(inv, recipe.getInputMaterial(), recipe.getInputItemData(), removeAmount);
                Map<Integer, ItemStack> leftovers = inv.addItem(output);

                if (!leftovers.isEmpty()) {
                    inv.setContents(before);
                    continue;
                }

                changed = true;
            }
        } while (changed);
    }

    // ───────────────────────────────────────────
    // Config + env helpers
    // ───────────────────────────────────────────

    private static TargetMode readTargetMode() {
        String raw = ConfigFile.contains("compression-trigger.auto.target-mode")
                ? ConfigFile.getString("compression-trigger.auto.target-mode")
                : "INVENTORY";
        String key = raw == null ? "INVENTORY" : raw.trim().toUpperCase(Locale.ROOT).replace('-', '_');

        if ("ALL".equals(key)) key = "INVENTORY";

        return switch (key) {
            case "INVENTORY" -> TargetMode.INVENTORY;
            case "HOLDING", "IN_HANDS", "HANDS" -> TargetMode.HOLDING;
            case "HOLD_IN_MAIN_HAND" -> TargetMode.HOLD_IN_MAIN_HAND;
            case "HOLD_IN_OFF_HAND" -> TargetMode.HOLD_IN_OFF_HAND;
            default -> TargetMode.INVENTORY;
        };
    }

    private static boolean isWorldAllowed(Player player) {
        boolean useWhitelist = ConfigFile.getBoolean("world-restrictions.whitelist-worlds", false);
        if (!useWhitelist) return true;
        List<String> allowedWorlds = ConfigFile.getStringList("world-restrictions.allowed-worlds");
        return allowedWorlds != null && allowedWorlds.contains(player.getWorld().getName());
    }
}
