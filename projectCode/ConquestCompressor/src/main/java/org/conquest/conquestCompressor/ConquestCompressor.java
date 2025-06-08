package org.conquest.conquestCompressor;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.conquest.conquestCompressor.commandHandler.CommandManager;
import org.conquest.conquestCompressor.compressingHandler.CompressorListener;
import org.conquest.conquestCompressor.compressingHandler.PlayerStateListener;
import org.conquest.conquestCompressor.configurationHandler.ConfigurationManager;
import org.conquest.conquestCompressor.functionalHandler.compressorHandler.CompressorManager;
import org.conquest.conquestCompressor.guiHandler.guiBuildingHandler.EditGUIListener;
import org.conquest.conquestCompressor.guiHandler.guiBuildingHandler.EditingSessionManager;
import org.conquest.conquestCompressor.guiHandler.guiBuildingHandler.EditorMenuManager;

import java.util.List;
import java.util.Objects;

/**
 * 🧱 ConquestCompressor
 * Main plugin class. Handles lifecycle, configuration, and system initialization.
 */
public final class ConquestCompressor extends JavaPlugin {

    private static ConquestCompressor instance;
    private ConfigurationManager configurationManager;

    @Override
    public void onEnable() {
        instance = this;
        getLogger().info("🔧  Enabling ConquestCompressor...");

        // 🔁 Load config and files
        configurationManager = new ConfigurationManager();
        configurationManager.initialize();

        // 🧠 Load GUI menu metadata
        EditorMenuManager.load();

        // 📜 Register commands
        setupCommands();

        // 🎧 Register listeners
        registerListeners(
                new EditGUIListener(),
                new CompressorListener(),
                new PlayerStateListener()
        );

        // 🧠 Start interval-based compression if enabled
        CompressorListener.initializeAutoCompression();

        getLogger().info("✅  ConquestCompressor enabled successfully.");
    }

    @Override
    public void onDisable() {
        getLogger().info("📦  Saving plugin state...");

        // ❌ Close sessions and clear memory
        EditingSessionManager.closeAllSync();
        EditingSessionManager.clear();

        EditorMenuManager.clear();

        // 🔻 Unregister all event listeners
        HandlerList.unregisterAll(this);

        // 🛑 Cancel scheduled tasks (like interval compression)
        Bukkit.getScheduler().cancelTasks(this);

        getLogger().info("🔻  ConquestCompressor has been disabled.");
    }

    /**
     * Reloads configuration and editor menu metadata.
     */
    public void reload() {
        getLogger().info("🔄  Reloading ConquestCompressor...");

        // ❌ Reset GUI sessions
        EditingSessionManager.closeAll();
        EditingSessionManager.clear();

        // ♻️ Re-initialize configuration and GUI menus
        configurationManager.initialize();
        EditorMenuManager.reload();

        // 🔁 Reload compression recipes
        CompressorManager.clear();
        CompressorManager.load();

        // 🔻 Unregister all listeners
        HandlerList.unregisterAll(this);

        // 🛑 Cancel all scheduled tasks (auto-compress task included)
        Bukkit.getScheduler().cancelTasks(this);

        // 🎧 Re-register fresh listener instances
        registerListeners(
                new EditGUIListener(),
                new CompressorListener()
        );

        // 🔁 Restart interval compression logic with updated config
        CompressorListener.resetAutoCompression();

        getLogger().info("✅  Reload complete.");
    }
    /**
     * Registers main command and aliases.
     */
    private void setupCommands() {
        CommandManager commandManager = new CommandManager();
        PluginCommand command = getCommand("conquestcompressor");

        if (command == null) {
            getLogger().severe("❌  Command 'conquestcompressor' not found in plugin.yml!");
            return;
        }

        command.setExecutor(commandManager);
        command.setTabCompleter(commandManager);

        List<String> aliases = getConfig().getStringList("command-aliases");
        if (!aliases.isEmpty()) {
            getLogger().info("🔗  Registered aliases: " + String.join(", ", aliases));
        }
    }

    /**
     * Registers plugin event listeners.
     */
    private void registerListeners(Listener... listeners) {
        for (Listener listener : listeners) {
            getServer().getPluginManager().registerEvents(listener, this);
        }

        // Add listeners here as needed
        // registerListeners(new CompressorEditorListener());
    }

    public static ConquestCompressor getInstance() {
        return instance;
    }

    public ConfigurationManager getConfigurationManager() {
        return configurationManager;
    }
}
