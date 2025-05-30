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
import org.conquest.conquestCompressor.configurationHandler.configurationFiles.ConfigFile;
import org.conquest.conquestCompressor.functionalHandler.ItemDataModel;
import org.conquest.conquestCompressor.functionalHandler.compressorHandler.CompressorManager;
import org.conquest.conquestCompressor.functionalHandler.compressorHandler.CompressorModel;

import java.util.Collection;
import java.util.HashMap;
import java.util.logging.Logger;

import static org.bukkit.event.block.Action.LEFT_CLICK_AIR;
import static org.bukkit.event.block.Action.LEFT_CLICK_BLOCK;
import static org.bukkit.event.block.Action.RIGHT_CLICK_AIR;
import static org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK;

/**
 * 🧠 CompressorListener
 * Handles automatic compression based on configured triggers.
 */
public class CompressorListener implements Listener {

    private static final Plugin plugin = ConquestCompressor.getInstance();
    private static final Logger log = plugin.getLogger();

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
                        if (player.hasPermission("conquestcompressor.user.auto")) {
                            compress(player);
                        }
                    }
                }
            }.runTaskTimer(plugin, delayTicks, delayTicks);

            intervalTaskInitialized = true;
            // log.info("🕒 Auto compression scheduled every " + trigger.getIntervalMillis() + "ms");
        } else {
            // log.info("🎯 Auto compression using event: " + trigger.getEventKey());
            // Listener is already registered during plugin init
        }
    }


    @EventHandler
    public void onClick(PlayerInteractEvent event) {
        if (!isEnabled("ON_LEFT_CLICK") && !isEnabled("ON_RIGHT_CLICK")) return;

        switch (event.getAction()) {
            case LEFT_CLICK_AIR, LEFT_CLICK_BLOCK -> {
                if (isEnabled("ON_LEFT_CLICK")) {
                    compress(event.getPlayer());
                }
            }
            case RIGHT_CLICK_AIR, RIGHT_CLICK_BLOCK -> {
                if (isEnabled("ON_RIGHT_CLICK")) {
                    compress(event.getPlayer());
                }
            }
        }
    }

    @EventHandler
    public void onItemPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!isEnabled("ON_ITEM_PICKUP")) return;

        compress(player);
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (!isEnabled("ON_CONTAINER_INVENTORY_OPEN")) return;

        compress(player);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (!isEnabled("ON_CONTAINER_INVENTORY_CLOSE")) return;

        compress(player);
    }

    @EventHandler
    public void onShiftToggle(PlayerToggleSneakEvent event) {
        if (!isEnabled("ON_SHIFT_TOGGLE")) return;

        compress(event.getPlayer());
    }

    @EventHandler
    public void onShiftClick(PlayerInteractEvent event) {
        if (!event.getPlayer().isSneaking()) return;
        if (!isEnabled("ON_SHIFT_LEFT_CLICK") && !isEnabled("ON_SHIFT_RIGHT_CLICK")) return;

        switch (event.getAction()) {
            case LEFT_CLICK_AIR, LEFT_CLICK_BLOCK -> {
                if (isEnabled("ON_SHIFT_LEFT_CLICK")) {
                    compress(event.getPlayer());
                }
            }
            case RIGHT_CLICK_AIR, RIGHT_CLICK_BLOCK -> {
                if (isEnabled("ON_SHIFT_RIGHT_CLICK")) {
                    compress(event.getPlayer());
                }
            }
        }
    }

    private boolean isEnabled(String key) {
        AutoCompressTrigger trigger = ConfigFile.getCompressTrigger();
        return trigger.isEvent() && key.equalsIgnoreCase(trigger.getEventKey());
    }

    public static void compress(Player player) {
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

                // Step 1: Clone inventory to allow rollback
                ItemStack[] beforeState = inv.getContents().clone();

                // Step 2: Remove input items
                ItemDataModel.removeMatching(inv, recipe.getInputMaterial(), recipe.getInputItemData(), removeAmount);

                // Step 3: Try to add output
                HashMap<Integer, ItemStack> leftovers = inv.addItem(outputItem);

                if (!leftovers.isEmpty()) {
                    // Not enough space — rollback
                    inv.setContents(beforeState);
                    //player.sendMessage("❌ Not enough space to compress " + matched + " " + recipe.getInputMaterial().name());
                    continue;
                }

                //player.sendMessage("✨ Auto-compressed " + removeAmount + " ➜ " + totalOutput + " " + recipe.getOutputMaterial().name());
                compressedAny = true;
            }

        } while (compressedAny); // Repeat until no more compressions can happen
    }


    private static boolean canFitInInventory(Inventory inventory, ItemStack item) {
        int toAdd = item.getAmount();

        for (ItemStack slot : inventory.getContents()) {
            if (slot == null || slot.getType().isAir()) {
                toAdd -= Math.min(toAdd, item.getMaxStackSize());
            } else if (slot.isSimilar(item)) {
                toAdd -= Math.min(toAdd, item.getMaxStackSize() - slot.getAmount());
            }

            if (toAdd <= 0) return true;
        }

        return false;
    }

}
