package io.github.goober0013.simplemoderationplus.commands;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.github.goober0013.simplemoderationplus.SimpleModerationPlus;
import io.github.goober0013.simplemoderationplus.api.CommandFeedback;
import io.github.goober0013.simplemoderationplus.api.ConfigUtils;
import io.github.goober0013.simplemoderationplus.api.MessageProperties;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import java.nio.file.Path;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SetJailCommand {

    public static final LiteralCommandNode<CommandSourceStack> setJail() {
        return Commands.literal("setjail")
            .requires(
                Commands.restricted(
                    s ->
                        s
                            .getSender()
                            .hasPermission("simplemoderationplus.setjail") &&
                        s.getSender() instanceof Player
                )
            )
            .executes(SetJailCommand::execute)
            .build();
    }

    private static final int execute(CommandContext<CommandSourceStack> ctx)
        throws CommandSyntaxException {
        CommandSender sender = ctx.getSource().getSender();

        if (!sender.hasPermission("simplemoderationplus.setjail")) {
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

        if (!(sender instanceof Player)) {
            sender.sendMessage(
                MessageProperties.getRed(
                    "command.error.only_players",
                    sender,
                    null,
                    null,
                    null
                )
            );
            return 0;
        }

        final Player player = (Player) sender;

        if (!player.locale().equals(Locale.of("en_US"))) {
            sender.sendMessage(player.locale().getLanguage().toString());
            sender.sendMessage(player.locale().getScript().toString());
            sender.sendMessage(player.locale().getCountry().toString());
            sender.sendMessage(player.locale().getVariant().toString());
        }

        final Path configFile = SimpleModerationPlus.dataFolder
            .toPath()
            .resolve("jail.yml");

        ConfigUtils.setLocation(configFile, ((Player) sender).getLocation());
        CommandFeedback.send(
            sender,
            MessageProperties.get("command.setjail", sender, null, null, null)
        );

        return 1;
    }
}
