package org.conquest.conquestCompressor.guiHandler.guiBuildingHandler.guiMenuModels;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.conquest.conquestCompressor.ConquestCompressor;
import org.conquest.conquestCompressor.responseHandler.effectHandler.SoundCompatModel;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * 🎧 EffectModel
 *
 * Represents a composite effect of sound + particles triggered by UI interaction.
 */
public class EffectModel {

    private final String soundType;
    private final float volume;
    private final float pitch;
    private final List<ParticleEffectModel> particles;

    public EffectModel(String soundType, float volume, float pitch, List<ParticleEffectModel> particles) {
        this.soundType = soundType;
        this.volume = volume;
        this.pitch = pitch;
        this.particles = particles;
    }

    public String getSoundType() {
        return soundType;
    }

    public float getVolume() {
        return volume;
    }

    public float getPitch() {
        return pitch;
    }

    public List<ParticleEffectModel> getParticles() {
        return particles;
    }

    /**
     * 🚀 Plays the effect at a location for the specified player.
     */
    public void play(Location location, Player player) {
        // 🔊 Play sound with compatibility fallback
        if (soundType != null && !soundType.isEmpty()) {
            Optional<Sound> optionalSound = SoundCompatModel.match(soundType);
            if (optionalSound.isPresent()) {
                player.playSound(location, optionalSound.get(), volume, pitch);
            } else {
                ConquestCompressor.getInstance().getLogger().warning("[ConquestDuels] ⚠️  Unknown or incompatible sound: '" + soundType + "'");
            }
        }

        // 💨 Spawn particles
        for (ParticleEffectModel model : particles) {
            try {
                Particle particle = Particle.valueOf(model.getType().toUpperCase(Locale.ROOT));
                player.getWorld().spawnParticle(
                        particle,
                        location.clone().add(0.5, 1.2, 0.5),
                        model.getCount(),
                        model.getOffsetX(),
                        model.getOffsetY(),
                        model.getOffsetZ(),
                        model.getSpeed()
                );
            } catch (IllegalArgumentException e) {
                ConquestCompressor.getInstance().getLogger().warning("[ConquestDuels] ⚠️  Invalid particle type: '" + model.getType() + "'");
            }
        }
    }


    /**
     * 🚀 Shortcut to play effect at player's location.
     */
    public void play(Player player) {
        play(player.getLocation(), player);
    }

    /**
     * 💨 ParticleEffectModel
     * Represents a single particle burst configuration inside an effect.
     */
    public static class ParticleEffectModel {
        private final String type;
        private final int count;
        private final float offsetX;
        private final float offsetY;
        private final float offsetZ;
        private final float speed;

        public ParticleEffectModel(String type, int count, float offsetX, float offsetY, float offsetZ, float speed) {
            this.type = type;
            this.count = count;
            this.offsetX = offsetX;
            this.offsetY = offsetY;
            this.offsetZ = offsetZ;
            this.speed = speed;
        }

        public String getType() { return type; }
        public int getCount() { return count; }
        public float getOffsetX() { return offsetX; }
        public float getOffsetY() { return offsetY; }
        public float getOffsetZ() { return offsetZ; }
        public float getSpeed() { return speed; }
    }
}
