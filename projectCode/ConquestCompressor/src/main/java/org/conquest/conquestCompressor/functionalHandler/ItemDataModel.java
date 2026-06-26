package org.conquest.conquestCompressor.functionalHandler;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.google.common.collect.Multimap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.conquest.conquestCompressor.ConquestCompressor;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 🎨 ItemDataModel
 * Represents metadata applied to an item.
 *
 * Supports:
 * - displayName
 * - lore
 * - customModelData
 * - enchantments
 * - attributes
 * - unbreakable
 * - itemFlags
 * - skullTexture
 * - skullOwnerUUID
 * - nbt.PublicBukkitValues through Bukkit PersistentDataContainer
 *
 * YAML examples:
 *
 * itemData:
 *   customModelData: 2
 *   attributes:
 *     - attribute: generic.attack_damage
 *       name: conquest_attack_damage
 *       amount: 12.0
 *       operation: ADD_NUMBER
 *       slot: MAINHAND
 *   nbt:
 *     placeholder: true
 *     PublicBukkitValues:
 *       minecraft:drop_id: stone1-coin
 */
public class ItemDataModel implements ConfigurationSerializable {

    private String displayName;
    private List<String> lore;
    private Integer customModelData;
    private List<EnchantmentDataModel> enchantments;
    private List<AttributeDataModel> attributes;
    private Boolean unbreakable;
    private Map<String, Object> nbt;
    private List<String> itemFlags;
    private String skullOwnerUUID;
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

