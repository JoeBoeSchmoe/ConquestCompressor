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
     * Clears all in-memory compressor recipes.
     */
    public static void clear() {
        compressorRecipes.clear();
    }

    /**
     * Reloads and parses all compressor recipes into memory.
     * Calls clear() before repopulating.
     */
    public static void load() {
        clear();
        ConfigurationSection root = GameAutocompressorFile.getConfig().getConfigurationSection("compressions");

        if (root == null) {
            log.warning("⚠️  No 'compressions' section found in gameAutocompressor.yml");
            return;
        }

        int count = 0;
        for (String key : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(key);
            if (section == null) continue;

            boolean enabled = section.getBoolean("enabled", true);
            if (!enabled) continue;

            // ─── Input ─────────────────────
            ConfigurationSection hasItems = section.getConfigurationSection("hasItems");
            if (hasItems == null) {
                log.warning("⚠️  Missing hasItems section in '" + key + "'");
                continue;
            }

            Material inputMat = Material.matchMaterial(hasItems.getString("material", ""));
            int inputAmt = hasItems.getInt("amount", 1);

            ItemDataModel inputData = null;
            if (hasItems.contains("itemData")) {
                ConfigurationSection inputDataSection = hasItems.getConfigurationSection("itemData");
                inputData = inputDataSection != null ? ItemDataModel.deserialize(inputDataSection.getValues(false)) : null;
            }

            // ─── Output ────────────────────
            ConfigurationSection giveItems = section.getConfigurationSection("giveItems");
            if (giveItems == null) {
                log.warning("⚠️  Missing giveItems section in '" + key + "'");
                continue;
            }

            Material outputMat = Material.matchMaterial(giveItems.getString("material", ""));
            int outputAmt = giveItems.getInt("amount", 1);

            ItemDataModel outputData = null;
            if (giveItems.contains("itemData")) {
                ConfigurationSection outputDataSection = giveItems.getConfigurationSection("itemData");
                outputData = outputDataSection != null ? ItemDataModel.deserialize(outputDataSection.getValues(false)) : null;
            }

            // ─── Validation ────────────────
            if (inputMat == null || outputMat == null) {
                log.warning("⚠️  Invalid material(s) in compressor '" + key + "'");
                continue;
            }

            CompressorModel model = new CompressorModel(
                    key,
                    true,
                    inputMat, inputAmt, inputData,
                    outputMat, outputAmt, outputData
            );

            register(model);
            count++;
        }

        log.info("✅  Loaded " + count + " compressor recipe(s).");
    }

    /**
     * Registers a compressor model at runtime.
     */
    public static void register(CompressorModel model) {
        if (model == null || model.getKey() == null) {
            log.warning("⚠️  Tried to register null model or null key.");
            return;
        }
        compressorRecipes.put(model.getKey(), model);
    }

    /**
     * Deletes a compressor by key from memory and config.
     */
    public static boolean deleteRecipe(String key) {
        if (!compressorRecipes.containsKey(key)) return false;
        compressorRecipes.remove(key);

        ConfigurationSection root = GameAutocompressorFile.getConfig().getConfigurationSection("compressions");
        if (root != null && root.contains(key)) {
            root.set(key, null);
            GameAutocompressorFile.save();
            log.info("🗑️ Deleted compressor recipe: " + key);
            return true;
        }

        return false;
    }

    /**
     * Gets all compressor models.
     */
    public static Collection<CompressorModel> getAllRecipes() {
        return compressorRecipes.values();
    }

    /**
     * Gets a compressor model by key.
     */
    public static CompressorModel getRecipe(String key) {
        return compressorRecipes.get(key);
    }

    /**
     * Checks if the manager is empty.
     */
    public static boolean isEmpty() {
        return compressorRecipes.isEmpty();
    }

    /**
     * Checks if a key is registered.
     */
    public static boolean exists(String key) {
        return compressorRecipes.containsKey(key);
    }
}
