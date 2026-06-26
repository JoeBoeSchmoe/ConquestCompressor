package org.conquest.conquestCompressor.configurationHandler.configurationFiles;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.conquest.conquestCompressor.ConquestCompressor;
import org.conquest.conquestCompressor.functionalHandler.ItemDataModel;
import org.conquest.conquestCompressor.functionalHandler.compressorHandler.CompressorManager;
import org.conquest.conquestCompressor.functionalHandler.compressorHandler.CompressorModel;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * 🔁 GameAutocompressorFile
 * Manages the loading and access to gameplayConfiguration/gameAutocompressor.yml
 */
public class GameAutocompressorFile {

    private static final ConquestCompressor plugin = ConquestCompressor.getInstance();
    private static final Logger log = plugin.getLogger();
    private static YamlConfiguration config;

    private GameAutocompressorFile() {
        // Utility class
    }

    /**
     * Loads or creates the gameAutocompressor.yml file.
     */
    public static void load() {
        try {
            File folder = plugin.getDataFolder();
            if (!folder.exists() && !folder.mkdirs()) {
                log.warning("⚠️  Failed to create plugin data folder: " + folder.getAbsolutePath());
            }

            File recipesDir = new File(folder, "gameplayConfiguration");
            if (!recipesDir.exists() && !recipesDir.mkdirs()) {
                log.warning("⚠️  Failed to create gameplayConfiguration directory: " + recipesDir.getAbsolutePath());
            }

            File file = new File(recipesDir, "gameAutocompressor.yml");

            if (!file.exists()) {
                try (InputStream in = plugin.getResource("gameplayConfiguration/gameAutocompressor.yml")) {
                    if (in != null) {
                        Files.copy(in, file.toPath());
                        log.info("📄  Created default gameAutocompressor.yml");
                    } else {
                        log.warning("⚠️  Missing embedded gameAutocompressor.yml resource.");
                    }
                }
            }

            config = YamlConfiguration.loadConfiguration(file);

            ConfigurationSection compressions = config.getConfigurationSection("compressions");
            if (compressions == null) {
                log.warning("⚠️  No 'compressions' section found in gameAutocompressor.yml.");
                return;
            }

            int loadedCount = 0;

            for (String key : compressions.getKeys(false)) {
                try {
                    ConfigurationSection section = compressions.getConfigurationSection(key);
                    if (section == null) continue;

                    boolean enabled = section.getBoolean("enabled", true);

                    // 🧱 Input
                    ConfigurationSection hasItems = section.getConfigurationSection("hasItems");
                    if (hasItems == null) {
                        log.warning("⚠️  Missing hasItems section for: " + key);
                        continue;
                    }

                    Material inputMaterial = Material.matchMaterial(hasItems.getString("material", ""));
                    int inputAmount = hasItems.getInt("amount", 1);
                    ItemDataModel inputData = null;

                    if (hasItems.contains("itemData")) {
                        ConfigurationSection dataSec = hasItems.getConfigurationSection("itemData");
                        if (dataSec != null) {
                            inputData = ItemDataModel.deserialize(toPlainMap(dataSec));
                        } else {
                            log.warning("⚠️  itemData in hasItems is not a ConfigurationSection for: " + key);
                        }
                    }

                    // 🎁 Output
                    ConfigurationSection giveItems = section.getConfigurationSection("giveItems");
                    if (giveItems == null) {
                        log.warning("⚠️  Missing giveItems section for: " + key);
                        continue;
                    }

                    Material outputMaterial = Material.matchMaterial(giveItems.getString("material", ""));
                    int outputAmount = giveItems.getInt("amount", 1);
                    ItemDataModel outputData = null;

                    if (giveItems.contains("itemData")) {
                        ConfigurationSection dataSec = giveItems.getConfigurationSection("itemData");
                        if (dataSec != null) {
                            outputData = ItemDataModel.deserialize(toPlainMap(dataSec));
                        } else {
                            log.warning("⚠️  itemData in giveItems is not a ConfigurationSection for: " + key);
                        }
                    }

                    if (inputMaterial == null || outputMaterial == null) {
                        log.warning("⚠️  Invalid material in recipe: " + key);
                        continue;
                    }

                    CompressorModel model = new CompressorModel(
                            key,
                            enabled,
                            inputMaterial,
                            inputAmount,
                            inputData,
                            outputMaterial,
                            outputAmount,
                            outputData
                    );

                    CompressorManager.register(model);
                    loadedCount++;

                } catch (Exception ex) {
                    log.warning("❌ Failed to load compressor recipe '" + key + "': " + ex.getMessage());
                }
            }

            log.info("✅  Loaded " + loadedCount + " compressor recipes from gameAutocompressor.yml");

        } catch (Exception e) {
            log.severe("❌  Failed to load gameAutocompressor.yml: " + e.getMessage());
        }
    }

