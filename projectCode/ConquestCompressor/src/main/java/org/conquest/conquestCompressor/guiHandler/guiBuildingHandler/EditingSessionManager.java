package org.conquest.conquestCompressor.guiHandler.guiBuildingHandler;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.conquest.conquestCompressor.ConquestCompressor;

import java.util.*;

/**
 * 🧠 EditingSessionManager
 * Manages EditingSessions for each player interacting with edit GUIs.
 */
public class EditingSessionManager {

    private static final Map<UUID, EditingSession> sessions = new HashMap<>();
    private static final long SESSION_TIMEOUT = 60_000L; // 60 seconds

    // ─────────────────────────────────────────
    // 🎮 Session Lifecycle
    // ─────────────────────────────────────────

    public static EditingSession getOrCreate(Player player) {
        return sessions.computeIfAbsent(player.getUniqueId(), EditingSession::new);
    }

    public static Optional<EditingSession> get(Player player) {
        return Optional.ofNullable(sessions.get(player.getUniqueId()));
    }

    public static void remove(UUID playerId) {
        sessions.remove(playerId);
    }

    public static void clear() {
        sessions.clear();
    }

    public static Map<UUID, EditingSession> getAllSessions() {
        return Collections.unmodifiableMap(sessions);
    }

    public static boolean hasSession(Player player) {
        return sessions.containsKey(player.getUniqueId());
    }

    // ─────────────────────────────────────────
    // ⏱️ Timeout & Cleanup
    // ─────────────────────────────────────────

    public static void tickCleanup() {
        long now = System.currentTimeMillis();
        sessions.entrySet().removeIf(e -> e.getValue().isExpired(SESSION_TIMEOUT));
    }

    /**
     * 🔒 Closes all inventories of players in editing sessions.
     */
    public static void closeAll() {
        for (UUID uuid : sessions.keySet()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                Bukkit.getScheduler().runTask(ConquestCompressor.getInstance(), () -> {
                    player.closeInventory(); // ✅ Explicit no-arg method
                });
            }
        }
    }

    public static void expireInactiveSessions(long timeoutMillis) {
        long now = System.currentTimeMillis();
        Set<UUID> expired = new HashSet<>();

        for (Map.Entry<UUID, EditingSession> entry : sessions.entrySet()) {
            EditingSession session = entry.getValue();
            if (session.wasClosed()) continue;
            if (now - session.getLastEditTime() > timeoutMillis) {
                expired.add(entry.getKey());
            }
        }

        for (UUID uuid : expired) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                Bukkit.getScheduler().runTask(ConquestCompressor.getInstance(), () -> {
                    player.closeInventory(); // Disambiguated call
                });
            }
            sessions.remove(uuid);
        }
    }
}
