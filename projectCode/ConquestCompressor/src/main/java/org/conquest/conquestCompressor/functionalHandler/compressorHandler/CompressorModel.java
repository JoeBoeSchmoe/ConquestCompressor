package org.conquest.conquestCompressor.functionalHandler.compressorHandler;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.conquest.conquestCompressor.functionalHandler.ItemDataModel;

/**
 * 🔁 CompressorModel
 * Represents a compression recipe from gameAutoCompressor.yml.
 */
public class CompressorModel {

    private final String key;
    private boolean enabled;

    private Material inputMaterial;
    private int inputAmount;
    private ItemDataModel inputItemData;

    private Material outputMaterial;
    private int outputAmount;
    private ItemDataModel outputItemData;

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

    // For dynamic creation
    public CompressorModel(String key) {
        this.key = key;
        this.enabled = true;
        this.inputMaterial = Material.STONE;
        this.inputAmount = -1;
        this.inputItemData = new ItemDataModel();
        this.outputMaterial = Material.DIAMOND;
        this.outputAmount = -1;
        this.outputItemData = new ItemDataModel();
    }

    public String getKey() {
        return key;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Material getInputMaterial() {
        return inputMaterial;
    }

    public void setInputMaterial(Material inputMaterial) {
        this.inputMaterial = inputMaterial;
    }

    public int getInputAmount() {
        return inputAmount;
    }

    public void setInputAmount(int inputAmount) {
        this.inputAmount = inputAmount;
    }

    public ItemDataModel getInputItemData() {
        return inputItemData;
    }

    public void setInputItemData(ItemDataModel inputItemData) {
        this.inputItemData = inputItemData;
    }

    public Material getOutputMaterial() {
        return outputMaterial;
    }

    public void setOutputMaterial(Material outputMaterial) {
        this.outputMaterial = outputMaterial;
    }

    public int getOutputAmount() {
        return outputAmount;
    }

    public void setOutputAmount(int outputAmount) {
        this.outputAmount = outputAmount;
    }

    public ItemDataModel getOutputItemData() {
        return outputItemData;
    }

    public void setOutputItemData(ItemDataModel outputItemData) {
        this.outputItemData = outputItemData;
    }

    public boolean isReal() {
        return !(inputMaterial == Material.STONE &&
                outputMaterial == Material.DIAMOND &&
                inputAmount == -1);
    }

    /**
     * Builds a new output item stack with this recipe's metadata applied.
     */
    public ItemStack buildOutputItem() {
        ItemStack item = new ItemStack(this.outputMaterial, 1);
        if (this.outputItemData != null) {
            this.outputItemData.applyTo(item);
        }
        return item;
    }
}