    public static void saveCompressorModel(CompressorModel model) {
        if (config == null || model == null) {
            log.warning("⚠️ Cannot save compressor model: config or model is null.");
            return;
        }

        String path = "compressions." + model.getKey();
        ConfigurationSection section = config.createSection(path);

        section.set("enabled", model.isEnabled());

        // 🔻 Save input under "hasItems"
        ConfigurationSection input = section.createSection("hasItems");
        input.set("material", model.getInputMaterial().name());
        input.set("amount", model.getInputAmount());

        if (model.getInputItemData() != null) {
            input.set("itemData", model.getInputItemData().serialize());
        }

        // 🔺 Save output under "giveItems"
        ConfigurationSection output = section.createSection("giveItems");
        output.set("material", model.getOutputMaterial().name());
        output.set("amount", model.getOutputAmount());

        if (model.getOutputItemData() != null) {
            output.set("itemData", model.getOutputItemData().serialize());
        }

        try {
            File file = new File(plugin.getDataFolder(), "gameplayConfiguration/gameAutocompressor.yml");
            config.save(file);
            log.info("💾  Saved compressor model: " + model.getKey());
        } catch (IOException e) {
            log.severe("❌  Failed to save compressor model '" + model.getKey() + "': " + e.getMessage());
        }
    }

    public static YamlConfiguration getConfig() {
        return config;
    }

    public static void save() {
        if (config == null) {
            log.warning("⚠️ Cannot save config: YamlConfiguration is null.");
            return;
        }

        try {
            File file = new File(plugin.getDataFolder(), "gameplayConfiguration/gameAutocompressor.yml");
            config.save(file);
            log.info("💾  Saved gameAutocompressor.yml");
        } catch (IOException e) {
            log.severe("❌  Failed to save gameAutocompressor.yml: " + e.getMessage());
        }
    }

    public static ConfigurationSection getSection(String path) {
        return config != null ? config.getConfigurationSection("compressions." + path) : null;
    }

    public static boolean contains(String path) {
        return config != null && config.contains("compressions." + path);
    }

    /**
     * Converts Bukkit ConfigurationSection trees into normal nested Maps.
     *
     * This matters for deep itemData values such as:
     *
     * itemData:
     *   attributes:
     *     - attribute: generic.attack_damage
     *       name: conquest_attack_damage
     *       amount: 12.0
     *       operation: ADD_NUMBER
     *       slot: MAINHAND
     *   nbt:
     *     placeholder: true
     *     PublicBukkitValues:
     *       minecraft:drop_id: stone1-coin
     *
     * Bukkit may keep nested YAML sections as ConfigurationSection objects.
     * ItemDataModel expects normal Map<String, Object> and List<Object> values.
     */
    private static Map<String, Object> toPlainMap(ConfigurationSection section) {
        Map<String, Object> map = new LinkedHashMap<>();

        if (section == null) {
            return map;
        }

        for (String key : section.getKeys(false)) {
            map.put(key, normalizeYamlValue(section.get(key)));
        }

        return map;
    }

    @SuppressWarnings("unchecked")
    private static Object normalizeYamlValue(Object value) {
        if (value instanceof ConfigurationSection childSection) {
            return toPlainMap(childSection);
        }

        if (value instanceof Map<?, ?> rawMap) {
            Map<String, Object> normalized = new LinkedHashMap<>();

            for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
                if (entry.getKey() == null) continue;

                String key = String.valueOf(entry.getKey());
                normalized.put(key, normalizeYamlValue(entry.getValue()));
            }

            return normalized;
        }

        if (value instanceof List<?> rawList) {
            List<Object> normalized = new ArrayList<>();

            for (Object entry : rawList) {
                normalized.add(normalizeYamlValue(entry));
            }

            return normalized;
        }

        return value;
    }
}