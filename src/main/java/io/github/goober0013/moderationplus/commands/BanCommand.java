package io.github.goober0013.moderationplus.commands;

import io.github.goober0013.moderationplus.api.AsyncHelper;
import io.github.goober0013.moderationplus.api.CommandDurationHelper;
import io.github.goober0013.moderationplus.api.ExpiryHelper;
import io.github.goober0013.moderationplus.api.JailHelper;
import io.github.goober0013.moderationplus.api.MessagesHelper;
import io.github.goober0013.moderationplus.api.PermissionsHelper;
import io.github.goober0013.moderationplus.api.PlayerLogHelper;
import io.github.goober0013.moderationplus.api.PropertiesHelper;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.BanEntry;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class BanCommand {

    // Call from ASYNC CONTEXT ONLY!
    public static void handleBan(CommandSourceStack src, String[] args) {
        CommandSender s = AsyncHelper.awaitSync(() -> {
            return src.getSender();
        });
        boolean banPerm = PermissionsHelper.playerHasPermission(
            s,
            "moderationplus.ban"
        );

        String name;
        OfflinePlayer t;
        boolean exemptPerm;
        if (args.length > 0) {
            name = args[0];
            t = AsyncHelper.awaitSync(() -> {
                return Bukkit.getOfflinePlayer(name);
            });
            exemptPerm = PermissionsHelper.playerHasPermission(
                t,
                "moderationplus.exempt"
            );
        } else {
            name = null;
            t = null;
            exemptPerm = false;
        }

        AsyncHelper.awaitSync(() -> {
            if (!banPerm) {
                s.sendMessage(
                    MessagesHelper.translateMessage(
                        "command.error.no_permission",
                        "§cYou don't have permission",
                        s.getName(),
                        name,
                        null,
                        null
                    )
                );
                return null;
            }
            if (args.length < 1) {
                s.sendMessage(
                    MessagesHelper.translateMessage(
                        "command.error.specify_a_player",
                        "§cSpecify a player",
                        s.getName(),
                        name,
                        null,
                        null
                    )
                );
                return null;
            }

            if (!t.hasPlayedBefore() && !t.isOnline()) {
                s.sendMessage(
                    MessagesHelper.translateMessage(
                        "command.error.invalid_player",
                        "§cThat player does not exist",
                        s.getName(),
                        name,
                        null,
                        null
                    )
                );
                return null;
            }

            if (exemptPerm && !(s instanceof ConsoleCommandSender)) {
                s.sendMessage(
                    MessagesHelper.translateMessage(
                        "command.error.exempt",
                        "§cThat player is exempt from moderation actions",
                        s.getName(),
                        name,
                        null,
                        null
                    )
                );
                return null;
            }
            List<Player> notify = MessagesHelper.notifyOnline(s);

            Date expires = null;
            String reasontemp = null;
            String second;
            Duration d;

            // Check if the player is banned
            BanList banList = Bukkit.getBanList(BanList.Type.NAME);
            BanEntry entry = banList.getBanEntry(t.getName());
            if (entry != null) {
                Date banExpiry = entry.getExpiration();
                if (banExpiry == null) {
                    s.sendMessage(
                        MessagesHelper.translateMessage(
                            "command.error.ban",
                            "§cNothing changed. The player is already banned",
                            s.getName(),
                            name,
                            null,
                            null
                        )
                    );
                    return null;
                }

                Instant now = Instant.now();
                Duration remaining = Duration.between(
                    now,
                    entry.getExpiration().toInstant()
                );

                if (args.length >= 2) {
                    second = args[1];
                    d = CommandDurationHelper.parseDuration(second);

                    if (remaining.compareTo(d) >= 0) {
                        s.sendMessage(
                            MessagesHelper.translateMessage(
                                "command.error.ban",
                                "§cNothing changed. The player is already banned",
                                s.getName(),
                                name,
                                null,
                                null
                            )
                        );
                        return null;
                    }
                } else {
                    d = null;
                    second = null;
                }
            } else {
                if (args.length >= 2) {
                    second = args[1];
                    d = CommandDurationHelper.parseDuration(second);
                } else {
                    second = null;
                    d = null;
                }
            }

            if (args.length >= 2) {
                if (second.matches("\\d+[smhd]")) {
                    if (d.getSeconds() < 1) {
                        s.sendMessage(
                            MessagesHelper.translateMessage(
                                "command.error.tooshort",
                                "§cUse a duration longer than 1 second",
                                s.getName(),
                                name,
                                null,
                                args[1]
                            )
                        );
                        return null;
                    }
                    expires = Date.from(Instant.now().plus(d));
                    if (args.length > 2) {
                        reasontemp = String.join(
                            "",
                            Arrays.copyOfRange(args, 2, args.length)
                        );
                    }
                } else {
                    reasontemp = String.join(
                        "",
                        Arrays.copyOfRange(args, 1, args.length)
                    );
                }
            }

            final String reason = reasontemp;

            Bukkit.getBanList(BanList.Type.NAME).addBan(
                name,
                reason,
                expires,
                s.getName()
            );
            if (t.isOnline()) {
                ((Player) t).kick(Component.text(reason != null ? reason : ""));
            }

            if (expires == null) {
                if (reason == null) {
                    s.sendMessage(
                        MessagesHelper.translateMessage(
                            "command.ban",
                            "Banned %p",
                            s.getName(),
                            name,
                            reason,
                            null
                        )
                    );
                    notify.forEach(p ->
                        p.sendMessage(
                            MessagesHelper.translateMessage(
                                "notify.ban",
                                "%a banned %p",
                                s.getName(),
                                name,
                                reason,
                                null
                            )
                        )
                    );
                    PlayerLogHelper.logAction(
                        t.getUniqueId(),
                        MessagesHelper.translateMessage(
                            "notify.ban",
                            "%a banned %p",
                            s.getName(),
                            name,
                            reason,
                            null
                        )
                    );
                } else {
                    s.sendMessage(
                        MessagesHelper.translateMessage(
                            "command.ban.reason",
                            "Banned %p: %r",
                            s.getName(),
                            name,
                            reason,
                            null
                        )
                    );
                    notify.forEach(p ->
                        p.sendMessage(
                            MessagesHelper.translateMessage(
                                "notify.ban.reason",
                                "%a banned %p: %r",
                                s.getName(),
                                name,
                                reason,
                                null
                            )
                        )
                    );
                    PlayerLogHelper.logAction(
                        t.getUniqueId(),
                        MessagesHelper.translateMessage(
                            "notify.ban.reason",
                            "%a banned %p: %r",
                            s.getName(),
                            name,
                            reason,
                            null
                        )
                    );
                }
            } else {
                if (reason == null) {
                    s.sendMessage(
                        MessagesHelper.translateMessage(
                            "command.ban.duration",
                            "Banned %p for %d",
                            s.getName(),
                            name,
                            reason,
                            args[1]
                        )
                    );
                    notify.forEach(p ->
                        p.sendMessage(
                            MessagesHelper.translateMessage(
                                "notify.ban.duration",
                                "%a banned %p for %d",
                                s.getName(),
                                name,
                                reason,
                                args[1]
                            )
                        )
                    );
                    PlayerLogHelper.logAction(
                        t.getUniqueId(),
                        MessagesHelper.translateMessage(
                            "notify.ban.duration",
                            "%a banned %p for %d",
                            s.getName(),
                            name,
                            reason,
                            args[1]
                        )
                    );
                } else {
                    s.sendMessage(
                        MessagesHelper.translateMessage(
                            "command.ban.reason_duration",
                            "Banned %p for %d: %r",
                            s.getName(),
                            name,
                            reason,
                            args[1]
                        )
                    );
                    notify.forEach(p ->
                        p.sendMessage(
                            MessagesHelper.translateMessage(
                                "notify.ban.reason_duration",
                                "%a banned %p for %d: %r",
                                s.getName(),
                                name,
                                reason,
                                args[1]
                            )
                        )
                    );
                    PlayerLogHelper.logAction(
                        t.getUniqueId(),
                        MessagesHelper.translateMessage(
                            "notify.ban.reason_duration",
                            "%a banned %p for %d: %r",
                            s.getName(),
                            name,
                            reason,
                            args[1]
                        )
                    );
                }
            }
            return null;
        });
    }
}
