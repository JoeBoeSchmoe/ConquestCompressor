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
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 🖱️ CompressorItemListener
 * Manual interaction path for "compressor items".
 *
 * Active only when: compression-trigger.strategy == MANUAL
 * Respects:
 *   - compression-trigger.manual.interactions
 *   - per-item trigger flags (left/right)
 *   - world restrictions
 *   - cooldown + consumeOnUse
 */
public class CompressorItemListener implements Listener {

    // Local enum for configured interaction types (no new classes introduced).
    private enum InteractionType {
        LEFT_CLICK_AIR, LEFT_CLICK_BLOCK,
        RIGHT_CLICK_AIR, RIGHT_CLICK_BLOCK,
        SHIFT_LEFT_CLICK, SHIFT_RIGHT_CLICK,
        ALL;

        static EnumSet<InteractionType> parse(List<String> raw) {
            if (raw == null || raw.isEmpty()) return EnumSet.noneOf(InteractionType.class);
            EnumSet<InteractionType> set = EnumSet.noneOf(InteractionType.class);
            for (String s : raw) {
                if (s == null) continue;
                String key = s.trim().toUpperCase(Locale.ROOT);
                if ("ALL".equals(key)) return EnumSet.allOf(InteractionType.class);
                try { set.add(InteractionType.valueOf(key)); } catch (IllegalArgumentException ignored) {}
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
        // Fast path: only process known click actions
        if (action != Action.LEFT_CLICK_AIR && action != Action.LEFT_CLICK_BLOCK
                && action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        // Match against configured interaction list (default to empty -> no manual triggers)
        final List<String> rawList = ConfigFile.getStringList("compression-trigger.manual.interactions");
        final EnumSet<InteractionType> enabled = InteractionType.parse(rawList);
        if (!matchesConfiguredInteraction(enabled, action, event.getPlayer().isSneaking())) return;

        // Only handle the hand that fired the event (prevents double-processing)
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
        final boolean left = (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK);
        final boolean right = (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK);
        if ((left && !model.leftClick()) || (right && !model.rightClick())) return;

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

    // ───────────────────────────────────────────
    // Configured interaction matching
    // ───────────────────────────────────────────

    private boolean matchesConfiguredInteraction(EnumSet<InteractionType> enabled, Action action, boolean sneaking) {
        if (enabled.isEmpty()) return false;
        if (enabled.contains(InteractionType.ALL)) return true;

        // SHIFT_* takes precedence when sneaking
        if (sneaking) {
            switch (action) {
                case LEFT_CLICK_AIR:
                case LEFT_CLICK_BLOCK:
                    if (enabled.contains(InteractionType.SHIFT_LEFT_CLICK)) return true;
                    break;
                case RIGHT_CLICK_AIR:
                case RIGHT_CLICK_BLOCK:
                    if (enabled.contains(InteractionType.SHIFT_RIGHT_CLICK)) return true;
                    break;
                default:
                    // ignore
            }
        }

        // Non-shift variants
        return switch (action) {
            case LEFT_CLICK_AIR    -> enabled.contains(InteractionType.LEFT_CLICK_AIR);
            case LEFT_CLICK_BLOCK  -> enabled.contains(InteractionType.LEFT_CLICK_BLOCK);
            case RIGHT_CLICK_AIR   -> enabled.contains(InteractionType.RIGHT_CLICK_AIR);
            case RIGHT_CLICK_BLOCK -> enabled.contains(InteractionType.RIGHT_CLICK_BLOCK);
            default -> false;
        };
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
