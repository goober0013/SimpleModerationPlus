package io.github.goober0013.simplemoderationplus.api;

import io.github.goober0013.simplemoderationplus.SimpleModerationPlus;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Manages vote‐kicks that expire in 1 minute,
 * require 60% of online players to pass,
 * and notify listeners on start, vote added, pass, and expire.
 */
public class VoteKickManager {

    private static final long EXPIRATION_TICKS = 60L * 20L;

    private static final Map<String, VoteKick> activeVotes =
        new ConcurrentHashMap<>();
    private static final List<VoteKickListener> listeners =
        Collections.synchronizedList(new ArrayList<>());

    public static final boolean clearVoteKicks(CommandSender sender) {
        if (activeVotes.isEmpty()) return false;
        activeVotes.clear();

        listeners.forEach(l -> l.onVoteKickCancelled(sender));
        return true;
    }

    /**
     * Starts a vote‐kick for the given target (by UUID string) initiated by the sender.
     * Does nothing if there are 2 or fewer players online or if one is already active.
     * @param initiator the CommandSender who starts the vote
     * @param target the OfflinePlayer to be vote‐kicked
     * @return true if vote‐kick started, false otherwise
     */
    public static final boolean startVoteKick(
        OfflinePlayer target,
        CommandSender initiator,
        String reason
    ) {
        final int online = SimpleModerationPlus.server
            .getOnlinePlayers()
            .size();
        if (online <= 2) return false; // not enough players to initiate a vote

        final String targetId = target.getUniqueId().toString();
        if (activeVotes.containsKey(targetId)) return false;

        // Calculate required votes: ceil(60% of current online players)
        int required = (int) Math.ceil(online * 0.6);
        if (required < 1) required = 1; // at least one vote

        final VoteKick vk = new VoteKick(targetId, required, reason, initiator);
        activeVotes.put(targetId, vk);

        // schedule expiration
        vk.task = Bukkit.getGlobalRegionScheduler().runDelayed(
            SimpleModerationPlus.instance,
            task -> expireVoteKick(targetId),
            EXPIRATION_TICKS
        );

        // notify listeners of start with initiator
        listeners.forEach(l -> l.onVoteKickStarted(initiator, target, reason));
        return true;
    }

    /**
     * Returns true if a vote‐kick is currently active for the given OfflinePlayer.
     */
    public static final boolean isVoteKickActive(OfflinePlayer target) {
        return activeVotes.containsKey(target.getUniqueId().toString());
    }

    public static final int getRequiredVotes(OfflinePlayer target) {
        final VoteKick vk = activeVotes.get(target.getUniqueId().toString());
        if (vk == null) return -1;

        return vk.requiredVotes;
    }

    public static final CommandSender getInitiator(OfflinePlayer target) {
        final VoteKick vk = activeVotes.get(target.getUniqueId().toString());
        if (vk == null) return null;

        return vk.initiator;
    }

    public static final String getReason(OfflinePlayer target) {
        final VoteKick vk = activeVotes.get(target.getUniqueId().toString());
        if (vk == null) return "Unknown";

        return vk.reason;
    }

    /**
     * Adds a vote from the sender for the given target.
     * @return -1 if no vote is active,
     *          0 if sender already voted,
     *         >0 current vote count
     */
    public static final int addVote(
        OfflinePlayer target,
        CommandSender sender
    ) {
        final String targetId = target.getUniqueId().toString();
        final VoteKick vk = activeVotes.get(targetId);
        if (vk == null) return -1;

        // determine voter ID string
        final String voterId;
        if (sender instanceof Player) {
            voterId = ((Player) sender).getUniqueId().toString();
        } else {
            voterId = "CONSOLE";
        }

        // ignore duplicate votes
        if (!vk.voters.add(voterId)) {
            return 0;
        }

        final int count = vk.voters.size();
        if (count >= vk.requiredVotes) {
            // vote passed: cancel expiration and notify
            vk.task.cancel();
            listeners.forEach(l ->
                l.onVotePassed(
                    getInitiator(target),
                    Bukkit.getOfflinePlayer(UUID.fromString(targetId)),
                    count
                )
            );

            activeVotes.remove(targetId);
        } else {
            // vote still ongoing
            listeners.forEach(l ->
                l.onVoteAdded(
                    getInitiator(target),
                    Bukkit.getOfflinePlayer(UUID.fromString(targetId)),
                    count
                )
            );
        }
        return count;
    }

    private static final void expireVoteKick(String targetId) {
        VoteKick vk = activeVotes.get(targetId);
        if (vk == null) return;
        listeners.forEach(l -> {
            if (vk.voters.size() >= vk.requiredVotes) return;

            l.onVoteKickExpired(
                Bukkit.getOfflinePlayer(UUID.fromString(targetId))
            );
        });

        activeVotes.remove(targetId);
    }

    /** Register a listener to be notified of vote‐kick events */
    public static final void registerListener(VoteKickListener listener) {
        listeners.add(listener);
    }

    /** Unregister a previously‐registered listener */
    public static final void unregisterListener(VoteKickListener listener) {
        listeners.remove(listener);
    }

    // --- internals ---

    private static final class VoteKick {

        final String targetId;
        final int requiredVotes;
        final String reason;
        final CommandSender initiator;
        final Set<String> voters = Collections.newSetFromMap(
            new ConcurrentHashMap<>()
        );
        ScheduledTask task;

        VoteKick(
            String targetId,
            int requiredVotes,
            String reason,
            CommandSender initiator
        ) {
            this.targetId = targetId;
            this.reason = reason;
            this.requiredVotes = requiredVotes;
            this.initiator = initiator;
        }
    }

    /**
     * Listener interface for vote‐kick lifecycle events.
     */
    public interface VoteKickListener {
        /** Called when votekicks are cancelled
         * @param sender CommandSender
         */
        void onVoteKickCancelled(CommandSender sender);

        /** Called once when a vote‐kick is started on a player */
        void onVoteKickStarted(
            CommandSender initiator,
            OfflinePlayer target,
            String reason
        );

        /**
         * Called whenever a new vote is added but hasn’t reached threshold yet.
         * @param voter the CommandSender who voted
         * @param target the OfflinePlayer being voted on
         * @param totalVotes the current number of unique votes
         */
        void onVoteAdded(
            CommandSender voter,
            OfflinePlayer target,
            int totalVotes
        );

        /**
         * Called once when the vote‐kick reaches the required threshold.
         * @param voter the CommandSender whose vote triggered the pass
         * @param target the OfflinePlayer being kicked
         * @param totalVotes the final vote count (>= required)
         */
        void onVotePassed(
            CommandSender voter,
            OfflinePlayer target,
            int totalVotes
        );

        /**
         * Called when the vote‐kick expires (after one minute)
         * @param target OfflinePlayer
         */
        void onVoteKickExpired(OfflinePlayer target);
    }
}
