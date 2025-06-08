package org.conquest.conquestCompressor.configurationHandler;

import org.bukkit.configuration.file.FileConfiguration;
import org.conquest.conquestCompressor.ConquestCompressor;
import org.conquest.conquestCompressor.configurationHandler.configurationFiles.*;
import org.conquest.conquestCompressor.configurationHandler.integrationFiles.PlaceHolderAPIManager;

import java.util.logging.Logger;

/**
 * 🧩 ConfigurationManager
 * Handles loading config.yml and initializing external integrations (Vault, PlaceholderAPI).
 * Loads statically managed configuration files like ConfigFile and message files.
 */
public class ConfigurationManager {

    private final ConquestCompressor plugin = ConquestCompressor.getInstance();
    private final Logger log = plugin.getLogger();
    private FileConfiguration config;

    /**
     * Initializes core config and third-party integrations.
     */
    public void initialize() {
        try {
            log.info("📦  Loading configuration...");

            // 🔃 Load all YAML files
            ConfigFile.load();
            AdminMessagesFile.load();
            UserMessagesFile.load();
            GameAutocompressorFile.load();
            PlayerToggleStatesFile.load();

            CompressorGUIFile.load();

            this.config = ConfigFile.getConfig();

            // ✅ Validate structure
            checkAll();

            // 🔌 Integrations
            setupPlaceholderAPI();

            log.info("✅  Configuration loading complete.");
        } catch (Exception e) {
            log.severe("❌  Failed to load configuration: " + e.getMessage());
        }
    }

    /**
     * Validates all required config keys.
     */
    private void checkAll() {
        log.info("🔍  Validating config.yml structure...");
        check("chat-prefix");

        // World restrictions
        check("world-restrictions.whitelist-worlds");
        check("world-restrictions.allowed-worlds");

        // Command and PAPI
        check("command-aliases");
        check("placeholders.use-placeholderapi");

        // Cooldowns
        check("cooldowns.command-delay-ms");
        check("cooldowns.gui-action-cooldown-ms");
        check("cooldowns.interaction-cooldown-ms");

        // GUI settings
        check("gui-settings.timeout-seconds");
    }

    /**
     * Validates if a single key exists.
     */
    private void check(String path) {
        if (!ConfigFile.contains(path)) {
            log.warning("⚠️  Missing config.yml key: '" + path + "'");
        }
    }
    /**
     * Initializes PlaceholderAPI integration if enabled.
     */
    private void setupPlaceholderAPI() {
        boolean enabled = ConfigFile.getBoolean("placeholders.use-placeholderapi", true);
        PlaceHolderAPIManager.initialize(enabled);
    }

    public FileConfiguration getConfig() {
        return config;
    }
}
