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
import org.conquest.conquestCompressor.commandHandler.permissionHandler.PermissionManager;
import org.conquest.conquestCompressor.configurationHandler.configurationFiles.ConfigFile;
import org.conquest.conquestCompressor.functionalHandler.ItemDataModel;
import org.conquest.conquestCompressor.functionalHandler.compressorHandler.CompressorItemManager;
import org.conquest.conquestCompressor.functionalHandler.compressorHandler.CompressorItemModel;
import org.conquest.conquestCompressor.functionalHandler.compressorHandler.CompressorManager;
import org.conquest.conquestCompressor.functionalHandler.compressorHandler.CompressorModel;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 🖱️ CompressorItemListener
 * Manual interaction path for "compressor items".
 *
 * Active only when: compression-trigger.strategy == MANUAL
 * Respects:
 *   - compression-trigger.manual.interactions (simplified set)
 *   - per-item trigger flags (left/right)
 *   - world restrictions
 *   - cooldown + consumeOnUse
 */
public class CompressorItemListener implements Listener {

    // ───────────────────────────────────────────
    // Simplified interaction set + legacy aliases
    // ───────────────────────────────────────────
    private enum InteractionType {
        LEFT_CLICK,
        RIGHT_CLICK,
        HOLD_SHIFT_LEFT_CLICK,
        HOLD_SHIFT_RIGHT_CLICK,
        ALL;

        // Legacy → simplified aliases
        private static final Map<String, InteractionType> ALIASES;
        static {
            Map<String, InteractionType> m = new HashMap<>();
            // canonical
            m.put("LEFT_CLICK", LEFT_CLICK);
            m.put("RIGHT_CLICK", RIGHT_CLICK);
            m.put("HOLD_SHIFT_LEFT_CLICK", HOLD_SHIFT_LEFT_CLICK);
            m.put("HOLD_SHIFT_RIGHT_CLICK", HOLD_SHIFT_RIGHT_CLICK);
            m.put("ALL", ALL);
            // legacy synonyms
            m.put("LEFT_CLICK_AIR", LEFT_CLICK);
            m.put("LEFT_CLICK_BLOCK", LEFT_CLICK);
            m.put("RIGHT_CLICK_AIR", RIGHT_CLICK);
            m.put("RIGHT_CLICK_BLOCK", RIGHT_CLICK);
            m.put("SHIFT_LEFT_CLICK", HOLD_SHIFT_LEFT_CLICK);
            m.put("SHIFT_RIGHT_CLICK", HOLD_SHIFT_RIGHT_CLICK);
            ALIASES = m;
        }

        static EnumSet<InteractionType> parse(List<String> raw) {
            if (raw == null || raw.isEmpty()) return EnumSet.noneOf(InteractionType.class);
            EnumSet<InteractionType> set = EnumSet.noneOf(InteractionType.class);
            for (String s : raw) {
                if (s == null) continue;
                String key = s.trim().toUpperCase(Locale.ROOT);
                InteractionType mapped = ALIASES.get(key);
                if (mapped != null) {
                    if (mapped == ALL) return EnumSet.allOf(InteractionType.class);
                    set.add(mapped);
                }
            }
            return set;
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        // Only active when strategy == MANUAL
        final String strategy = ConfigFile.contains("compression-trigger.strategy")
                ? ConfigFile.getString("compression-trigger.strategy")
                : "AUTO";
        if (!"MANUAL".equalsIgnoreCase(strategy)) return;

        final Action action = event.getAction();
        // Only care about left/right clicks
        if (action != Action.LEFT_CLICK_AIR && action != Action.LEFT_CLICK_BLOCK
                && action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        // Parse simplified manual interactions
        final List<String> rawList = ConfigFile.getStringList("compression-trigger.manual.interactions");
        final EnumSet<InteractionType> enabled = InteractionType.parse(rawList);
        if (enabled.isEmpty()) return;

        // Map event → simplified type (with shift modifier)
        final boolean sneaking = event.getPlayer().isSneaking();
        final InteractionType fired = mapToInteraction(action, sneaking);

        if (!enabled.contains(InteractionType.ALL) && !enabled.contains(fired)) return;

        // Only handle the hand that fired (avoid double-processing)
        final EquipmentSlot hand = event.getHand();
        if (hand == null) return;

        final ItemStack stack = event.getItem();
        if (stack == null || stack.getType() == Material.AIR) return;

        final Player player = event.getPlayer();

        // World restrictions
        if (!isWorldAllowed(player)) return;

        // Resolve compressor item model (PDC fast path + meta fallback)
        final CompressorItemModel model = CompressorItemManager.match(stack).orElse(null);
        if (model == null || !model.enabled()) return;

        // Per-item trigger constraint (still enforced)
        final boolean isLeft = (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK);
        final boolean isRight = !isLeft;
        if ((isLeft && !model.leftClick()) || (isRight && !model.rightClick())) return;

        // Cooldown (ticks)
        final long nowTick = currentTick(player);
        if (CompressorItemManager.isOnCooldown(player, model, nowTick)) return;

        // Compress only with this item's recipe set
        final boolean compressed = compressForItem(player, model);
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

    // Map Bukkit action + shift to simplified interaction
    private InteractionType mapToInteraction(Action action, boolean sneaking) {
        final boolean isLeft = (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK);
        if (sneaking) {
            return isLeft ? InteractionType.HOLD_SHIFT_LEFT_CLICK : InteractionType.HOLD_SHIFT_RIGHT_CLICK;
        } else {
            return isLeft ? InteractionType.LEFT_CLICK : InteractionType.RIGHT_CLICK;
        }
    }

    // ───────────────────────────────────────────
    // Compression limited to the item's recipes
    // ───────────────────────────────────────────

    private boolean compressForItem(Player player, CompressorItemModel itemModel) {
        Inventory inv = player.getInventory();

        // Collect enabled + permitted recipe models for this item
        List<CompressorModel> recipes = new ArrayList<>();
        for (String key : itemModel.recipeKeys()) {
            CompressorModel r = CompressorManager.getRecipe(key);
            if (r == null || !r.isEnabled()) continue;

            // Permission gate (wildcard or specific)
            if (!PermissionManager.hasRecipe(player, key)) continue;

            recipes.add(r);
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
        boolean useWhitelist = ConfigFile.contains("world-restrictions.whitelist-worlds")
                && ConfigFile.getBoolean("world-restrictions.whitelist-worlds", false);
        if (!useWhitelist) return true;
        List<String> allowed = ConfigFile.getStringList("world-restrictions.allowed-worlds");
        return allowed != null && allowed.contains(player.getWorld().getName());
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
