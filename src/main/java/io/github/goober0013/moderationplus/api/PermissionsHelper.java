package io.github.goober0013.moderationplus.api;

import io.github.goober0013.moderationplus.api.AsyncHelper;
import io.github.goober0013.moderationplus.api.CommandDurationHelper;
import io.github.goober0013.moderationplus.api.ExpiryHelper;
import io.github.goober0013.moderationplus.api.JailHelper;
import io.github.goober0013.moderationplus.api.MessagesHelper;
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

public class PermissionsHelper {

    // Call from ASYNC CONTEXT ONLY!
    public static boolean playerHasPermission(Object subject, String node) {
        boolean existed = AsyncHelper.awaitSync(() -> {
            if (subject instanceof OfflinePlayer) {
                return ((OfflinePlayer) subject).hasPlayedBefore();
            }
            // non-OfflinePlayer (Player, Console, etc.) always “exists”
            return true;
        });
        if (!existed) {
            return false;
        }

        // 2) Vault check (thread-safe)
        if (PropertiesHelper.perms != null) {
            String name;
            if (subject instanceof OfflinePlayer) name =
                ((OfflinePlayer) subject).getName();
            else if (subject instanceof Player) name =
                ((Player) subject).getName();
            else if (subject instanceof CommandSender) name =
                ((CommandSender) subject).getName();
            else name = null;

            if (
                name != null &&
                PropertiesHelper.perms.playerHas((String) null, name, node)
            ) {
                return true;
            }
        }

        AtomicBoolean allowed = new AtomicBoolean(false);
        CountDownLatch latch = new CountDownLatch(1);

        AsyncHelper.awaitSync(() -> {
            // 3) Console always has permission
            if (subject instanceof ConsoleCommandSender) {
                allowed.set(true);
            }
            // 4) OfflinePlayer OP check
            else if (subject instanceof OfflinePlayer) {
                allowed.set(((OfflinePlayer) subject).isOp());
            }
            // 5) Online Player API
            else if (subject instanceof Player) {
                Player p = (Player) subject;
                allowed.set(p.isOp() || p.hasPermission(node));
            }
            // 6) Other CommandSender OP check
            else if (subject instanceof CommandSender) {
                try {
                    allowed.set(((CommandSender) subject).isOp());
                } catch (NoSuchMethodError ignore) {
                    allowed.set(false);
                }
            }
            latch.countDown();
            return null;
        });

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 8) Return the result
        return allowed.get();
    }
}
