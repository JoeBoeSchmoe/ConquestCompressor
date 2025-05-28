package org.conquest.conquestCompressor.configurationHandler.configurationFiles;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.conquest.conquestCompressor.ConquestCompressor;
import org.conquest.conquestCompressor.functionalHandler.compressorHandler.CompressorManager;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
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

            CompressorManager.load();
            log.info("✅  Loaded gameAutocompressor.yml successfully.");

        } catch (Exception e) {
            log.severe("❌  Failed to load gameAutocompressor.yml: " + e.getMessage());
        }
    }

    public static YamlConfiguration getConfig() {
        return config;
    }

    public static ConfigurationSection getSection(String path) {
        return config != null ? config.getConfigurationSection("compressions." + path) : null;
    }

    public static boolean contains(String path) {
        return config != null && config.contains("compressions." + path);
    }
}
