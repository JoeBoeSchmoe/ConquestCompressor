package org.conquest.conquestCompressor.commandHandler.subcommandHandler;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.conquest.conquestCompressor.commandHandler.permissionHandler.PermissionManager;
import org.conquest.conquestCompressor.commandHandler.permissionHandler.PermissionModels;
import org.conquest.conquestCompressor.configurationHandler.configurationFiles.PlayerToggleStatesFile;
import org.conquest.conquestCompressor.responseHandler.MessageResponseManager;
import org.conquest.conquestCompressor.responseHandler.messageModels.UserMessageModels;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 🎮 UserCommands
 * Handles all non-admin /compressor subcommands.
 */
public class UserCommands {

    // In-memory toggle state
    private static final Set<UUID> disabledAutoCompression = new HashSet<>(PlayerToggleStatesFile.loadDisabled());

    public static boolean handle(Player player, String subcommand, String[] args) {
        switch (subcommand) {
            case "help":
                return handleHelp(player);
            case "toggle":
                return handleToggle(player);
            default:
                MessageResponseManager.send(player, UserMessageModels.UNKNOWN_COMMAND);
                return true;
        }
    }

    private static boolean handleHelp(Player player) {
        if (!PermissionManager.has(player, PermissionModels.USER_HELP)) {
            MessageResponseManager.send(player, UserMessageModels.NOT_PLAYER);
            return true;
        }

        MessageResponseManager.sendUserHelpPage(player, "user-help", 1);
        return true;
    }

    private static boolean handleToggle(Player player) {
        if (!PermissionManager.has(player, PermissionModels.USER_TOGGLE)) {
            MessageResponseManager.send(player, UserMessageModels.NOT_PLAYER);
            return true;
        }

        UUID uuid = player.getUniqueId();
        boolean wasDisabled = disabledAutoCompression.contains(uuid);

        if (wasDisabled) {
            enableAutoCompression(uuid);
            MessageResponseManager.send(player, UserMessageModels.TOGGLE_ON);
        } else {
            disableAutoCompression(uuid);
            MessageResponseManager.send(player, UserMessageModels.TOGGLE_OFF);
        }

        return true;
    }

    public static boolean isAutoCompressEnabled(Player player) {
        return !disabledAutoCompression.contains(player.getUniqueId());
    }

    public static void disableAutoCompression(UUID uuid) {
        disabledAutoCompression.add(uuid);
        PlayerToggleStatesFile.saveDisabled(disabledAutoCompression);
    }

    public static void enableAutoCompression(UUID uuid) {
        disabledAutoCompression.remove(uuid);
        PlayerToggleStatesFile.saveDisabled(disabledAutoCompression);
    }

    public static boolean sendNotPlayer(CommandSender sender) {
        MessageResponseManager.send((Player) sender, UserMessageModels.NOT_PLAYER);
        return true;
    }

    public static boolean sendUsageHint(Player player) {
        if (PermissionManager.has(player, PermissionModels.USER_HELP)) {
            MessageResponseManager.send(player, UserMessageModels.USAGE_HINT);
        } else {
            MessageResponseManager.send(player, UserMessageModels.NOT_PLAYER);
        }
        return true;
    }
}
