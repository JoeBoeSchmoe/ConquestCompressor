package org.conquest.conquestCompressor.guiHandler.guiBuildingHandler;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.conquest.conquestCompressor.configurationHandler.configurationFiles.GameAutocompressorFile;
import org.conquest.conquestCompressor.functionalHandler.ItemDataModel;
import org.conquest.conquestCompressor.functionalHandler.compressorHandler.CompressorManager;
import org.conquest.conquestCompressor.functionalHandler.compressorHandler.CompressorModel;
import org.conquest.conquestCompressor.guiHandler.guiBuildingHandler.guiMenuModels.DuelMenuMeta;
import org.conquest.conquestCompressor.guiHandler.guiBuildingHandler.guiMenuModels.GUIFileEnums;
import org.conquest.conquestCompressor.guiHandler.guiBuildingHandler.guiUtilites.ClickActionManager;
import org.conquest.conquestCompressor.guiHandler.guiBuildingHandler.guiUtilites.EditingMenuHolder;
import org.conquest.conquestCompressor.guiHandler.guiBuildingHandler.guiUtilites.ItemBuilder;
import org.conquest.conquestCompressor.responseHandler.MessageResponseManager;
import org.conquest.conquestCompressor.responseHandler.messageModels.AdminMessageModels;

import java.util.*;

public class EditGUIListener implements Listener {

    private static final Map<UUID, Long> lastGuiClick = new HashMap<>();

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        HumanEntity entity = event.getWhoClicked();
        if (!(entity instanceof Player player)) return;

        Inventory top = event.getView().getTopInventory();
        Inventory bottom = event.getView().getBottomInventory();
        if (!(top.getHolder() instanceof EditingMenuHolder holder)) return;
        if (holder.getMenuType() != GUIFileEnums.COMPRESSOR) return;

        DuelMenuMeta meta = EditorMenuManager.getMeta(holder.getMenuType());
        if (meta == null) return;

        int slot = event.getSlot();

        if (event.getClickedInventory() == bottom) return;

        String action = ClickActionManager.getActionForSlot(slot, meta);
        if (action == null) {
            event.setCancelled(true);
            return;
        }

        switch (action.toLowerCase(Locale.ROOT)) {
            case "input", "output" -> {
                ItemStack cursor = event.getCursor();
                ItemStack current = top.getItem(slot);

                if (ItemBuilder.hasPlaceholderTag(current)) {
                    if (cursor.getType().isAir()) {
                        MessageResponseManager.send(player, AdminMessageModels.PLACEHOLDER_REQUIRES_ITEM);
                        event.setCancelled(true);
                        return;
                    }
                    top.setItem(slot, null);
                }

                event.setCancelled(false);
            }

            case "cancel" -> {
                event.setCancelled(true);
                ClickActionManager.handle(player, "cancel", holder.getMenuType());
            }

            case "confirm" -> {
                event.setCancelled(true);

                int inputSlot = getSlotByAction(meta, "input");
                int outputSlot = getSlotByAction(meta, "output");

                ItemStack input = top.getItem(inputSlot);
                ItemStack output = top.getItem(outputSlot);

                if (!isRealItem(input) || !isRealItem(output)) {
                    MessageResponseManager.send(player, AdminMessageModels.GUI_INVALID_ITEMS);
                    return;
                }

                var session = EditingSessionManager.get(player).orElse(null);
                if (session == null || session.getEditingCompressor() == null) return;

                CompressorModel model = session.getEditingCompressor();

                model.setInputMaterial(input.getType());
                model.setInputAmount(input.getAmount());
                model.setInputItemData(ItemDataModel.fromItem(input));

                model.setOutputMaterial(output.getType());
                model.setOutputAmount(output.getAmount());
                model.setOutputItemData(ItemDataModel.fromItem(output));

                GameAutocompressorFile.saveCompressorModel(model);
                CompressorManager.register(model);

                ClickActionManager.handle(player, "confirm", holder.getMenuType());
            }

            default -> event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof EditingMenuHolder holder)) return;
        if (holder.getMenuType() != GUIFileEnums.COMPRESSOR) return;

        DuelMenuMeta meta = EditorMenuManager.getMeta(holder.getMenuType());
        int inputSlot = getSlotByAction(meta, "input");
        int outputSlot = getSlotByAction(meta, "output");

        for (int raw : event.getRawSlots()) {
            if (raw < top.getSize() && raw != inputSlot && raw != outputSlot) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof EditingMenuHolder holder)) return;

        if (holder.getMenuType() == GUIFileEnums.COMPRESSOR) {
            Bukkit.getScheduler().runTaskLater(
                    org.conquest.conquestCompressor.ConquestCompressor.getInstance(),
                    () -> {
                        if (!(player.getOpenInventory().getTopInventory().getHolder() instanceof EditingMenuHolder)) {
                            EditingSessionManager.get(player).ifPresent(EditingSession::markClosed);
                        }
                    },
                    1L
            );
        }
    }

    private boolean isRealItem(ItemStack item) {
        return item != null && !item.getType().isAir() && !ItemBuilder.hasPlaceholderTag(item);
    }

    private int getSlotByAction(DuelMenuMeta meta, String actionName) {
        for (Map<String, Object> layoutItem : meta.getLayout()) {
            String action = (String) layoutItem.get("action");
            if (action != null && action.equalsIgnoreCase(actionName)) {
                return (int) layoutItem.getOrDefault("slot", -1);
            }
        }
        return -1;
    }
}
