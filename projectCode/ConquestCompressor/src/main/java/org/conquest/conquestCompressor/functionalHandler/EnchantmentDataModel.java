package org.conquest.conquestCompressor.functionalHandler;

import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.Registry;

/**
 * ✨ EnchantmentDataModel
 * Represents an enchantment name and level from YAML config,
 * using the Bukkit registry (safe even in 1.21+).
 */
public class EnchantmentDataModel {

    private String name;
    private int level;

    public EnchantmentDataModel() {
    }

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
     * Resolves the enchantment using NamespacedKey.
     * This is deprecated in 1.21 but no alternative exists yet.
     *
     * @return the resolved Enchantment, or null if invalid
     */
    @SuppressWarnings("deprecation") // Safe fallback until 1.21+ alternative is released
    public Enchantment getEnchantment() {
        NamespacedKey key = NamespacedKey.minecraft(name.toLowerCase());
        return Registry.ENCHANTMENT.get(key);
    }

    public boolean isValid() {
        return getEnchantment() != null;
    }
}
