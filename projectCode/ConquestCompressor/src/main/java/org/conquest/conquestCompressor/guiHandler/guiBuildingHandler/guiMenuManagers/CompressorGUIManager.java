package org.conquest.conquestCompressor.guiHandler.guiBuildingHandler.guiMenuManagers;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.conquest.conquestCompressor.guiHandler.guiBuildingHandler.EditingSession;
import org.conquest.conquestCompressor.guiHandler.guiBuildingHandler.EditingSessionManager;
import org.conquest.conquestCompressor.guiHandler.guiBuildingHandler.EditorMenuManager;
import org.conquest.conquestCompressor.guiHandler.guiBuildingHandler.guiMenuModels.DuelMenuMeta;
import org.conquest.conquestCompressor.guiHandler.guiBuildingHandler.guiMenuModels.FillerItemModel;
import org.conquest.conquestCompressor.guiHandler.guiBuildingHandler.guiMenuModels.GUIFileEnums;
import org.conquest.conquestCompressor.guiHandler.guiBuildingHandler.guiUtilites.EditingMenuHolder;
import org.conquest.conquestCompressor.guiHandler.guiBuildingHandler.guiUtilites.ItemBuilder;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.*;

/**
 * ⚙️ CompressorGUIManager
 * Opens and builds the auto-compressor editor GUI for players.
 */
public class CompressorGUIManager {

    private static final GUIFileEnums MENU_TYPE = GUIFileEnums.COMPRESSOR;

    public static void open(Player player) {
        DuelMenuMeta meta = EditorMenuManager.getMeta(MENU_TYPE);
        if (meta == null) {
            return;
        }

        EditingSession session = EditingSessionManager.getOrCreate(player);
        session.touch();

        int rows = meta.getRows();
        int size = rows * 9;
        Component title = MiniMessage.miniMessage().deserialize(meta.getTitleFormat());

        Inventory inv = Bukkit.createInventory(new EditingMenuHolder(MENU_TYPE), size, title);

        // Fill with filler if enabled
        if (meta.isUsesFiller() && meta.getFillerItem() != null) {
            FillerItemModel fillerItem = meta.getFillerItem();

            ItemStack filler = ItemBuilder.create(Map.of(
                    "material", fillerItem.getMaterial(),
                    "name", fillerItem.getName(),
                    "amount", fillerItem.getAmount(),
                    "lore", fillerItem.getLore(),
                    "enchanted", fillerItem.isEnchanted(),
                    "customData", fillerItem.getCustomData()
            ));

            for (int i = 0; i < size; i++) {
                inv.setItem(i, filler);
            }
        }

        // Build and place layout items
        List<Map<String, Object>> layoutItems = meta.getLayout();
        if (layoutItems != null) {
            for (Map<String, Object> layoutItem : layoutItems) {
                int slot = (int) layoutItem.getOrDefault("slot", -1);
                if (slot >= 0 && slot < size) {
                    inv.setItem(slot, ItemBuilder.create(layoutItem));
                }
            }
        }

        player.openInventory(inv);
        session.markOpen();

        if (meta.getEffects().containsKey("open")) {
            meta.getEffects().get("open").play(player);
        }
    }

    public static void refreshAllOpenMenus() {
        for (UUID uuid : EditingSessionManager.getAllSessions().keySet()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                Inventory inv = player.getOpenInventory().getTopInventory();
                if (inv.getHolder() instanceof EditingMenuHolder holder &&
                        holder.getMenuType() == MENU_TYPE) {
                    open(player);
                }
            }
        }
    }
}
