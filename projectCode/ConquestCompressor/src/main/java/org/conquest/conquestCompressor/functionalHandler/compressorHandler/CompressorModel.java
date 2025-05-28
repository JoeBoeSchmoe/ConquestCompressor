package org.conquest.conquestCompressor.functionalHandler.compressorHandler;

import org.bukkit.Material;
import org.conquest.conquestCompressor.functionalHandler.ItemDataModel;

/**
 * 🔁 CompressorModel
 * Represents a compression recipe from gameAutoCompressor.yml.
 */
public class CompressorModel {

    private final String key;
    private final boolean enabled;

    private final Material inputMaterial;
    private final int inputAmount;
    private final ItemDataModel inputItemData;

    private final Material outputMaterial;
    private final int outputAmount;
    private final ItemDataModel outputItemData;

    public CompressorModel(String key,
                           boolean enabled,
                           Material inputMaterial,
                           int inputAmount,
                           ItemDataModel inputItemData,
                           Material outputMaterial,
                           int outputAmount,
                           ItemDataModel outputItemData) {
        this.key = key;
        this.enabled = enabled;
        this.inputMaterial = inputMaterial;
        this.inputAmount = inputAmount;
        this.inputItemData = inputItemData;
        this.outputMaterial = outputMaterial;
        this.outputAmount = outputAmount;
        this.outputItemData = outputItemData;
    }

    public String getKey() {
        return key;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Material getInputMaterial() {
        return inputMaterial;
    }

    public int getInputAmount() {
        return inputAmount;
    }

    public ItemDataModel getInputItemData() {
        return inputItemData;
    }

    public Material getOutputMaterial() {
        return outputMaterial;
    }

    public int getOutputAmount() {
        return outputAmount;
    }

    public ItemDataModel getOutputItemData() {
        return outputItemData;
    }
}
