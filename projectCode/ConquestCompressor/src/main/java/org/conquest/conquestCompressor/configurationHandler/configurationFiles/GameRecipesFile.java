package org.conquest.conquestCompressor.configurationHandler.configurationFiles;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.conquest.conquestCompressor.ConquestCompressor;
import org.conquest.conquestCompressor.functionalHandler.recipeHandler.RecipeManager;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.logging.Logger;

/**
 * 📘 GameRecipesFile
 * Manages the loading and access to gameplayConfiguration/gameRecipes.yml
 */
public class GameRecipesFile {

    private static final ConquestCompressor plugin = ConquestCompressor.getInstance();
    private static final Logger log = plugin.getLogger();
    private static YamlConfiguration config;

    private GameRecipesFile() {
        // Utility class
    }

    /**
     * Loads or creates the gameRecipes.yml file.
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

            File file = new File(recipesDir, "gameRecipes.yml");

            if (!file.exists()) {
                try (InputStream in = plugin.getResource("gameplayConfiguration/gameRecipes.yml")) {
                    if (in != null) {
                        Files.copy(in, file.toPath());
                        log.info("📄  Created default gameRecipes.yml");
                    } else {
                        log.warning("⚠️  Missing embedded gameRecipes.yml resource.");
                    }
                }
            }

            config = YamlConfiguration.loadConfiguration(file);
            RecipeManager.load();
            log.info("✅  Loaded gameRecipes.yml successfully.");

        } catch (Exception e) {
            log.severe("❌  Failed to load gameRecipes.yml: " + e.getMessage());
        }
    }

    public static YamlConfiguration getConfig() {
        return config;
    }

    public static ConfigurationSection getSection(String path) {
        return config != null ? config.getConfigurationSection("recipes." + path) : null;
    }

    public static boolean contains(String path) {
        return config != null && config.contains("recipes." + path);
    }
}
