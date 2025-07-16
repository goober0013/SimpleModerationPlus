package io.github.goober0013.simplemoderationplus.api;

import com.destroystokyo.paper.profile.PlayerProfile;
import io.github.goober0013.simplemoderationplus.SimpleModerationPlus;
import io.github.goober0013.simplemoderationplus.api.MuteEntry;
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

/**
 * Manages mutes for player profiles, caching entries to reduce IO.
 */
public class ProfileMuteList {

    private static Path mutesDir;
    private static final Map<UUID, MuteEntry> cache = new ConcurrentHashMap<
        UUID,
        MuteEntry
    >();

    /**
     * Initializes the playermutes directory and loads existing mutes.
     */
    public static void load() {
        mutesDir = SimpleModerationPlus.dataFolder
            .toPath()
            .resolve("playermutes");
        SimpleModerationPlus.logger.info(
            MessageProperties.get("log.info.enable_mute_list")
        );
        try {
            // ensure the folder exists
            if (!Files.exists(mutesDir)) {
                Files.createDirectories(mutesDir);
            }

            // open *all* files, filter for .mute in code
            try (
                DirectoryStream<Path> stream = Files.newDirectoryStream(
                    mutesDir
                );
            ) {
                for (Path file : stream) {
                    // skip anything that doesn’t end in ".mute"
                    if (!file.getFileName().toString().endsWith(".mute")) {
                        SimpleModerationPlus.logger.severe(
                            MessageProperties.get("log.error.file_extension")
                        );
                        continue;
                    }

                    try {
                        // load each mute and cache if still valid
                        MuteEntry entry = MuteEntry.loadFromFile(file);
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
     * Creates and caches a mute entry.
     */
    public static final MuteEntry addMute(
        PlayerProfile profile,
        String reason,
        Duration duration,
        String source
    ) {
        MuteEntry entry = new MuteEntry(profile, source, duration, reason);
        cache.put(profile.getId(), entry);
        return entry;
    }

    /**
     * Removes a mute entry and its file.
     */
    public static final boolean unmute(PlayerProfile profile) {
        cache.remove(profile.getId());
        Path file = mutesDir.resolve(profile.getId().toString() + ".mute");
        try {
            return Files.deleteIfExists(file);
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Gets the mute entry, loading from disk if needed.
     */
    public static final MuteEntry get(PlayerProfile profile) {
        UUID id = profile.getId();
        MuteEntry entry = cache.get(id);
        if (entry == null) {
            Path file = mutesDir.resolve(id.toString() + ".mute");
            if (Files.exists(file)) {
                entry = MuteEntry.loadFromFile(file);
                if (entry != null && !isExpired(entry)) {
                    cache.put(id, entry);
                } else {
                    return null;
                }
            }
        }
        return isMuted(profile) ? entry : null;
    }

    /**
     * Returns all active mute entries.
     */
    public static final Collection<MuteEntry> getEntries() {
        List<MuteEntry> entries = new ArrayList<>();
        for (UUID id : new ArrayList<>(cache.keySet())) {
            MuteEntry entry = cache.get(id);
            if (entry != null && !isExpired(entry)) {
                entries.add(entry);
            } else {
                cache.remove(id);
            }
        }
        return entries;
    }

    /**
     * Checks if a profile is currently muted.
     */
    public static final boolean isMuted(PlayerProfile profile) {
        MuteEntry entry = cache.get(profile.getId());
        if (entry == null) {
            return false;
        }
        if (isExpired(entry)) {
            unmute(profile);
            return false;
        }
        return true;
    }

    // Helper to test expiration without side-effects
    private static final boolean isExpired(MuteEntry entry) {
        if (entry.getDuration() == null) {
            return false;
        }
        return Instant.now().isAfter(
            entry.getCreated().plus(entry.getDuration())
        );
    }
}
