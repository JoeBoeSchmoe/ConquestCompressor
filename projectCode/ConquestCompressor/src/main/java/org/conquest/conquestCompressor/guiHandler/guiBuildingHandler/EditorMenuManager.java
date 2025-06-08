package org.conquest.conquestCompressor.guiHandler.guiBuildingHandler;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.conquest.conquestCompressor.configurationHandler.configurationFiles.CompressorGUIFile;
import org.conquest.conquestCompressor.guiHandler.guiBuildingHandler.guiMenuModels.DuelMenuMeta;
import org.conquest.conquestCompressor.guiHandler.guiBuildingHandler.guiMenuModels.EffectModel;
import org.conquest.conquestCompressor.guiHandler.guiBuildingHandler.guiMenuModels.FillerItemModel;
import org.conquest.conquestCompressor.guiHandler.guiBuildingHandler.guiMenuModels.GUIFileEnums;
import org.conquest.conquestCompressor.guiHandler.guiBuildingHandler.guiUtilites.EffectModelParser;
import org.conquest.conquestCompressor.guiHandler.guiBuildingHandler.guiUtilites.FillerItemParser;

import java.util.*;

/**
 * 🧰 EditorMenuManager
 * Parses and manages metadata for editor GUIs like compressor and recipe menus.
 */
public class EditorMenuManager {

    private static final Map<GUIFileEnums, DuelMenuMeta> loadedMeta = new EnumMap<>(GUIFileEnums.class);

    public static void load() {
        loadedMeta.clear();
        for (GUIFileEnums menuType : GUIFileEnums.values()) {
            DuelMenuMeta meta = parse(menuType);
            loadedMeta.put(menuType, meta);
        }
    }

    private static DuelMenuMeta parse(GUIFileEnums menuType) {
        FileConfiguration config = menuType.getConfig();

        String title = config.getString("menu.title", "<gray>Menu");
        boolean usesFiller = config.getBoolean("menu.filler", true);
        int rows = Math.min(6, Math.max(1, config.getInt("menu.rows-per-page", 1)));

        List<Map<String, Object>> layout = new ArrayList<>();
        if (config.isList("menu.layout")) {
            for (Map<?, ?> raw : config.getMapList("menu.layout")) {
                Map<String, Object> mapped = new HashMap<>();
                raw.forEach((k, v) -> {
                    if (k instanceof String key) mapped.put(key, v);
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
        ConfigurationSection soundSection = config.getConfigurationSection("menu.sounds");
        if (soundSection != null) {
            for (String key : soundSection.getKeys(false)) {
                ConfigurationSection sfx = soundSection.getConfigurationSection(key);
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

        Map<String, Object> item = null;
        ConfigurationSection itemSection = config.getConfigurationSection("item");
        if (itemSection != null) {
            item = DuelMenuMeta.extractSection(itemSection);
        }

        return new DuelMenuMeta(menuType, title, rows, usesFiller, layout, fillerItem, effects, emptyItem, item);
    }

    public static DuelMenuMeta getMeta(GUIFileEnums type) {
        return loadedMeta.get(type);
    }

    public static boolean hasMeta(GUIFileEnums type) {
        return loadedMeta.containsKey(type);
    }

    public static void reload() {
        CompressorGUIFile.load();
        load();
    }

    public static Set<GUIFileEnums> getLoadedMenus() {
        return Collections.unmodifiableSet(loadedMeta.keySet());
    }

    public static void clear() {
        loadedMeta.clear();
    }

    public static void putMeta(GUIFileEnums type, DuelMenuMeta meta) {
        loadedMeta.put(type, meta);
    }
}
