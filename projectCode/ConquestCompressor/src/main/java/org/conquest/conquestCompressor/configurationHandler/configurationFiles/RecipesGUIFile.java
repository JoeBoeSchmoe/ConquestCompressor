package org.conquest.conquestCompressor.configurationHandler.configurationFiles;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.conquest.conquestCompressor.ConquestCompressor;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.*;
import java.util.logging.Logger;

public class RecipesGUIFile {

    private static final ConquestCompressor plugin = ConquestCompressor.getInstance();

    private static final Logger log = plugin.getLogger();

    private static FileConfiguration config;

    private static final List<RecipeEditorButton> buttonCache = new ArrayList<>();

    private RecipesGUIFile() {
        // Utility class
    }

    public static void load() {
        File file = new File(plugin.getDataFolder(), "guiConfiguration/recipeEditorGUI.yml");

        if (!file.exists()) {
            File folder = file.getParentFile();
            if (!folder.exists() && !folder.mkdirs()) {
                log.warning("⚠️ Failed to create folder: " + folder.getAbsolutePath());
            }

            try (InputStream in = plugin.getResource("guiConfiguration/recipeEditorGUI.yml")) {
                if (in != null) {
                    log.info("📄 Created default recipeEditorGUI.yml");
                    Files.copy(in, file.toPath());
                } else {
                    log.warning("⚠️ Embedded recipeEditorGUI.yml not found in JAR.");
                }
            } catch (IOException e) {
                log.severe("❌ Failed to create recipeEditorGUI.yml: " + e.getMessage());
            }
        }

        config = YamlConfiguration.loadConfiguration(file);
        parseButtons();
        log.info("✅ Loaded recipeEditorGUI.yml successfully.");
    }

    private static void parseButtons() {
        buttonCache.clear();

        List<?> layoutList = config.getList("menu.layout");
        if (layoutList == null) return;

        for (Object obj : layoutList) {
            if (!(obj instanceof Map<?, ?> rawMap)) continue;

            try {
                RecipeEditorButton button = new RecipeEditorButton();

                Object slotObj = rawMap.get("slot");
                button.slot = (slotObj instanceof Number) ? ((Number) slotObj).intValue() : 0;

                Object amountObj = rawMap.get("amount");
                button.amount = (amountObj instanceof Number) ? ((Number) amountObj).intValue() : 1;

                button.action = String.valueOf(rawMap.get("action"));
                button.name = String.valueOf(rawMap.get("name"));
                button.material = Material.valueOf(String.valueOf(rawMap.get("material")).toUpperCase(Locale.ROOT));
                button.enchanted = Boolean.TRUE.equals(rawMap.get("enchanted"));

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
                log.warning("⚠️ Failed to parse a recipe button: " + e.getMessage());
            }
        }
    }

    public static List<RecipeEditorButton> getButtonCache() {
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
        File file = new File(plugin.getDataFolder(), "GUIConfiguration/recipeEditorGUI.yml");
        try {
            config.save(file);
            log.info("💾 Saved recipeEditorGUI.yml");
        } catch (IOException e) {
            log.warning("❌ Failed to save recipeEditorGUI.yml: " + e.getMessage());
        }
    }

    public static class RecipeEditorButton {
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
