package org.conquest.conquestCompressor.functionalHandler.recipeHandler;

import org.bukkit.Material;
import org.conquest.conquestCompressor.functionalHandler.ItemDataModel;

import java.util.Map;

/**
 * 🧩 RecipeModel
 * Represents a single shaped recipe from gameRecipes.yml.
 * Each slot maps to a material and optional item metadata.
 */
public class RecipeModel {

    private final String key;
    private final boolean enabled;

    // Map of slot (CraftingSlot1...9) to Material
    private final Map<String, Material> layout;

    // Map of slot to item metadata (NBT, lore, etc)
    private final Map<String, ItemDataModel> layoutItemData;

    // Output
    private final Material resultMaterial;
    private final int resultAmount;
    private final ItemDataModel resultItemData;

    public RecipeModel(String key,
                       boolean enabled,
                       Map<String, Material> layout,
                       Map<String, ItemDataModel> layoutItemData,
                       Material resultMaterial,
                       int resultAmount,
                       ItemDataModel resultItemData) {
        this.key = key;
        this.enabled = enabled;
        this.layout = layout;
        this.layoutItemData = layoutItemData;
        this.resultMaterial = resultMaterial;
        this.resultAmount = resultAmount;
        this.resultItemData = resultItemData;
    }

    public String getKey() {
        return key;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Map<String, Material> getLayout() {
        return layout;
    }

    public Map<String, ItemDataModel> getLayoutItemData() {
        return layoutItemData;
    }

    public Material getResultMaterial() {
        return resultMaterial;
    }

    public int getResultAmount() {
        return resultAmount;
    }

    public ItemDataModel getResultItemData() {
        return resultItemData;
    }
}
