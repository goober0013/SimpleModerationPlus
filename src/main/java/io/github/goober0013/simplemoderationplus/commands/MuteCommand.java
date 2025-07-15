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
import io.github.goober0013.simplemoderationplus.api.MuteEntry;
import io.github.goober0013.simplemoderationplus.api.ProfileMuteList;
import io.github.goober0013.simplemoderationplus.api.ProfilePermissions;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.PlayerProfileListResolver;
import java.time.Duration;
import java.util.Collection;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MuteCommand {

    public static final LiteralCommandNode<CommandSourceStack> mute() {
        return Commands.literal("mute")
            .requires(
                Commands.restricted(s ->
                    s.getSender().hasPermission("simplemoderationplus.mute")
                )
            )
            .then(
                Commands.argument("player", ArgumentTypes.playerProfiles())
                    // permmute
                    .executes(MuteCommand::execute)
                    // tempmute
                    .then(
                        Commands.argument(
                            "duration",
                            DurationArgumentType.duration()
                        ).executes(MuteCommand::execute)
                    )
                    // permmute with reason
                    .then(
                        Commands.argument("reason", greedyString()).executes(
                            MuteCommand::execute
                        )
                    )
                    // tempmute with reason
                    .then(
                        Commands.argument(
                            "duration",
                            DurationArgumentType.duration()
                        ).then(
                            Commands.argument(
                                "reason",
                                greedyString()
                            ).executes(MuteCommand::execute)
                        )
                    )
            )
            .build();
    }

    public static final LiteralCommandNode<CommandSourceStack> permmute() {
        return Commands.literal("permmute")
            .requires(
                Commands.restricted(s ->
                    s.getSender().hasPermission("simplemoderationplus.mute")
                )
            )
            .then(
                Commands.argument("player", ArgumentTypes.playerProfiles())
                    // permmute
                    .executes(MuteCommand::execute)
                    // permmute with reason
                    .then(
                        Commands.argument("reason", greedyString()).executes(
                            MuteCommand::execute
                        )
                    )
            )
            .build();
    }

    public static final LiteralCommandNode<CommandSourceStack> tempmute() {
        return Commands.literal("tempmute")
            .requires(
                Commands.restricted(s ->
                    s.getSender().hasPermission("simplemoderationplus.mute")
                )
            )
            .then(
                Commands.argument("player", ArgumentTypes.playerProfiles())
                    // tempmute
                    .then(
                        Commands.argument(
                            "duration",
                            DurationArgumentType.duration()
                        ).executes(MuteCommand::execute)
                    )
                    // tempmute with reason
                    .then(
                        Commands.argument(
                            "duration",
                            DurationArgumentType.duration()
                        ).then(
                            Commands.argument(
                                "reason",
                                greedyString()
                            ).executes(MuteCommand::execute)
                        )
                    )
            )
            .build();
    }

    private static final int execute(CommandContext<CommandSourceStack> ctx)
        throws CommandSyntaxException {
        CommandSender sender = ctx.getSource().getSender();

        if (!sender.hasPermission("simplemoderationplus.mute")) {
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
                        "simplemoderationplus.exempt"
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

                            final boolean isMuted =
                                ProfileMuteList.get(profile) != null;
                            if (isMuted) {
                                if (isMuted && duration == null) {
                                    sender.sendMessage(
                                        MessageProperties.getRed(
                                            "command.error.already_muted",
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
                                    "simplemoderationplus.bypassexempt"
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

                            // Add to PROFILE mute list
                            ProfileMuteList.addMute(
                                profile,
                                reason,
                                duration,
                                sender.getName()
                            );

                            // If the player is online, notify them
                            final Player online = Bukkit.getPlayer(
                                profile.getId()
                            );
                            if (online != null) {
                                online.sendMessage(
                                    MessageProperties.getRed(
                                        "player" +
                                        muteKey(
                                            reason != null,
                                            duration != null,
                                            isMuted
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
                                    muteKey(
                                        reason != null,
                                        duration != null,
                                        isMuted
                                    ),
                                    sender,
                                    profile,
                                    duration,
                                    reason
                                )
                            );

                            ModerationLogger.write(
                                "modlogs" +
                                muteKey(
                                    reason != null,
                                    duration != null,
                                    isMuted
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

    private static final String muteKey(
        boolean reason,
        boolean duration,
        boolean isMuted
    ) {
        StringBuilder key = new StringBuilder(".mute");

        if (reason || duration) {
            key.append('.');
            if (reason) {
                key.append("reason");
            }
            if (reason && duration) {
                key.append('_');
            }
            if (duration) {
                if (isMuted) {
                    key.append("change");
                } else {
                    key.append("duration");
                }
            }
        }

        return key.toString();
    }
}
