package org.conquest.conquestCompressor.compressingHandler;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.conquest.conquestCompressor.ConquestCompressor;
import org.conquest.conquestCompressor.commandHandler.subcommandHandler.UserCommands;
import org.conquest.conquestCompressor.configurationHandler.configurationFiles.ConfigFile;
import org.conquest.conquestCompressor.functionalHandler.ItemDataModel;
import org.conquest.conquestCompressor.functionalHandler.compressorHandler.CompressorManager;
import org.conquest.conquestCompressor.functionalHandler.compressorHandler.CompressorModel;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;

/**
 * 🧠 CompressorListener
 * Handles automatic compression based on configured triggers.
 */
public class CompressorListener implements Listener {

    private static final Plugin plugin = ConquestCompressor.getInstance();
    private static boolean intervalTaskInitialized = false;

    public static void initializeAutoCompression() {
        AutoCompressTrigger trigger = ConfigFile.getCompressTrigger();

        if (trigger.isInterval()) {
            if (intervalTaskInitialized) return;

            long delayTicks = trigger.getIntervalMillis() / 50L;

            new BukkitRunnable() {
                @Override
                public void run() {
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        if (shouldCompress(player)) {
                            compress(player);
                        }
                    }
                }
            }.runTaskTimer(plugin, delayTicks, delayTicks);

            intervalTaskInitialized = true;
        }
    }

    @EventHandler
    public void onClick(PlayerInteractEvent event) {
        if (!isEnabled("ON_LEFT_CLICK") && !isEnabled("ON_RIGHT_CLICK")) return;

        switch (event.getAction()) {
            case LEFT_CLICK_AIR, LEFT_CLICK_BLOCK -> {
                if (isEnabled("ON_LEFT_CLICK") && shouldCompress(event.getPlayer())) {
                    compress(event.getPlayer());
                }
            }
            case RIGHT_CLICK_AIR, RIGHT_CLICK_BLOCK -> {
                if (isEnabled("ON_RIGHT_CLICK") && shouldCompress(event.getPlayer())) {
                    compress(event.getPlayer());
                }
            }
        }
    }

    @EventHandler
    public void onItemPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!isEnabled("ON_ITEM_PICKUP") || !shouldCompress(player)) return;

        compress(player);
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (!isEnabled("ON_CONTAINER_INVENTORY_OPEN") || !shouldCompress(player)) return;

        compress(player);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (!isEnabled("ON_CONTAINER_INVENTORY_CLOSE") || !shouldCompress(player)) return;

        compress(player);
    }

    @EventHandler
    public void onShiftToggle(PlayerToggleSneakEvent event) {
        if (!isEnabled("ON_SHIFT_TOGGLE")) return;
        if (shouldCompress(event.getPlayer())) {
            compress(event.getPlayer());
        }
    }

    @EventHandler
    public void onShiftClick(PlayerInteractEvent event) {
        if (!event.getPlayer().isSneaking()) return;
        if (!isEnabled("ON_SHIFT_LEFT_CLICK") && !isEnabled("ON_SHIFT_RIGHT_CLICK")) return;

        switch (event.getAction()) {
            case LEFT_CLICK_AIR, LEFT_CLICK_BLOCK -> {
                if (isEnabled("ON_SHIFT_LEFT_CLICK") && shouldCompress(event.getPlayer())) {
                    compress(event.getPlayer());
                }
            }
            case RIGHT_CLICK_AIR, RIGHT_CLICK_BLOCK -> {
                if (isEnabled("ON_SHIFT_RIGHT_CLICK") && shouldCompress(event.getPlayer())) {
                    compress(event.getPlayer());
                }
            }
        }
    }

    private static boolean shouldCompress(Player player) {
        return player.hasPermission("conquestcompressor.user.auto") && UserCommands.isAutoCompressEnabled(player);
    }

    private boolean isEnabled(String key) {
        AutoCompressTrigger trigger = ConfigFile.getCompressTrigger();
        return trigger.isEvent() && key.equalsIgnoreCase(trigger.getEventKey());
    }

    public static void compress(Player player) {
        boolean useWhitelist = ConfigFile.getBoolean("world-restrictions.whitelist-worlds", false);
        if (useWhitelist) {
            String worldName = player.getWorld().getName();
            List<String> allowedWorlds = ConfigFile.getStringList("world-restrictions.allowed-worlds");
            if (!allowedWorlds.contains(worldName)) return;
        }

        Inventory inv = player.getInventory();
        Collection<CompressorModel> recipes = CompressorManager.getAllRecipes();

        boolean compressedAny;

        do {
            compressedAny = false;

            for (CompressorModel recipe : recipes) {
                if (!recipe.isEnabled()) continue;

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

    public static void resetAutoCompression() {
        intervalTaskInitialized = false;
        initializeAutoCompression();
    }
}
