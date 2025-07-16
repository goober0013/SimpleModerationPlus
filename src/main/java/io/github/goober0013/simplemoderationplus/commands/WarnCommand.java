package io.github.goober0013.simplemoderationplus.commands;

import static com.mojang.brigadier.arguments.StringArgumentType.greedyString;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.github.goober0013.simplemoderationplus.SimpleModerationPlus;
import io.github.goober0013.simplemoderationplus.api.CommandFeedback;
import io.github.goober0013.simplemoderationplus.api.MessageProperties;
import io.github.goober0013.simplemoderationplus.api.ModerationLogger;
import io.github.goober0013.simplemoderationplus.api.MuteEntry;
import io.github.goober0013.simplemoderationplus.api.ProfileMuteList;
import io.github.goober0013.simplemoderationplus.api.ProfilePermissions;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.PlayerProfileListResolver;
import java.util.Collection;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class WarnCommand {

    public static final LiteralCommandNode<CommandSourceStack> warn() {
        return Commands.literal("warn")
            .requires(
                Commands.restricted(s ->
                    s.getSender().hasPermission("simplemoderationplus.warn")
                )
            )
            .then(
                Commands.argument("player", ArgumentTypes.playerProfiles())
                    // warn
                    .executes(WarnCommand::execute)
                    // warn with reason
                    .then(
                        Commands.argument("reason", greedyString()).executes(
                            WarnCommand::execute
                        )
                    )
            )
            .build();
    }

    private static final int execute(CommandContext<CommandSourceStack> ctx)
        throws CommandSyntaxException {
        CommandSender sender = ctx.getSource().getSender();

        if (!sender.hasPermission("simplemoderationplus.warn")) {
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
        String reasonholder;
        try {
            reasonholder = ctx.getArgument("reason", String.class);
        } catch (IllegalArgumentException e) {
            reasonholder = null;
        }
        final String reason = reasonholder;

        if (profiles.isEmpty()) {
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
                                        null,
                                        reason
                                    )
                                );
                                return;
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
                                        null,
                                        reason
                                    )
                                );
                                return;
                            }

                            // If the player is online, notify them
                            final Player online = Bukkit.getPlayer(
                                profile.getId()
                            );
                            if (online != null) {
                                online.sendMessage(
                                    MessageProperties.getRed(
                                        "player" + warnKey(reason != null),
                                        sender,
                                        profile,
                                        null,
                                        reason
                                    )
                                );
                            }

                            CommandFeedback.send(
                                sender,
                                MessageProperties.get(
                                    "command" + warnKey(reason != null),
                                    sender,
                                    profile,
                                    null,
                                    reason
                                )
                            );

                            ModerationLogger.write(
                                "modlogs" + warnKey(reason != null),
                                sender,
                                profile,
                                null,
                                reason
                            );
                        }
                    );
                }
            );
        }

        return 1;
    }

    private static final String warnKey(boolean reason) {
        StringBuilder key = new StringBuilder(".warn");

        if (reason) {
            key.append(".reason");
        }

        return key.toString();
    }
}
