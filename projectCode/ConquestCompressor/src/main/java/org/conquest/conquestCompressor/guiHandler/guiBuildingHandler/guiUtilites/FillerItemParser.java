package org.conquest.conquestCompressor.guiHandler.guiBuildingHandler.guiUtilites;

import org.bukkit.configuration.ConfigurationSection;
import org.conquest.conquestCompressor.guiHandler.guiBuildingHandler.guiMenuModels.EffectModel;
import org.conquest.conquestCompressor.guiHandler.guiBuildingHandler.guiMenuModels.FillerItemModel;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class FillerItemParser {

    public static FillerItemModel parse(ConfigurationSection section) {
        if (section == null) return null;

        String material = section.getString("material", "GRAY_STAINED_GLASS_PANE");
        String name = section.getString("name", "<gray>");
        int amount = section.getInt("amount", 1);
        List<String> lore = section.getStringList("lore");
        boolean enchanted = section.getBoolean("enchanted", false);

        // Handle optional customData
        Map<String, Object> customData = Collections.emptyMap();
        ConfigurationSection dataSection = section.getConfigurationSection("customData");
        if (dataSection != null) {
            customData = dataSection.getValues(true);
        }

        // Handle optional visual/audio effects
        EffectModel effect = EffectModelParser.parseEffect(section);

        return new FillerItemModel(material, name, amount, lore, enchanted, customData, effect);
    }
}
