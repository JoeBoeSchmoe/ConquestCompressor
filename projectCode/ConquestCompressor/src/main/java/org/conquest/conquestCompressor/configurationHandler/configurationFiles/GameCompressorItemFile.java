package org.conquest.conquestCompressor.configurationHandler.configurationFiles;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.conquest.conquestCompressor.ConquestCompressor;
import org.conquest.conquestCompressor.functionalHandler.ItemDataModel;
import org.conquest.conquestCompressor.functionalHandler.compressorHandler.CompressorItemManager;
import org.conquest.conquestCompressor.functionalHandler.compressorHandler.CompressorItemModel;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * 🎛️ GameCompressorItemFile
 * Manages the loading/saving of gameplayConfiguration/gameCompressorItems.yml
 *
 * Layout:
 * compressorItems:
 *   <key>:
 *     enabled: true
 *     item:
 *       material: GOLD_INGOT
 *       amount: 1
 *       itemData: { ... ItemDataModel.serialize() ... }
 *     trigger:
 *       leftClick: true
 *       rightClick: true
 *       cooldownTicks: 10
 *       consumeOnUse: false
 *     recipes: [gold_ingot_to_block]
 */
public class GameCompressorItemFile {

    private static final ConquestCompressor plugin = ConquestCompressor.getInstance();
    private static final Logger log = plugin.getLogger();
    private static YamlConfiguration config;

    private static final String REL_DIR = "gameplayConfiguration";
    private static final String FILE_NAME = "gameCompressorItems.yml";
    private static final String ROOT = "compressorItems";

    private GameCompressorItemFile() {
        // utility
    }

    /**
     * Loads or creates the gameCompressorItems.yml file, parses entries,
     * constructs CompressorItemModel instances and registers them into CompressorItemManager.
     */
    public static void load() {
        try {
            File folder = plugin.getDataFolder();
            if (!folder.exists() && !folder.mkdirs()) {
                log.warning("⚠️  Failed to create plugin data folder: " + folder.getAbsolutePath());
            }

            File cfgDir = new File(folder, REL_DIR);
            if (!cfgDir.exists() && !cfgDir.mkdirs()) {
                log.warning("⚠️  Failed to create " + REL_DIR + " directory: " + cfgDir.getAbsolutePath());
            }

            File file = new File(cfgDir, FILE_NAME);
            if (!file.exists()) {
                try (InputStream in = plugin.getResource(REL_DIR + "/" + FILE_NAME)) {
                    if (in != null) {
                        Files.copy(in, file.toPath());
                        log.info("📄  Created default " + FILE_NAME);
                    } else {
                        log.warning("⚠️  Missing embedded " + FILE_NAME + " resource.");
                    }
                }
            }

            config = YamlConfiguration.loadConfiguration(file);

            ConfigurationSection items = config.getConfigurationSection(ROOT);
            if (items == null) {
                log.warning("⚠️  No '" + ROOT + "' section found in " + FILE_NAME + ".");
                return;
            }

            // Reset registry (if your manager exposes this)
            CompressorItemManager.clear();

            int loadedCount = 0;
            for (String key : items.getKeys(false)) {
                try {
                    ConfigurationSection section = items.getConfigurationSection(key);
                    if (section == null) continue;

                    boolean enabled = section.getBoolean("enabled", true);

                    // ---- item ----
                    ConfigurationSection itemSec = section.getConfigurationSection("item");
                    if (itemSec == null) {
                        log.warning("⚠️  Missing 'item' section for: " + key);
                        continue;
                    }

                    String matName = itemSec.getString("material", "");
                    Material material = Material.matchMaterial(matName);
                    int amount = Math.max(1, itemSec.getInt("amount", 1));

                    ItemDataModel itemData = null;
                    if (itemSec.isConfigurationSection("itemData")) {
                        ConfigurationSection itemDataSec = itemSec.getConfigurationSection("itemData");
                        if (itemDataSec != null) {
                            itemData = ItemDataModel.deserialize(itemDataSec.getValues(false));
                        }
                    }

                    if (material == null) {
                        log.warning("⚠️  Invalid item.material for compressor item: " + key);
                        continue;
                    }

                    // ---- trigger ----
                    ConfigurationSection trig = section.getConfigurationSection("trigger");
                    if (trig == null) {
                        log.warning("⚠️  Missing 'trigger' section for: " + key);
                        continue;
                    }
                    boolean leftClick = trig.getBoolean("leftClick", true);
                    boolean rightClick = trig.getBoolean("rightClick", true);
                    int cooldownTicks = trig.getInt("cooldownTicks", 0);
                    boolean consumeOnUse = trig.getBoolean("consumeOnUse", false);

                    // ---- recipes ----
                    List<String> recipes = section.getStringList("recipes");
                    if (recipes == null || recipes.isEmpty()) {
                        log.warning("⚠️  No 'recipes' defined for: " + key);
                        continue;
                    }

                    CompressorItemModel model = new CompressorItemModel(
                            key,
                            enabled,
                            material,
                            amount,
                            itemData,
                            leftClick,
                            rightClick,
                            cooldownTicks,
                            consumeOnUse,
                            new ArrayList<>(recipes)
                    );

                    CompressorItemManager.register(model);
                    loadedCount++;

                } catch (Exception ex) {
                    log.warning("❌  Failed to load compressor item '" + key + "': " + ex.getMessage());
                }
            }

            log.info("✅  Loaded " + loadedCount + " compressor items from " + FILE_NAME);

        } catch (Exception e) {
            log.severe("❌  Failed to load " + FILE_NAME + ": " + e.getMessage());
        }
    }

