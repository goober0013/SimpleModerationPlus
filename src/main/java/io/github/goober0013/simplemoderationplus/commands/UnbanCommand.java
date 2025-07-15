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
import io.github.goober0013.simplemoderationplus.api.MessageProperties;
import io.github.goober0013.simplemoderationplus.api.ModerationLogger;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.PlayerProfileListResolver;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.bukkit.ban.ProfileBanList;
import org.bukkit.command.CommandSender;

public class UnbanCommand {

    public static final LiteralCommandNode<CommandSourceStack> unban() {
        return Commands.literal("unban")
            .requires(
                Commands.restricted(
                    s ->
                        s
                            .getSender()
                            .hasPermission("simplemoderationplus.unban") ||
                        s.getSender().hasPermission("minecraft.command.pardon")
                )
            )
            .then(
                Commands.argument("player", ArgumentTypes.playerProfiles())
                    .suggests(suggestBannedPlayers)
                    // unban
                    .executes(UnbanCommand::execute)
                    // unban with reason
                    .then(
                        Commands.argument("reason", greedyString()).executes(
                            UnbanCommand::execute
                        )
                    )
            )
            .build();
    }

    private static final int execute(CommandContext<CommandSourceStack> ctx)
        throws CommandSyntaxException {
        CommandSender sender = ctx.getSource().getSender();

        if (
            !(sender.hasPermission("simplemoderationplus.unmute") ||
                sender.hasPermission("minecraft.command.pardon"))
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
            if (!SimpleModerationPlus.banList.isBanned(profile)) {
                sender.sendMessage(
                    MessageProperties.getRed(
                        "command.error.isnt_banned",
                        sender,
                        profile,
                        null,
                        reason
                    )
                );

                return 0;
            }

            // Remove from PROFILE ban list
            SimpleModerationPlus.banList.pardon(profile);

            CommandFeedback.send(
                sender,
                MessageProperties.get(
                    "command" + unbanKey(reason != null),
                    sender,
                    profile,
                    null,
                    reason
                )
            );

            ModerationLogger.write(
                "modlogs" + unbanKey(reason != null),
                sender,
                profile,
                null,
                reason
            );
        }

        return 1;
    }

    private static final String unbanKey(boolean reason) {
        StringBuilder key = new StringBuilder(".unban");

        if (reason) {
            key.append(".reason");
        }

        return key.toString();
    }

    private static final SuggestionProvider<
        CommandSourceStack
    > suggestBannedPlayers = UnbanCommand::provideBannedPlayerSuggestions;

    private static CompletableFuture<
        Suggestions
    > provideBannedPlayerSuggestions(
        CommandContext<CommandSourceStack> ctx,
        SuggestionsBuilder sb
    ) {
        for (String name : SimpleModerationPlus.banList
            .getEntries()
            .stream()
            .map(entry -> entry.getBanTarget())
            .filter(player -> player instanceof PlayerProfile)
            .map(entry -> ((PlayerProfile) entry).getName())
            .collect(Collectors.toList())) {
            sb.suggest(name);
        }
        return sb.buildFuture();
    }
}
