package org.conquest.conquestCompressor.guiHandler;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.conquest.conquestCompressor.guiHandler.guiBuildingHandler.EditingSessionManager;
import org.conquest.conquestCompressor.guiHandler.guiBuildingHandler.EditorMenuManager;
import org.conquest.conquestCompressor.guiHandler.guiBuildingHandler.guiMenuModels.DuelMenuMeta;
import org.conquest.conquestCompressor.guiHandler.guiBuildingHandler.guiMenuModels.EffectModel;
import org.conquest.conquestCompressor.guiHandler.guiBuildingHandler.guiMenuModels.FillerItemModel;
import org.conquest.conquestCompressor.guiHandler.guiBuildingHandler.guiMenuModels.GUIFileEnums;
import org.conquest.conquestCompressor.guiHandler.guiBuildingHandler.guiUtilites.EffectModelParser;
import org.conquest.conquestCompressor.guiHandler.guiBuildingHandler.guiUtilites.FillerItemParser;

import java.util.*;

/**
 * 📂 GUIOpener
 * Handles opening compressor and recipe edit GUIs, including fallback meta build.
 */
public class GUIOpener {

    public static void open(Player player, GUIFileEnums type) {
        ensureMetaBuilt(type);

        EditingSessionManager.getOrCreate(player).touch();

        // Delegated GUI open call — you'll plug in actual menu managers later
        switch (type) {
            case COMPRESSOR -> {
                // TODO: CompressorMenuManager.open(player);
            }
            case RECIPES -> {
                // TODO: RecipeEditorMenuManager.open(player);
            }
        }
    }

    private static void ensureMetaBuilt(GUIFileEnums type) {
        if (EditorMenuManager.hasMeta(type)) return;

        FileConfiguration config = type.getConfig();

        String title = config.getString("menu.title", "<gray>Menu");
        boolean usesFiller = config.getBoolean("menu.filler", true);
        int rows = config.getInt("menu.rows-per-page", 1); // Default to 1 if missing

        List<Map<String, Object>> layout = new ArrayList<>();
        if (config.isList("menu.layout")) {
            for (Map<?, ?> raw : config.getMapList("menu.layout")) {
                Map<String, Object> mapped = new HashMap<>();
                raw.forEach((k, v) -> {
                    if (k instanceof String key) {
                        mapped.put(key, v);
                    }
                });
                layout.add(mapped);
            }
        }

        FillerItemModel fillerItem = null;
        ConfigurationSection fillerSection = config.getConfigurationSection("menu.filler-item");
        if (fillerSection != null) {
            fillerItem = FillerItemParser.parse(fillerSection);
        }

        Map<String, EffectModel> effects = new HashMap<>();
        ConfigurationSection soundsSection = config.getConfigurationSection("menu.sounds");
        if (soundsSection != null) {
            for (String key : soundsSection.getKeys(false)) {
                ConfigurationSection sfx = soundsSection.getConfigurationSection(key);
                if (sfx != null) {
                    EffectModel model = EffectModelParser.parseEffect(sfx);
                    if (model != null) {
                        effects.put(key.toLowerCase(Locale.ROOT), model);
                    }
                }
            }
        }

        Map<String, Object> emptyItem = null;
        ConfigurationSection emptySection = config.getConfigurationSection("menu.empty-item");
        if (emptySection != null) {
            emptyItem = DuelMenuMeta.extractSection(emptySection);
        }

        Map<String, Object> playerItem = null;
        ConfigurationSection itemSection = config.getConfigurationSection("item");
        if (itemSection != null) {
            playerItem = DuelMenuMeta.extractSection(itemSection);
        }

        DuelMenuMeta meta = new DuelMenuMeta(type, title, rows, usesFiller, layout, fillerItem, effects, emptyItem, playerItem);
        EditorMenuManager.putMeta(type, meta);
    }
}
