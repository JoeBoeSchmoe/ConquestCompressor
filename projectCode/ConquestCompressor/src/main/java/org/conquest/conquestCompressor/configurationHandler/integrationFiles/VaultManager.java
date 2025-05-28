package org.conquest.conquestCompressor.configurationHandler.integrationFiles;

import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.conquest.conquestCompressor.ConquestCompressor;

import java.util.logging.Logger;

/**
 * 💰 SafeVaultHook - safely hooks into Vault only if present
 */
public class VaultManager {

    private static Object economy = null;
    private static boolean usingVault = false;

    public static void initialize(boolean configEnabled) {
        Logger log = ConquestCompressor.getInstance().getLogger();

        if (!configEnabled) {
            log.info("💰  Vault disabled via config.");
            return;
        }

        if (!Bukkit.getPluginManager().isPluginEnabled("Vault")) {
            log.warning("⚠️  Vault plugin is missing.");
            return;
        }

        try {
            Class<?> econClass = Class.forName("net.milkbowl.vault.economy.Economy");
            RegisteredServiceProvider<?> provider = Bukkit.getServicesManager().getRegistration(econClass);

            if (provider == null) {
                log.warning("⚠️  Vault found, but no economy provider registered.");
                return;
            }

            economy = provider.getProvider();
            usingVault = true;
            log.info("✅  Vault hooked with provider: " + provider.getProvider().getClass().getSimpleName());

        } catch (ClassNotFoundException e) {
            log.severe("❌  Vault classes not found. Is it installed?");
        } catch (Exception e) {
            log.severe("❌  Unexpected error while hooking Vault: " + e.getMessage());
        }
    }

    public static boolean isUsingVault() {
        return usingVault;
    }

    public static Object getEconomy() {
        return economy;
    }
}
