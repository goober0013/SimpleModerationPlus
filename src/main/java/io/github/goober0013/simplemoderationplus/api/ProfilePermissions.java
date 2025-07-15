package io.github.goober0013.simplemoderationplus.api;

import com.destroystokyo.paper.profile.PlayerProfile;
import io.github.goober0013.simplemoderationplus.SimpleModerationPlus;
import io.github.goober0013.simplemoderationplus.api.MessageProperties;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

/**
 * API class for checking permissions on PlayerProfiles using Vault or fallback to operator status.
 */
public class ProfilePermissions {

    private static Permission vaultPerm;
    private static boolean vaultEnabled;

    /**
     * Initializes the permissions hook. Detects whether Vault is present.
     * Must be called during plugin enable.
     */
    public static final void load() {
        if (!SimpleModerationPlus.pluginManager.isPluginEnabled("Vault")) {
            vaultEnabled = false;
            vaultPerm = null;
            return;
        }
        RegisteredServiceProvider<Permission> rsp =
            SimpleModerationPlus.servicesManager.getRegistration(
                Permission.class
            );
        if (rsp != null) {
            vaultPerm = rsp.getProvider();
            vaultEnabled = true;
            SimpleModerationPlus.logger.info(
                MessageProperties.get("log.enable_vault")
            );
        } else {
            vaultEnabled = false;
            SimpleModerationPlus.logger.info(
                MessageProperties.get("log.no_vault")
            );
        }
    }

    /**
     * Checks whether the given PlayerProfile has the specified permission.
     * If Vault is enabled, uses Vault API, otherwise checks operator status.
     *
     * @param profile   the PlayerProfile to check
     * @param permission the permission node to test
     * @return true if the profile has the permission
     */
    public static final boolean playerHas(
        PlayerProfile profile,
        String permission
    ) {
        // Fallback OfflinePlayer for operator status
        OfflinePlayer offline = Bukkit.getOfflinePlayer(profile.getId());

        if (vaultEnabled && vaultPerm != null) {
            // Attempt Vault-based check first
            if (offline != null) {
                return vaultPerm.playerHas(null, offline, permission);
            }
        }
        // Vault missing or player offline: fallback to operator check
        return offline != null && offline.isOp();
    }
}
