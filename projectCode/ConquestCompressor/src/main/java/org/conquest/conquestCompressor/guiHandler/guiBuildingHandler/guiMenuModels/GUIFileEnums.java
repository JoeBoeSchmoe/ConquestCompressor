package org.conquest.conquestCompressor.guiHandler.guiBuildingHandler.guiMenuModels;

import org.bukkit.configuration.file.FileConfiguration;
import org.conquest.conquestCompressor.configurationHandler.configurationFiles.CompressorGUIFile;
import org.conquest.conquestCompressor.configurationHandler.configurationFiles.RecipesGUIFile;

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

    public abstract FileConfiguration getConfig();
}
