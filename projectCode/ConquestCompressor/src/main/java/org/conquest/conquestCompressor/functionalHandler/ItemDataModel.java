package org.conquest.conquestCompressor.functionalHandler;

import java.util.List;
import java.util.Map;

/**
 * 🎨 ItemData
 * Represents metadata applied to an item (NBT, visual, model data, etc).
 * Used in recipe outputs and inputs.
 */
public class ItemDataModel {

    private String displayName;
    private List<String> lore;
    private Integer customModelData;
    private List<EnchantmentDataModel> enchantments;
    private Boolean unbreakable;
    private Map<String, Object> nbt;

    public ItemDataModel() {
        // Default constructor for YAML/JSON parsing
    }

    public ItemDataModel(String displayName,
                         List<String> lore,
                         Integer customModelData,
                         List<EnchantmentDataModel> enchantments,
                         Boolean unbreakable,
                         Map<String, Object> nbt) {
        this.displayName = displayName;
        this.lore = lore;
        this.customModelData = customModelData;
        this.enchantments = enchantments;
        this.unbreakable = unbreakable;
        this.nbt = nbt;
    }

    public String getDisplayName() {
        return displayName;
    }

    public List<String> getLore() {
        return lore;
    }

    public Integer getCustomModelData() {
        return customModelData;
    }

    public List<EnchantmentDataModel> getEnchantments() {
        return enchantments;
    }

    public Boolean isUnbreakable() {
        return unbreakable != null && unbreakable;
    }

    public Map<String, Object> getNbt() {
        return nbt;
    }

    /**
     * Parses a list of maps from YAML into a single ItemDataModel.
     */
    public static ItemDataModel fromList(List<Map<?, ?>> rawData) {
        if (rawData == null || rawData.isEmpty()) return null;

        Map<String, Object> map = new java.util.HashMap<>();
        for (Map<?, ?> entry : rawData) {
            for (Map.Entry<?, ?> e : entry.entrySet()) {
                if (e.getKey() instanceof String) {
                    map.put((String) e.getKey(), e.getValue());
                }
            }
        }

        return new ItemDataModel(
                null, // displayName
                null, // lore
                null, // customModelData
                null, // enchantments
                null, // unbreakable
                map.isEmpty() ? null : map
        );
    }
}
