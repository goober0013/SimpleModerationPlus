package io.github.goober0013.simplemoderationplus.listeners;

import com.destroystokyo.paper.profile.PlayerProfile;
import io.github.goober0013.simplemoderationplus.SimpleModerationPlus;
import io.github.goober0013.simplemoderationplus.api.ConfigUtils;
import io.github.goober0013.simplemoderationplus.api.JailEntry;
import io.github.goober0013.simplemoderationplus.api.MessageProperties;
import io.github.goober0013.simplemoderationplus.api.ProfileJailList;
import io.github.goober0013.simplemoderationplus.api.WorldguardRegionHelper;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public class JailListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public static final void onPlayerMove(PlayerMoveEvent event) {
        handleJailEvent(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public static final void onPlayerJoin(PlayerJoinEvent event) {
        handleJailEvent(event.getPlayer());
    }

    // Change respawn of jailed targets
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        final Player player = event.getPlayer();
        final PlayerProfile profile = player.getPlayerProfile();

        // If the player is jailed
        if (ProfileJailList.isJailed(profile)) {
            final JailEntry jailEntry = ProfileJailList.get(profile);
            final String reason = jailEntry.getReason();
            final String source = jailEntry.getSource();
            final Duration duration = jailEntry.getDuration();
            final Duration remaining;
            if (duration == null) {
                remaining = null;
            } else {
                remaining = Duration.between(
                    Instant.now(),
                    jailEntry.getCreated().plus(duration)
                );
            }

            // Notify the player that they are jailed
            player.sendMessage(
                MessageProperties.getRed(
                    jailKey(reason != null, duration != null),
                    source,
                    player.getPlayerProfile(),
                    remaining,
                    reason
                )
            );

            final Path configFile = SimpleModerationPlus.dataFolder
                .toPath()
                .resolve("jail.yml");

            try {
                final Location jail = ConfigUtils.loadLocation(
                    configFile,
                    "jail"
                );
                event.setRespawnLocation(jail);
            } catch (IOException e) {
                SimpleModerationPlus.logger.severe(
                    MessageProperties.get("log.error.ioexception")
                );
            }
        }
    }

    private static final void handleJailEvent(Player player) {
        final PlayerProfile profile = player.getPlayerProfile();

        // If the player is jailed
        if (ProfileJailList.isJailed(profile)) {
            if (
                !WorldguardRegionHelper.isInRegion(player.getLocation(), "jail")
            ) {
                final JailEntry jailEntry = ProfileJailList.get(profile);
                final String reason = jailEntry.getReason();
                final String source = jailEntry.getSource();
                final Duration duration = jailEntry.getDuration();
                final Duration remaining;
                if (duration == null) {
                    remaining = null;
                } else {
                    remaining = Duration.between(
                        Instant.now(),
                        jailEntry.getCreated().plus(duration)
                    );
                }

                // Notify the player that they are jailed
                player.sendMessage(
                    MessageProperties.getRed(
                        jailKey(reason != null, duration != null),
                        source,
                        player.getPlayerProfile(),
                        remaining,
                        reason
                    )
                );

                final Path configFile = SimpleModerationPlus.dataFolder
                    .toPath()
                    .resolve("jail.yml");

                try {
                    final Location jail = ConfigUtils.loadLocation(
                        configFile,
                        "jail"
                    );
                    player.teleport(jail);
                } catch (IOException e) {
                    SimpleModerationPlus.logger.severe(
                        MessageProperties.get("log.error.ioexception")
                    );
                }
            }
        }
    }

    private static final String jailKey(boolean reason, boolean duration) {
        StringBuilder key = new StringBuilder("player.isjailed");

        if (reason || duration) {
            key.append('.');
            if (reason) {
                key.append("reason");
            }
            if (reason && duration) {
                key.append('_');
            }
            if (duration) {
                key.append("duration");
            }
        }

        return key.toString();
    }
}