    public ItemDataModel withAttributes(List<AttributeDataModel> attributes) {
        this.attributes = attributes;
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

        List<AttributeDataModel> attributeList = extractAttributes(meta);

        Map<String, Object> realNBT = new LinkedHashMap<>();
        realNBT.put("placeholder", true);

        Map<String, Object> publicBukkitValues = extractPublicBukkitValues(meta);
        if (!publicBukkitValues.isEmpty()) {
            realNBT.put("PublicBukkitValues", publicBukkitValues);
        }

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
                ConquestCompressor.getInstance().getLogger().warning(
                        "⚠️  Could not extract skull owner UUID: " + e.getMessage()
                );
            }
        }

        return new ItemDataModel(displayName, lore, modelData, enchantList, unbreakable, realNBT)
                .withItemFlags(flags)
                .withSkullTexture(base64Texture)
                .withSkullOwnerUUID(skullOwnerUUID)
                .withAttributes(attributeList);
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        Map<String, Object> map = new LinkedHashMap<>();

        map.put("displayName", displayName);
        map.put("lore", lore);
        map.put("customModelData", customModelData);
        map.put("unbreakable", unbreakable);
        map.put("nbt", nbt != null ? nbt : new LinkedHashMap<>());
        map.put("itemFlags", itemFlags != null ? itemFlags : Collections.emptyList());

        map.put("skullTexture", skullTextureBase64);
        map.put("skullOwnerUUID", skullOwnerUUID);

        if (enchantments != null && !enchantments.isEmpty()) {
            List<Map<String, Object>> serialized = new ArrayList<>();
            for (EnchantmentDataModel enchant : enchantments) {
                serialized.add(enchant.serialize());
            }
            map.put("enchantments", serialized);
        } else {
            map.put("enchantments", Collections.emptyList());
        }

        if (attributes != null && !attributes.isEmpty()) {
            List<Map<String, Object>> serializedAttributes = new ArrayList<>();
            for (AttributeDataModel attribute : attributes) {
                serializedAttributes.add(attribute.serialize());
            }
            map.put("attributes", serializedAttributes);
        } else {
            map.put("attributes", Collections.emptyList());
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
            nbt = new LinkedHashMap<>();

            for (Map.Entry<?, ?> entry : rawNBT.entrySet()) {
                if (!(entry.getKey() instanceof String key)) continue;

                Object value = entry.getValue();

                if (isPublicBukkitValuesKey(key) && value instanceof Map<?, ?> rawPublicBukkitValues) {
                    nbt.put("PublicBukkitValues", sanitizeStringObjectMap(rawPublicBukkitValues));
                } else {
                    nbt.put(key, value);
                }
            }
        }

        List<EnchantmentDataModel> enchantments = new ArrayList<>();
        if (map.get("enchantments") instanceof List<?> rawList) {
            for (Object obj : rawList) {
                if (obj instanceof Map<?, ?> rawMap) {
                    Map<String, Object> safeMap = new LinkedHashMap<>();

                    for (Map.Entry<?, ?> e : rawMap.entrySet()) {
                        if (e.getKey() instanceof String key) {
                            safeMap.put(key, e.getValue());
                        }
                    }

                    try {
                        enchantments.add(EnchantmentDataModel.deserialize(safeMap));
                    } catch (Exception ex) {
                        if (safeMap.containsKey("name") && safeMap.containsKey("level")) {
                            Object levelObj = safeMap.get("level");
                            int level = levelObj instanceof Number num ? num.intValue() : 1;

                            enchantments.add(new EnchantmentDataModel(
                                    String.valueOf(safeMap.get("name")),
                                    level
                            ));
                        }
                    }
                }
            }
        }

        List<AttributeDataModel> attributes = new ArrayList<>();
        if (map.get("attributes") instanceof List<?> rawAttributes) {
            for (Object obj : rawAttributes) {
                if (!(obj instanceof Map<?, ?> rawMap)) continue;

                Map<String, Object> safeMap = new LinkedHashMap<>();
                for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
                    if (entry.getKey() instanceof String key) {
                        safeMap.put(key, entry.getValue());
                    }
                }

                AttributeDataModel attributeData = AttributeDataModel.deserialize(safeMap);
                if (attributeData != null) {
                    attributes.add(attributeData);
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
        String ownerUUID = (map.get("skullOwnerUUID") instanceof String s) ? s : null;

        return new ItemDataModel(displayName, lore, customModelData, enchantments, unbreakable, nbt)
                .withItemFlags(flags)
                .withSkullTexture(skullTexture)
                .withSkullOwnerUUID(ownerUUID)
                .withAttributes(attributes);
    }

    public boolean matches(ItemStack item) {
        if (item == null || item.getType().isAir()) return false;

        ItemMeta meta = item.getItemMeta();

        if (requiresMeta() && meta == null) {
            return false;
        }

        MiniMessage mini = MiniMessage.miniMessage();

        if (displayName != null) {
            if (meta == null) return false;

            Component actualName = meta.displayName();
            String actualSerialized = actualName != null ? mini.serialize(actualName) : "";

            if (!displayName.equals(actualSerialized)) return false;
        }

        if (lore != null && !lore.isEmpty()) {
            if (meta == null) return false;

            List<Component> actualLore = meta.lore();
            if (actualLore == null || actualLore.size() != lore.size()) return false;

            for (int i = 0; i < lore.size(); i++) {
                if (!lore.get(i).equals(mini.serialize(actualLore.get(i)))) return false;
            }
        }

        if (customModelData != null) {
            if (meta == null || !meta.hasCustomModelData() || meta.getCustomModelData() != customModelData) {
                return false;
            }
        }

        if (unbreakable != null) {
            if (meta == null || meta.isUnbreakable() != unbreakable) return false;
        }

        if (enchantments != null && !enchantments.isEmpty()) {
            if (meta == null) return false;

            for (EnchantmentDataModel expected : enchantments) {
                Enchantment ench = Enchantment.getByKey(NamespacedKey.minecraft(expected.getName()));
                if (ench == null || !meta.hasEnchant(ench) || meta.getEnchantLevel(ench) != expected.getLevel()) {
                    return false;
                }
            }
        }

        if (itemFlags != null && !itemFlags.isEmpty()) {
            if (meta == null) return false;

            for (String flagName : itemFlags) {
                try {
                    ItemFlag flag = ItemFlag.valueOf(flagName);
                    if (!meta.hasItemFlag(flag)) return false;
                } catch (IllegalArgumentException e) {
                    return false;
                }
            }
        }

        if (!matchesAttributes(meta)) {
            return false;
        }

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
        } else if (skullTextureBase64 != null || skullOwnerUUID != null) {
            return false;
        }

        return matchesNbt(meta);
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
     * - If dataModel != null -> use dataModel.matches(item).
     * - If dataModel == null -> only match plain stacks.
     */
    public static boolean strictMatches(ItemStack item, Material material, ItemDataModel dataModel) {
        if (item == null || item.getType() != material) return false;

        if (dataModel != null) {
            return dataModel.matches(item);
        }

        ItemMeta meta = item.getItemMeta();
        return meta == null || isMetaEmpty(meta, item.getType());
    }

    /**
     * Returns true if the meta carries no visible, behavioral, attribute, skull, or PDC customization.
     */
    private static boolean isMetaEmpty(ItemMeta meta, Material type) {
        if (meta == null) return true;

        if (meta.displayName() != null) return false;

        if (meta.lore() != null && !meta.lore().isEmpty()) return false;

        if (meta.hasCustomModelData()) return false;

        if (!meta.getEnchants().isEmpty()) return false;

        if (meta.isUnbreakable()) return false;

        if (!meta.getItemFlags().isEmpty()) return false;

        if (meta.hasAttributeModifiers()) return false;

        if (!meta.getPersistentDataContainer().isEmpty()) return false;

        if (type == Material.PLAYER_HEAD && meta instanceof SkullMeta skullMeta) {
            try {
                String texture = SkullTextureResolver.extractTextureFromMeta(skullMeta);
                if (texture != null) return false;

                if (skullMeta.getPlayerProfile() != null && skullMeta.getPlayerProfile().getId() != null) {
                    return false;
                }
            } catch (Exception ignored) {
            }
        }

        return true;
    }

    public void applyTo(ItemStack item) {
        if (item == null || item.getType().isAir()) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        MiniMessage mini = MiniMessage.miniMessage();

        if (displayName != null) {
            meta.displayName(mini.deserialize(displayName));
        }

        if (lore != null && !lore.isEmpty()) {
            List<Component> loreComponents = new ArrayList<>();

            for (String line : lore) {
                loreComponents.add(mini.deserialize(line));
            }

            meta.lore(loreComponents);
        }

        if (customModelData != null) {
            meta.setCustomModelData(customModelData);
        }

        if (unbreakable != null) {
            meta.setUnbreakable(unbreakable);
        }

        if (enchantments != null) {
            for (EnchantmentDataModel enchant : enchantments) {
                Enchantment ench = Enchantment.getByKey(NamespacedKey.minecraft(enchant.getName()));

                if (ench != null) {
                    meta.addEnchant(ench, enchant.getLevel(), true);
                } else {
                    ConquestCompressor.getInstance().getLogger().warning(
                            "⚠️  Unknown enchantment: " + enchant.getName()
                    );
                }
            }
        }

        if (itemFlags != null) {
            for (String flagName : itemFlags) {
                try {
                    ItemFlag flag = ItemFlag.valueOf(flagName);
                    meta.addItemFlags(flag);
                } catch (IllegalArgumentException e) {
                    ConquestCompressor.getInstance().getLogger().warning(
                            "⚠️  Unknown ItemFlag: " + flagName
                    );
                }
            }
        }

        applyAttributes(meta);
        applyNbt(meta);

        item.setItemMeta(meta);

        if (item.getType() == Material.PLAYER_HEAD && skullTextureBase64 != null) {
            SkullTextureResolver.applyTexture(item, skullTextureBase64, skullOwnerUUID);
        }
    }

    /**
     * Converts this data model into an actual ItemStack using the given material.
     *
     * @param material The base item material.
     * @return A fully constructed ItemStack with metadata.
     */
    public ItemStack toItemStack(Material material) {
        if (material == null) material = Material.STONE;

        ItemStack item = new ItemStack(material);
        applyTo(item);
        return item;
    }

    // ───────────────────────────────────────────
    // Attribute support
    // ───────────────────────────────────────────

    private void applyAttributes(ItemMeta meta) {
        if (meta == null || attributes == null || attributes.isEmpty()) return;

        for (AttributeDataModel data : attributes) {
            Attribute attribute = parseAttribute(data.getAttribute());
            if (attribute == null) {
                ConquestCompressor.getInstance().getLogger().warning(
                        "⚠️  Unknown attribute: " + data.getAttribute()
                );
                continue;
            }

            AttributeModifier.Operation operation = parseAttributeOperation(data.getOperation());
            if (operation == null) {
                ConquestCompressor.getInstance().getLogger().warning(
                        "⚠️  Unknown attribute operation: " + data.getOperation()
                );
                continue;
            }

            EquipmentSlotGroup slotGroup = parseEquipmentSlotGroup(data.getSlot());
            if (slotGroup == null) {
                slotGroup = EquipmentSlotGroup.ANY;
            }

            NamespacedKey modifierKey = buildAttributeModifierKey(data, attribute);

            AttributeModifier modifier = new AttributeModifier(
                    modifierKey,
                    data.getAmount(),
                    operation,
                    slotGroup
            );

            try {
                meta.addAttributeModifier(attribute, modifier);
            } catch (IllegalArgumentException ex) {
                ConquestCompressor.getInstance().getLogger().warning(
                        "⚠️  Could not add attribute modifier '" + data.getName() + "' for "
                                + data.getAttribute() + ": " + ex.getMessage()
                );
            }
        }
    }

    private boolean matchesAttributes(ItemMeta meta) {
        if (attributes == null || attributes.isEmpty()) return true;
        if (meta == null || !meta.hasAttributeModifiers()) return false;

        for (AttributeDataModel expected : attributes) {
            Attribute attribute = parseAttribute(expected.getAttribute());
            if (attribute == null) return false;

            Collection<AttributeModifier> actualModifiers = meta.getAttributeModifiers(attribute);
            if (actualModifiers == null || actualModifiers.isEmpty()) return false;

            boolean found = false;

            for (AttributeModifier actual : actualModifiers) {
                if (!attributeModifierMatches(expected, actual)) continue;

                found = true;
                break;
            }

            if (!found) return false;
        }

        return true;
    }

    private static boolean attributeModifierMatches(AttributeDataModel expected, AttributeModifier actual) {
        if (expected == null || actual == null) return false;

        AttributeModifier.Operation expectedOperation = parseAttributeOperation(expected.getOperation());
        if (expectedOperation == null || actual.getOperation() != expectedOperation) return false;

        if (Double.compare(actual.getAmount(), expected.getAmount()) != 0) return false;

        String expectedName = expected.getName();
        if (expectedName != null && !expectedName.isBlank()) {
            NamespacedKey actualKey = actual.getKey();
            String actualKeyName = actualKey != null ? actualKey.getKey() : "";
            String actualFullKey = actualKey != null ? actualKey.getNamespace() + ":" + actualKey.getKey() : "";
            String actualName = actual.getName();

            if (!expectedName.equalsIgnoreCase(actualName)
                    && !expectedName.equalsIgnoreCase(actualKeyName)
                    && !expectedName.equalsIgnoreCase(actualFullKey)) {
                return false;
            }
        }

        EquipmentSlotGroup expectedSlotGroup = parseEquipmentSlotGroup(expected.getSlot());
        if (expectedSlotGroup != null && actual.getSlotGroup() != expectedSlotGroup) {
            return false;
        }

        return true;
    }

    private static List<AttributeDataModel> extractAttributes(ItemMeta meta) {
        List<AttributeDataModel> list = new ArrayList<>();

        if (meta == null || !meta.hasAttributeModifiers() || meta.getAttributeModifiers() == null) {
            return list;
        }

        Multimap<Attribute, AttributeModifier> modifiers = meta.getAttributeModifiers();
        if (modifiers == null || modifiers.isEmpty()) {
            return list;
        }

        for (Map.Entry<Attribute, AttributeModifier> entry : modifiers.entries()) {
            Attribute attribute = entry.getKey();
            AttributeModifier modifier = entry.getValue();

            if (attribute == null || modifier == null) continue;

            String attributeKey = attribute.getKey().getNamespace() + ":" + attribute.getKey().getKey();
            String modifierKey = modifier.getKey() != null
                    ? modifier.getKey().getNamespace() + ":" + modifier.getKey().getKey()
                    : modifier.getName();

            EquipmentSlotGroup slotGroup = modifier.getSlotGroup();
            String slot = slotGroup != null ? slotGroup.toString() : "ANY";

            list.add(new AttributeDataModel(
                    attributeKey,
                    modifierKey,
                    modifier.getAmount(),
                    modifier.getOperation().name(),
                    slot
            ));
        }

        return list;
    }

    private static Attribute parseAttribute(String raw) {
        if (raw == null || raw.isBlank()) return null;

        String key = raw.trim().toLowerCase(Locale.ROOT);

        if (key.startsWith("minecraft:")) {
            key = key.substring("minecraft:".length());
        }

        if (key.startsWith("generic.")) {
            key = key.substring("generic.".length());
        }

        key = key.toUpperCase(Locale.ROOT)
                .replace('.', '_')
                .replace('-', '_')
                .replace(' ', '_');

        List<String> candidates = new ArrayList<>();
        candidates.add(key);

        if (!key.startsWith("GENERIC_")) {
            candidates.add("GENERIC_" + key);
        }

        if (key.startsWith("GENERIC_")) {
            candidates.add(key.substring("GENERIC_".length()));
        }

        for (String candidate : candidates) {
            try {
                return Attribute.valueOf(candidate);
            } catch (IllegalArgumentException ignored) {
            }
        }

        return null;
    }

    private static AttributeModifier.Operation parseAttributeOperation(String raw) {
        if (raw == null || raw.isBlank()) {
            return AttributeModifier.Operation.ADD_NUMBER;
        }

        String key = raw.trim().toUpperCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');

        return switch (key) {
            case "ADD_NUMBER", "ADD", "ADDITION" -> AttributeModifier.Operation.ADD_NUMBER;
            case "ADD_SCALAR", "ADD_SCALAR_BASE", "SCALAR" -> AttributeModifier.Operation.ADD_SCALAR;
            case "MULTIPLY_SCALAR_1", "MULTIPLY", "MULTIPLY_PERCENTAGE", "MULTIPLY_SCALAR" ->
                    AttributeModifier.Operation.MULTIPLY_SCALAR_1;
            default -> null;
        };
    }

    private static EquipmentSlotGroup parseEquipmentSlotGroup(String raw) {
        if (raw == null || raw.isBlank()) return null;

        String key = raw.trim().toUpperCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');

        return switch (key) {
            case "ANY", "ALL" -> EquipmentSlotGroup.ANY;
            case "HAND", "HANDS" -> EquipmentSlotGroup.HAND;
            case "MAINHAND", "MAIN_HAND", "HAND_MAIN" -> EquipmentSlotGroup.MAINHAND;
            case "OFFHAND", "OFF_HAND", "HAND_OFF" -> EquipmentSlotGroup.OFFHAND;
            case "HEAD", "HELMET" -> EquipmentSlotGroup.HEAD;
            case "CHEST", "CHESTPLATE", "BODY" -> EquipmentSlotGroup.CHEST;
            case "LEGS", "LEGGINGS" -> EquipmentSlotGroup.LEGS;
            case "FEET", "BOOTS" -> EquipmentSlotGroup.FEET;
            case "ARMOR" -> EquipmentSlotGroup.ARMOR;
            case "SADDLE" -> EquipmentSlotGroup.SADDLE;
            default -> null;
        };
    }

    private static NamespacedKey buildAttributeModifierKey(AttributeDataModel data, Attribute attribute) {
        String rawName = data.getName();

        if (rawName != null && rawName.contains(":")) {
            NamespacedKey parsed = parseNamespacedKey(rawName);
            if (parsed != null) return parsed;
        }

        String base = rawName != null && !rawName.isBlank()
                ? rawName
                : attribute.getKey().getKey() + "_" + data.getOperation() + "_" + data.getAmount();

        String safeKey = sanitizeKey(base);
        if (safeKey.isBlank()) {
            safeKey = "attribute_modifier";
        }

        return new NamespacedKey(ConquestCompressor.getInstance(), safeKey);
    }

    private static String sanitizeKey(String raw) {
        if (raw == null) return "";

        String key = raw.trim().toLowerCase(Locale.ROOT);

        if (key.contains(":")) {
            key = key.substring(key.indexOf(':') + 1);
        }

        key = key.replaceAll("[^a-z0-9/._-]", "_");
        key = key.replaceAll("_+", "_");

        while (key.startsWith("_")) {
            key = key.substring(1);
        }

        while (key.endsWith("_")) {
            key = key.substring(0, key.length() - 1);
        }

        if (key.isBlank()) {
            key = UUID.nameUUIDFromBytes(raw.getBytes(StandardCharsets.UTF_8)).toString().replace("-", "");
        }

        return key;
    }

    public static class AttributeDataModel {

        private final String attribute;
        private final String name;
        private final double amount;
        private final String operation;
        private final String slot;

        public AttributeDataModel(String attribute, String name, double amount, String operation, String slot) {
            this.attribute = attribute;
            this.name = name;
            this.amount = amount;
            this.operation = operation;
            this.slot = slot;
        }

        public Map<String, Object> serialize() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("attribute", attribute);
            map.put("name", name);
            map.put("amount", amount);
            map.put("operation", operation);
            map.put("slot", slot);
            return map;
        }

        public static AttributeDataModel deserialize(Map<String, Object> map) {
            if (map == null) return null;

            String attribute = String.valueOf(map.getOrDefault("attribute", "")).trim();
            if (attribute.isEmpty()) return null;

            String fallbackName = attribute
                    .replace("minecraft:", "")
                    .replace("generic.", "")
                    .replace(':', '_')
                    .replace('.', '_');

            String name = String.valueOf(map.getOrDefault("name", fallbackName)).trim();

            Object amountObj = map.get("amount");
            double amount = amountObj instanceof Number num ? num.doubleValue() : 0.0D;

            String operation = String.valueOf(map.getOrDefault("operation", "ADD_NUMBER")).trim();
            String slot = String.valueOf(map.getOrDefault("slot", "ANY")).trim();

            return new AttributeDataModel(attribute, name, amount, operation, slot);
        }

        public String getAttribute() {
            return attribute;
        }

        public String getName() {
            return name;
        }

        public double getAmount() {
            return amount;
        }

        public String getOperation() {
            return operation;
        }

        public String getSlot() {
            return slot;
        }
    }

    // ───────────────────────────────────────────
    // Real NBT / PublicBukkitValues support
    // ───────────────────────────────────────────

    private void applyNbt(ItemMeta meta) {
        if (meta == null || nbt == null || nbt.isEmpty()) return;

        Map<String, Object> publicBukkitValues = getPublicBukkitValuesMap();
        if (publicBukkitValues.isEmpty()) return;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        for (Map.Entry<String, Object> entry : publicBukkitValues.entrySet()) {
            NamespacedKey key = parseNamespacedKey(entry.getKey());
            if (key == null) continue;

            setPdcValue(pdc, key, entry.getValue());
        }
    }

    private boolean matchesNbt(ItemMeta meta) {
        if (nbt == null || nbt.isEmpty()) return true;

        Map<String, Object> publicBukkitValues = getPublicBukkitValuesMap();

        /*
         * Keeps backward compatibility with:
         *
         * nbt:
         *   placeholder: true
         *
         * Placeholder-only NBT should not cause a failed match.
         */
        if (publicBukkitValues.isEmpty()) {
            return true;
        }

        if (meta == null) return false;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        for (Map.Entry<String, Object> entry : publicBukkitValues.entrySet()) {
            NamespacedKey key = parseNamespacedKey(entry.getKey());
            if (key == null) return false;

            if (!pdcValueMatches(pdc, key, entry.getValue())) {
                return false;
            }
        }

        return true;
    }

    private Map<String, Object> getPublicBukkitValuesMap() {
        if (nbt == null || nbt.isEmpty()) return Collections.emptyMap();

        Object raw = null;

        for (Map.Entry<String, Object> entry : nbt.entrySet()) {
            if (isPublicBukkitValuesKey(entry.getKey())) {
                raw = entry.getValue();
                break;
            }
        }

        if (!(raw instanceof Map<?, ?> rawMap)) {
            return Collections.emptyMap();
        }

        return sanitizeStringObjectMap(rawMap);
    }

    private static Map<String, Object> sanitizeStringObjectMap(Map<?, ?> rawMap) {
        Map<String, Object> values = new LinkedHashMap<>();

        if (rawMap == null || rawMap.isEmpty()) {
            return values;
        }

        for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) continue;

            String key = String.valueOf(entry.getKey()).trim();
            if (key.isEmpty()) continue;

            values.put(key, entry.getValue());
        }

        return values;
    }

    private static boolean isPublicBukkitValuesKey(String key) {
        if (key == null) return false;

        String normalized = key.trim()
                .replace("-", "")
                .replace("_", "")
                .toLowerCase(Locale.ROOT);

        return normalized.equals("publicbukkitvalues");
    }

    private static NamespacedKey parseNamespacedKey(String raw) {
        if (raw == null) return null;

        String value = raw.trim();
        if (value.isEmpty()) return null;

        String namespace;
        String key;

        int split = value.indexOf(':');

        if (split >= 0) {
            namespace = value.substring(0, split).trim();
            key = value.substring(split + 1).trim();
        } else {
            namespace = "minecraft";
            key = value;
        }

        if (namespace.isEmpty() || key.isEmpty()) {
            return null;
        }

        try {
            return new NamespacedKey(
                    namespace.toLowerCase(Locale.ROOT),
                    key.toLowerCase(Locale.ROOT)
            );
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static void setPdcValue(PersistentDataContainer pdc, NamespacedKey key, Object value) {
        if (pdc == null || key == null || value == null) return;

        if (value instanceof Boolean bool) {
            pdc.set(key, PersistentDataType.BYTE, (byte) (bool ? 1 : 0));
            return;
        }

        if (value instanceof Byte b) {
            pdc.set(key, PersistentDataType.BYTE, b);
            return;
        }

        if (value instanceof Short s) {
            pdc.set(key, PersistentDataType.SHORT, s);
            return;
        }

        if (value instanceof Integer i) {
            pdc.set(key, PersistentDataType.INTEGER, i);
            return;
        }

        if (value instanceof Long l) {
            pdc.set(key, PersistentDataType.LONG, l);
            return;
        }

        if (value instanceof Float f) {
            pdc.set(key, PersistentDataType.FLOAT, f);
            return;
        }

        if (value instanceof Double d) {
            pdc.set(key, PersistentDataType.DOUBLE, d);
            return;
        }

        /*
         * Default behavior for ItemEdit / ItemTag compatibility.
         *
         * Example:
         * PublicBukkitValues:
         *   minecraft:drop_id: stone1-coin
         *
         * This writes a STRING value:
         * minecraft:drop_id = "stone1-coin"
         */
        pdc.set(key, PersistentDataType.STRING, String.valueOf(value));
    }

    private static boolean pdcValueMatches(PersistentDataContainer pdc, NamespacedKey key, Object expected) {
        if (pdc == null || key == null || expected == null) return false;

        if (expected instanceof Boolean bool) {
            Byte actual = pdc.get(key, PersistentDataType.BYTE);
            return actual != null && actual == (byte) (bool ? 1 : 0);
        }

        if (expected instanceof Byte b) {
            Byte actual = pdc.get(key, PersistentDataType.BYTE);
            return Objects.equals(actual, b);
        }

        if (expected instanceof Short s) {
            Short actual = pdc.get(key, PersistentDataType.SHORT);
            return Objects.equals(actual, s);
        }

        if (expected instanceof Integer i) {
            Integer actual = pdc.get(key, PersistentDataType.INTEGER);
            return Objects.equals(actual, i);
        }

        if (expected instanceof Long l) {
            Long actual = pdc.get(key, PersistentDataType.LONG);
            return Objects.equals(actual, l);
        }

        if (expected instanceof Float f) {
            Float actual = pdc.get(key, PersistentDataType.FLOAT);
            return Objects.equals(actual, f);
        }

        if (expected instanceof Double d) {
            Double actual = pdc.get(key, PersistentDataType.DOUBLE);
            return Objects.equals(actual, d);
        }

        String actual = pdc.get(key, PersistentDataType.STRING);
        return Objects.equals(actual, String.valueOf(expected));
    }

    private static Map<String, Object> extractPublicBukkitValues(ItemMeta meta) {
        Map<String, Object> values = new LinkedHashMap<>();

        if (meta == null) return values;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        for (NamespacedKey key : pdc.getKeys()) {
            String configKey = key.getNamespace() + ":" + key.getKey();

            Object value = readKnownPdcValue(pdc, key);
            if (value != null) {
                values.put(configKey, value);
            }
        }

        return values;
    }

    private static Object readKnownPdcValue(PersistentDataContainer pdc, NamespacedKey key) {
        if (pdc == null || key == null) return null;

        String stringValue = pdc.get(key, PersistentDataType.STRING);
        if (stringValue != null) return stringValue;

        Integer integerValue = pdc.get(key, PersistentDataType.INTEGER);
        if (integerValue != null) return integerValue;

        Long longValue = pdc.get(key, PersistentDataType.LONG);
        if (longValue != null) return longValue;

        Double doubleValue = pdc.get(key, PersistentDataType.DOUBLE);
        if (doubleValue != null) return doubleValue;

        Float floatValue = pdc.get(key, PersistentDataType.FLOAT);
        if (floatValue != null) return floatValue;

        Byte byteValue = pdc.get(key, PersistentDataType.BYTE);
        if (byteValue != null) return byteValue;

        Short shortValue = pdc.get(key, PersistentDataType.SHORT);
        if (shortValue != null) return shortValue;

        return null;
    }

    private boolean requiresMeta() {
        if (displayName != null) return true;
        if (lore != null && !lore.isEmpty()) return true;
        if (customModelData != null) return true;
        if (unbreakable != null) return true;
        if (enchantments != null && !enchantments.isEmpty()) return true;
        if (attributes != null && !attributes.isEmpty()) return true;
        if (itemFlags != null && !itemFlags.isEmpty()) return true;
        if (skullTextureBase64 != null) return true;
        if (skullOwnerUUID != null) return true;

        return nbt != null && !getPublicBukkitValuesMap().isEmpty();
    }

    // ───────────────────────────────────────────
    // Getters
    // ───────────────────────────────────────────

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

    public List<AttributeDataModel> getAttributes() {
        return attributes != null ? attributes : Collections.emptyList();
    }

    public boolean isUnbreakable() {
        return unbreakable != null && unbreakable;
    }

    public Map<String, Object> getNbt() {
        return nbt != null ? nbt : Collections.emptyMap();
    }
}