package io.github.goober0013.simplemoderationplus.listeners;

import com.destroystokyo.paper.profile.PlayerProfile;
import io.github.goober0013.simplemoderationplus.api.MessageProperties;
import io.github.goober0013.simplemoderationplus.api.MuteEntry;
import io.github.goober0013.simplemoderationplus.api.ProfileMuteList;
import io.papermc.paper.event.player.AsyncChatEvent;
import java.time.Duration;
import java.time.Instant;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class MuteListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public static final void onPlayerChat(AsyncChatEvent event) {
        final Player player = event.getPlayer();
        final PlayerProfile profile = player.getPlayerProfile();

        // If the player is muted (and the mute hasn’t expired), cancel the message
        if (ProfileMuteList.isMuted(profile)) {
            event.setCancelled(true);

            final MuteEntry muteEntry = ProfileMuteList.get(profile);
            final String reason = muteEntry.getReason();
            final String source = muteEntry.getSource();
            final Duration duration = muteEntry.getDuration();
            final Duration remaining;
            if (duration == null) {
                remaining = null;
            } else {
                remaining = Duration.between(
                    Instant.now(),
                    muteEntry.getCreated().plus(duration)
                );
            }

            // Notify the player that they are muted
            player.sendMessage(
                MessageProperties.getRed(
                    muteKey(reason != null, duration != null),
                    source,
                    player.getPlayerProfile(),
                    remaining,
                    reason
                )
            );
        }
    }

    private static final String muteKey(boolean reason, boolean duration) {
        StringBuilder key = new StringBuilder("player.ismuted");

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
