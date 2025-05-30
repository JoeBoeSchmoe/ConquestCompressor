package org.conquest.conquestCompressor.commandHandler.permissionHandler;

import org.bukkit.command.CommandSender;

/**
 * 🔐 PermissionManager
 * Checks player permissions using {@link PermissionModels}.
 */
public class PermissionManager {

    /**
     * Checks if sender has the permission directly or through a wildcard parent.
     *
     * @param sender     The command sender
     * @param permission The permission to check
     * @return true if allowed
     */
    public static boolean has(CommandSender sender, PermissionModels permission) {
        if (sender.isOp()) return true;

        String node = permission.getNode();

        // Direct permission check
        if (sender.hasPermission(node)) return true;

        // Check wildcard parents
        String[] parts = node.split("\\.");
        StringBuilder current = new StringBuilder();

        for (int i = 0; i < parts.length - 1; i++) {
            if (i > 0) current.append(".");
            current.append(parts[i]);

            if (sender.hasPermission(current + ".*")) {
                return true;
            }
        }

        return false;
    }
}
