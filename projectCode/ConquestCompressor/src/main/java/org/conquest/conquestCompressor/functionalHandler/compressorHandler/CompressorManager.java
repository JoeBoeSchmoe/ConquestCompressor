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

            // ─── Input ────────────────────────────────────────
            ConfigurationSection hasItems = section.getConfigurationSection("hasItems");
            if (hasItems == null) {
                return;
            }
            String inputMatStr = hasItems.getString("material");
            Material inputMat = inputMatStr != null ? Material.matchMaterial(inputMatStr) : null;
            int inputAmt = hasItems.getInt("amount", 1);

            ConfigurationSection inputDataSection = hasItems.getConfigurationSection("itemData");
            Map<String, Object> inputRawData = inputDataSection != null ? inputDataSection.getValues(true) : null;
            ItemDataModel inputData = ItemDataModel.deserialize(inputRawData);

            // ─── Output ───────────────────────────────────────
            ConfigurationSection giveItems = section.getConfigurationSection("giveItems");
            if (giveItems == null) {
                return;
            }
            String outputMatStr = giveItems.getString("material");
            Material outputMat = outputMatStr != null ? Material.matchMaterial(outputMatStr) : null;
            int outputAmt = giveItems.getInt("amount", 1);

            ConfigurationSection outputDataSection = giveItems.getConfigurationSection("itemData");
            Map<String, Object> outputRawData = outputDataSection != null ? outputDataSection.getValues(true) : null;
            ItemDataModel outputData = ItemDataModel.deserialize(outputRawData);

            // ─── Validation ───────────────────────────────────
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

    public static boolean deleteRecipe(String key) {
        if (!compressorRecipes.containsKey(key)) {
            return false;
        }

        // Remove from in-memory map
        compressorRecipes.remove(key);

        // Remove from config and save
        ConfigurationSection root = GameAutocompressorFile.getConfig().getConfigurationSection("compressions");
        if (root != null && root.contains(key)) {
            root.set(key, null);
            GameAutocompressorFile.save(); // Persist deletion
            log.info("🗑️ Deleted compressor recipe '" + key + "'");
            return true;
        }

        return false;
    }

    /**
     * Registers a new compressor recipe at runtime.
     */
    public static void register(CompressorModel model) {
        if (model == null || model.getKey() == null) {
            log.warning("⚠️  Tried to register null compressor model or model with null key.");
            return;
        }

        compressorRecipes.put(model.getKey(), model);
        //log.info("➕ Registered compressor recipe: " + model.getKey());
    }

}
