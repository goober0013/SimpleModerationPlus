package io.github.goober0013.simplemoderationplus.api;

import com.destroystokyo.paper.profile.PlayerProfile;
import io.github.goober0013.simplemoderationplus.SimpleModerationPlus;
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
 * A single mute entry for a player, persisted to disk upon creation.
 */
/**
 * A single mute entry for a player, persisted to disk.
 */
public class MuteEntry {

    private final PlayerProfile profile;
    private final Instant created;
    private final Duration duration;
    private final String source;
    private final String reason;
    private final Path file;

    /**
     * Constructs a new mute entry, writes a marker file.
     */
    public MuteEntry(
        PlayerProfile profile,
        String source,
        Duration duration,
        String reason
    ) {
        this.profile = profile;
        this.source = source;
        this.duration = duration;
        this.reason = reason;
        final String reasonString;
        if (reason == null) {
            reasonString = "";
        } else {
            reasonString = reason;
        }
        this.created = Instant.now();
        Path dir = SimpleModerationPlus.dataFolder
            .toPath()
            .resolve("playermutes");
        this.file = dir.resolve(profile.getId().toString() + ".mute");
        try {
            Files.createDirectories(dir);
            final String content = String.format(
                "Muted at: %s%nDuration: %s%nSource: %s%nReason: %s%n",
                created,
                duration,
                source,
                reasonString
            );
            Files.writeString(
                file,
                content,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
            );
        } catch (IOException e) {
            // swallow to avoid exceptions escaping
        }
    }

    /**
     * Loads a mute entry from an existing .mute file.
     */
    public static final MuteEntry loadFromFile(Path file) {
        try {
            final List<String> lines = Files.readAllLines(file);
            if (lines.size() < 4) {
                return null;
            }
            final Instant created = Instant.parse(
                lines.get(0).substring("Muted at: ".length())
            );
            final String durationLine = lines
                .get(1)
                .substring("Duration: ".length());
            final Duration duration;
            if (durationLine != "null") {
                duration = Duration.parse(durationLine);
            } else {
                duration = null;
            }
            final String source = lines.get(2).substring("Source: ".length());
            final String reasonLine = lines
                .get(3)
                .substring("Reason: ".length());
            final String reason;
            if (reasonLine == "") {
                reason = null;
            } else {
                reason = reasonLine;
            }
            final String filename = file.getFileName().toString();
            final UUID uuid = UUID.fromString(
                filename.substring(0, filename.length() - ".mute".length())
            );
            final PlayerProfile profile =
                SimpleModerationPlus.server.createProfile(uuid, null);
            return new MuteEntry(
                profile,
                created,
                duration,
                source,
                reason,
                file
            );
        } catch (Exception e) {
            SimpleModerationPlus.logger.severe("Error resolving mute file");
            return null;
        }
    }

    // Private constructor for loading from file
    private MuteEntry(
        PlayerProfile profile,
        Instant created,
        Duration duration,
        String source,
        String reason,
        Path file
    ) {
        this.profile = profile;
        this.created = created;
        this.duration = duration;
        this.source = source;
        this.reason = reason;
        this.file = file;
    }

    // Getters
    public final PlayerProfile getProfile() {
        return profile;
    }

    public final Instant getCreated() {
        return created;
    }

    public final Duration getDuration() {
        return duration;
    }

    public final String getSource() {
        return source;
    }

    public final String getReason() {
        return reason;
    }

    public final Path getFile() {
        return file;
    }
}
