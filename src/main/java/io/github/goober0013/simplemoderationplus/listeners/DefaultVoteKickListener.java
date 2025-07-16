package io.github.goober0013.simplemoderationplus.listeners;

import com.destroystokyo.paper.profile.PlayerProfile;
import io.github.goober0013.simplemoderationplus.api.MessageProperties;
import io.github.goober0013.simplemoderationplus.api.ModerationLogger;
import io.github.goober0013.simplemoderationplus.api.VoteKickManager;
import io.github.goober0013.simplemoderationplus.api.VoteKickManager.VoteKickListener;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import net.kyori.adventure.audience.Audience;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

public class DefaultVoteKickListener implements VoteKickListener {

    public static final Duration duration = Duration.ofMinutes(10);

    /**
     * Registers this default listener with the VoteKickManager.
     */
    public static final void load() {
        VoteKickManager.registerListener(new DefaultVoteKickListener());
    }

    @Override
    public final void onVoteKickCancelled(CommandSender sender) {
        final List<Audience> recipients = Bukkit.getOnlinePlayers()
            .stream()
            .map(p -> (Audience) p)
            .collect(Collectors.toList());

        recipients.add((Audience) Bukkit.getConsoleSender());

        final Audience audience = Audience.audience(
            recipients.toArray(Audience[]::new)
        );
        audience.sendMessage(
            MessageProperties.get(
                "command.votekick.cancel",
                sender,
                null,
                null,
                null
            )
        );

        ModerationLogger.write(
            "command.votekick.cancel",
            sender,
            null,
            null,
            null
        );
    }

    @Override
    public final void onVoteKickStarted(
        CommandSender initiator,
        OfflinePlayer targetPlayer,
        String reason
    ) {
        final PlayerProfile target = targetPlayer.getPlayerProfile();
        final Audience audience = audienceExcluding(initiator, target);

        audience.sendMessage(
            MessageProperties.get(
                "player.votekick.others",
                initiator,
                target,
                duration,
                reason
            )
        );

        if (targetPlayer.isOnline()) {
            ((Player) targetPlayer).sendMessage(
                MessageProperties.getRed(
                    "player.votekick",
                    initiator,
                    target,
                    duration,
                    reason
                )
            );
        }

        initiator.sendMessage(
            MessageProperties.get(
                "command.votekick",
                initiator,
                target,
                duration,
                reason
            )
        );

        ModerationLogger.write(
            "modlogs.votekick",
            initiator,
            target,
            duration,
            reason
        );
    }

    @Override
    public final void onVoteAdded(
        CommandSender voter,
        OfflinePlayer targetPlayer,
        int totalVotes
    ) {
        final PlayerProfile target = targetPlayer.getPlayerProfile();

        final int requiredVotes = VoteKickManager.getRequiredVotes(
            targetPlayer
        );
        if (requiredVotes == -1) {
            return;
        }
        final String playerCount = totalVotes + "/" + requiredVotes;

        final Audience audience = audienceExcluding(voter, target);

        audience.sendMessage(
            MessageProperties.get(
                "player.votekick.status.others",
                voter,
                target,
                duration,
                playerCount
            )
        );

        if (targetPlayer.isOnline()) {
            ((Player) targetPlayer).sendMessage(
                MessageProperties.getRed(
                    "player.votekick.status",
                    voter,
                    target,
                    duration,
                    playerCount
                )
            );
        }

        voter.sendMessage(
            MessageProperties.get(
                "player.votekick.status.voter",
                voter,
                target,
                duration,
                playerCount
            )
        );
    }

    @Override
    public final void onVotePassed(
        CommandSender voter,
        OfflinePlayer targetPlayer,
        int totalVotes
    ) {
        final PlayerProfile target = targetPlayer.getPlayerProfile();
        final Audience audience = audienceExcluding(voter, target);
        final String reason = VoteKickManager.getReason(targetPlayer);
        final CommandSender initiator = VoteKickManager.getInitiator(
            targetPlayer
        );

        audience.sendMessage(
            MessageProperties.get(
                "player.votekick.passed.others",
                initiator,
                target,
                duration,
                reason
            )
        );

        voter.sendMessage(
            MessageProperties.get(
                "player.votekick.passed",
                initiator,
                target,
                duration,
                reason
            )
        );

        if (targetPlayer.isOnline()) {
            ((Player) targetPlayer).kick(
                MessageProperties.get(
                    "player.votekick.passed.kick",
                    initiator,
                    target,
                    duration,
                    reason
                )
            );
        }

        targetPlayer.ban(reason, duration, initiator.getName());

        ModerationLogger.write(
            "modlogs.votekick.passed",
            initiator,
            target,
            duration,
            reason
        );
    }

