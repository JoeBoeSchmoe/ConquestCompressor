package org.conquest.conquestCompressor.guiHandler.guiBuildingHandler.guiMenuModels;

import org.bukkit.configuration.ConfigurationSection;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 🧠 DuelMenuMeta
 * Holds parsed config metadata for GUI rendering and behavior.
 */
public class DuelMenuMeta {

    private final GUIFileEnums menuType;
    private final String titleFormat;
    private final int rows;
    private final boolean usesFiller;
    private final List<Map<String, Object>> layout;
    private final FillerItemModel fillerItem;
    private final Map<String, EffectModel> effects;
    private final Map<String, Object> emptyItem; // 🪦 Optional empty state item
    private final Map<String, Object> item;      // 👤 Optional player item config


    public DuelMenuMeta(GUIFileEnums menuType, String titleFormat, int rows,
                        boolean usesFiller, List<Map<String, Object>> layout,
                        FillerItemModel fillerItem, Map<String, EffectModel> effects,
                        Map<String, Object> emptyItem, Map<String, Object> item) {
        this.menuType = menuType;
        this.titleFormat = titleFormat;
        this.rows = rows;
        this.usesFiller = usesFiller;
        this.layout = layout;
        this.fillerItem = fillerItem;
        this.effects = effects;
        this.emptyItem = emptyItem;
        this.item = item;
    }

    public GUIFileEnums getMenuType() {
        return menuType;
    }

    public String getTitleFormat() {
        return titleFormat;
    }

    public int getRows() {
        return rows;
    }

    public boolean isUsesFiller() {
        return usesFiller;
    }

    public List<Map<String, Object>> getLayout() {
        return layout;
    }

    public FillerItemModel getFillerItem() {
        return fillerItem;
    }

    public Map<String, EffectModel> getEffects() {
        return effects;
    }

    public Map<String, Object> getEmptyItem() {
        return emptyItem;
    }

    public Map<String, Object> getItem() {
        return item;
    }

    public static Map<String, Object> extractSection(ConfigurationSection section) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (String key : section.getKeys(false)) {
            Object value = section.get(key);

            if (value instanceof ConfigurationSection nested) {
                map.put(key, extractSection(nested)); // 🧠 Recursively resolve!
            } else {
                map.put(key, value);
            }
        }
        return map;
    }

}
