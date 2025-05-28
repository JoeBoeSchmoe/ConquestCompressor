package org.conquest.conquestCompressor.commandHandler.permissionHandler;

import org.bukkit.command.CommandSender;

/**
 * 🔐 PermissionManager
 * Checks player permissions using {@link PermissionModels}.
 */
public class PermissionManager {

    /**
     * Checks if sender has permission or is op.
     *
     * @param sender     The command sender
     * @param permission The permission to check
     * @return true if allowed
     */
    public static boolean has(CommandSender sender, PermissionModels permission) {
        return sender.isOp() || sender.hasPermission(permission.getNode());
    }

}
