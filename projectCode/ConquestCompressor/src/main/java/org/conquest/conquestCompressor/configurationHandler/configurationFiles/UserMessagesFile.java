package org.conquest.conquestCompressor.configurationHandler.configurationFiles;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.conquest.conquestCompressor.ConquestCompressor;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.logging.Logger;

/**
 * 📩 UserMessagesFile
 * Manages the loading and access to messagesConfiguration/userMessages.yml
 */
public class UserMessagesFile {

    private static final ConquestCompressor plugin = ConquestCompressor.getInstance();
    private static final Logger log = plugin.getLogger();
    private static YamlConfiguration config;

    private UserMessagesFile() {
        // Utility class
    }

    /**
     * Loads or creates the userMessages.yml file.
     */
    public static void load() {
        try {
            File folder = plugin.getDataFolder();
            if (!folder.exists() && !folder.mkdirs()) {
                log.warning("⚠️  Failed to create plugin data folder: " + folder.getAbsolutePath());
            }

            File messagesDir = new File(folder, "messagesConfiguration");
            if (!messagesDir.exists() && !messagesDir.mkdirs()) {
                log.warning("⚠️  Failed to create messagesConfiguration directory: " + messagesDir.getAbsolutePath());
            }

            File file = new File(messagesDir, "userMessages.yml");

            if (!file.exists()) {
                try (InputStream in = plugin.getResource("messagesConfiguration/userMessages.yml")) {
                    if (in != null) {
                        Files.copy(in, file.toPath());
                        log.info("📄  Created default userMessages.yml");
                    } else {
                        log.warning("⚠️  Missing embedded userMessages.yml resource.");
                    }
                }
            }

            config = YamlConfiguration.loadConfiguration(file);
            log.info("✅  Loaded userMessages.yml successfully.");

        } catch (Exception e) {
            log.severe("❌  Failed to load userMessages.yml: " + e.getMessage());
        }
    }

    public static YamlConfiguration getConfig() {
        return config;
    }

    public static ConfigurationSection getSection(String path) {
        return config != null ? config.getConfigurationSection("messages." + path) : null;
    }

    public static boolean contains(String path) {
        return config != null && config.contains("messages." + path);
    }
}
