package io.github.goober0013.simplemoderationplus.api;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

/**
 * API class designed to help with commands that must be run TWICE to confirm
 */
public class CommandConfirmation {

    // sender name -> (command string -> time recorded)
    private static final Map<String, ConfirmationEntry> confirmationMap =
        new ConcurrentHashMap<>();

    /**
     * Record that a CommandSender ran a specific command at the current time
     *
     * @param sender  the command sender (player, console, etc.)
     * @param command the exact command string (including arguments)
     */
    public static final void logCommand(CommandSender sender, String command) {
        final String senderKey;
        if (sender instanceof ConsoleCommandSender) {
            senderKey = "CONSOLE";
        } else {
            senderKey = ((Player) sender).getUniqueId().toString();
        }
        confirmationMap.compute(senderKey, (k, v) ->
            new ConfirmationEntry(command)
        );
    }

    /**
     * Check if a CommandSender has run a given command within the last 30 seconds
     *
     * @param sender  the command sender (player, console, etc.)
     * @param command the command string to verify
     * @return true if run within 30 seconds, false otherwise
     */
    public static final boolean isConfirmed(
        CommandSender sender,
        String command
    ) {
        final String senderKey;
        if (sender instanceof ConsoleCommandSender) {
            senderKey = "CONSOLE";
        } else {
            senderKey = ((Player) sender).getUniqueId().toString();
        }

        final ConfirmationEntry confirmationEntry = confirmationMap.get(
            senderKey
        );
        if (confirmationEntry == null) {
            return false;
        }

        // Either the key is stale or we already used it
        // So we remove the key
        confirmationMap.remove(senderKey);

        final Instant last = confirmationEntry.getCreated();
        if (last == null) {
            return false;
        }

        final Instant cutoff = Instant.now().minusSeconds(30);
        if (last.isAfter(cutoff)) {
            return true;
        } else {
            return false;
        }
    }

    private static final class ConfirmationEntry {

        private final String command;
        private final Instant created;

        public ConfirmationEntry(String command) {
            this.command = command;
            this.created = Instant.now();
        }

        public final String getCommand() {
            return this.command;
        }

        public final Instant getCreated() {
            return this.created;
        }
    }
}
