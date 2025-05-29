package org.conquest.conquestCompressor.guiHandler.guiBuildingHandler.guiUtilites;

import org.bukkit.configuration.ConfigurationSection;
import org.conquest.conquestCompressor.guiHandler.guiBuildingHandler.guiMenuModels.EffectModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 🎆 EffectModelParser
 *
 * Parses a full EffectModel from a ConfigurationSection.
 * Includes sound and optional particle effects using the nested ParticleEffectModel.
 */
public class EffectModelParser {

    public static EffectModel parseEffect(ConfigurationSection baseSection) {
        if (baseSection == null) return null;

        ConfigurationSection soundSec = baseSection.getConfigurationSection("sound");
        List<Map<?, ?>> rawParticles = baseSection.getMapList("particles");

        if (soundSec == null && rawParticles.isEmpty()) {
            return null;
        }

        String soundType = "BLOCK_NOTE_BLOCK_BASS";
        float volume = 1.0f;
        float pitch = 1.0f;

        if (soundSec != null) {
            soundType = soundSec.getString("type", soundType);
            volume = (float) soundSec.getDouble("volume", volume);
            pitch = (float) soundSec.getDouble("pitch", pitch);
        }

        List<EffectModel.ParticleEffectModel> particles = new ArrayList<>();
        for (Map<?, ?> p : rawParticles) {
            String type = String.valueOf(p.get("type")).toUpperCase(Locale.ROOT);
            int count = (p.get("count") instanceof Number n) ? n.intValue() : 10;
            float offsetX = (p.get("offsetX") instanceof Number n) ? n.floatValue() : 0f;
            float offsetY = (p.get("offsetY") instanceof Number n) ? n.floatValue() : 0f;
            float offsetZ = (p.get("offsetZ") instanceof Number n) ? n.floatValue() : 0f;
            float speed = (p.get("speed") instanceof Number n) ? n.floatValue() : 0f;

            particles.add(new EffectModel.ParticleEffectModel(type, count, offsetX, offsetY, offsetZ, speed));
        }

        return new EffectModel(soundType, volume, pitch, particles);
    }
}
