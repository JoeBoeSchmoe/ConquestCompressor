package org.conquest.conquestCompressor.functionalHandler;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.conquest.conquestCompressor.ConquestCompressor;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * 🎨 ItemDataModel
 * Represents metadata applied to an item (NBT, visual, model data, etc).
 * Used in compressor recipe definitions.
 */
public class ItemDataModel implements ConfigurationSerializable {

    private String displayName;
    private List<String> lore;
    private Integer customModelData;
    private List<EnchantmentDataModel> enchantments;
    private Boolean unbreakable;
    private Map<String, Object> nbt;

    public ItemDataModel() {}

    public ItemDataModel(
            String displayName,
            List<String> lore,
            Integer customModelData,
            List<EnchantmentDataModel> enchantments,
            Boolean unbreakable,
            Map<String, Object> nbt
    ) {
        this.displayName = displayName;
        this.lore = lore;
        this.customModelData = customModelData;
        this.enchantments = enchantments;
        this.unbreakable = unbreakable;
        this.nbt = nbt;
    }

    public static ItemDataModel fromItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;

        MiniMessage mini = MiniMessage.miniMessage();

        String displayName = meta.displayName() != null ? mini.serialize(Objects.requireNonNull(meta.displayName())) : null;

        List<String> lore = null;
        if (meta.lore() != null) {
            lore = new ArrayList<>();
            for (Component line : Objects.requireNonNull(meta.lore())) {
                lore.add(mini.serialize(line));
            }
        }

        Integer modelData = meta.hasCustomModelData() ? meta.getCustomModelData() : null;
        Boolean unbreakable = meta.isUnbreakable();

        List<EnchantmentDataModel> enchantList = new ArrayList<>();
        for (Map.Entry<Enchantment, Integer> entry : meta.getEnchants().entrySet()) {
            enchantList.add(new EnchantmentDataModel(entry.getKey().getKey().getKey(), entry.getValue()));
        }

        Map<String, Object> fakeNBT = new HashMap<>();
        fakeNBT.put("placeholder", true); // Replace with real NBT if needed

        return new ItemDataModel(displayName, lore, modelData, enchantList, unbreakable, fakeNBT);
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        Map<String, Object> map = new LinkedHashMap<>();

        map.put("displayName", displayName);
        map.put("lore", lore);
        map.put("customModelData", customModelData);
        map.put("unbreakable", unbreakable);
        map.put("nbt", nbt != null ? nbt : new HashMap<>());

        // ✨ Normalize enchantments to a clean YAML-safe list
        if (enchantments != null && !enchantments.isEmpty()) {
            List<Map<String, Object>> serialized = new ArrayList<>();
            for (EnchantmentDataModel enchant : enchantments) {
                serialized.add(enchant.serialize());
            }
            map.put("enchantments", serialized);
        } else {
            map.put("enchantments", Collections.emptyList());
        }

