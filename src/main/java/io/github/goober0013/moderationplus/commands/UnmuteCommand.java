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

public class UnmuteCommand {

    // Call from ASYNC CONTEXT ONLY!
    public static void handleUnmute(CommandSourceStack src, String[] args) {
        CommandSender s = AsyncHelper.awaitSync(() -> {
            return src.getSender();
        });
        boolean banPerm = PermissionsHelper.playerHasPermission(
            s,
            "moderationplus.unmute"
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

            File f = new File(
                PropertiesHelper.dataFolder,
                "playermutes/" + Bukkit.getOfflinePlayer(name).getUniqueId()
            );
            if (!f.exists() || ExpiryHelper.hasExpired(f)) {
                s.sendMessage(
                    MessagesHelper.translateMessage(
                        "command.error.unmute",
                        "§cNothing changed. The player isn't muted",
                        s.getName(),
                        name,
                        null,
                        null
                    )
                );
                return null;
            }
            f.delete();
            s.sendMessage(
                MessagesHelper.translateMessage(
                    "command.unmute",
                    "Unmuted %p",
                    s.getName(),
                    name,
                    null,
                    null
                )
            );
            MessagesHelper.notifyOnline(s).forEach(p ->
                p.sendMessage(
                    MessagesHelper.translateMessage(
                        "notify.unmute",
                        "%a unmuted %p",
                        s.getName(),
                        name,
                        null,
                        null
                    )
                )
            );
            PlayerLogHelper.logAction(
                Bukkit.getOfflinePlayer(name).getUniqueId(),
                MessagesHelper.translateMessage(
                    "notify.unmute",
                    "%a unmuted %p",
                    s.getName(),
                    name,
                    null,
                    null
                )
            );
            return null;
        });
    }
}
