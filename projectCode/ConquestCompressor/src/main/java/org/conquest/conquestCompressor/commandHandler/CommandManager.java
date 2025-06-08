package org.conquest.conquestCompressor.commandHandler;

import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.conquest.conquestCompressor.commandHandler.subcommandHandler.AdminCommands;
import org.conquest.conquestCompressor.commandHandler.subcommandHandler.UserCommands;
import org.conquest.conquestCompressor.cooldownHandler.CommandCooldownManager;
import org.conquest.conquestCompressor.responseHandler.MessageResponseManager;
import org.conquest.conquestCompressor.responseHandler.messageModels.AdminMessageModels;
import org.conquest.conquestCompressor.responseHandler.messageModels.UserMessageModels;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * 🧭 CommandManager
 * Central executor and tab completer for /conquestCompressor and its aliases.
 * Routes subcommands to AdminCommands and UserCommands based on alias map and permission level.
 */
public class CommandManager implements CommandExecutor, TabCompleter {

    private static final Map<String, String> ALIAS_MAP = new HashMap<>();

    static {
        registerAliases();
    }

    private static void registerAliases() {
        // Core commands
        ALIAS_MAP.put("help", "help");
        ALIAS_MAP.put("h", "help");

        ALIAS_MAP.put("toggle", "toggle");
        ALIAS_MAP.put("t", "toggle");

        ALIAS_MAP.put("give", "give");
        ALIAS_MAP.put("g", "give");

        ALIAS_MAP.put("info", "info");
        ALIAS_MAP.put("i", "info");

        // Admin group
        ALIAS_MAP.put("admin", "admin");

        // Utility
        ALIAS_MAP.put("reload", "reload");
        ALIAS_MAP.put("r", "reload");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            return UserCommands.sendNotPlayer(sender);
        }

        if (CommandCooldownManager.isOnCooldown(player.getUniqueId())) {
            MessageResponseManager.send(player, UserMessageModels.COMMAND_ON_COOLDOWN);
            return true;
        }

        CommandCooldownManager.mark(player.getUniqueId());

        if (args.length == 0) {
            return UserCommands.sendUsageHint(player);
        }

        String input = args[0].toLowerCase();
        String subcommand = ALIAS_MAP.getOrDefault(input, null);

        if (subcommand == null) {
            MessageResponseManager.send(player, AdminMessageModels.ADMIN_USAGE_HINT);
            return true;
        }

        if (subcommand.equals("admin")) {
            return handleAdmin(player, args);
        }

        return UserCommands.handle(player, subcommand, args);
    }

    private boolean handleAdmin(Player player, String[] args) {
        if (args.length < 2) {
            MessageResponseManager.send(player, AdminMessageModels.ADMIN_USAGE_HINT);
            return true;
        }

        return AdminCommands.handle(player, args);
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        return AutoTabManager.getSuggestions(sender, args);
    }
}
