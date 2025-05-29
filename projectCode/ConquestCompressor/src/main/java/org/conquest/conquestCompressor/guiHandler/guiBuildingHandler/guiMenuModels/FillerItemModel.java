package org.conquest.conquestCompressor.guiHandler.guiBuildingHandler.guiMenuModels;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 🧱 FillerItemModel
 * Represents a static filler item (like background panes), parsed once during GUI bootstrap.
 */
public class FillerItemModel {

    private final String material;
    private final String name;
    private final int amount;
    private final List<String> lore;
    private final boolean enchanted;
    private final Map<String, Object> customData;
    private final EffectModel effect;

    public FillerItemModel(
            String material,
            String name,
            int amount,
            List<String> lore,
            boolean enchanted,
            Map<String, Object> customData,
            EffectModel effect
    ) {
        this.material = material;
        this.name = name;
        this.amount = amount <= 0 ? 1 : amount;
        this.lore = lore;
        this.enchanted = enchanted;
        this.customData = customData;
        this.effect = effect;
    }

    public ItemStack toItemStack() {
        Material mat = Material.matchMaterial(material.toUpperCase(Locale.ROOT));
        if (mat == null) mat = Material.BARRIER;

        ItemStack item = new ItemStack(mat, amount);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        MiniMessage mini = MiniMessage.miniMessage();

        if (name != null && !name.isBlank()) {
            meta.displayName(mini.deserialize(name));
        }

        if (lore != null && !lore.isEmpty()) {
            List<Component> parsedLore = lore.stream()
                    .map(mini::deserialize)
                    .map(line -> line.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE))
                    .toList();
            meta.lore(parsedLore);
        }

        if (enchanted) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        }

        // 💎 Hide item info
        meta.addItemFlags(
                ItemFlag.HIDE_ATTRIBUTES,
                ItemFlag.HIDE_ENCHANTS,
                ItemFlag.HIDE_UNBREAKABLE,
                ItemFlag.HIDE_DESTROYS,
                ItemFlag.HIDE_PLACED_ON
        );

        // 🧪 Optional dummy enchant to trigger flags
        if (!meta.hasEnchants() && enchanted) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        }

        // 🧬 Apply customData (NB: you likely apply this via ItemBuilder — this is a stub if needed)
        // CustomNBTUtil.apply(meta, customData); ← if implemented

        item.setItemMeta(meta);
        return item;
    }

    public String getMaterial() {
        return material;
    }

    public String getName() {
        return name;
    }

    public int getAmount() {
        return amount;
    }

    public List<String> getLore() {
        return lore;
    }

    public boolean isEnchanted() {
        return enchanted;
    }

    public Map<String, Object> getCustomData() {
        return customData;
    }

    public EffectModel getEffect() {
        return effect;
    }
}
