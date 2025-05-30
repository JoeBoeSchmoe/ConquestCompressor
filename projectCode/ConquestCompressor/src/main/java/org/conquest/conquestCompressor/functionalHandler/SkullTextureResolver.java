package org.conquest.conquestCompressor.functionalHandler;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.conquest.conquestCompressor.ConquestCompressor;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 🎭 SkullTextureResolver
 * Applies and extracts Base64 skin textures from PLAYER_HEADs using Paper's PlayerProfile.
 */
public class SkullTextureResolver {

    /**
     * Applies a Base64 texture to a PLAYER_HEAD using Paper API.
     *
     * @param item   ItemStack of type PLAYER_HEAD
     * @param base64 The Base64-encoded skin texture
     */
    public static void applyTexture(ItemStack item, String base64) {
        if (item == null || base64 == null) return;
        if (!(item.getItemMeta() instanceof SkullMeta meta)) return;

        try {
            UUID uuid = UUID.nameUUIDFromBytes(base64.getBytes(StandardCharsets.UTF_8));
            PlayerProfile profile = Bukkit.createProfile(uuid);
            profile.getProperties().add(new ProfileProperty("textures", base64));
            meta.setPlayerProfile(profile);
            item.setItemMeta(meta);
        } catch (Exception e) {
            ConquestCompressor.getInstance().getLogger().warning("❌ Failed to apply skull texture: " + e.getMessage());
        }
    }

    /**
     * Extracts Base64 texture from SkullMeta using Paper's PlayerProfile API or fallback parsing.
     *
     * @param meta SkullMeta to inspect
     * @return base64 texture string or null
     */
    public static String extractTextureFromMeta(SkullMeta meta) {
        try {
            PlayerProfile profile = meta.getPlayerProfile();
            if (profile != null) {
                for (ProfileProperty property : profile.getProperties()) {
                    if ("textures".equals(property.getName())) {
                        return property.getValue();
                    }
                }
            }
        } catch (Exception e) {
            ConquestCompressor.getInstance().getLogger().warning("⚠️ Failed to read texture from PlayerProfile: " + e.getMessage());
        }

        // Fallback parsing (legacy or plugin-added skulls)
        try {
            Map<String, Object> serialized = meta.serialize();
            Object skullOwner = serialized.get("skull-owner");

            if (skullOwner != null) {
                String ownerString = skullOwner.toString();
                Matcher matcher = Pattern.compile("value=([a-zA-Z0-9+/=]{100,})").matcher(ownerString);
                if (matcher.find()) return matcher.group(1);
            }

            if (skullOwner instanceof Map<?, ?> ownerMap) {
                Object props = ownerMap.get("properties");
                if (props instanceof Map<?, ?> propsMap) {
                    Object textures = propsMap.get("textures");
                    if (textures instanceof List<?> list) {
                        for (Object o : list) {
                            if (o instanceof Map<?, ?> m && m.get("value") instanceof String base64) {
                                return base64;
                            }
                        }
                    }
                }
            }

        } catch (Exception e) {
            ConquestCompressor.getInstance().getLogger().warning("⚠️ Fallback texture parse failed: " + e.getMessage());
        }

        ConquestCompressor.getInstance().getLogger().warning("⚠️ No skull texture found in extractTextureFromMeta()");
        return null;
    }
}
