package org.conquest.conquestCompressor.guiHandler.guiBuildingHandler.guiUtilites;

import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.conquest.conquestCompressor.guiHandler.guiBuildingHandler.guiMenuModels.GUIFileEnums;
import org.jetbrains.annotations.NotNull;

/**
 * 🧩 EditingMenuHolder
 * Associates an inventory with a GUI menu type, enabling identification during click events.
 */
public class EditingMenuHolder implements InventoryHolder {

    private final GUIFileEnums type;

    public EditingMenuHolder(GUIFileEnums type) {
        this.type = type;
    }

    /**
     * Gets the associated menu enum (RECIPES, COMPRESSOR, etc.)
     */
    public GUIFileEnums getMenuType() {
        return type;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return Bukkit.createInventory(null, 9); // Dummy inventory to satisfy Bukkit
    }
}
