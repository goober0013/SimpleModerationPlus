package io.github.goober0013.simplemoderationplus.commands;

import static com.mojang.brigadier.arguments.StringArgumentType.greedyString;
import static com.mojang.brigadier.arguments.StringArgumentType.word;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.github.goober0013.simplemoderationplus.SimpleModerationPlus;
import io.github.goober0013.simplemoderationplus.api.CommandFeedback;
import io.github.goober0013.simplemoderationplus.api.DurationArgumentType;
import io.github.goober0013.simplemoderationplus.api.MessageProperties;
import io.github.goober0013.simplemoderationplus.api.ModerationLogger;
import io.github.goober0013.simplemoderationplus.api.ProfilePermissions;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.PlayerProfileListResolver;
import java.time.Duration;
import java.util.Collection;
import net.kyori.adventure.text.Component;
import org.bukkit.BanEntry;
import org.bukkit.Bukkit;
import org.bukkit.ban.ProfileBanList;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BanCommand {

    public static final LiteralCommandNode<CommandSourceStack> ban() {
        return Commands.literal("ban")
            .requires(
                Commands.restricted(
                    s ->
                        s
                            .getSender()
                            .hasPermission("simplemoderationplus.ban") ||
                        s.getSender().hasPermission("minecraft.command.ban")
                )
            )
            .then(
                Commands.argument("player", ArgumentTypes.playerProfiles())
                    // permban
                    .executes(BanCommand::execute)
                    // tempban
                    .then(
                        Commands.argument(
                            "duration",
                            DurationArgumentType.duration()
                        ).executes(BanCommand::execute)
                    )
                    // permban with reason
                    .then(
                        Commands.argument("reason", greedyString()).executes(
                            BanCommand::execute
                        )
                    )
                    // tempban with reason
                    .then(
                        Commands.argument(
                            "duration",
                            DurationArgumentType.duration()
                        ).then(
                            Commands.argument(
                                "reason",
                                greedyString()
                            ).executes(BanCommand::execute)
                        )
                    )
            )
            .build();
    }

    public static final LiteralCommandNode<CommandSourceStack> permban() {
        return Commands.literal("permban")
            .requires(
                Commands.restricted(
                    s ->
                        s
                            .getSender()
                            .hasPermission("simplemoderationplus.ban") ||
                        s.getSender().hasPermission("minecraft.command.ban")
                )
            )
            .then(
                Commands.argument("player", ArgumentTypes.playerProfiles())
                    // permban
                    .executes(BanCommand::execute)
                    // permban with reason
                    .then(
                        Commands.argument("reason", greedyString()).executes(
                            BanCommand::execute
                        )
                    )
            )
            .build();
    }

    public static final LiteralCommandNode<CommandSourceStack> tempban() {
        return Commands.literal("tempban")
            .requires(
                Commands.restricted(
                    s ->
                        s
                            .getSender()
                            .hasPermission("simplemoderationplus.ban") ||
                        s.getSender().hasPermission("minecraft.command.ban")
                )
            )
            .then(
                Commands.argument("player", ArgumentTypes.playerProfiles())
                    // tempban
                    .then(
                        Commands.argument(
                            "duration",
                            DurationArgumentType.duration()
                        ).executes(BanCommand::execute)
                    )
                    // tempban with reason
                    .then(
                        Commands.argument(
                            "duration",
                            DurationArgumentType.duration()
                        ).then(
                            Commands.argument(
                                "reason",
                                greedyString()
                            ).executes(BanCommand::execute)
                        )
                    )
            )
            .build();
    }

    private static final int execute(CommandContext<CommandSourceStack> ctx)
        throws CommandSyntaxException {
        CommandSender sender = ctx.getSource().getSender();

        if (
            !(sender.hasPermission("simplemoderationplus.ban") ||
                sender.hasPermission("minecraft.command.ban"))
        ) {
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
        // Extract the list of GameProfile objects
        Collection<PlayerProfile> profiles = resolver.resolve(ctx.getSource());

        // Read optional arguments
        Duration durationholder;
        String reasonholder;
        try {
            durationholder = ctx.getArgument("duration", Duration.class);
        } catch (IllegalArgumentException e) {
            durationholder = null;
        }
        try {
            reasonholder = ctx.getArgument("reason", String.class);
        } catch (IllegalArgumentException e) {
            reasonholder = null;
        }
        final Duration duration = durationholder;
        final String reason = reasonholder;

        if (profiles.isEmpty()) {
            sender.sendMessage(
                MessageProperties.getRed(
                    "command.error.specify_a_player",
                    sender,
                    null,
                    duration,
                    reason
                )
            );
            return 0;
        }

        for (PlayerProfile profile : profiles) {
            SimpleModerationPlus.scheduler.runTaskAsynchronously(
                SimpleModerationPlus.instance,
                () -> {
                    final boolean exempt = ProfilePermissions.playerHas(
                        profile,
                        "moderationplus.exempt"
                    );

                    SimpleModerationPlus.scheduler.runTask(
                        SimpleModerationPlus.instance,
                        () -> {
                            if (
                                sender instanceof Player &&
                                ((Player) sender).getUniqueId().equals(
                                    profile.getId()
                                )
                            ) {
                                sender.sendMessage(
                                    MessageProperties.getRed(
                                        "command.error.run_on_yourself",
                                        sender,
                                        profile,
                                        duration,
                                        reason
                                    )
                                );
                                return;
                            }

                            final boolean isBanned =
                                SimpleModerationPlus.banList.isBanned(profile);
                            if (isBanned) {
                                if (
                                    SimpleModerationPlus.banList
                                            .getBanEntry(profile)
                                            .getExpiration() ==
                                        null &&
                                    duration == null
                                ) {
                                    sender.sendMessage(
                                        MessageProperties.getRed(
                                            "command.error.already_banned",
                                            sender,
                                            profile,
                                            duration,
                                            reason
                                        )
                                    );
                                    return;
                                }
                            }

                            if (
                                exempt &&
                                !sender.hasPermission(
                                    "moderationplus.bypassexempt"
                                )
                            ) {
                                sender.sendMessage(
                                    MessageProperties.getRed(
                                        "command.error.exempt",
                                        sender,
                                        profile,
                                        duration,
                                        reason
                                    )
                                );
                                return;
                            }

                            // Add to PROFILE ban list
                            SimpleModerationPlus.banList.addBan(
                                profile,
                                reason,
                                duration,
                                sender.getName()
                            );

                            // If the player is online, kick them
                            final Player online = Bukkit.getPlayer(
                                profile.getId()
                            );
                            if (online != null) {
                                online.kick(
                                    MessageProperties.get(
                                        "player" +
                                        banKey(
                                            reason != null,
                                            duration != null,
                                            isBanned
                                        ),
                                        sender,
                                        profile,
                                        duration,
                                        reason
                                    )
                                );
                            }

                            CommandFeedback.send(
                                sender,
                                MessageProperties.get(
                                    "command" +
                                    banKey(
                                        reason != null,
                                        duration != null,
                                        isBanned
                                    ),
                                    sender,
                                    profile,
                                    duration,
                                    reason
                                )
                            );

                            ModerationLogger.write(
                                "modlogs" +
                                banKey(
                                    reason != null,
                                    duration != null,
                                    isBanned
                                ),
                                sender,
                                profile,
                                duration,
                                reason
                            );

                            return;
                        }
                    );

                    return;
                }
            );
        }

        return 1;
    }

    private static final String banKey(
        boolean reason,
        boolean duration,
        boolean isBanned
    ) {
        StringBuilder key = new StringBuilder(".ban");

        if (reason || duration) {
            key.append('.');
            if (reason) {
                key.append("reason");
            }
            if (reason && duration) {
                key.append('_');
            }
            if (duration) {
                if (isBanned) {
                    key.append("change");
                } else {
                    key.append("duration");
                }
            }
        }

        return key.toString();
    }
}
