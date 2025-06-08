package org.conquest.conquestCompressor.configurationHandler.configurationFiles;

import org.conquest.conquestCompressor.ConquestCompressor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.conquest.conquestCompressor.compressingHandler.AutoCompressTrigger;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;
import java.util.logging.Logger;

/**
 * ⚙️ configFile
 * Loads and manages access to config.yml as a static utility.
 */
public class ConfigFile {

    private static final ConquestCompressor plugin = ConquestCompressor.getInstance();
    private static final Logger log = plugin.getLogger();
    private static YamlConfiguration config;

    private ConfigFile() {
        // Utility class — prevent instantiation
    }

    /**
     * Loads or creates the config.yml file from the plugin's data folder.
     */
    public static void load() {
        try {
            File dataFolder = plugin.getDataFolder();
            if (!dataFolder.exists() && !dataFolder.mkdirs()) {
                log.warning("⚠️  Failed to create plugin data folder: " + dataFolder.getAbsolutePath());
            }

            File configFile = new File(dataFolder, "config.yml");

            if (!configFile.exists()) {
                try (InputStream in = plugin.getResource("config.yml")) {
                    if (in != null) {
                        Files.copy(in, configFile.toPath());
                        log.info("📄  Created default config.yml");
                    } else {
                        log.warning("⚠️  Missing embedded config.yml resource in plugin jar!");
                    }
                }
            }

            config = YamlConfiguration.loadConfiguration(configFile);
            log.info("✅  Loaded config.yml successfully.");

        } catch (Exception e) {
            log.severe("❌  Failed to load config.yml: " + e.getMessage());
        }
    }

    public static YamlConfiguration getConfig() {
        return config;
    }

    public static boolean contains(String path) {
        return config != null && config.contains(path);
    }

    public static String getString(String path) {
        return config != null ? config.getString(path) : null;
    }

    public static boolean getBoolean(String path, boolean def) {
        return config != null ? config.getBoolean(path, def) : def;
    }

    public static int getInt(String path, int def) {
        return config != null ? config.getInt(path, def) : def;
    }

    public static double getDouble(String path, double def) {
        return config != null ? config.getDouble(path, def) : def;
    }

    public static List<String> getStringList(String path) {
        return config != null ? config.getStringList(path) : List.of();
    }

    public static AutoCompressTrigger getCompressTrigger() {
        String raw = getString("auto-compress-interval");
        return AutoCompressTrigger.fromConfig(raw);
    }
}