        return map;
    }

    @SuppressWarnings("unchecked")
    public static ItemDataModel deserialize(Map<String, Object> map) {
        if (map == null) return null;

        String displayName = (map.get("displayName") instanceof String s) ? s : null;

        List<String> lore = null;
        if (map.get("lore") instanceof List<?> rawLore) {
            lore = new ArrayList<>();
            for (Object line : rawLore) {
                if (line instanceof String str) {
                    lore.add(str);
                }
            }
        }

        Integer customModelData = (map.get("customModelData") instanceof Number num) ? num.intValue() : null;
        Boolean unbreakable = (map.get("unbreakable") instanceof Boolean b) ? b : null;

        Map<String, Object> nbt = null;
        if (map.get("nbt") instanceof Map<?, ?> rawNBT) {
            nbt = new HashMap<>();
            for (Map.Entry<?, ?> entry : rawNBT.entrySet()) {
                if (entry.getKey() instanceof String key) {
                    nbt.put(key, entry.getValue());
                }
            }
        }

        List<EnchantmentDataModel> enchantments = new ArrayList<>();
        if (map.get("enchantments") instanceof List<?> rawList) {
            for (Object obj : rawList) {
                if (obj instanceof Map<?, ?> rawMap) {
                    Map<String, Object> safeMap = new HashMap<>();
                    for (Map.Entry<?, ?> e : rawMap.entrySet()) {
                        if (e.getKey() instanceof String key) {
                            safeMap.put(key, e.getValue());
                        }
                    }

                    // Handle missing Bukkit class path manually
                    try {
                        enchantments.add(EnchantmentDataModel.deserialize(safeMap));
                    } catch (Exception ex) {
                        if (safeMap.containsKey("name") && safeMap.containsKey("level")) {
                            enchantments.add(new EnchantmentDataModel(
                                    String.valueOf(safeMap.get("name")),
                                    ((Number) safeMap.get("level")).intValue()
                            ));
                        }
                    }
                }
            }
        }

        return new ItemDataModel(displayName, lore, customModelData, enchantments, unbreakable, nbt);
    }

    public boolean matches(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;

        ItemMeta meta = item.getItemMeta();
        MiniMessage mini = MiniMessage.miniMessage();

        if (displayName != null) {
            Component actualName = meta.displayName();
            String actualSerialized = actualName != null ? mini.serialize(actualName) : "";
            if (!displayName.equals(actualSerialized)) return false;
        }

        if (lore != null && !lore.isEmpty()) {
            List<Component> actualLore = meta.lore();
            if (actualLore == null || actualLore.size() != lore.size()) return false;
            for (int i = 0; i < lore.size(); i++) {
                if (!lore.get(i).equals(mini.serialize(actualLore.get(i)))) return false;
            }
        }

        if (customModelData != null && (!meta.hasCustomModelData() || meta.getCustomModelData() != customModelData)) {
            return false;
        }

        if (unbreakable != null && meta.isUnbreakable() != unbreakable) {
            return false;
        }

        if (enchantments != null && !enchantments.isEmpty()) {
            for (EnchantmentDataModel expected : enchantments) {
                Enchantment ench = Enchantment.getByKey(org.bukkit.NamespacedKey.minecraft(expected.getName()));
                if (ench == null || !meta.hasEnchant(ench) || meta.getEnchantLevel(ench) != expected.getLevel()) {
                    return false;
                }
            }
        }

        if (nbt != null && !nbt.isEmpty()) {
            Map<String, Object> fakeTag = fromItem(item).getNbt();
            for (Map.Entry<String, Object> entry : nbt.entrySet()) {
                if (!Objects.equals(fakeTag.get(entry.getKey()), entry.getValue())) return false;
            }
        }

        return true;
    }

    public static int countMatching(Inventory inventory, Material material, ItemDataModel dataModel) {
        int count = 0;
        for (ItemStack item : inventory.getContents()) {
            if (item == null || item.getType() != material) continue;
            if (dataModel != null && !dataModel.matches(item)) continue;
            count += item.getAmount();
        }
        return count;
    }

    public static void removeMatching(Inventory inventory, Material material, ItemDataModel dataModel, int amountToRemove) {
        Iterator<ItemStack> iterator = inventory.iterator();
        while (iterator.hasNext() && amountToRemove > 0) {
            ItemStack item = iterator.next();
            if (item == null || item.getType() != material) continue;
            if (dataModel != null && !dataModel.matches(item)) continue;

            int stackAmount = item.getAmount();
            if (stackAmount <= amountToRemove) {
                amountToRemove -= stackAmount;
                item.setAmount(0);
            } else {
                item.setAmount(stackAmount - amountToRemove);
                amountToRemove = 0;
            }
        }
    }

    public void applyTo(ItemStack item) {
        if (item == null) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        MiniMessage mini = MiniMessage.miniMessage();

        if (displayName != null) meta.displayName(mini.deserialize(displayName));
        if (lore != null && !lore.isEmpty()) {
            List<Component> loreComponents = new ArrayList<>();
            for (String line : lore) {
                loreComponents.add(mini.deserialize(line));
            }
            meta.lore(loreComponents);
        }
        if (customModelData != null) meta.setCustomModelData(customModelData);
        if (unbreakable != null) meta.setUnbreakable(unbreakable);

        if (enchantments != null) {
            for (EnchantmentDataModel enchant : enchantments) {
                Enchantment ench = Enchantment.getByKey(org.bukkit.NamespacedKey.minecraft(enchant.getName()));
                if (ench != null) {
                    meta.addEnchant(ench, enchant.getLevel(), true);
                } else {
                    ConquestCompressor.getInstance().getLogger().warning("⚠️ Unknown enchantment: " + enchant.getName());
                }
            }
        }

        item.setItemMeta(meta);
    }

    public String getDisplayName() {
        return displayName;
    }

    public List<String> getLore() {
        return lore != null ? lore : Collections.emptyList();
    }

    public Integer getCustomModelData() {
        return customModelData;
    }

    public List<EnchantmentDataModel> getEnchantments() {
        return enchantments != null ? enchantments : Collections.emptyList();
    }

    public boolean isUnbreakable() {
        return unbreakable != null && unbreakable;
    }

    public Map<String, Object> getNbt() {
        return nbt != null ? nbt : Collections.emptyMap();
    }
}
