package org.conquest.conquestCompressor.functionalHandler.compressorHandler;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.conquest.conquestCompressor.configurationHandler.configurationFiles.GameAutocompressorFile;
import org.conquest.conquestCompressor.functionalHandler.ItemDataModel;

import java.util.*;
import java.util.logging.Logger;

/**
 * 🧠 CompressorManager
 * Loads and caches all compressor recipes from gameAutocompressor.yml.
 */
public class CompressorManager {

    private static final Map<String, CompressorModel> compressorRecipes = new HashMap<>();
    private static final Logger log = org.conquest.conquestCompressor.ConquestCompressor.getInstance().getLogger();

    private CompressorManager() {
        // Utility class
    }

    /**
     * Loads and parses all compressor recipes into memory.
     */
    public static void load() {
        compressorRecipes.clear();
        ConfigurationSection root = GameAutocompressorFile.getConfig().getConfigurationSection("compressions");

        if (root == null) {
            log.warning("⚠️  No 'compressions' section found in gameAutocompressor.yml");
            return;
        }

        for (String key : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(key);
            if (section == null) continue;

            boolean enabled = section.getBoolean("enabled", true);
            if (!enabled) continue;

            // Parse input
            ConfigurationSection hasItems = section.getConfigurationSection("hasItems");
            String inputMatStr = hasItems.getString("material");
            Material inputMat = inputMatStr != null ? Material.matchMaterial(inputMatStr) : null;
            int inputAmt = hasItems.getInt("amount", 1);
            List<Map<?, ?>> inputRawData = hasItems.getMapList("itemData");
            ItemDataModel inputData = ItemDataModel.fromList(inputRawData);

            // Parse output
            ConfigurationSection giveItems = section.getConfigurationSection("giveItems");
            String outputMatStr = giveItems.getString("material");
            Material outputMat = outputMatStr != null ? Material.matchMaterial(outputMatStr) : null;
            int outputAmt = giveItems.getInt("amount", 1);
            List<Map<?, ?>> outputRawData = giveItems.getMapList("itemData");
            ItemDataModel outputData = ItemDataModel.fromList(outputRawData);

            // Validation
            if (inputMat == null || outputMat == null) {
                log.warning("⚠️  Invalid material in compression recipe '" + key + "'");
                continue;
            }

            CompressorModel model = new CompressorModel(
                    key,
                    true,
                    inputMat, inputAmt, inputData,
                    outputMat, outputAmt, outputData
            );
            compressorRecipes.put(key, model);
        }

        log.info("✅  Loaded " + compressorRecipes.size() + " compressor recipe(s). ✔");
    }

    /**
     * Gets all loaded compression recipes.
     */
    public static Collection<CompressorModel> getAllRecipes() {
        return compressorRecipes.values();
    }

    /**
     * Gets a recipe by key.
     */
    public static CompressorModel getRecipe(String key) {
        return compressorRecipes.get(key);
    }

    /**
     * Checks if any recipes are loaded.
     */
    public static boolean isEmpty() {
        return compressorRecipes.isEmpty();
    }
}
