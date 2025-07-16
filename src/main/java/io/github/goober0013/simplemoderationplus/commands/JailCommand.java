package io.github.goober0013.simplemoderationplus.commands;

import static com.mojang.brigadier.arguments.StringArgumentType.greedyString;
import static com.mojang.brigadier.arguments.StringArgumentType.word;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.github.goober0013.simplemoderationplus.SimpleModerationPlus;
import io.github.goober0013.simplemoderationplus.api.CommandFeedback;
import io.github.goober0013.simplemoderationplus.api.ConfigUtils;
import io.github.goober0013.simplemoderationplus.api.DurationArgumentType;
import io.github.goober0013.simplemoderationplus.api.JailEntry;
import io.github.goober0013.simplemoderationplus.api.MessageProperties;
import io.github.goober0013.simplemoderationplus.api.ModerationLogger;
import io.github.goober0013.simplemoderationplus.api.ProfileJailList;
import io.github.goober0013.simplemoderationplus.api.ProfilePermissions;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.PlayerProfileListResolver;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Collection;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class JailCommand {

    public static final LiteralCommandNode<CommandSourceStack> jail() {
        return Commands.literal("jail")
            .requires(
                Commands.restricted(s ->
                    s.getSender().hasPermission("simplemoderationplus.jail")
                )
            )
            .then(
                Commands.argument("player", ArgumentTypes.playerProfiles())
                    // permjail
                    .executes(JailCommand::execute)
                    // tempjail
                    .then(
                        Commands.argument(
                            "duration",
                            DurationArgumentType.duration()
                        ).executes(JailCommand::execute)
                    )
                    // permjail with reason
                    .then(
                        Commands.argument("reason", greedyString()).executes(
                            JailCommand::execute
                        )
                    )
                    // tempjail with reason
                    .then(
                        Commands.argument(
                            "duration",
                            DurationArgumentType.duration()
                        ).then(
                            Commands.argument(
                                "reason",
                                greedyString()
                            ).executes(JailCommand::execute)
                        )
                    )
            )
            .build();
    }

    public static final LiteralCommandNode<CommandSourceStack> permjail() {
        return Commands.literal("permjail")
            .requires(
                Commands.restricted(s ->
                    s.getSender().hasPermission("simplemoderationplus.jail")
                )
            )
            .then(
                Commands.argument("player", ArgumentTypes.playerProfiles())
                    // permjail
                    .executes(JailCommand::execute)
                    // permjail with reason
                    .then(
                        Commands.argument("reason", greedyString()).executes(
                            JailCommand::execute
                        )
                    )
            )
            .build();
    }

    public static final LiteralCommandNode<CommandSourceStack> tempjail() {
        return Commands.literal("tempjail")
            .requires(
                Commands.restricted(s ->
                    s.getSender().hasPermission("simplemoderationplus.jail")
                )
            )
            .then(
                Commands.argument("player", ArgumentTypes.playerProfiles())
                    // tempjail
                    .then(
                        Commands.argument(
                            "duration",
                            DurationArgumentType.duration()
                        ).executes(JailCommand::execute)
                    )
                    // tempjail with reason
                    .then(
                        Commands.argument(
                            "duration",
                            DurationArgumentType.duration()
                        ).then(
                            Commands.argument(
                                "reason",
                                greedyString()
                            ).executes(JailCommand::execute)
                        )
                    )
            )
            .build();
    }

    private static final int execute(CommandContext<CommandSourceStack> ctx)
        throws CommandSyntaxException {
        CommandSender sender = ctx.getSource().getSender();

        if (!sender.hasPermission("simplemoderationplus.jail")) {
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
            SimpleModerationPlus.asyncScheduler.runNow(
                SimpleModerationPlus.instance,
                s -> {
                    final boolean exempt = ProfilePermissions.playerHas(
                        profile,
                        "simplemoderationplus.exempt"
                    );

                    SimpleModerationPlus.scheduler.execute(
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

                            final boolean isJailed =
                                ProfileJailList.get(profile) != null;
                            if (isJailed) {
                                if (isJailed && duration == null) {
                                    sender.sendMessage(
                                        MessageProperties.getRed(
                                            "command.error.already_jailed",
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

                            // Add to PROFILE jail list
                            ProfileJailList.addJail(
                                profile,
                                reason,
                                duration,
                                sender.getName()
                            );

                            // If the player is online, notify and teleport them
                            final Player online = Bukkit.getPlayer(
                                profile.getId()
                            );
                            if (online != null) {
                                // Notify
                                online.sendMessage(
                                    MessageProperties.getRed(
                                        "player" +
                                        jailKey(
                                            reason != null,
                                            duration != null,
                                            isJailed
                                        ),
                                        sender,
                                        profile,
                                        duration,
                                        reason
                                    )
                                );

                                // Teleport
                                try {
                                    final Path configFile =
                                        SimpleModerationPlus.dataFolder
                                            .toPath()
                                            .resolve("jail.yml");

                                    final Location jail =
                                        ConfigUtils.loadLocation(
                                            configFile,
                                            "jail"
                                        );
                                    online.teleportAsync(jail);
                                } catch (IOException e) {
                                    SimpleModerationPlus.logger.severe(
                                        MessageProperties.get(
                                            "log.error.ioexception"
                                        )
                                    );
                                }
                            }

                            CommandFeedback.send(
                                sender,
                                MessageProperties.get(
                                    "command" +
                                    jailKey(
                                        reason != null,
                                        duration != null,
                                        isJailed
                                    ),
                                    sender,
                                    profile,
                                    duration,
                                    reason
                                )
                            );

                            ModerationLogger.write(
                                "modlogs" +
                                jailKey(
                                    reason != null,
                                    duration != null,
                                    isJailed
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

    private static final String jailKey(
        boolean reason,
        boolean duration,
        boolean isJailed
    ) {
        StringBuilder key = new StringBuilder(".jail");

        if (reason || duration) {
            key.append('.');
            if (reason) {
                key.append("reason");
            }
            if (reason && duration) {
                key.append('_');
            }
            if (duration) {
                if (isJailed) {
                    key.append("change");
                } else {
                    key.append("duration");
                }
            }
        }

        return key.toString();
    }
}
