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
import io.github.goober0013.simplemoderationplus.api.MuteEntry;
import io.github.goober0013.simplemoderationplus.api.ProfileMuteList;
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

public class UnmuteCommand {

    public static final LiteralCommandNode<CommandSourceStack> unmute() {
        return Commands.literal("unmute")
            .requires(
                Commands.restricted(s ->
                    s.getSender().hasPermission("simplemoderationplus.unmute")
                )
            )
            .then(
                Commands.argument("player", ArgumentTypes.playerProfiles())
                    .suggests(suggestMutedPlayers)
                    // unban
                    .executes(UnmuteCommand::execute)
                    // unban with reason
                    .then(
                        Commands.argument("reason", greedyString()).executes(
                            UnmuteCommand::execute
                        )
                    )
            )
            .build();
    }

    private static final int execute(CommandContext<CommandSourceStack> ctx)
        throws CommandSyntaxException {
        CommandSender sender = ctx.getSource().getSender();

        if (!sender.hasPermission("simplemoderationplus.unmute")) {
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
            if (!ProfileMuteList.isMuted(profile)) {
                sender.sendMessage(
                    MessageProperties.getRed(
                        "command.error.isnt_muted",
                        sender,
                        profile,
                        null,
                        reason
                    )
                );

                return 0;
            }

            // Remove from PROFILE mute list
            ProfileMuteList.unmute(profile);

            // If the player is online, notify them
            final Player online = Bukkit.getPlayer(profile.getId());
            if (online != null) {
                online.sendMessage(
                    MessageProperties.get(
                        "player" + unmuteKey(reason != null),
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
                    "command" + unmuteKey(reason != null),
                    sender,
                    profile,
                    null,
                    reason
                )
            );

            ModerationLogger.write(
                "modlogs" + unmuteKey(reason != null),
                sender,
                profile,
                null,
                reason
            );
        }

        return 1;
    }

    private static final String unmuteKey(boolean reason) {
        StringBuilder key = new StringBuilder(".unmute");

        if (reason) {
            key.append(".reason");
        }

        return key.toString();
    }

    private static final SuggestionProvider<
        CommandSourceStack
    > suggestMutedPlayers = UnmuteCommand::provideMutedPlayerSuggestions;

    private static CompletableFuture<Suggestions> provideMutedPlayerSuggestions(
        CommandContext<CommandSourceStack> ctx,
        SuggestionsBuilder sb
    ) {
        for (String name : ProfileMuteList.getEntries()
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
