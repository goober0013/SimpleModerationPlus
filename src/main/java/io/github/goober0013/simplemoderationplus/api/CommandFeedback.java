package io.github.goober0013.simplemoderationplus.api;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

public final class CommandFeedback {

    /**
     * Sends a message to the given sender, then broadcasts it to all operators
     * in gray italic format like "[playerName: message]".
     *
     * @param sender the original command sender
     * @param text   the core message as an Adventure Component
     */
    public static void send(CommandSender sender, Component text) {
        // Direct feedback to the issuer
        sender.sendMessage(text);

        // Build gray italic broadcast "[senderName: message]"
        Component formatted = Component.text(
            "[",
            NamedTextColor.GRAY,
            TextDecoration.ITALIC
        )
            .append(
                Component.text(
                    sender.getName(),
                    NamedTextColor.GRAY,
                    TextDecoration.ITALIC
                )
            )
            .append(
                Component.text(": ", NamedTextColor.GRAY, TextDecoration.ITALIC)
            )
            .append(
                text.color(NamedTextColor.GRAY).decorate(TextDecoration.ITALIC)
            )
            .append(
                Component.text("]", NamedTextColor.GRAY, TextDecoration.ITALIC)
            );

        Collection<? extends Player> online = Bukkit.getOnlinePlayers();

        // determine if the issuer is a player and grab their UUID
        final UUID senderUuid;
        if (sender instanceof Player pl) {
            senderUuid = pl.getUniqueId();
        } else {
            senderUuid = null;
        }

        // build the audience list, excluding the issuer by UUID
        List<Audience> recipients = Bukkit.getOnlinePlayers()
            .stream()
            // if senderUuid is non-null, filter out that UUID
            .filter(p ->
                !(sender instanceof Player &&
                    p.getUniqueId().equals(((Player) sender).getUniqueId()))
            ) // keep only those with the feedback permission
            .filter(p -> p.hasPermission("minecraft.admin.command_feedback"))
            .map(p -> (Audience) p)
            .collect(Collectors.toList());

        if (!(sender instanceof ConsoleCommandSender)) {
            recipients.add((Audience) Bukkit.getConsoleSender());
        }

        Audience audience = Audience.audience(
            recipients.toArray(Audience[]::new)
        );

        // Broadcast to operators with the standard command-feedback permission
        audience.sendMessage(formatted);
    }
}
