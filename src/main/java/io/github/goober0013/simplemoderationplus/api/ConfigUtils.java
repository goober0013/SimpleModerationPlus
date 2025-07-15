package io.github.goober0013.simplemoderationplus.api;

import io.github.goober0013.simplemoderationplus.SimpleModerationPlus;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public class ConfigUtils {

    private static class CachedLoc {

        final long lastModified;
        final Location location;

        CachedLoc(long lastModified, Location location) {
            this.lastModified = lastModified;
            this.location = location;
        }
    }

    // key = absolutePath + "|" + rootPath
    private static final Map<String, CachedLoc> cache =
        new ConcurrentHashMap<>();

    /**
     * Loads (or returns cached) Location for the given YAML file path and root path.
     *
     * @param path     the Path to the YML file
     * @param rootPath e.g. "jail"
     * @return a Bukkit Location built from the config values
     * @throws IllegalArgumentException if the configured world isn't loaded
     * @throws IOException              if an I/O error occurs reading the file
     */
    public static Location loadLocation(Path path, String rootPath)
        throws IOException, IllegalArgumentException {
        String key = path.toAbsolutePath().toString() + "|" + rootPath;
        FileTime ft = Files.getLastModifiedTime(path);
        long modified = ft.toMillis();

        // return cached if unchanged
        CachedLoc cached = cache.get(key);
        if (cached != null && cached.lastModified == modified) {
            return cached.location.clone();
        }

        // load fresh from YAML
        try (
            InputStreamReader reader = new InputStreamReader(
                Files.newInputStream(path),
                StandardCharsets.UTF_8
            );
        ) {
            FileConfiguration config = YamlConfiguration.loadConfiguration(
                reader
            ); // loadConfiguration(Reader) :contentReference[oaicite:9]{index=9}
            String worldName = config.getString(rootPath + ".world", "");
            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                throw new IllegalArgumentException(
                    "World '" + worldName + "' not found"
                );
            }

            double x = config.getDouble(rootPath + ".x");
            double y = config.getDouble(rootPath + ".y");
            double z = config.getDouble(rootPath + ".z");
            float yaw = (float) config.getDouble(rootPath + ".yaw");
            float pitch = (float) config.getDouble(rootPath + ".pitch");

            Location loc = new Location(world, x, y, z, yaw, pitch);
            cache.put(key, new CachedLoc(modified, loc));
            return loc.clone();
        }
    }

    public static final void setLocation(Path file, Location pos) {
        try {
            Files.createDirectories(SimpleModerationPlus.dataFolder.toPath());
        } catch (IOException e) {
            SimpleModerationPlus.logger.severe(
                MessageProperties.get("log.error.ioexception")
            );
            return;
        }

        // Create the config object
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("jail.world", pos.getWorld().getName());
        cfg.set("jail.x", pos.getX());
        cfg.set("jail.y", pos.getY());
        cfg.set("jail.z", pos.getZ());
        cfg.set("jail.yaw", pos.getYaw());
        cfg.set("jail.pitch", pos.getPitch());

        // Convert to string object
        String yaml = cfg.saveToString();

        // Save changes to disk
        try {
            Files.writeString(
                file,
                yaml,
                StandardOpenOption.CREATE, // create file if needed
                StandardOpenOption.TRUNCATE_EXISTING // overwrite
            );
        } catch (IOException e) {
            SimpleModerationPlus.logger.severe(
                MessageProperties.get("log.error.ioexception")
            );
        }
    }
}
