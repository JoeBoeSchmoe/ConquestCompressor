package org.conquest.conquestCompressor.responseHandler.effectHandler;

import net.kyori.adventure.text.Component;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.conquest.conquestCompressor.ConquestCompressor;
import org.conquest.conquestCompressor.responseHandler.ComponentSerializerManager;

import java.util.Map;
import java.util.logging.Logger;

/**
 * ✨ ActionBarResponseManager
 * Handles sending ActionBar messages from response models.
 */
public class ActionBarResponseManager {

    private static final Logger log = ConquestCompressor.getInstance().getLogger();

    /**
     * Sends an actionbar to the player if defined in the configuration.
     *
     * @param player Player to send to
     * @param section Config section containing actionbar data
     * @param placeholders Placeholders to apply
     */
    public static void send(Player player, ConfigurationSection section, Map<String, String> placeholders) {
        if (player == null || section == null) return;
        if (!section.isConfigurationSection("actionbar")) return;

        ConfigurationSection actionbarSection = section.getConfigurationSection("actionbar");
        if (actionbarSection == null) return;

        try {
            String raw = actionbarSection.getString("text");
            int duration = actionbarSection.getInt("duration", 60);
            int fadeIn = actionbarSection.getInt("fadeIn", 5);
            int stay = actionbarSection.getInt("stay", 50);
            int fadeOut = actionbarSection.getInt("fadeOut", 5);
            String color = actionbarSection.getString("color", "WHITE");

            if (raw != null && !raw.isEmpty()) {
                Component actionbarComponent = ComponentSerializerManager.format(player, raw, placeholders);
                player.sendActionBar(actionbarComponent);

            }

        } catch (Exception e) {
            log.warning("⚠️  Failed to send actionbar to player " + player.getName() + ": " + e.getMessage());
        }
    }
}
