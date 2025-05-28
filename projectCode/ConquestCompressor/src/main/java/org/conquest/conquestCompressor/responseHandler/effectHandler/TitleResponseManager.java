package org.conquest.conquestCompressor.responseHandler.effectHandler;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.conquest.conquestCompressor.ConquestCompressor;
import org.conquest.conquestCompressor.configurationHandler.integrationFiles.PlaceHolderAPIManager;

import java.time.Duration;
import java.util.Map;

/**
 * 🎬 TitleResponseManager
 * Supports one-line YAML timing syntax:
 * title:
 *   text: "<gold><bold>Spawner Admin</bold></gold>"
 *   subtitle: "<gray>Use /spawners admin help to get started."
 *   timings: { fadeIn: 10, stay: 40, fadeOut: 200 }
 */
public class TitleResponseManager {

    public static void send(Player player, ConfigurationSection section, Map<String, String> placeholders) {
        if (player == null || section == null) return;

        ConfigurationSection titleSection = section.getConfigurationSection("title");
        if (titleSection == null) return;

        String rawTitle = titleSection.getString("text", "").trim();
        String rawSubtitle = titleSection.getString("subtitle", "").trim();

        if (rawTitle.isEmpty() && rawSubtitle.isEmpty()) return;

        Component title = rawTitle.isEmpty() ? Component.empty()
                : PlaceHolderAPIManager.parse(player, rawTitle, placeholders);
        Component subtitle = rawSubtitle.isEmpty() ? Component.empty()
                : PlaceHolderAPIManager.parse(player, rawSubtitle, placeholders);

        int fadeIn = 10, stay = 40, fadeOut = 20;

        Object timingsObj = titleSection.get("timings");
        if (timingsObj instanceof ConfigurationSection timingConfig) {
            fadeIn = timingConfig.getInt("fadeIn", fadeIn);
            stay = timingConfig.getInt("stay", stay);
            fadeOut = timingConfig.getInt("fadeOut", fadeOut);
        }

        if (!isEmpty(title) || !isEmpty(subtitle)) {
            player.clearTitle();
            player.showTitle(Title.title(
                    title,
                    subtitle,
                    Title.Times.times(
                            Duration.ofMillis(fadeIn * 50L),
                            Duration.ofMillis(stay * 50L),
                            Duration.ofMillis(fadeOut * 50L)
                    )
            ));

            ConquestCompressor.getInstance().getLogger().info(
                    "[Title] Sent to " + player.getName() +
                            " | Title: " + rawTitle +
                            " | Subtitle: " + rawSubtitle +
                            " | Timings: fadeIn=" + fadeIn + ", stay=" + stay + ", fadeOut=" + fadeOut
            );
        }
    }

    private static boolean isEmpty(Component component) {
        return component == null
                || Component.empty().equals(component)
                || net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                .serialize(component).isBlank();
    }
}
