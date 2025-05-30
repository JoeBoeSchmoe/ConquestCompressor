package org.conquest.conquestCompressor.configurationHandler.integrationFiles;

import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.conquest.conquestCompressor.ConquestCompressor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * 📘 SafePlaceholderAPIManager - avoids hard reference to PAPI
 */
public class PlaceHolderAPIManager {

    private static boolean usingPapi = false;
    private static final MiniMessage MINI = MiniMessage.miniMessage();

    /**
     * Initializes PlaceholderAPI support if enabled.
     *
     * @param configEnabled Whether PlaceholderAPI is enabled in config
     */
    public static void initialize(boolean configEnabled) {
        Logger log = ConquestCompressor.getInstance().getLogger();

        if (!configEnabled) {
            log.info("📘  PlaceholderAPI is disabled in config.yml.");
            return;
        }

        if (!Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            log.warning("⚠️  PlaceholderAPI is enabled in config, but the plugin is missing.");
            return;
        }

        new PlaceholderExpansion() {
            @Override public @NotNull String getIdentifier() { return "conquestcompressor"; }
            @Override public @NotNull String getAuthor() { return "Conquest Coder"; }
            @Override public @NotNull String getVersion() { return "1.0"; }

            @Override
            public @Nullable String onPlaceholderRequest(Player player, @NotNull String identifier) {
                if (player == null) return "";

                return switch (identifier.toLowerCase()) {
                    case "player" -> player.getName();
                    case "displayname" -> plain(player.displayName());
                    case "elo", "wins", "losses", "streak", "best_streak" -> "0"; // TODO: Hook real stats later
                    case "state" -> "Idle";
                    default -> null;
                };
            }
        }.register();

        usingPapi = true;
        log.info("✅  PlaceholderAPI hooked successfully. Custom placeholders registered.");
    }

    /**
     * @return true if PlaceholderAPI is hooked and available
     */
    public static boolean isUsingPlaceholderAPI() {
        return usingPapi;
    }

    // ===========================
    // 🎨 MiniMessage Parsing
    // ===========================

    /**
     * Parses a MiniMessage string with PlaceholderAPI and custom placeholders applied.
     */
    public static Component parse(@Nullable Player player, String raw, Map<String, String> placeholders) {
        if (raw == null || raw.isEmpty()) return Component.empty();

        String parsed = raw;

        // 🔥 Apply custom {placeholders}
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            parsed = parsed.replace("{" + entry.getKey() + "}", entry.getValue());
        }

        // 🔥 Apply PlaceholderAPI %placeholders% if enabled
        if (player != null && usingPapi) {
            parsed = PlaceholderAPI.setPlaceholders(player, parsed);
        }

        return MINI.deserialize(parsed);
    }

    /**
     * Parses MiniMessage text without custom placeholders.
     */
    public static Component parse(@Nullable Player player, String raw) {
        return parse(player, raw, Collections.emptyMap());
    }

    /**
     * Parses text and returns a plain String (for scoreboard, signs, etc.).
     */
    public static String parsePlain(@Nullable Player player, String raw, Map<String, String> placeholders) {
        return PlainTextComponentSerializer.plainText().serialize(parse(player, raw, placeholders));
    }

    private static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    // ===========================
    // 📦 Static Placeholder Helper
    // ===========================

    public static class PlaceholderSet {
        private final Map<String, String> placeholders = new HashMap<>();

        public PlaceholderSet add(String key, String value) {
            placeholders.put(key, value);
            return this;
        }

        public Map<String, String> build() {
            return placeholders;
        }

        /**
         * Applies custom {placeholders} to a raw String.
         */
        public static String applyToStatic(String raw, Map<String, String> externalPlaceholders) {
            if (raw == null) return "";
            String result = raw;
            for (Map.Entry<String, String> entry : externalPlaceholders.entrySet()) {
                result = result.replace("{" + entry.getKey() + "}", entry.getValue());
            }
            return result;
        }
    }

    /**
     * Only applies %PlaceholderAPI% without MiniMessage formatting.
     */
    public static String parsePlaceholders(@Nullable Player player, String raw) {
        if (raw == null || raw.isEmpty()) return "";
        String parsed = raw;
        if (player != null && usingPapi) {
            parsed = PlaceholderAPI.setPlaceholders(player, parsed);
        }
        return parsed;
    }
}
