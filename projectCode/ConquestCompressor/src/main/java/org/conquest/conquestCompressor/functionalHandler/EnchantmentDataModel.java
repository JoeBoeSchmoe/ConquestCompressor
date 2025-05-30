package org.conquest.conquestCompressor.functionalHandler;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.enchantments.Enchantment;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * ✨ EnchantmentDataModel
 * Represents an enchantment name and level from YAML config,
 * using the Bukkit registry (safe even in 1.21+).
 */
public class EnchantmentDataModel implements ConfigurationSerializable {

    private final String name;
    private final int level;

    public EnchantmentDataModel(String name, int level) {
        this.name = name;
        this.level = level;
    }

    public String getName() {
        return name;
    }

    public int getLevel() {
        return level;
    }

    /**
     * Resolves the Bukkit enchantment from the namespaced ID.
     * Safe for all modern versions (1.19.3+).
     *
     * @return Bukkit Enchantment or null if not found
     */
    public Enchantment getEnchantment() {
        NamespacedKey key = NamespacedKey.minecraft(name.toLowerCase());
        return Registry.ENCHANTMENT.get(key);
    }

    public boolean isValid() {
        return getEnchantment() != null;
    }

    // ──────────────────────────────
    // 📦 Serialization
    // ──────────────────────────────

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", name);  // use "name" for clarity in YAML
        map.put("level", level);
        return map;
    }

    /**
     * Deserializes safely from either 'id' or 'name' keys.
     *
     * @param map Raw YAML map
     * @return EnchantmentDataModel instance
     */
    public static EnchantmentDataModel deserialize(Map<String, Object> map) {
        if (map == null) return null;

        // Accept both 'name' and legacy 'id'
        String name = map.getOrDefault("name", map.get("id")) instanceof String s ? s : null;
        int level = (map.get("level") instanceof Number num) ? num.intValue() : 1;

        return new EnchantmentDataModel(name != null ? name : "untyped", level);
    }
}
