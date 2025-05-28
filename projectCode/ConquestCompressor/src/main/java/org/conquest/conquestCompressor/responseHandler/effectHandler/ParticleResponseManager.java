package org.conquest.conquestCompressor.responseHandler.effectHandler;

import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.conquest.conquestCompressor.ConquestCompressor;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

/**
 * 💨 ParticleResponseManager
 * Handles spawning particles defined in message config sections.
 */
public class ParticleResponseManager {

    private static final Logger log = ConquestCompressor.getInstance().getLogger();

    /**
     * Spawns all particles defined in a message section for the player.
     *
     * @param player  Player to display particles to
     * @param section The section containing a `particles:` list
     */
    public static void play(Player player, ConfigurationSection section) {
        if (player == null || section == null || !section.isList("particles")) return;

        List<Map<?, ?>> particles = section.getMapList("particles");
        if (particles.isEmpty()) return;

        for (Map<?, ?> particleData : particles) {
            spawnParticle(player, particleData);
        }
    }

    private static void spawnParticle(Player player, Map<?, ?> data) {
        if (!(data.get("type") instanceof String)) {
            log.warning("⚠️  Missing or invalid particle type.");
            return;
        }

        String typeString = data.get("type").toString().toUpperCase(Locale.ROOT);
        Particle particle;

        try {
            particle = Particle.valueOf(typeString);
        } catch (IllegalArgumentException e) {
            log.warning("⚠️  Invalid particle type in config: '" + typeString + "'");
            return;
        }

        int count = parseInt(data.get("count"));
        double speed = parseDouble(data.get("speed"));
        Vector offset = parseOffset(data.get("offset"));

        player.spawnParticle(particle,
                aboveHead(player),
                count,
                offset.getX(), offset.getY(), offset.getZ(),
                speed);
    }

    private static Vector parseOffset(Object raw) {
        if (raw instanceof List<?> list && list.size() == 3) {
            try {
                double x = Double.parseDouble(list.get(0).toString());
                double y = Double.parseDouble(list.get(1).toString());
                double z = Double.parseDouble(list.get(2).toString());
                return new Vector(x, y, z);
            } catch (Exception ignored) {}
        }
        return new Vector(0, 0, 0);
    }

    private static int parseInt(Object value) {
        try {
            return Integer.parseInt(value.toString());
        } catch (Exception ignored) {
            return 1;
        }
    }

    private static double parseDouble(Object value) {
        try {
            return Double.parseDouble(value.toString());
        } catch (Exception ignored) {
            return 0.01;
        }
    }

    private static org.bukkit.Location aboveHead(Player player) {
        return player.getLocation().add(0, 1.0, 0);
    }
}
