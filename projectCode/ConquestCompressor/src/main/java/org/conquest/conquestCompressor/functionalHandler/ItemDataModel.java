package org.conquest.conquestCompressor.functionalHandler;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
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
    private List<String> itemFlags;
    private String skullTextureBase64;

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

    public ItemDataModel withItemFlags(List<String> flags) {
        this.itemFlags = flags;
        return this;
    }

    public ItemDataModel withSkullTexture(String base64) {
        this.skullTextureBase64 = base64;
        return this;
    }

    public static ItemDataModel fromItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;

        MiniMessage mini = MiniMessage.miniMessage();

        String displayName = meta.displayName() != null
                ? mini.serialize(Objects.requireNonNull(meta.displayName()))
                : null;

        List<String> lore = null;
        if (meta.lore() != null) {
            lore = new ArrayList<>();
            for (Component line : meta.lore()) {
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
        fakeNBT.put("placeholder", true);

        List<String> flags = new ArrayList<>();
        for (ItemFlag flag : meta.getItemFlags()) {
            flags.add(flag.name());
        }

        String base64Texture = null;
        if (item.getType() == Material.PLAYER_HEAD && meta instanceof SkullMeta skullMeta) {
            base64Texture = SkullTextureResolver.extractTextureFromMeta(skullMeta);

            if (base64Texture != null) {
                ConquestCompressor.getInstance().getLogger().info("🧠 Extracted skull texture: " + base64Texture.substring(0, Math.min(30, base64Texture.length())) + "...");
            } else {
                ConquestCompressor.getInstance().getLogger().warning("⚠️ No skull texture found during fromItem()");
            }
        }

        return new ItemDataModel(displayName, lore, modelData, enchantList, unbreakable, fakeNBT)
                .withItemFlags(flags)
                .withSkullTexture(base64Texture);
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        Map<String, Object> map = new LinkedHashMap<>();

        map.put("displayName", displayName);
        map.put("lore", lore);
        map.put("customModelData", customModelData);
        map.put("unbreakable", unbreakable);
        map.put("nbt", nbt != null ? nbt : new HashMap<>());
        map.put("itemFlags", itemFlags != null ? itemFlags : Collections.emptyList());
        map.put("skullTexture", skullTextureBase64); // ✅ Only include texture

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

        List<String> flags = null;
        if (map.get("itemFlags") instanceof List<?> rawFlags) {
            flags = new ArrayList<>();
            for (Object f : rawFlags) {
                if (f instanceof String s) flags.add(s);
            }
        }

        String skullTexture = (map.get("skullTexture") instanceof String s) ? s : null;

        return new ItemDataModel(displayName, lore, customModelData, enchantments, unbreakable, nbt)
                .withItemFlags(flags)
                .withSkullTexture(skullTexture); // ✅ skullOwner is not used at all
    }

    public boolean matches(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        MiniMessage mini = MiniMessage.miniMessage();

        // ✅ Check display name
        if (displayName != null) {
            Component actualName = meta.displayName();
            String actualSerialized = actualName != null ? mini.serialize(actualName) : "";
            if (!displayName.equals(actualSerialized)) return false;
        }

        // ✅ Check lore
        if (lore != null && !lore.isEmpty()) {
            List<Component> actualLore = meta.lore();
            if (actualLore == null || actualLore.size() != lore.size()) return false;
            for (int i = 0; i < lore.size(); i++) {
                if (!lore.get(i).equals(mini.serialize(actualLore.get(i)))) return false;
            }
        }

        // ✅ Check model data
        if (customModelData != null) {
            if (!meta.hasCustomModelData() || meta.getCustomModelData() != customModelData) return false;
        }

        // ✅ Check unbreakable
        if (unbreakable != null && meta.isUnbreakable() != unbreakable) return false;

        // ✅ Check enchantments
        if (enchantments != null && !enchantments.isEmpty()) {
            for (EnchantmentDataModel expected : enchantments) {
                Enchantment ench = Enchantment.getByKey(org.bukkit.NamespacedKey.minecraft(expected.getName()));
                if (ench == null || !meta.hasEnchant(ench) || meta.getEnchantLevel(ench) != expected.getLevel()) {
                    return false;
                }
            }
        }

        // ✅ Check item flags
        if (itemFlags != null && !itemFlags.isEmpty()) {
            for (String flagName : itemFlags) {
                try {
                    ItemFlag flag = ItemFlag.valueOf(flagName);
                    if (!meta.hasItemFlag(flag)) return false;
                } catch (IllegalArgumentException e) {
                    // unknown flag, treat as mismatch
                    return false;
                }
            }
        }

        // ✅ Check skull texture without recursive fromItem() call
        if (skullTextureBase64 != null && item.getType() == Material.PLAYER_HEAD && meta instanceof SkullMeta skullMeta) {
            String actualTexture = SkullTextureResolver.extractTextureFromMeta(skullMeta);
            if (actualTexture == null || !skullTextureBase64.equals(actualTexture)) {
                return false;
            }
        }

        // ✅ Simulated NBT comparison (minimal)
        if (nbt != null && !nbt.isEmpty()) {
            // Note: Avoid full fromItem() call — simulate as much as needed
            Map<String, Object> fakeTag = new HashMap<>();
            fakeTag.put("placeholder", true);

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

        // ✅ Apply Display Name
        if (displayName != null) {
            meta.displayName(mini.deserialize(displayName));
        }

        // ✅ Apply Lore
        if (lore != null && !lore.isEmpty()) {
            List<Component> loreComponents = new ArrayList<>();
            for (String line : lore) {
                loreComponents.add(mini.deserialize(line));
            }
            meta.lore(loreComponents);
        }

        // ✅ Apply Custom Model Data and Unbreakable
        if (customModelData != null) {
            meta.setCustomModelData(customModelData);
        }

        if (unbreakable != null) {
            meta.setUnbreakable(unbreakable);
        }

        // ✅ Apply Enchantments
        if (enchantments != null) {
            for (EnchantmentDataModel enchant : enchantments) {
                Enchantment ench = Enchantment.getByKey(org.bukkit.NamespacedKey.minecraft(enchant.getName()));
                if (ench != null) {
                    meta.addEnchant(ench, enchant.getLevel(), true);
                } else {
                    ConquestCompressor.getInstance().getLogger().warning("⚠️  Unknown enchantment: " + enchant.getName());
                }
            }
        }

        // ✅ Apply Item Flags
        if (itemFlags != null) {
            for (String flagName : itemFlags) {
                try {
                    ItemFlag flag = ItemFlag.valueOf(flagName);
                    meta.addItemFlags(flag);
                } catch (IllegalArgumentException e) {
                    ConquestCompressor.getInstance().getLogger().warning("⚠️  Unknown ItemFlag: " + flagName);
                }
            }
        }

        // ✅ Apply meta now
        item.setItemMeta(meta);

        // ✅ Apply Skull Texture AFTER meta is set
        if (item.getType() == Material.PLAYER_HEAD && skullTextureBase64 != null) {
            SkullTextureResolver.applyTexture(item, skullTextureBase64);
        }
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
