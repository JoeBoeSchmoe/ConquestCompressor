package org.conquest.conquestCompressor.guiHandler.guiBuildingHandler.guiUtilites;

import org.bukkit.entity.Player;
import org.conquest.conquestCompressor.guiHandler.guiBuildingHandler.EditorMenuManager;
import org.conquest.conquestCompressor.guiHandler.guiBuildingHandler.guiMenuModels.EffectModel;
import org.conquest.conquestCompressor.guiHandler.guiBuildingHandler.guiMenuModels.DuelMenuMeta;
import org.conquest.conquestCompressor.guiHandler.guiBuildingHandler.guiMenuModels.GUIFileEnums;

import java.util.Map;

/**
 * 🖱️ ClickActionManager
 *
 * Handles click actions from buttons like "confirm" and "cancel" in compressor/recipe menus.
 */
public class ClickActionManager {

    public static void handle(Player player, String action, GUIFileEnums type) {
        DuelMenuMeta meta = EditorMenuManager.getMeta(type);
        if (meta == null) return;

        // 🔊 Play associated effect if configured
        EffectModel effect = meta.getEffects().get(action);
        if (effect != null) {
            effect.play(player);
        }

        // 🧭 Execute logic for known actions
        switch (action) {
            case "confirm" -> {
                // TODO: Trigger save logic here
                player.sendMessage("✅ Recipe confirmed.");
                player.closeInventory();
            }
            case "cancel" -> {
                player.sendMessage("❌ Recipe editing cancelled.");
                player.closeInventory();
            }
            default -> {
                player.sendMessage("⚠️ Unknown action: " + action);
            }
        }
    }

    public static String getActionForSlot(int slot, DuelMenuMeta meta) {
        if (meta == null || meta.getLayout() == null) return null;

        for (Map<String, Object> entry : meta.getLayout()) {
            Object rawSlot = entry.get("slot");
            Object rawAction = entry.get("action");

            if (rawSlot instanceof Integer && rawAction instanceof String) {
                if ((int) rawSlot == slot) {
                    return ((String) rawAction).toLowerCase();
                }
            }
        }

        return null;
    }
}
