package org.conquest.conquestCompressor.configurationHandler.configurationFiles;

import org.bukkit.configuration.file.YamlConfiguration;
import org.conquest.conquestCompressor.ConquestCompressor;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * 💾 PlayerToggleStatesFile
 * Manages saving/loading player toggle states for auto-compression.
 */
public class PlayerToggleStatesFile {

    private static final ConquestCompressor plugin = ConquestCompressor.getInstance();
    private static final Logger log = plugin.getLogger();
    private static File file;
    private static YamlConfiguration config;

    public static void load() {
        try {
            File folder = new File(plugin.getDataFolder(), "userData");
            if (!folder.exists() && !folder.mkdirs()) {
                log.warning("⚠️  Failed to create userData directory.");
            }

            file = new File(folder, "playerToggleStates.yml");
            if (!file.exists() && !file.createNewFile()) {
                log.warning("⚠️  Failed to create playerToggleStates.yml.");
            }

            config = YamlConfiguration.loadConfiguration(file);
            log.info("✅  Loaded playerToggleStates.yml");

        } catch (IOException e) {
            log.severe("❌  Failed to load playerToggleStates.yml: " + e.getMessage());
        }
    }

    public static void saveDisabled(Set<UUID> disabled) {
        if (config == null) return;
        config.set("disabled", disabled.stream().map(UUID::toString).collect(Collectors.toList()));
        try {
            config.save(file);
        } catch (IOException e) {
            log.severe("❌  Failed to save toggle states: " + e.getMessage());
        }
    }

    public static Set<UUID> loadDisabled() {
        if (config == null) return Set.of();
        return config.getStringList("disabled").stream()
                .map(UUID::fromString)
                .collect(Collectors.toSet());
    }
}
