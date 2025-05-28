package org.conquest.conquestCompressor.responseHandler.effectHandler;

import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.conquest.conquestCompressor.ConquestCompressor;

import java.util.Optional;
import java.util.logging.Logger;

/**
 * 🔊 SoundResponseManager
 * Handles playing configured sound effects from message sections.
 */
public class SoundResponseManager {

    private static final Logger log = ConquestCompressor.getInstance().getLogger();

    /**
     * Plays a sound to a player from a given configuration section.
     *
     * @param player Player to receive the sound
     * @param section Section containing `sound.type`, `sound.volume`, `sound.pitch`
     */
    public static void play(Player player, ConfigurationSection section) {
        if (player == null || section == null) return;

        ConfigurationSection soundSection = section.getConfigurationSection("sound");
        if (soundSection == null) return;

        String typeString = soundSection.getString("type");
        if (typeString == null || typeString.isEmpty()) return;

        Optional<Sound> optionalSound = SoundCompatModel.match(typeString);
        if (optionalSound.isEmpty()) return;

        Sound sound = optionalSound.get();

        float volume = (float) soundSection.getDouble("volume", 1.0);
        float pitch = (float) soundSection.getDouble("pitch", 1.0);

        player.playSound(player.getLocation(), sound, SoundCategory.MASTER, volume, pitch);
    }
}
