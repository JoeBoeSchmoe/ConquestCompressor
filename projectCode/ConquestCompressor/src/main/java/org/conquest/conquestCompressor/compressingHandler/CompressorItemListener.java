package org.conquest.conquestCompressor.compressingHandler;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.conquest.conquestCompressor.configurationHandler.configurationFiles.ConfigFile;
import org.conquest.conquestCompressor.functionalHandler.ItemDataModel;
import org.conquest.conquestCompressor.functionalHandler.compressorHandler.CompressorItemManager;
import org.conquest.conquestCompressor.functionalHandler.compressorHandler.CompressorItemModel;
import org.conquest.conquestCompressor.functionalHandler.compressorHandler.CompressorManager;
import org.conquest.conquestCompressor.functionalHandler.compressorHandler.CompressorModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 🖱️ CompressorItemListener
 * Lets players left/right-click with a configured "compressor item" to run its recipes,
 * regardless of auto-compress permission/toggle.
 * - Respects per-item triggers (left/right), cooldownTicks, and consumeOnUse.
 * - Obeys world restrictions from config.
 */
public class CompressorItemListener implements Listener {

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.LEFT_CLICK_AIR && action != Action.LEFT_CLICK_BLOCK
                && action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        // Only handle the hand that fired the event (prevents double-processing)
        EquipmentSlot hand = event.getHand();
        if (hand == null) return;

        ItemStack stack = event.getItem();
        if (stack == null || stack.getType() == Material.AIR) return;

        Player player = event.getPlayer();

        // Resolve compressor item model (PDC fast path + meta fallback)
        CompressorItemModel model = CompressorItemManager.match(stack).orElse(null);
        if (model == null || !model.enabled()) return;

        // Check per-item trigger
        boolean left = (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK);
        boolean right = (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK);
        if ((left && !model.leftClick()) || (right && !model.rightClick())) return;

        // World restrictions
        if (!isWorldAllowed(player)) return;

        // Cooldown (ticks)
        long nowTick = currentTick(player);
        if (CompressorItemManager.isOnCooldown(player, model, nowTick)) return;

        // Compress only with this item's recipe set
        boolean compressed = compressForItem(player, model);

        if (compressed) {
            // Start cooldown (if any)
            CompressorItemManager.startCooldown(player, model, nowTick);

            // Optionally consume one
            if (model.consumeOnUse()) {
                consumeOneFromHand(player, hand);
            }

            // Prevent normal right-click behavior (placing blocks, using items)
            event.setCancelled(true);
        }
    }

    // ───────────────────────────────────────────
    // Compression limited to the item's recipes
    // ───────────────────────────────────────────

    private boolean compressForItem(Player player, CompressorItemModel itemModel) {
        Inventory inv = player.getInventory();

        // Collect enabled recipe models for this item
        List<CompressorModel> recipes = new ArrayList<>();
        for (String key : itemModel.recipeKeys()) {
            CompressorModel r = CompressorManager.getRecipe(key);
            if (r != null && r.isEnabled()) recipes.add(r);
        }
        if (recipes.isEmpty()) return false;

        boolean any = false;
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

                // Snapshot -> remove -> try add -> rollback if no space
                ItemStack[] before = inv.getContents().clone();
                ItemDataModel.removeMatching(inv, recipe.getInputMaterial(), recipe.getInputItemData(), removeAmount);
                Map<Integer, ItemStack> leftovers = inv.addItem(output);

                if (!leftovers.isEmpty()) {
                    inv.setContents(before); // rollback if insufficient space
                    continue;
                }

                changed = true;
                any = true;
            }
        } while (changed);

        return any;
    }

    // ───────────────────────────────────────────
    // Helpers
    // ───────────────────────────────────────────

    private boolean isWorldAllowed(Player player) {
        boolean useWhitelist = ConfigFile.getBoolean("world-restrictions.whitelist-worlds", false);
        if (!useWhitelist) return true;
        List<String> allowed = ConfigFile.getStringList("world-restrictions.allowed-worlds");
        return allowed.contains(player.getWorld().getName());
    }

    private void consumeOneFromHand(Player player, EquipmentSlot hand) {
        ItemStack held = (hand == EquipmentSlot.HAND)
                ? player.getInventory().getItemInMainHand()
                : player.getInventory().getItemInOffHand();

        if (held.getType() == Material.AIR) return;

        int amt = held.getAmount();
        if (amt <= 1) {
            if (hand == EquipmentSlot.HAND) {
                player.getInventory().setItemInMainHand(null);
            } else {
                player.getInventory().setItemInOffHand(null);
            }
        } else {
            held.setAmount(amt - 1);
            if (hand == EquipmentSlot.HAND) {
                player.getInventory().setItemInMainHand(held);
            } else {
                player.getInventory().setItemInOffHand(held);
            }
        }
    }

    private long currentTick(Player player) {
        try {
            // Paper 1.21+ fast tick source
            return Bukkit.getServer().getCurrentTick();
        } catch (NoSuchMethodError ignored) {
            // Cross-version fallback
            return player.getWorld().getFullTime();
        }
    }
}
