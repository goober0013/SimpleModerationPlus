package io.github.goober0013.simplemoderationplus.commands;

import static com.mojang.brigadier.arguments.StringArgumentType.greedyString;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.github.goober0013.simplemoderationplus.SimpleModerationPlus;
import io.github.goober0013.simplemoderationplus.api.CommandConfirmation;
import io.github.goober0013.simplemoderationplus.api.CommandCooldown;
import io.github.goober0013.simplemoderationplus.api.CommandFeedback;
import io.github.goober0013.simplemoderationplus.api.MessageProperties;
import io.github.goober0013.simplemoderationplus.api.ModerationLogger;
import io.github.goober0013.simplemoderationplus.api.MuteEntry;
import io.github.goober0013.simplemoderationplus.api.ProfileMuteList;
import io.github.goober0013.simplemoderationplus.api.ProfilePermissions;
import io.github.goober0013.simplemoderationplus.api.VoteKickManager;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.PlayerProfileListResolver;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class VoteKickCommand {

    public static final LiteralCommandNode<CommandSourceStack> vk() {
        return Commands.literal("vk")
            .requires(
                Commands.restricted(s ->
                    s
                        .getSender()
                        .hasPermission("simplemoderationplus.votekick.vote")
                )
            )
            .then(
                Commands.argument("player", ArgumentTypes.playerProfiles())
                    // vote in votekick
                    .executes(VoteKickCommand::execute)
                    // start votekick
                    .then(
                        Commands.argument("reason", greedyString())
                            .requires(
                                Commands.restricted(s ->
                                    s
                                        .getSender()
                                        .hasPermission(
                                            "simplemoderationplus.votekick"
                                        )
                                )
                            )
                            .executes(VoteKickCommand::execute)
                    )
            )
            .build();
    }

    public static final LiteralCommandNode<CommandSourceStack> vkcancel() {
        return Commands.literal("vkcancel")
            .requires(
                Commands.restricted(s ->
                    s
                        .getSender()
                        .hasPermission("simplemoderationplus.votekick.cancel")
                )
            )
            .executes(VoteKickCommand::cancel)
            .build();
    }

    private static final int cancel(CommandContext<CommandSourceStack> ctx)
        throws CommandSyntaxException {
        CommandSender sender = ctx.getSource().getSender();

        if (!sender.hasPermission("simplemoderationplus.votekick.cancel")) {
            sender.sendMessage(
                MessageProperties.getRed(
                    "command.error.no_permission",
                    sender,
                    null,
                    null,
                    null
                )
            );
            return 0;
        }

        if (!VoteKickManager.clearVoteKicks(sender)) {
            sender.sendMessage(
                MessageProperties.getRed(
                    "command.error.vkcancel",
                    sender,
                    null,
                    null,
                    null
                )
            );

            return 0;
        }
        return 1;
    }

    private static final int execute(CommandContext<CommandSourceStack> ctx)
        throws CommandSyntaxException {
        CommandSender sender = ctx.getSource().getSender();

        if (!sender.hasPermission("simplemoderationplus.votekick.vote")) {
            sender.sendMessage(
                MessageProperties.getRed(
                    "command.error.no_permission",
                    sender,
                    null,
                    null,
                    null
                )
            );
            return 0;
        }

        PlayerProfileListResolver resolver = ctx.getArgument(
            "player",
            PlayerProfileListResolver.class
        );

        // Read optional arguments
        String reasonholder;
        try {
            reasonholder = ctx.getArgument("reason", String.class);
        } catch (IllegalArgumentException e) {
            reasonholder = null;
        }
        final String reason = reasonholder;

        // Extract the list of GameProfile objects
        Collection<PlayerProfile> profiles = resolver.resolve(ctx.getSource());

        if (profiles.isEmpty() || profiles.size() > 1) {
            sender.sendMessage(
                MessageProperties.getRed(
                    "command.error.specify_a_player",
                    sender,
                    null,
                    null,
                    reason
                )
            );
            return 0;
        }

        final PlayerProfile profile = profiles.iterator().next();

        if (
            sender instanceof Player &&
            ((Player) sender).getUniqueId().equals(profile.getId())
        ) {
            sender.sendMessage(
                MessageProperties.getRed(
                    "command.error.run_on_yourself",
                    sender,
                    profile,
                    null,
                    reason
                )
            );
            return 0;
        }

        // Check that the player is actually online
        final UUID uuid = profile.getId();
        if (uuid == null) return 0;

        final OfflinePlayer offline = Bukkit.getOfflinePlayer(uuid);
        final long lastPlayed = offline.getLastSeen();

        if (
            lastPlayed <
            Instant.now().toEpochMilli() - Duration.ofMinutes(1).toMillis()
        ) {
            sender.sendMessage(
                MessageProperties.getRed(
                    "command.error.isnt_online",
                    sender,
                    profile,
                    null,
                    reason
                )
            );
            return 0;
        }

        // Check that the player isn't already banned
        if (SimpleModerationPlus.banList.isBanned(profile)) {
            sender.sendMessage(
                MessageProperties.getRed(
                    "command.error.isnt_online",
                    sender,
                    profile,
                    null,
                    reason
                )
            );
            return 0;
        }

        final int online = SimpleModerationPlus.server
            .getOnlinePlayers()
            .size();
        if (online <= 2) {
            sender.sendMessage(
                MessageProperties.getRed(
                    "command.error.not_enough_players",
                    sender,
                    profile,
                    null,
                    reason
                )
            );
            return 0;
        }

        SimpleModerationPlus.scheduler.runTaskAsynchronously(
            SimpleModerationPlus.instance,
            () -> {
                final boolean exempt = ProfilePermissions.playerHas(
                    profile,
                    "simplemoderationplus.exempt"
                );

                SimpleModerationPlus.scheduler.runTask(
                    SimpleModerationPlus.instance,
                    () -> {
                        if (exempt) {
                            sender.sendMessage(
                                MessageProperties.getRed(
                                    "command.error.exempt",
                                    sender,
                                    profile,
                                    null,
                                    reason
                                )
                            );
                            return;
                        }

                        // Check if we are voting on an existing votekick
                        if (VoteKickManager.isVoteKickActive(offline)) {
                            final int voteCounted = VoteKickManager.addVote(
                                offline,
                                sender
                            );
                            if (voteCounted != -1) {
                                if (voteCounted == 0) {
                                    sender.sendMessage(
                                        MessageProperties.getRed(
                                            "command.error.already_voted",
                                            sender,
                                            profile,
                                            null,
                                            reason
                                        )
                                    );
                                    return;
                                }

                                return;
                            }
                        }

                        // Starting a vote kick

                        // Check for a valid reason
                        if (reason == null) {
                            sender.sendMessage(
                                MessageProperties.getRed(
                                    "command.error.specify_a_reason",
                                    sender,
                                    profile,
                                    null,
                                    reason
                                )
                            );
                            return;
                        }

                        // Check the cooldown (if sender is not exempt)
                        if (
                            !sender.hasPermission("simplemoderationplus.exempt")
                        ) {
                            if (
                                CommandCooldown.isOnCooldown(
                                    sender,
                                    "vk",
                                    60000
                                )
                            ) {
                                sender.sendMessage(
                                    MessageProperties.getRed(
                                        "command.error.cooldown",
                                        sender,
                                        profile,
                                        null,
                                        reason
                                    )
                                );

                                return;
                            }
                        }

                        // Confirm to start the votekick
                        final String commandRan = ctx.getInput();
                        if (
                            !CommandConfirmation.isConfirmed(sender, commandRan)
                        ) {
                            CommandConfirmation.logCommand(sender, commandRan);
                            sender.sendMessage(
                                MessageProperties.getRed(
                                    "command.votekick.confirm",
                                    sender,
                                    profile,
                                    null,
                                    reason
                                )
                            );
                            return;
                        }

                        // Set the cooldown
                        CommandCooldown.setCooldown(sender, "vk");

                        VoteKickManager.startVoteKick(offline, sender, reason);
                        VoteKickManager.addVote(offline, sender);
                    }
                );
            }
        );

        return 1;
    }
}