    /**
     * Saves/updates a single compressor item entry by serializing the model back into YAML.
     * Does not persist to disk until you call save().
     */
    public static void saveCompressorItem(CompressorItemModel model) {
        if (config == null || model == null) {
            log.warning("⚠️  Cannot save compressor item: config or model is null.");
            return;
        }

        // Ensure root
        ConfigurationSection root = config.getConfigurationSection(ROOT);
        if (root == null) root = config.createSection(ROOT);

        String path = ROOT + "." + model.key();
        config.set(path, null);
        ConfigurationSection sec = config.createSection(path);

        sec.set("enabled", model.enabled());

        // ---- item ----
        ConfigurationSection itemSec = sec.createSection("item");
        itemSec.set("material", model.material().name());
        itemSec.set("amount", Math.max(1, model.amount()));
        model.itemData().ifPresent(idm -> itemSec.set("itemData", idm.serialize()));

        // ---- trigger ----
        ConfigurationSection trig = sec.createSection("trigger");
        trig.set("leftClick", model.leftClick());
        trig.set("rightClick", model.rightClick());
        trig.set("cooldownTicks", model.cooldownTicks());
        trig.set("consumeOnUse", model.consumeOnUse());

        // ---- recipes ----
        sec.set("recipes", new ArrayList<>(model.recipeKeys()));

        log.info("💾  Prepared compressor item for saving: " + model.key());
    }

    /**
     * Persists the current YAML to disk.
     */
    public static void save() {
        if (config == null) {
            log.warning("⚠️  Cannot save config: YamlConfiguration is null.");
            return;
        }

        try {
            File file = new File(plugin.getDataFolder(), REL_DIR + "/" + FILE_NAME);
            config.save(file);
            log.info("💾  Saved " + FILE_NAME);
        } catch (IOException e) {
            log.severe("❌  Failed to save " + FILE_NAME + ": " + e.getMessage());
        }
    }

    public static YamlConfiguration getConfig() {
        return config;
    }

    public static ConfigurationSection getSection(String path) {
        return config != null ? config.getConfigurationSection(ROOT + "." + path) : null;
    }

    public static boolean contains(String path) {
        return config != null && config.contains(ROOT + "." + path);
    }
}
