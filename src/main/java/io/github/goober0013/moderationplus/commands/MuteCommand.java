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

public class MuteCommand {

    // Call from ASYNC CONTEXT ONLY!
    public static void handleMute(CommandSourceStack src, String[] args) {
        CommandSender s = AsyncHelper.awaitSync(() -> {
            return src.getSender();
        });
        boolean banPerm = PermissionsHelper.playerHasPermission(
            s,
            "moderationplus.mute"
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

            if (!t.hasPlayedBefore() && !t.isOnline()) {
                s.sendMessage(
                    MessagesHelper.translateMessage(
                        "command.error.invalid_player",
                        "§cThat player does not exist",
                        s.getName(),
                        t.getName(),
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

            File f = new File(
                PropertiesHelper.dataFolder,
                "playermutes/" + t.getUniqueId()
            );
            f.getParentFile().mkdirs();
            long expiry = ExpiryHelper.writeExpiry(f, args);
            String reason; // null if none

            if (args.length > 2) {
                reason = String.join(
                    "",
                    Arrays.copyOfRange(args, 2, args.length)
                );
            } else {
                reason = null;
            }
            List<Player> notify = MessagesHelper.notifyOnline(s);
            if (expiry == -1) {
                if (reason == null) {
                    s.sendMessage(
                        MessagesHelper.translateMessage(
                            "command.mute",
                            "Muted %p",
                            s.getName(),
                            name,
                            reason,
                            null
                        )
                    );
                    notify.forEach(p ->
                        p.sendMessage(
                            MessagesHelper.translateMessage(
                                "notify.mute",
                                "%a muted %p",
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
                            "notify.mute",
                            "%a muted %p",
                            s.getName(),
                            name,
                            reason,
                            null
                        )
                    );
                    if (t.isOnline()) {
                        ((Player) t).sendMessage(
                            MessagesHelper.translateMessage(
                                "player.mute",
                                "You were muted",
                                s.getName(),
                                name,
                                reason,
                                null
                            )
                        );
                    }
                } else {
                    s.sendMessage(
                        MessagesHelper.translateMessage(
                            "command.mute.reason",
                            "Muted %p: %r",
                            s.getName(),
                            name,
                            reason,
                            null
                        )
                    );
                    notify.forEach(p ->
                        p.sendMessage(
                            MessagesHelper.translateMessage(
                                "notify.mute.reason",
                                "%a muted %p: %r",
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
                            "notify.mute.reason",
                            "%a muted %p: %r",
                            s.getName(),
                            name,
                            reason,
                            null
                        )
                    );
                    if (t.isOnline()) {
                        ((Player) t).sendMessage(
                            MessagesHelper.translateMessage(
                                "player.mute.reason",
                                "You were muted: %r",
                                s.getName(),
                                name,
                                reason,
                                null
                            )
                        );
                    }
                }
            } else {
                if (reason == null) {
                    s.sendMessage(
                        MessagesHelper.translateMessage(
                            "command.mute.duration",
                            "Muted %p for %d",
                            s.getName(),
                            name,
                            reason,
                            args[1]
                        )
                    );
                    notify.forEach(p ->
                        p.sendMessage(
                            MessagesHelper.translateMessage(
                                "notify.mute.duration",
                                "%a muted %p for %d",
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
                            "notify.mute.duration",
                            "%a muted %p for %d",
                            s.getName(),
                            name,
                            reason,
                            args[1]
                        )
                    );
                    if (t.isOnline()) {
                        ((Player) t).sendMessage(
                            MessagesHelper.translateMessage(
                                "player.mute.duration",
                                "You were muted for %d",
                                s.getName(),
                                name,
                                reason,
                                args[1]
                            )
                        );
                    }
                } else {
                    s.sendMessage(
                        MessagesHelper.translateMessage(
                            "command.mute.reason_duration",
                            "Muted %p for %d: %r",
                            s.getName(),
                            name,
                            reason,
                            args[1]
                        )
                    );
                    notify.forEach(p ->
                        p.sendMessage(
                            MessagesHelper.translateMessage(
                                "notify.mute.reason_duration",
                                "%a muted %p for %d: %r",
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
                            "notify.mute.reason_duration",
                            "%a muted %p for %d: %r",
                            s.getName(),
                            name,
                            reason,
                            args[1]
                        )
                    );
                    if (t.isOnline()) {
                        ((Player) t).sendMessage(
                            MessagesHelper.translateMessage(
                                "player.mute.reason_duration",
                                "You were muted for %d: %r",
                                s.getName(),
                                name,
                                reason,
                                args[1]
                            )
                        );
                    }
                }
            }

            return null;
        });
    }
}
