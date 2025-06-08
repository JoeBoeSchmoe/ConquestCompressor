package org.conquest.conquestCompressor.guiHandler.guiBuildingHandler.guiMenuManagers;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.conquest.conquestCompressor.functionalHandler.compressorHandler.CompressorModel;
import org.conquest.conquestCompressor.guiHandler.guiBuildingHandler.EditingSession;
import org.conquest.conquestCompressor.guiHandler.guiBuildingHandler.EditingSession.SessionMode;
import org.conquest.conquestCompressor.guiHandler.guiBuildingHandler.EditingSessionManager;
import org.conquest.conquestCompressor.guiHandler.guiBuildingHandler.EditorMenuManager;
import org.conquest.conquestCompressor.guiHandler.guiBuildingHandler.guiMenuModels.DuelMenuMeta;
import org.conquest.conquestCompressor.guiHandler.guiBuildingHandler.guiMenuModels.FillerItemModel;
import org.conquest.conquestCompressor.guiHandler.guiBuildingHandler.guiMenuModels.GUIFileEnums;
import org.conquest.conquestCompressor.guiHandler.guiBuildingHandler.guiUtilites.EditingMenuHolder;
import org.conquest.conquestCompressor.guiHandler.guiBuildingHandler.guiUtilites.ItemBuilder;

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

        EditingSession session = EditingSessionManager.getOrCreate(player,
                EditingSessionManager.hasSession(player)
                        ? EditingSessionManager.get(player).map(EditingSession::getMode).orElse(SessionMode.EDITING)
                        : SessionMode.EDITING
        );
        session.touch();

        int rows = meta.getRows();
        int size = rows * 9;
        Component title = MiniMessage.miniMessage().deserialize(meta.getTitleFormat());

        Inventory inv = Bukkit.createInventory(new EditingMenuHolder(MENU_TYPE), size, title);

        // 🧱 Fill background
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
            ItemBuilder.setPlaceholderTag(filler);
            for (int i = 0; i < size; i++) {
                inv.setItem(i, filler);
            }
        }

        // 🧩 Build layout
        List<Map<String, Object>> layoutItems = meta.getLayout();
        boolean isEditing = session.getMode() == SessionMode.EDITING;
        CompressorModel model = session.isEditingCompressor() ? session.getEditingCompressor() : null;
        boolean showCompressorIcons = isEditing && model != null && model.isReal();

        if (layoutItems != null) {
            for (Map<String, Object> layoutItem : layoutItems) {
                int slot = (int) layoutItem.getOrDefault("slot", -1);
                if (slot < 0 || slot >= size) continue;

                String action = (String) layoutItem.get("action");
                boolean isInput = "input".equalsIgnoreCase(action);
                boolean isOutput = "output".equalsIgnoreCase(action);

                // 🔄 Use compressor input/output if model is real
                if (showCompressorIcons) {
                    if (isInput && model.getInputMaterial() != null) {
                        ItemStack input = new ItemStack(model.getInputMaterial(), Math.max(1, model.getInputAmount()));
                        if (model.getInputItemData() != null) model.getInputItemData().applyTo(input);
                        ItemBuilder.setPlaceholderTag(input);
                        inv.setItem(slot, input);
                        continue;
                    }

                    if (isOutput && model.getOutputMaterial() != null) {
                        ItemStack output = new ItemStack(model.getOutputMaterial(), Math.max(1, model.getOutputAmount()));
                        if (model.getOutputItemData() != null) model.getOutputItemData().applyTo(output);
                        ItemBuilder.setPlaceholderTag(output);
                        inv.setItem(slot, output);
                        continue;
                    }
                }

                // 📦 Render layout-defined item (including buttons & edit icons)
                Map<String, Object> itemMap = new HashMap<>(layoutItem);
                if (isEditing && layoutItem.containsKey("edit-icon")) {
                    Object editIcon = layoutItem.get("edit-icon");
                    if (editIcon instanceof Map<?, ?> iconMap) {
                        for (Map.Entry<?, ?> entry : iconMap.entrySet()) {
                            if (entry.getKey() instanceof String key) {
                                itemMap.put(key, entry.getValue());
                            }
                        }
                    }
                }

                ItemStack placeholder = ItemBuilder.create(itemMap);
                ItemBuilder.setPlaceholderTag(placeholder);
                inv.setItem(slot, placeholder);
            }
        }

        player.openInventory(inv);
        session.markOpen();

        if (meta.getEffects().containsKey("open")) {
            meta.getEffects().get("open").play(player);
        }

    }
}
