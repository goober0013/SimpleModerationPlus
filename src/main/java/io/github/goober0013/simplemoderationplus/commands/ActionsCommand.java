package io.github.goober0013.simplemoderationplus.commands;

import static com.mojang.brigadier.arguments.StringArgumentType.greedyString;
import static com.mojang.brigadier.arguments.StringArgumentType.word;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.github.goober0013.simplemoderationplus.api.CommandFeedback;
import io.github.goober0013.simplemoderationplus.api.MessageProperties;
import io.github.goober0013.simplemoderationplus.api.ModerationLogger;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.PlayerProfileListResolver;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;

public class ActionsCommand {

    public static final LiteralCommandNode<CommandSourceStack> actions() {
        return Commands.literal("actions")
            .requires(
                Commands.restricted(s ->
                    s.getSender().hasPermission("simplemoderationplus.actions")
                )
            )
            .then(
                Commands.argument("player", ArgumentTypes.playerProfiles())
                    // view actions
                    .executes(ActionsCommand::execute)
            )
            .build();
    }

    private static final int execute(CommandContext<CommandSourceStack> ctx)
        throws CommandSyntaxException {
        CommandSender sender = ctx.getSource().getSender();

        if (!sender.hasPermission("simplemoderationplus.actions")) {
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

        if (profiles.isEmpty()) {
            sender.sendMessage(
                MessageProperties.getRed(
                    "command.error.specify_a_player",
                    sender,
                    null,
                    null,
                    null
                )
            );
            return 0;
        }

        // Your ban logic here, e.g.:
        for (PlayerProfile profile : profiles) {
            final List<Component> playerLog = ModerationLogger.get(profile);

            if (playerLog == null) {
                CommandFeedback.send(
                    sender,
                    MessageProperties.get(
                        "command.actions.empty",
                        sender,
                        profile,
                        null,
                        null
                    )
                );

                continue;
            }

            CommandFeedback.send(
                sender,
                MessageProperties.get(
                    "command.actions",
                    sender,
                    profile,
                    null,
                    null
                )
            );

            List<Component> firstFive = playerLog
                .stream()
                .limit(5)
                .collect(Collectors.toList()); // collect limited elements
            Collections.reverse(firstFive); // reverse in place
            firstFive.forEach(action -> {
                CommandFeedback.send(sender, action);
            });
        }

        return 1;
    }
}
