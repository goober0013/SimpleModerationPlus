package io.github.goober0013.simplemoderationplus.api;

import com.destroystokyo.paper.profile.PlayerProfile;
import io.github.goober0013.simplemoderationplus.SimpleModerationPlus;
import io.github.goober0013.simplemoderationplus.api.MessageProperties;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

public class ModerationLogger {

    private static Path logsDir;

    /**
     * Initialize ModerationLogger
     */
    public static final void load() {
        // Convert the plugin data folder (a File) to a Path, then resolve "playerlogs"
        Path dataFolderPath = SimpleModerationPlus.dataFolder.toPath();
        logsDir = dataFolderPath.resolve("playerlogs");

        try {
            // Create the directory (and any missing parents) if it doesn’t exist
            Files.createDirectories(logsDir); // atomic creation of nested dirs
        } catch (IOException e) {
            SimpleModerationPlus.logger.severe(
                MessageProperties.get("log.error.ioexception")
            );
        }
    }

    /**
     * Appends an action to the player and moderator's log files
     * @param key String
     * @param sender CommandSender
     * @param profile PlayerProfile
     * @param duration Duration
     * @param reason String
     * @return void
     */
    public static final void write(
        String key,
        CommandSender sender,
        PlayerProfile profile,
        Duration duration,
        String reason
    ) {
        if (sender instanceof ConsoleCommandSender) {
            write("CONSOLE", key, sender, profile, duration, reason);
        } else {
            write(
                ((Player) sender).getUniqueId().toString(),
                key,
                sender,
                profile,
                duration,
                reason
            );
        }

        if (profile != null) {
            write(
                profile.getId().toString(),
                key,
                sender,
                profile,
                duration,
                reason
            );
        }
    }

    private static final void write(
        String player,
        String key,
        CommandSender sender,
        PlayerProfile profile,
        Duration duration,
        String reason
    ) {
        Path logFile = logsDir.resolve(player + ".log");
        try {
            Files.createDirectories(logsDir); // Ensures log dir exists before I do my work
            final DateTimeFormatter format = DateTimeFormatter.ofPattern(
                "dd-MMM-yy HH:mm z"
            );
            final String time =
                '[' +
                ZonedDateTime.now(ZoneId.systemDefault()).format(format) +
                "] ";

            final String durationString;
            if (duration != null) {
                durationString = DurationFormatUtils.formatDurationWords(
                    duration.toMillis(),
                    true,
                    true
                );
            } else {
                durationString = "{2}";
            }

            final String adminArg = sender != null ? sender.getName() : "{0}";
            final String playerArg = profile != null
                ? profile.getName()
                : "{1}";

            final String full =
                String.join(
                    "§",
                    time,
                    key,
                    adminArg,
                    playerArg,
                    durationString,
                    reason
                ) +
                System.lineSeparator();

            Files.write(
                logFile,
                full.getBytes(),
                java.nio.file.StandardOpenOption.CREATE,
                java.nio.file.StandardOpenOption.APPEND
            );
        } catch (IOException e) {
            SimpleModerationPlus.logger.severe(
                MessageProperties.get("log.error.ioexception")
            );
        }
    }

    /**
     * Returs log lines (newest first) or null if the file does not exist
     * @param player PlayerProfile
     * @return List
     */
    public static final List<Component> get(PlayerProfile player) {
        return get(player.getId());
    }

    private static final List<Component> get(UUID player) {
        Path logFile = logsDir.resolve(player + ".log");
        try {
            if (!Files.exists(logFile)) {
                return null;
            }
            List<String> lines = Files.readAllLines(logFile);
            Collections.reverse(lines);
            return lines
                .stream()
                .<Component>map(line -> {
                    // Split into exactly 6 parts
                    String[] parts = line.split("§", 6);
                    String time = parts[0];
                    String key = parts[1];
                    String sender = parts[2];
                    String profile = parts[3];
                    String duration = parts[4];
                    String reason = parts[5];

                    // Build and return the Component
                    return Component.text(time).append(
                        Component.translatable(
                            key,
                            List.of(
                                Component.text(sender),
                                Component.text(profile),
                                Component.text(duration),
                                Component.text(reason)
                            )
                        )
                    );
                })
                .collect(Collectors.toList());
        } catch (IOException e) {
            SimpleModerationPlus.logger.severe(
                MessageProperties.get("log.error.ioexception")
            );
            return null;
        }
    }
}
