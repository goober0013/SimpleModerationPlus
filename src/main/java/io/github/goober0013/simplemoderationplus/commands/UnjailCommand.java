package io.github.goober0013.simplemoderationplus.commands;

import static com.mojang.brigadier.arguments.StringArgumentType.greedyString;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.github.goober0013.simplemoderationplus.SimpleModerationPlus;
import io.github.goober0013.simplemoderationplus.api.CommandFeedback;
import io.github.goober0013.simplemoderationplus.api.JailEntry;
import io.github.goober0013.simplemoderationplus.api.MessageProperties;
import io.github.goober0013.simplemoderationplus.api.ModerationLogger;
import io.github.goober0013.simplemoderationplus.api.ProfileJailList;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.PlayerProfileListResolver;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class UnjailCommand {

    public static final LiteralCommandNode<CommandSourceStack> unjail() {
        return Commands.literal("unjail")
            .requires(
                Commands.restricted(s ->
                    s.getSender().hasPermission("simplemoderationplus.unjail")
                )
            )
            .then(
                Commands.argument("player", ArgumentTypes.playerProfiles())
                    .suggests(suggestJailedPlayers)
                    // unban
                    .executes(UnjailCommand::execute)
                    // unban with reason
                    .then(
                        Commands.argument("reason", greedyString()).executes(
                            UnjailCommand::execute
                        )
                    )
            )
            .build();
    }

    private static final int execute(CommandContext<CommandSourceStack> ctx)
        throws CommandSyntaxException {
        CommandSender sender = ctx.getSource().getSender();

        if (!sender.hasPermission("simplemoderationplus.unjail")) {
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
        String reason;
        try {
            reason = ctx.getArgument("reason", String.class);
        } catch (IllegalArgumentException e) {
            reason = null;
        }

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
            if (!ProfileJailList.isJailed(profile)) {
                sender.sendMessage(
                    MessageProperties.getRed(
                        "command.error.isnt_jailed",
                        sender,
                        profile,
                        null,
                        reason
                    )
                );

                return 0;
            }

            // Remove from PROFILE jail list
            ProfileJailList.unjail(profile);

            // If the player is online, notify them
            final Player online = Bukkit.getPlayer(profile.getId());
            if (online != null) {
                online.sendMessage(
                    MessageProperties.get(
                        "player" + unjailKey(reason != null),
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
                    "command" + unjailKey(reason != null),
                    sender,
                    profile,
                    null,
                    reason
                )
            );

            ModerationLogger.write(
                "modlogs" + unjailKey(reason != null),
                sender,
                profile,
                null,
                reason
            );
        }

        return 1;
    }

    private static final String unjailKey(boolean reason) {
        StringBuilder key = new StringBuilder(".unjail");

        if (reason) {
            key.append(".reason");
        }

        return key.toString();
    }

    private static final SuggestionProvider<
        CommandSourceStack
    > suggestJailedPlayers = UnjailCommand::provideJailedPlayerSuggestions;

    private static CompletableFuture<
        Suggestions
    > provideJailedPlayerSuggestions(
        CommandContext<CommandSourceStack> ctx,
        SuggestionsBuilder sb
    ) {
        for (String name : ProfileJailList.getEntries()
            .stream()
            .map(entry -> entry.getProfile())
            .filter(player -> player instanceof PlayerProfile)
            .map(player -> ((PlayerProfile) player).getName())
            .filter(text -> text != null)
            .collect(Collectors.toList())) {
            sb.suggest(name);
        }
        return sb.buildFuture();
    }
}
