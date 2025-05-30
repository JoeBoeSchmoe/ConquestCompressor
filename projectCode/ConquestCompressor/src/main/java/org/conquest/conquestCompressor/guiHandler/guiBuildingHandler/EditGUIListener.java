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
import org.conquest.conquestCompressor.configurationHandler.configurationFiles.CompressorGUIFile;
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

                if (isPlaceholder(current, meta, slot)) {
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

                if (!isRealItem(input, meta, inputSlot) || !isRealItem(output, meta, outputSlot)) {
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

                // ✅ Save to config
                GameAutocompressorFile.saveCompressorModel(model);

                // ✅ Register this updated model directly without reloading all
                CompressorManager.register(model);

                // 🎉 Optional: confirm visually/sound
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

    private boolean isRealItem(ItemStack item, DuelMenuMeta meta, int slot) {
        return item != null && !item.getType().isAir() && !isPlaceholder(item, meta, slot);
    }

    private boolean isPlaceholder(ItemStack item, DuelMenuMeta meta, int slot) {
        if (item == null || item.getType().isAir()) return false;

        if (meta.getFillerItem() != null) {
            ItemStack filler = ItemBuilder.create(Map.of(
                    "material", meta.getFillerItem().getMaterial(),
                    "name", meta.getFillerItem().getName(),
                    "amount", meta.getFillerItem().getAmount(),
                    "lore", meta.getFillerItem().getLore(),
                    "enchanted", meta.getFillerItem().isEnchanted(),
                    "customData", meta.getFillerItem().getCustomData()
            ));
            if (itemsEqual(item, filler)) return true;
        }

        for (Map<String, Object> layoutItem : meta.getLayout()) {
            if ((int) layoutItem.getOrDefault("slot", -1) == slot) {
                ItemStack layoutPlaceholder = ItemBuilder.create(layoutItem);
                if (itemsEqual(item, layoutPlaceholder)) return true;
            }
        }

        return false;
    }

    private boolean itemsEqual(ItemStack a, ItemStack b) {
        if (a == null || b == null) return false;
        if (!a.getType().equals(b.getType())) return false;
        if (a.hasItemMeta() != b.hasItemMeta()) return false;

        ItemMeta metaA = a.getItemMeta();
        ItemMeta metaB = b.getItemMeta();

        if (metaA == null || metaB == null) return false;

        Component nameA = metaA.displayName();
        Component nameB = metaB.displayName();
        if (!Objects.equals(nameA, nameB)) return false;

        List<Component> loreA = metaA.lore();
        List<Component> loreB = metaB.lore();
        if (!Objects.equals(loreA, loreB)) return false;

        if (metaA.hasCustomModelData() != metaB.hasCustomModelData()) return false;
        return !metaA.hasCustomModelData() || metaA.getCustomModelData() == metaB.getCustomModelData();
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
