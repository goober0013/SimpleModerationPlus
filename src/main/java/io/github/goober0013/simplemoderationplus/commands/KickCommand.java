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

public class KickCommand {

    public static final LiteralCommandNode<CommandSourceStack> kick() {
        return Commands.literal("kick")
            .requires(
                Commands.restricted(
                    s ->
                        s
                            .getSender()
                            .hasPermission("simplemoderationplus.kick") ||
                        s.getSender().hasPermission("minecraft.command.kick")
                )
            )
            .then(
                Commands.argument("player", ArgumentTypes.playerProfiles())
                    // kick
                    .executes(KickCommand::execute)
                    // kick with reason
                    .then(
                        Commands.argument("reason", greedyString()).executes(
                            KickCommand::execute
                        )
                    )
            )
            .build();
    }

    private static final int execute(CommandContext<CommandSourceStack> ctx)
        throws CommandSyntaxException {
        CommandSender sender = ctx.getSource().getSender();

        if (
            !(sender.hasPermission("simplemoderationplus.kick") ||
                sender.hasPermission("minecraft.command.kick"))
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
                                        null,
                                        reason
                                    )
                                );
                                return;
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
                                        null,
                                        reason
                                    )
                                );
                                return;
                            }

                            // If the player is online, kick them
                            final Player online = Bukkit.getPlayer(
                                profile.getId()
                            );
                            if (online != null) {
                                online.kick(
                                    MessageProperties.get(
                                        "player" + kickKey(reason != null),
                                        sender,
                                        profile,
                                        null,
                                        reason
                                    )
                                );
                            } else {
                                sender.sendMessage(
                                    MessageProperties.getRed(
                                        "command.error.isnt_online",
                                        sender,
                                        profile,
                                        null,
                                        reason
                                    )
                                );

                                return;
                            }

                            CommandFeedback.send(
                                sender,
                                MessageProperties.get(
                                    "command" + kickKey(reason != null),
                                    sender,
                                    profile,
                                    null,
                                    reason
                                )
                            );

                            ModerationLogger.write(
                                "modlogs" + kickKey(reason != null),
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

    private static final String kickKey(boolean reason) {
        StringBuilder key = new StringBuilder(".kick");

        if (reason) {
            key.append(".reason");
        }

        return key.toString();
    }
}
