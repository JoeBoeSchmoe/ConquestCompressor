package org.conquest.conquestCompressor.responseHandler;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.conquest.conquestCompressor.ConquestCompressor;
import org.conquest.conquestCompressor.configurationHandler.configurationFiles.AdminMessagesFile;
import org.conquest.conquestCompressor.configurationHandler.configurationFiles.UserMessagesFile;
import org.conquest.conquestCompressor.configurationHandler.integrationFiles.PlaceHolderAPIManager;
import org.conquest.conquestCompressor.responseHandler.effectHandler.*;

import org.conquest.conquestCompressor.responseHandler.messageModels.AdminMessageModels;
import org.conquest.conquestCompressor.responseHandler.messageModels.UserMessageModels;

import java.util.*;
import java.util.logging.Logger;

/**
 * 📬 MessageResponseManager
 * Central handler for structured messages, visual/auditory feedback, and YAML-defined interactions.
 */
public class MessageResponseManager {

    private static final Logger log = ConquestCompressor.getInstance().getLogger();
    private static String cachedPrefix = null;

    // ------------------ Generic Message Senders ------------------

    public static void send(Player player, AdminMessageModels model) {
        send(player, model, Collections.emptyMap());
    }

    public static void send(Player player, AdminMessageModels model, Map<String, String> placeholders) {
        ConfigurationSection section = getMessageSection(model);
        if (player == null || section == null) return;
        sendFormatted(player, section, placeholders);
        playEffects(player, section, placeholders);
    }

    public static void send(Player player, UserMessageModels model) {
        send(player, model, Collections.emptyMap());
    }

    public static void send(Player player, UserMessageModels model, Map<String, String> placeholders) {
        ConfigurationSection section = getMessageSection(model);
        if (player == null || section == null) return;
        sendFormatted(player, section, placeholders);
        playEffects(player, section, placeholders);
    }

    public static void send(CommandSender sender, AdminMessageModels model, Map<String, String> placeholders) {
        ConfigurationSection section = getMessageSection(model);
        if (sender == null || section == null) return;
        sendFormatted(sender, section, placeholders);
        playEffects(sender, section, placeholders);
    }

    public static void send(CommandSender sender, UserMessageModels model, Map<String, String> placeholders) {
        ConfigurationSection section = getMessageSection(model);
        if (sender == null || section == null) return;
        sendFormatted(sender, section, placeholders);
        playEffects(sender, section, placeholders);
    }

    // ------------------ Help Page Sender ------------------

    public static void sendHelpPage(Player player, String key, int page) {
        ConfigurationSection section = AdminMessagesFile.getSection(key);
        if (section == null) {
            log.warning("⚠️ Help section missing: " + key);
            return;
        }

        List<Map<?, ?>> allComponents = section.getMapList("components");
        List<Map<?, ?>> visibleComponents = new ArrayList<>();

        for (Map<?, ?> raw : allComponents) {
            String permission = (String) raw.get("permission");
            if (permission == null || permission.isBlank() || player.hasPermission(permission)) {
                visibleComponents.add(raw);
            }
        }

        int perPage = 7;
        int maxPage = Math.max(1, (int) Math.ceil(visibleComponents.size() / (double) perPage));
        page = Math.max(1, Math.min(page, maxPage));

        int start = (page - 1) * perPage;
        int end = Math.min(start + perPage, visibleComponents.size());
        List<Map<?, ?>> pageComponents = visibleComponents.subList(start, end);

        // 📜 Header
        if (section.isList("text")) {
            List<String> lines = section.getStringList("text");
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                if (i == lines.size() - 2) {
                    line += " <gray>(Page " + page + "/" + maxPage + ")";
                }
                player.sendMessage(PlaceHolderAPIManager.parse(player, line));
            }
        }

        // 🎛️ Components
        for (Map<?, ?> map : pageComponents) {
            Component comp = ComponentSerializerManager.deserializeComponent(map, player, Map.of(
                    "page", String.valueOf(page),
                    "max", String.valueOf(maxPage)
            ));
            player.sendMessage(comp);
        }

        player.sendMessage(Component.empty());

        // ✨ Effects
        playEffects(player, section, Map.of("page", String.valueOf(page), "max", String.valueOf(maxPage)));
    }

    // ------------------ Internal Message Handlers ------------------

    private static void sendFormatted(CommandSender sender, ConfigurationSection section, Map<String, String> placeholders) {
        if (section == null) return;

        List<String> lines = section.getStringList("text");
        String hover = section.getString("hover", "");
        String click = section.getString("click", "");
        String clickTypeRaw = section.getString("clickType", "RUN_COMMAND");
        boolean showPrefix = section.getBoolean("prefix", true);
        Component prefixComponent = showPrefix ? getPrefixComponent() : Component.empty();

        ClickEvent.Action clickAction = null;
        try {
            clickAction = ClickEvent.Action.valueOf(clickTypeRaw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {}

        for (String line : lines) {
            if (line == null || line.trim().isEmpty()) continue;
            Component base = PlaceHolderAPIManager.parse(sender instanceof Player ? (Player) sender : null, line, placeholders);

            if (!hover.isBlank()) {
                base = base.hoverEvent(PlaceHolderAPIManager.parse(sender instanceof Player ? (Player) sender : null, hover, placeholders));
            }
            if (!click.isBlank() && clickAction != null) {
                String parsedClick = PlaceHolderAPIManager.PlaceholderSet.applyToStatic(click, placeholders);
                base = base.clickEvent(ClickEvent.clickEvent(clickAction, parsedClick));
            }

            base = prefixComponent.append(base);
            sender.sendMessage(base);
        }
    }

    private static void playEffects(CommandSender sender, ConfigurationSection section, Map<String, String> placeholders) {
        if (!(sender instanceof Player player)) return;

        SoundResponseManager.play(player, section);
        ParticleResponseManager.play(player, section);
        BossBarResponseManager.send(player, section, placeholders);
        ActionBarResponseManager.send(player, section, placeholders);
        EffectResponseManager.send(player, section, placeholders);
        TitleResponseManager.send(player, section, placeholders); // 🆕 Now playing titles!
    }

    // ------------------ Config Section Access ------------------

    private static ConfigurationSection getMessageSection(AdminMessageModels model) {
        ConfigurationSection section = AdminMessagesFile.getSection(model.getPath());
        if (section == null) log.warning("⚠️ Missing admin message section: " + model.getPath());
        return section;
    }

    private static ConfigurationSection getMessageSection(UserMessageModels model) {
        ConfigurationSection section = UserMessagesFile.getSection(model.getPath());
        if (section == null) log.warning("⚠️ Missing user message section: " + model.getPath());
        return section;
    }

    // ------------------ Prefix Utilities ------------------

    private static String getPrefix() {
        if (cachedPrefix == null) {
            cachedPrefix = ConquestCompressor.getInstance()
                    .getConfigurationManager()
                    .getConfig()
                    .getString("chat-prefix", "<gray>[ConquestSpawners]</gray> ");
        }
        return cachedPrefix;
    }

    private static Component getPrefixComponent() {
        return PlaceHolderAPIManager.parse(null, getPrefix());
    }

    private static String getPrefixPlain() {
        return net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                .serialize(getPrefixComponent());
    }

    public static void resetPrefixCache() {
        cachedPrefix = null;
    }
}
