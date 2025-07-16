package io.github.goober0013.simplemoderationplus.api;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import io.github.goober0013.simplemoderationplus.SimpleModerationPlus;
import org.bukkit.Bukkit;
import org.bukkit.Location;

/**
 * Helper for checking WorldGuard regions.
 */
public class WorldguardRegionHelper {

    private static boolean enabled;
    private static RegionContainer regionContainer;

    public static final void load() {
        enabled = ((SimpleModerationPlus.pluginManager.isPluginEnabled(
                    "WorldEdit"
                ) ||
                SimpleModerationPlus.pluginManager.isPluginEnabled(
                    "FastAsyncWorldEdit"
                )) &&
            SimpleModerationPlus.pluginManager.isPluginEnabled("WorldGuard"));
    }

    /**
     * Checks whether the given Bukkit Location is inside the named WorldGuard region.
     *
     * @param location   the Bukkit Location to test
     * @param regionName the id of the WorldGuard region
     * @return false if WorldGuard or WorldEdit is missing, or if the region doesn't exist;
     *         otherwise true if the location is contained in the region
     */
    public static final boolean isInRegion(
        Location location,
        String regionName
    ) {
        if (!enabled) {
            return false;
        }

        // Null‐checks
        if (location == null || location.getWorld() == null) {
            return false;
        }

        // Get the RegionContainer for this world
        RegionContainer container = WorldGuard.getInstance()
            .getPlatform()
            .getRegionContainer();

        if (container == null) {
            return false;
        }
        RegionManager manager = container.get(
            BukkitAdapter.adapt(location.getWorld())
        );
        if (manager == null) {
            return false;
        }

        // Find the region by name
        ProtectedRegion region = manager.getRegion(regionName);
        if (region == null) {
            return false;
        }

        // Test if the block location is inside
        BlockVector3 pt = BukkitAdapter.asBlockVector(location);
        return region.contains(pt);
    }
}
