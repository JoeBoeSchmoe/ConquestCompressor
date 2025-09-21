package org.conquest.conquestCompressor.functionalHandler.compressorHandler;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.conquest.conquestCompressor.ConquestCompressor;
import org.conquest.conquestCompressor.functionalHandler.ItemDataModel;

import java.util.*;

/**
 * Represents a single click-compressor item definition (from gameCompressorItems.yml).
 *
 * Schema (per key under compressorItems):
 *   enabled: boolean
 *   item:
 *     material: STRING
 *     itemData: { displayName, lore[], customModelData, unbreakable, enchantments[], nbt{}, itemFlags[], skullTexture, skullOwnerUUID }
 *   trigger:
 *     leftClick: boolean
 *     rightClick: boolean
 *     cooldownTicks: int
 *     consumeOnUse: boolean
 *   recipes: [STRING,...]
 *
 * Notes:
 * - Item creation/matching delegates to ItemDataModel (consistent with CompressorModel.buildOutputItem()).
 * - Adds PDC marker + compressor_key for robust recognition.
 * - Stack size (amount) is intentionally ignored; items are identified by type/meta only.
 */
public class CompressorItemModel {

    // --- PDC keys (static, match manager) ---
    private static final NamespacedKey PDC_MARK =
            new NamespacedKey(ConquestCompressor.getInstance(), "is_compressor_item");
    private static final NamespacedKey PDC_KEY =
            new NamespacedKey(ConquestCompressor.getInstance(), "compressor_key");

    private final String key;
    private final boolean enabled;

    // Item template
    private final Material material;
    private final ItemDataModel itemData; // nullable

    // Trigger
    private final boolean leftClick;
    private final boolean rightClick;
    private final int cooldownTicks;
    private final boolean consumeOnUse;

    // Recipes
    private final List<String> recipeKeys;

    public CompressorItemModel(
            String key,
            boolean enabled,
            Material material,
            ItemDataModel itemData,
            boolean leftClick,
            boolean rightClick,
            int cooldownTicks,
            boolean consumeOnUse,
            List<String> recipeKeys
    ) {
        this.key = Objects.requireNonNull(key, "key");
        this.enabled = enabled;
        this.material = Objects.requireNonNull(material, "material");
        this.itemData = itemData;
        this.leftClick = leftClick;
        this.rightClick = rightClick;
        this.cooldownTicks = Math.max(0, cooldownTicks);
        this.consumeOnUse = consumeOnUse;
        this.recipeKeys = List.copyOf(Objects.requireNonNullElseGet(recipeKeys, List::of));
    }

    public static CompressorItemModel fromConfig(String key, ConfigurationSection section) {
        boolean enabled = section.getBoolean("enabled", true);

        ConfigurationSection itemSec = require(section.getConfigurationSection("item"),
                "Missing 'item' for compressorItems." + key);

        String matName = itemSec.getString("material", "STONE");
        Material material = Material.matchMaterial(matName);
        if (material == null) {
            throw new IllegalArgumentException("Unknown material '" + matName + "' for compressorItems." + key);
        }

        // amount is intentionally ignored (backward-compat tolerant if present)

        // Deserialize ItemDataModel from nested map
        ItemDataModel dataModel = null;
        if (itemSec.isConfigurationSection("itemData")) {
            ConfigurationSection dataSec = itemSec.getConfigurationSection("itemData");
            if (dataSec != null) {
                dataModel = ItemDataModel.deserialize(toPlainMap(dataSec));
            }
        }

        ConfigurationSection trig = require(section.getConfigurationSection("trigger"),
                "Missing 'trigger' for compressorItems." + key);
        boolean leftClick = trig.getBoolean("leftClick", true);
        boolean rightClick = trig.getBoolean("rightClick", true);
        int cooldownTicks = trig.getInt("cooldownTicks", 0);
        boolean consumeOnUse = trig.getBoolean("consumeOnUse", false);

        List<String> recipes = section.getStringList("recipes");
        if (recipes == null || recipes.isEmpty()) {
            throw new IllegalStateException("compressorItems." + key + " requires at least one recipe under 'recipes:'");
        }

        return new CompressorItemModel(
                key, enabled, material, dataModel,
                leftClick, rightClick, cooldownTicks, consumeOnUse, recipes
        );
    }

    /**
     * Builds the in-game item using ItemDataModel, then injects PDC identifiers.
     * Stack size is not set here (defaults to 1) because amount is irrelevant.
     */
    public ItemStack buildItem() {
        ItemStack stack = (itemData != null)
                ? itemData.toItemStack(material)
                : new ItemStack(material); // default amount 1

        // Inject PDC
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.set(PDC_MARK, PersistentDataType.BYTE, (byte) 1);
            pdc.set(PDC_KEY, PersistentDataType.STRING, key);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    /**
     * Matches an arbitrary stack to this model.
     * 1) Prefer PDC match (fast & precise).
     * 2) Fallback: require Material + ItemDataModel.matches(...) if itemData is defined.
     *    If no itemData, require a "plain" stack of this material.
     * Stack size is never considered.
     */
    public boolean matches(ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR) return false;
        if (stack.getType() != this.material) return false;

        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            Byte marker = pdc.get(PDC_MARK, PersistentDataType.BYTE);
            String k = pdc.get(PDC_KEY, PersistentDataType.STRING);
            if (marker != null && marker == (byte) 1 && key.equals(k)) {
                return true;
            }
        }

        if (itemData != null) {
            return itemData.matches(stack);
        } else {
            return ItemDataModel.strictMatches(stack, this.material, null);
        }
    }

    // --- getters used by file/manager code ---
    public String key() { return key; }
    public boolean enabled() { return enabled; }
    public Material material() { return material; }
    public Optional<ItemDataModel> itemData() { return Optional.ofNullable(itemData); }
    public boolean leftClick() { return leftClick; }
    public boolean rightClick() { return rightClick; }
    public int cooldownTicks() { return cooldownTicks; }
    public boolean consumeOnUse() { return consumeOnUse; }
    public List<String> recipeKeys() { return recipeKeys; }

    // --- helpers ---
    private static ConfigurationSection require(ConfigurationSection sec, String msg) {
        if (sec == null) throw new IllegalStateException(msg);
        return sec;
    }

    private static Map<String, Object> toPlainMap(ConfigurationSection section) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (String k : section.getKeys(false)) {
            Object v = section.get(k);
            if (v instanceof ConfigurationSection cs) {
                map.put(k, toPlainMap(cs));
            } else {
                map.put(k, v);
            }
        }
        return map;
    }

    @Override
    public String toString() {
        return "CompressorItemModel{" +
                "key='" + key + '\'' +
                ", material=" + material +
                ", recipes=" + recipeKeys +
                '}';
    }
}
