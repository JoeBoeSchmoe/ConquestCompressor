package org.conquest.conquestCompressor.configurationHandler.configurationFiles;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.plugin.java.JavaPlugin;
import org.conquest.conquestCompressor.ConquestCompressor;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.*;
import java.util.logging.Logger;

public class CompressorGUIFile {

    private static final ConquestCompressor plugin = ConquestCompressor.getInstance();

    private static final Logger log = plugin.getLogger();
    private static FileConfiguration config;

    private CompressorGUIFile() {
        // Utility class
    }

    private static final List<CompressorButton> buttonCache = new ArrayList<>();

    public static void load() {
        File file = new File(plugin.getDataFolder(), "guiConfiguration/compressorEditorGUI.yml");

        if (!file.exists()) {
            File folder = file.getParentFile();
            if (!folder.exists() && !folder.mkdirs()) {
                log.warning("⚠️  Failed to create folder: " + folder.getAbsolutePath());
            }

            try (InputStream in = plugin.getResource("guiConfiguration/compressorEditorGUI.yml")) {
                if (in != null) {
                    log.info("📄  Created default compressorEditorGUI.yml");
                    Files.copy(in, file.toPath());
                } else {
                    log.warning("⚠️  Embedded compressorEditorGUI.yml not found in JAR.");
                }
            } catch (IOException e) {
                log.severe("❌  Failed to create compressorEditorGUI.yml: " + e.getMessage());
            }
        }

        config = YamlConfiguration.loadConfiguration(file);
        parseButtons();
        log.info("✅  Loaded compressorGUI.yml successfully.");
    }

    private static void parseButtons() {
        buttonCache.clear();

        List<?> layoutList = config.getList("menu.layout");
        if (layoutList == null) return;

        for (Object obj : layoutList) {
            if (!(obj instanceof Map<?, ?> rawMap)) continue;

            try {
                CompressorButton button = new CompressorButton();

                // Safe slot parsing
                Object slotObj = rawMap.get("slot");
                button.slot = (slotObj instanceof Number) ? ((Number) slotObj).intValue() : 0;

                // Safe amount parsing
                Object amountObj = rawMap.get("amount");
                button.amount = (amountObj instanceof Number) ? ((Number) amountObj).intValue() : 1;

                // Other simple fields
                button.action = String.valueOf(rawMap.get("action"));
                button.name = String.valueOf(rawMap.get("name"));
                button.material = Material.valueOf(String.valueOf(rawMap.get("material")).toUpperCase(Locale.ROOT));
                button.enchanted = Boolean.TRUE.equals(rawMap.get("enchanted"));

                // Lore list
                Object loreObj = rawMap.get("lore");
                if (loreObj instanceof List<?>) {
                    List<String> loreList = new ArrayList<>();
                    for (Object line : (List<?>) loreObj) {
                        loreList.add(String.valueOf(line));
                    }
                    button.lore = loreList;
                } else {
                    button.lore = List.of();
                }

                // Custom Data
                Object customDataObj = rawMap.get("customData");
                if (customDataObj instanceof Map<?, ?> customMap) {
                    Map<String, Object> parsedCustom = new HashMap<>();
                    for (Map.Entry<?, ?> entry : customMap.entrySet()) {
                        if (entry.getKey() instanceof String) {
                            parsedCustom.put((String) entry.getKey(), entry.getValue());
                        }
                    }
                    button.customData = parsedCustom;
                } else {
                    button.customData = Map.of();
                }

                buttonCache.add(button);
            } catch (Exception e) {
                log.warning("⚠️  Failed to parse a compressor button: " + e.getMessage());
            }
        }
    }

    public static List<CompressorButton> getButtonCache() {
        return Collections.unmodifiableList(buttonCache);
    }

    public static Map<String, Object> extractCustomDataFromItem(ItemStack item) {
        Map<String, Object> customData = new LinkedHashMap<>();
        if (item == null || !item.hasItemMeta()) return customData;

        ItemMeta meta = item.getItemMeta();

        if (meta.hasCustomModelData()) {
            customData.put("CustomModelData", meta.getCustomModelData());
        }

        if (meta.isUnbreakable()) {
            customData.put("Unbreakable", true);
        }

        if (!meta.getItemFlags().isEmpty()) {
            List<String> flags = new ArrayList<>();
            for (ItemFlag flag : meta.getItemFlags()) {
                flags.add(flag.name());
            }
            customData.put("ItemFlags", flags);
        }

        if (meta instanceof Damageable damageable && damageable.hasDamage()) {
            customData.put("Damage", damageable.getDamage());
        }

        if (!item.getEnchantments().isEmpty()) {
            Map<String, Integer> enchants = new LinkedHashMap<>();
            for (Map.Entry<Enchantment, Integer> entry : item.getEnchantments().entrySet()) {
                enchants.put(entry.getKey().getKey().getKey(), entry.getValue());
            }
            customData.put("Enchantments", enchants);
        }

        return customData;
    }

    public static FileConfiguration getConfig() {
        return config;
    }

    public static void save() {
        File file = new File(plugin.getDataFolder(), "GUIConfiguration/compressorGUI.yml");
        try {
            config.save(file);
            log.info("💾 Saved compressorGUI.yml");
        } catch (IOException e) {
            log.warning("❌ Failed to save compressorGUI.yml: " + e.getMessage());
        }
    }

    public static class CompressorButton {
        public int slot;
        public String action;
        public String name;
        public Material material;
        public int amount;
        public boolean enchanted;
        public List<String> lore;
        public Map<String, Object> customData;
    }
}
