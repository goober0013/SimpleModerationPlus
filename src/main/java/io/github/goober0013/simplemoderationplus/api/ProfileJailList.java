package io.github.goober0013.simplemoderationplus.api;

import com.destroystokyo.paper.profile.PlayerProfile;
import io.github.goober0013.simplemoderationplus.SimpleModerationPlus;
import io.github.goober0013.simplemoderationplus.api.JailEntry;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Manages jailtime for player profiles, caching entries to reduce IO.
 */
public class ProfileJailList {

    private static Path jailsDir;
    private static final Map<UUID, JailEntry> cache = new ConcurrentHashMap<>();

    /**
     * Initializes the playerjails directory and loads existing jailtimes.
     */
    public static final void load() {
        jailsDir = SimpleModerationPlus.dataFolder
            .toPath()
            .resolve("playerjails");
        SimpleModerationPlus.logger.info(
            MessageProperties.get("log.info.enable_jail_list")
        );
        try {
            // ensure the folder exists
            if (!Files.exists(jailsDir)) {
                Files.createDirectories(jailsDir);
            }

            // open *all* files, filter for .jail in code
            try (
                DirectoryStream<Path> stream = Files.newDirectoryStream(
                    jailsDir
                );
            ) {
                for (Path file : stream) {
                    // skip anything that doesn’t end in ".jail"
                    if (!file.getFileName().toString().endsWith(".jail")) {
                        SimpleModerationPlus.logger.severe(
                            MessageProperties.get("log.error.file_extension")
                        );
                        continue;
                    }

                    try {
                        // load each jail and cache if still valid
                        JailEntry entry = JailEntry.loadFromFile(file);
                        if (entry != null) {
                            if (!isExpired(entry)) {
                                cache.put(entry.getProfile().getId(), entry);
                            } else {
                                // clean up expired files
                                Files.deleteIfExists(file);
                            }
                        }
                    } catch (Exception e) {
                        // log parse/load errors per‐file so you can fix the bad one
                        SimpleModerationPlus.logger.severe(
                            MessageProperties.get("log.error.ioexception")
                        );
                    }
                }
            }
        } catch (IOException e) {
            // log directory creation or stream-open failures
            SimpleModerationPlus.logger.severe(
                MessageProperties.get("log.error.ioexception")
            );
        }
    }

    /**
     * Creates and caches a jail entry.
     */
    public static final JailEntry addJail(
        PlayerProfile profile,
        String reason,
        Duration duration,
        String source
    ) {
        JailEntry entry = new JailEntry(profile, source, duration, reason);
        cache.put(profile.getId(), entry);
        return entry;
    }

    /**
     * Removes a jail entry and its file.
     */
    public static final boolean unjail(PlayerProfile profile) {
        cache.remove(profile.getId());
        Path file = jailsDir.resolve(profile.getId().toString() + ".jail");
        try {
            return Files.deleteIfExists(file);
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Gets the jail entry, loading from disk if needed.
     */
    public static final JailEntry get(PlayerProfile profile) {
        UUID id = profile.getId();
        JailEntry entry = cache.get(id);
        if (entry == null) {
            Path file = jailsDir.resolve(id.toString() + ".jail");
            if (Files.exists(file)) {
                entry = JailEntry.loadFromFile(file);
                if (entry != null && !isExpired(entry)) {
                    cache.put(id, entry);
                } else {
                    return null;
                }
            }
        }
        return isJailed(profile) ? entry : null;
    }

    /**
     * Returns all active jail entries.
     */
    public static final Collection<JailEntry> getEntries() {
        List<JailEntry> entries = new ArrayList<>();
        for (UUID id : new ArrayList<>(cache.keySet())) {
            JailEntry entry = cache.get(id);
            if (entry != null && !isExpired(entry)) {
                entries.add(entry);
            } else {
                cache.remove(id);
            }
        }
        return entries;
    }

    /**
     * Checks if a profile is currently jailed.
     */
    public static final boolean isJailed(PlayerProfile profile) {
        JailEntry entry = cache.get(profile.getId());
        if (entry == null) {
            return false;
        }
        if (isExpired(entry)) {
            unjail(profile);
            return false;
        }
        return true;
    }

    // Helper to test expiration without side-effects
    private static final boolean isExpired(JailEntry entry) {
        if (entry.getDuration() == null) {
            return false;
        }
        return Instant.now().isAfter(
            entry.getCreated().plus(entry.getDuration())
        );
    }
}
