package org.conquest.conquestCompressor.functionalHandler.compressorHandler;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.conquest.conquestCompressor.ConquestCompressor;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Static registry for CompressorItemModel entries.
 * Matches the pattern used by GameAutocompressor's CompressorManager.
 *
 * Provides:
 *  - register(..), unregister(..), clear(), all(), get(key)
 *  - match(ItemStack) with fast PDC path (is_compressor_item + compressor_key)
 *  - cooldown helpers per (player, compressorKey)
 */
public final class CompressorItemManager {

    private static final ConquestCompressor plugin = ConquestCompressor.getInstance();

    // Registry: key -> model
    private static final Map<String, CompressorItemModel> BY_KEY = new LinkedHashMap<>();

    // Cooldowns: key -> (playerUUID -> expiresAtTick)
    private static final Map<String, Map<UUID, Long>> COOLDOWNS = new ConcurrentHashMap<>();

    // PDC keys used by CompressorItemModel.buildItem(...)
    private static final NamespacedKey PDC_MARK =
            new NamespacedKey(plugin, "is_compressor_item");
    private static final NamespacedKey PDC_KEY =
            new NamespacedKey(plugin, "compressor_key");

    private CompressorItemManager() {
        // utility
    }

    // ---------------- registry ----------------

    public static void register(CompressorItemModel model) {
        if (model == null) return;
        BY_KEY.put(model.key(), model);
    }

    public static void unregister(String key) {
        if (key == null) return;
        BY_KEY.remove(key);
        COOLDOWNS.remove(key);
    }

    public static void clear() {
        BY_KEY.clear();
        COOLDOWNS.clear();
    }

    public static Map<String, CompressorItemModel> all() {
        return Collections.unmodifiableMap(BY_KEY);
    }

    public static Optional<CompressorItemModel> get(String key) {
        return Optional.ofNullable(BY_KEY.get(key));
    }

    // ---------------- matching ----------------

    /**
     * Resolve a model from an ItemStack (e.g., main/off-hand).
     * Strategy:
     *  1) O(1) PDC lookup: (is_compressor_item == 1) + compressor_key
     *  2) Fallback: iterate models and call model.matches(stack) (uses ItemDataModel matching).
     */
    public static Optional<CompressorItemModel> match(ItemStack stack) {
        if (stack == null || stack.getType().isAir()) return Optional.empty();

        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            Byte mark = pdc.get(PDC_MARK, PersistentDataType.BYTE);
            String key = pdc.get(PDC_KEY, PersistentDataType.STRING);
            if (mark != null && mark == (byte) 1 && key != null) {
                CompressorItemModel m = BY_KEY.get(key);
                if (m != null) return Optional.of(m);
            }
        }

        // Fallback: strict check via model matcher
        for (CompressorItemModel m : BY_KEY.values()) {
            if (m.matches(stack)) return Optional.of(m);
        }
        return Optional.empty();
    }

    // ---------------- cooldowns ----------------

    /**
     * @param nowTick A consistent tick source:
     *                - Paper 1.21+: Bukkit.getServer().getCurrentTick()
     *                - Cross-version: player.getWorld().getFullTime()
     */
    public static boolean isOnCooldown(Player player, CompressorItemModel model, long nowTick) {
        Map<UUID, Long> map = COOLDOWNS.get(model.key());
        if (map == null) return false;
        Long until = map.get(player.getUniqueId());
        return until != null && until > nowTick;
    }

    public static void startCooldown(Player player, CompressorItemModel model, long nowTick) {
        int duration = Math.max(0, model.cooldownTicks());
        if (duration <= 0) return;
        COOLDOWNS.computeIfAbsent(model.key(), k -> new ConcurrentHashMap<>())
                .put(player.getUniqueId(), nowTick + duration);
    }

    public static void clearCooldowns(String modelKey) {
        Map<UUID, Long> m = COOLDOWNS.get(modelKey);
        if (m != null) m.clear();
    }

    public static void clearAllCooldowns() {
        COOLDOWNS.values().forEach(Map::clear);
    }
}
