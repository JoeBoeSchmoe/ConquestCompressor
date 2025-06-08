package org.conquest.conquestCompressor.guiHandler.guiBuildingHandler.guiUtilites;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

/**
 * 🎨 ItemBuilder
 * Constructs ItemStacks from YAML config maps for use in GUI layouts.
 */
public class ItemBuilder {

    private static final MiniMessage mini = MiniMessage.miniMessage();

    public static ItemStack create(Map<String, Object> data) {
        String materialName = String.valueOf(data.getOrDefault("material", "BARRIER"));
        Material material = safeMatchMaterial(materialName);

        int amount = 1;
        if (data.containsKey("amount")) {
            Object amt = data.get("amount");
            if (amt instanceof Number num) {
                amount = Math.max(1, Math.min(64, num.intValue()));
            }
        }

        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        if (data.containsKey("name")) {
            meta.displayName(mini.deserialize(String.valueOf(data.get("name"))));
        }

        if (data.containsKey("lore")) {
            List<String> rawLore = safeStringList(data.get("lore"));
            List<Component> parsedLore = rawLore.stream()
                    .map(mini::deserialize)
                    .map(line -> line.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE))
                    .toList();
            meta.lore(parsedLore);
        }

        if (Boolean.TRUE.equals(data.get("enchanted"))) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        }

        if (data.containsKey("customData") && data.get("customData") instanceof Map<?, ?> rawMap) {
            Map<String, Object> customData = convertToStringObjectMap(rawMap);
            applyCustomData(meta, customData);
        }

        addHideAttributes(meta);
        item.setItemMeta(meta);
        return item;
    }

    private static void applyCustomData(ItemMeta meta, Map<String, Object> customData) {
        if (meta == null || customData == null) return;

        if (customData.containsKey("CustomModelData")) {
            Object val = customData.get("CustomModelData");
            if (val instanceof Number number) {
                meta.setCustomModelData(number.intValue());
            }
        }

        if (customData.containsKey("Unbreakable")) {
            if (Boolean.TRUE.equals(customData.get("Unbreakable"))) {
                meta.setUnbreakable(true);
            }
        }

        if (customData.containsKey("ItemFlags")) {
            List<String> flags = safeStringList(customData.get("ItemFlags"));
            for (String flagName : flags) {
                try {
                    ItemFlag flag = ItemFlag.valueOf(flagName.toUpperCase(Locale.ROOT));
                    meta.addItemFlags(flag);
                } catch (IllegalArgumentException ignored) {}
            }
        }
    }

    public static List<String> safeStringList(Object obj) {
        List<String> out = new ArrayList<>();
        if (obj instanceof List<?> rawList) {
            for (Object o : rawList) {
                if (o instanceof String s) {
                    out.add(s);
                }
            }
        }
        return out;
    }

    private static void addHideAttributes(ItemMeta meta) {
        meta.addItemFlags(
                ItemFlag.HIDE_ATTRIBUTES,
                ItemFlag.HIDE_ENCHANTS,
                ItemFlag.HIDE_UNBREAKABLE,
                ItemFlag.HIDE_DESTROYS,
                ItemFlag.HIDE_PLACED_ON
        );
    }

    private static final NamespacedKey PLACEHOLDER_KEY =
            new NamespacedKey(JavaPlugin.getProvidingPlugin(ItemBuilder.class), "placeholder");

    public static void setPlaceholderTag(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return;
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(PLACEHOLDER_KEY, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
    }

    public static boolean hasPlaceholderTag(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        Byte tagged = item.getItemMeta().getPersistentDataContainer().get(PLACEHOLDER_KEY, PersistentDataType.BYTE);
        return tagged != null && tagged == (byte) 1;
    }

    public static ItemStack withPersistentKey(ItemStack item, String key, String value) {
        if (item == null || !item.hasItemMeta()) return item;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        NamespacedKey namespacedKey = new NamespacedKey(JavaPlugin.getProvidingPlugin(ItemBuilder.class), key);
        meta.getPersistentDataContainer().set(namespacedKey, PersistentDataType.STRING, value);
        item.setItemMeta(meta);
        return item;
    }

    public static boolean hasPersistentKey(ItemStack item, String key) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        NamespacedKey namespacedKey = new NamespacedKey(JavaPlugin.getProvidingPlugin(ItemBuilder.class), key);
        return meta.getPersistentDataContainer().has(namespacedKey, PersistentDataType.STRING);
    }

    private static Material safeMatchMaterial(String input) {
        if (input == null) return Material.BARRIER;
        Material match = Material.matchMaterial(input.toUpperCase(Locale.ROOT));
        return match != null ? match : Material.BARRIER;
    }

    private static Map<String, Object> convertToStringObjectMap(Map<?, ?> rawMap) {
        Map<String, Object> result = new HashMap<>();
        for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
            if (entry.getKey() instanceof String key) {
                result.put(key, entry.getValue());
            }
        }
        return result;
    }
}