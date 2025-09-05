package org.conquest.conquestCompressor.functionalHandler;

import com.destroystokyo.paper.profile.PlayerProfile;
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
    private String skullOwnerUUID; // NEW: The profile UUID used in applyTexture()

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

    public ItemDataModel withSkullOwnerUUID(String uuid) {
        this.skullOwnerUUID = uuid;
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
        fakeNBT.put("placeholder", true);

        List<String> flags = new ArrayList<>();
        for (ItemFlag flag : meta.getItemFlags()) {
            flags.add(flag.name());
        }

        String base64Texture = null;
        String skullOwnerUUID = null;

        if (item.getType() == Material.PLAYER_HEAD && meta instanceof SkullMeta skullMeta) {
            base64Texture = SkullTextureResolver.extractTextureFromMeta(skullMeta);

            try {
                PlayerProfile profile = skullMeta.getPlayerProfile();
                if (profile != null && profile.getId() != null) {
                    skullOwnerUUID = profile.getId().toString();
                }
            } catch (Exception e) {
                ConquestCompressor.getInstance().getLogger().warning("⚠️  Could not extract skull owner UUID: " + e.getMessage());
            }
        }

        return new ItemDataModel(displayName, lore, modelData, enchantList, unbreakable, fakeNBT)
                .withItemFlags(flags)
                .withSkullTexture(base64Texture)
                .withSkullOwnerUUID(skullOwnerUUID);
    }


    @Override
    public @NotNull Map<String, Object> serialize() {
        Map<String, Object> map = new LinkedHashMap<>();

        // ✅ Basic fields
        map.put("displayName", displayName);
        map.put("lore", lore);
        map.put("customModelData", customModelData);
        map.put("unbreakable", unbreakable);
        map.put("nbt", nbt != null ? nbt : new HashMap<>());
        map.put("itemFlags", itemFlags != null ? itemFlags : Collections.emptyList());

        // ✅ Skull-specific fields
        map.put("skullTexture", skullTextureBase64);
        map.put("skullOwnerUUID", skullOwnerUUID);

        // ✅ Enchantments
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

        // ✅ Skull-specific fields
        String skullTexture = (map.get("skullTexture") instanceof String s) ? s : null;
        String ownerUUID = (map.get("skullOwnerUUID") instanceof String s) ? s : null;

        return new ItemDataModel(displayName, lore, customModelData, enchantments, unbreakable, nbt)
                .withItemFlags(flags)
                .withSkullTexture(skullTexture)
                .withSkullOwnerUUID(ownerUUID);
    }

    public boolean matches(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        MiniMessage mini = MiniMessage.miniMessage();

        // ✅ Display name
        if (displayName != null) {
            Component actualName = meta.displayName();
            String actualSerialized = actualName != null ? mini.serialize(actualName) : "";
            if (!displayName.equals(actualSerialized)) return false;
        }

        // ✅ Lore
        if (lore != null && !lore.isEmpty()) {
            List<Component> actualLore = meta.lore();
            if (actualLore == null || actualLore.size() != lore.size()) return false;
            for (int i = 0; i < lore.size(); i++) {
                if (!lore.get(i).equals(mini.serialize(actualLore.get(i)))) return false;
            }
        }

        // ✅ Model data
        if (customModelData != null) {
            if (!meta.hasCustomModelData() || meta.getCustomModelData() != customModelData) return false;
        }

        // ✅ Unbreakable
        if (unbreakable != null && meta.isUnbreakable() != unbreakable) return false;

        // ✅ Enchantments
        if (enchantments != null && !enchantments.isEmpty()) {
            for (EnchantmentDataModel expected : enchantments) {
                Enchantment ench = Enchantment.getByKey(org.bukkit.NamespacedKey.minecraft(expected.getName()));
                if (ench == null || !meta.hasEnchant(ench) || meta.getEnchantLevel(ench) != expected.getLevel()) {
                    return false;
                }
            }
        }

        // ✅ Item flags
        if (itemFlags != null && !itemFlags.isEmpty()) {
            for (String flagName : itemFlags) {
                try {
                    ItemFlag flag = ItemFlag.valueOf(flagName);
                    if (!meta.hasItemFlag(flag)) return false;
                } catch (IllegalArgumentException e) {
                    return false;
                }
            }
        }

        // ✅ Skull-specific check (base64 texture + UUID)
        if (item.getType() == Material.PLAYER_HEAD && meta instanceof SkullMeta skullMeta) {
            String actualTexture = SkullTextureResolver.extractTextureFromMeta(skullMeta);
            if (skullTextureBase64 != null && !skullTextureBase64.equals(actualTexture)) {
                return false;
            }

            if (skullOwnerUUID != null) {
                try {
                    UUID actualUUID = skullMeta.getPlayerProfile() != null
                            ? skullMeta.getPlayerProfile().getId()
                            : null;

                    if (actualUUID == null || !skullOwnerUUID.equalsIgnoreCase(actualUUID.toString())) {
                        return false;
                    }
                } catch (Exception e) {
                    return false;
                }
            }
        }

        // ✅ Fake NBT match
        if (nbt != null && !nbt.isEmpty()) {
            Map<String, Object> fakeTag = new HashMap<>();
            fakeTag.put("placeholder", true);
            for (Map.Entry<String, Object> entry : nbt.entrySet()) {
                if (!Objects.equals(fakeTag.get(entry.getKey()), entry.getValue())) return false;
            }
        }

        return true;
    }

    public static int countMatching(Inventory inventory, Material material, ItemDataModel dataModel) {
        if (inventory == null || material == null) return 0;

        int count = 0;
        for (ItemStack item : inventory.getContents()) {
            if (strictMatches(item, material, dataModel)) {
                count += item.getAmount();
            }
        }
        return count;
    }

    public static void removeMatching(Inventory inventory, Material material, ItemDataModel dataModel, int amountToRemove) {
        if (inventory == null || material == null || amountToRemove <= 0) return;

        for (int i = 0; i < inventory.getSize() && amountToRemove > 0; i++) {
            ItemStack item = inventory.getItem(i);
            if (!strictMatches(item, material, dataModel)) continue;

            int stackAmount = item.getAmount();
            if (stackAmount <= amountToRemove) {
                amountToRemove -= stackAmount;
                inventory.clear(i);
            } else {
                item.setAmount(stackAmount - amountToRemove);
                inventory.setItem(i, item);
                amountToRemove = 0;
            }
        }
    }

    /**
     * Strict matching:
     * - Always requires exact Material.
     * - If dataModel != null  -> use dataModel.matches(item) (full meta match).
     * - If dataModel == null  -> only match "plain" items (no name, lore, enchants, flags, model data, unbreakable, skull texture/owner).
     */
    public static boolean strictMatches(ItemStack item, Material material, ItemDataModel dataModel) {
        if (item == null || item.getType() != material) return false;

        if (dataModel != null) {
            // Use the existing full metadata checker
            return dataModel.matches(item);
        }

        // No data model provided -> accept ONLY plain/vanilla stacks
        ItemMeta meta = item.getItemMeta();
        return meta == null || isMetaEmpty(meta, item.getType());
    }

    /** Returns true if the meta carries no visible/behavioral customization. */
    private static boolean isMetaEmpty(ItemMeta meta, Material type) {
        if (meta == null) return true;

        // Display name
        if (meta.displayName() != null) return false;

        // Lore
        if (meta.lore() != null && !meta.lore().isEmpty()) return false;

        // Custom model data
        if (meta.hasCustomModelData()) return false;

        // Enchants
        if (!meta.getEnchants().isEmpty()) return false;

        // Unbreakable
        if (meta.isUnbreakable()) return false;

        // Item flags
        if (!meta.getItemFlags().isEmpty()) return false;

        // Skull-specific: any texture/owner means "not plain"
        if (type == Material.PLAYER_HEAD && meta instanceof org.bukkit.inventory.meta.SkullMeta skullMeta) {
            try {
                String texture = SkullTextureResolver.extractTextureFromMeta(skullMeta);
                if (texture != null) return false;
                if (skullMeta.getPlayerProfile() != null && skullMeta.getPlayerProfile().getId() != null) return false;
            } catch (Exception ignored) {
            }
        }

        return true;
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

        // ✅ Set metadata BEFORE applying skull profile
        item.setItemMeta(meta);

        // ✅ Apply Skull Texture + Owner UUID using updated resolver
        if (item.getType() == Material.PLAYER_HEAD && skullTextureBase64 != null) {
            SkullTextureResolver.applyTexture(item, skullTextureBase64, skullOwnerUUID);
        }
    }

    /**
     * Converts this data model into an actual ItemStack using the given material.
     * @param material The base item material (e.g. IRON_INGOT)
     * @return A fully constructed ItemStack with metadata
     */
    public ItemStack toItemStack(Material material) {
        if (material == null) material = Material.STONE; // fallback

        ItemStack item = new ItemStack(material);
        applyTo(item);
        return item;
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
