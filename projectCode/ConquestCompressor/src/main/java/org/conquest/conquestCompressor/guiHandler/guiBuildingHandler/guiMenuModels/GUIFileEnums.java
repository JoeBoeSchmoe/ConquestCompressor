package org.conquest.conquestCompressor.guiHandler.guiBuildingHandler.guiMenuModels;

import org.bukkit.configuration.file.FileConfiguration;
import org.conquest.conquestCompressor.configurationHandler.configurationFiles.CompressorGUIFile;
import org.conquest.conquestCompressor.configurationHandler.configurationFiles.RecipesGUIFile;

/**
 * 📁 GUIFileEnums
 *
 * Enum to link each GUI type to its corresponding configuration file source.
 */
public enum GUIFileEnums {

    RECIPES {
        @Override
        public FileConfiguration getConfig() {
            return RecipesGUIFile.getConfig();
        }
    },

    COMPRESSOR {
        @Override
        public FileConfiguration getConfig() {
            return CompressorGUIFile.getConfig();
        }
    };

    /**
     * Returns the Bukkit config object associated with this menu type.
     *
     * @return FileConfiguration instance tied to the enum
     */
    public abstract FileConfiguration getConfig();
}