    @Override
    public final void onVoteKickExpired(OfflinePlayer targetPlayer) {
        final PlayerProfile target = targetPlayer.getPlayerProfile();
        final String reason = VoteKickManager.getReason(targetPlayer);
        final CommandSender initiator = VoteKickManager.getInitiator(
            targetPlayer
        );
        final Audience audience = audienceExcluding(initiator, target);

        audience.sendMessage(
            MessageProperties.getRed(
                "player.votekick.failed.others",
                initiator,
                target,
                duration,
                reason
            )
        );

        initiator.sendMessage(
            MessageProperties.getRed(
                "player.votekick.failed.initiator",
                initiator,
                target,
                duration,
                reason
            )
        );

        if (targetPlayer.isOnline()) {
            ((Player) targetPlayer).sendMessage(
                MessageProperties.get(
                    "player.votekick.failed",
                    initiator,
                    target,
                    duration,
                    reason
                )
            );
        }

        ModerationLogger.write(
            "modlogs.votekick.failed",
            initiator,
            target,
            duration,
            reason
        );
    }

    private static final Audience audienceExcluding(
        CommandSender sender,
        PlayerProfile profile
    ) {
        Collection<? extends Player> online = Bukkit.getOnlinePlayers();

        // determine if the issuer is a player and grab their UUID
        final UUID senderUuid;
        if (sender instanceof Player pl) {
            senderUuid = pl.getUniqueId();
        } else {
            senderUuid = null;
        }

        // build the audience list, excluding the issuer by UUID
        List<Audience> recipients = Bukkit.getOnlinePlayers()
            .stream()
            // filter the sender and profile
            .filter(p ->
                !(sender instanceof Player &&
                    (p.getUniqueId().equals(((Player) sender).getUniqueId()) ||
                        p.getUniqueId().equals(profile.getId())))
            )
            .map(p -> (Audience) p)
            .collect(Collectors.toList());

        if (!(sender instanceof ConsoleCommandSender)) {
            recipients.add((Audience) Bukkit.getConsoleSender());
        }

        return Audience.audience(recipients.toArray(Audience[]::new));
    }

    private static final Audience audienceExcluding(PlayerProfile profile) {
        Collection<? extends Player> online = Bukkit.getOnlinePlayers();

        // build the audience list, excluding the issuer by UUID
        List<Audience> recipients = Bukkit.getOnlinePlayers()
            .stream()
            // filter the sender and profile
            .filter(p -> !((p.getUniqueId().equals(profile.getId()))))
            .map(p -> (Audience) p)
            .collect(Collectors.toList());

        recipients.add((Audience) Bukkit.getConsoleSender());

        return Audience.audience(recipients.toArray(Audience[]::new));
    }

    private static final Audience audienceExcluding(CommandSender sender) {
        Collection<? extends Player> online = Bukkit.getOnlinePlayers();

        // determine if the issuer is a player and grab their UUID
        final UUID senderUuid;
        if (sender instanceof Player pl) {
            senderUuid = pl.getUniqueId();
        } else {
            senderUuid = null;
        }

        // build the audience list, excluding the issuer by UUID
        List<Audience> recipients = Bukkit.getOnlinePlayers()
            .stream()
            // filter the sender and profile
            .filter(p ->
                !(sender instanceof Player &&
                    (p.getUniqueId().equals(((Player) sender).getUniqueId())))
            )
            .map(p -> (Audience) p)
            .collect(Collectors.toList());

        if (!(sender instanceof ConsoleCommandSender)) {
            recipients.add((Audience) Bukkit.getConsoleSender());
        }

        return Audience.audience(recipients.toArray(Audience[]::new));
    }
}
