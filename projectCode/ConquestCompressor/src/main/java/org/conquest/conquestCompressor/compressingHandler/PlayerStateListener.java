package org.conquest.conquestCompressor.compressingHandler;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.conquest.conquestCompressor.commandHandler.subcommandHandler.UserCommands;
import org.conquest.conquestCompressor.configurationHandler.configurationFiles.PlayerToggleStatesFile;

import java.util.Set;
import java.util.UUID;

/**
 * 🧍 PlayerStateListener
 * Handles caching and persistence of player toggle state on join/quit.
 */
public class PlayerStateListener implements Listener {

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        if (!UserCommands.isAutoCompressEnabled(event.getPlayer())) {
            UserCommands.disableAutoCompression(uuid); // 🔄 Save as disabled
        } else {
            UserCommands.enableAutoCompression(uuid); // 🟢 Ensure enabled state is clean
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        Set<UUID> disabled = PlayerToggleStatesFile.loadDisabled();

        if (disabled.contains(uuid)) {
            // 🛑 Player already has compression disabled
            return;
        }

        // ✅ Ensure they're enabled in memory just in case
        UserCommands.enableAutoCompression(uuid);
    }
}
