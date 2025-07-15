package io.github.goober0013.simplemoderationplus.api;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;

/**
 * Manages per-sender, per-command cooldowns without scheduling tasks.
 * Uses system timestamps to compare cooldowns.
 * Supports both player and console senders via string IDs.
 */
public class CommandCooldown {

    // Map of sender ID (UUID string or "CONSOLE") to a map of command names and the timestamp (ms) when they last used it
    private static final Map<String, Map<String, Long>> cooldowns =
        new HashMap<>();

    /**
     * Checks if a given sender is on cooldown for a command.
     * Removes expired entries during the check.
     *
     * @param sender The command sender (player or console)
     * @param command The command name (use a unique key per command)
     * @param cooldownMillis Cooldown duration in milliseconds
     * @return true if still on cooldown, false if cooldown expired or not set
     */
    public static final boolean isOnCooldown(
        CommandSender sender,
        String command,
        long cooldownMillis
    ) {
        String id = getSenderId(sender);
        Map<String, Long> userMap = cooldowns.get(id);
        if (userMap == null) {
            return false;
        }
        Long lastUsed = userMap.get(command);
        if (lastUsed == null) {
            return false;
        }
        long now = System.currentTimeMillis();
        long elapsed = now - lastUsed;
        if (elapsed >= cooldownMillis) {
            // Cleanup expired entry
            userMap.remove(command);
            if (userMap.isEmpty()) {
                cooldowns.remove(id);
            }
            return false;
        }
        return true;
    }

    /**
     * Gets the remaining cooldown time for a sender and command.
     * Removes expired entries during the check.
     *
     * @param sender The command sender
     * @param command The command key
     * @param cooldownMillis Total cooldown duration in milliseconds
     * @return Remaining milliseconds, or 0 if no cooldown
     */
    public static final long getRemaining(
        CommandSender sender,
        String command,
        long cooldownMillis
    ) {
        String id = getSenderId(sender);
        Map<String, Long> userMap = cooldowns.get(id);
        if (userMap == null || !userMap.containsKey(command)) {
            return 0;
        }
        long lastUsed = userMap.get(command);
        long elapsed = System.currentTimeMillis() - lastUsed;
        long remaining = cooldownMillis - elapsed;
        if (remaining <= 0) {
            // Cleanup expired entry
            userMap.remove(command);
            if (userMap.isEmpty()) {
                cooldowns.remove(id);
            }
            return 0;
        }
        return remaining;
    }

    /**
     * Sets the cooldown for a sender and command to now.
     *
     * @param sender The command sender
     * @param command The command key
     */
    public static final void setCooldown(CommandSender sender, String command) {
        String id = getSenderId(sender);
        Map<String, Long> userMap = cooldowns.computeIfAbsent(id, k ->
            new HashMap<>()
        );
        userMap.put(command, System.currentTimeMillis());
    }

    /**
     * Clears the cooldown for a specific sender and command.
     *
     * @param sender The command sender
     * @param command The command key
     */
    public static final void clearCooldown(
        CommandSender sender,
        String command
    ) {
        String id = getSenderId(sender);
        Map<String, Long> userMap = cooldowns.get(id);
        if (userMap != null) {
            userMap.remove(command);
            if (userMap.isEmpty()) {
                cooldowns.remove(id);
            }
        }
    }

    /**
     * Clears all cooldowns for a sender.
     *
     * @param sender The command sender
     */
    public static final void clearAllCooldowns(CommandSender sender) {
        String id = getSenderId(sender);
        cooldowns.remove(id);
    }

    /**
     * Resolves a unique ID string from the CommandSender.
     * Uses player name or "CONSOLE" for console sender.
     */
    private static final String getSenderId(CommandSender sender) {
        if (sender instanceof ConsoleCommandSender) {
            return "CONSOLE";
        }
        return sender.getName();
    }
}
