package org.conquest.conquestCompressor.functionalHandler.recipeHandler;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.conquest.conquestCompressor.configurationHandler.configurationFiles.GameRecipesFile;
import org.conquest.conquestCompressor.functionalHandler.ItemDataModel;

import java.util.*;
import java.util.logging.Logger;

/**
 * 📦 RecipeManager
 * Loads and caches all crafting recipes from gameRecipes.yml.
 */
public class RecipeManager {

    private static final Map<String, RecipeModel> recipeMap = new HashMap<>();
    private static final Logger log = org.conquest.conquestCompressor.ConquestCompressor.getInstance().getLogger();

    private RecipeManager() {
        // Utility class
    }

    /**
     * Loads and parses all crafting recipes into memory.
     */
    public static void load() {
        recipeMap.clear();
        ConfigurationSection root = GameRecipesFile.getConfig().getConfigurationSection("recipes");

        if (root == null) {
            log.warning("⚠️  No 'recipes' section found in gameRecipes.yml");
            return;
        }

        for (String key : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(key);
            if (section == null) continue;

            boolean enabled = section.getBoolean("enabled", true);
            if (!enabled) continue;

            ConfigurationSection layoutSection = section.getConfigurationSection("layout");
            if (layoutSection == null) continue;

            Map<String, Material> layout = new HashMap<>();
            Map<String, ItemDataModel> layoutItemData = new HashMap<>();

            for (int i = 1; i <= 9; i++) {
                String slotKey = "CraftingSlot" + i;
                ConfigurationSection slot = layoutSection.getConfigurationSection(slotKey);
                if (slot == null) continue;

                String matStr = slot.getString("material");
                Material mat = matStr != null ? Material.matchMaterial(matStr) : null;
                if (mat != null) layout.put(slotKey, mat);

                List<Map<?, ?>> rawItemData = slot.getMapList("itemData");
                ItemDataModel dataModel = ItemDataModel.fromList(rawItemData);
                if (dataModel != null) layoutItemData.put(slotKey, dataModel);
            }

            ConfigurationSection given = section.getConfigurationSection("given");
            if (given == null) continue;

            String resultMatStr = given.getString("material");
            Material resultMat = resultMatStr != null ? Material.matchMaterial(resultMatStr) : null;
            int resultAmt = given.getInt("amount", 1);
            List<Map<?, ?>> resultRawData = given.getMapList("itemData");
            ItemDataModel resultData = ItemDataModel.fromList(resultRawData);

            if (resultMat == null) {
                log.warning("⚠️  Invalid result material in recipe '" + key + "'");
                continue;
            }

            RecipeModel model = new RecipeModel(
                    key,
                    true,
                    layout,
                    layoutItemData,
                    resultMat,
                    resultAmt,
                    resultData
            );

            recipeMap.put(key, model);
        }

        log.info("✅  Loaded " + recipeMap.size() + " crafting recipe(s). ✔");
    }

    public static Collection<RecipeModel> getAllRecipes() {
        return recipeMap.values();
    }

    public static RecipeModel getRecipe(String key) {
        return recipeMap.get(key);
    }

    public static boolean isEmpty() {
        return recipeMap.isEmpty();
    }
}
