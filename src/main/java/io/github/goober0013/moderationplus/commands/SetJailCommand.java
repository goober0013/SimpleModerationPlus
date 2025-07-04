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

public class SetJailCommand {

    // Call from ASYNC CONTEXT ONLY!
    public static void handleSetJail(CommandSourceStack src, String[] args) {
        CommandSender s = AsyncHelper.awaitSync(() -> {
            return src.getSender();
        });
        boolean banPerm = PermissionsHelper.playerHasPermission(
            s,
            "moderationplus.setjail"
        );

        AsyncHelper.awaitSync(() -> {
            if (!banPerm) {
                s.sendMessage(
                    MessagesHelper.translateMessage(
                        "command.error.no_permission",
                        "§cYou don't have permission",
                        s.getName(),
                        null,
                        null,
                        null
                    )
                );
                return null;
            }

            if (!(s instanceof Player)) {
                s.sendMessage(
                    MessagesHelper.translateMessage(
                        "command.error.only_players",
                        "§cOnly players may execute this command",
                        s.getName(),
                        null,
                        null,
                        null
                    )
                );
                return null;
            }

            Player p = (Player) s;
            var cfg = PropertiesHelper.config;
            var loc = p.getLocation();
            cfg.set("jail.world", loc.getWorld().getName());
            cfg.set("jail.x", loc.getX());
            cfg.set("jail.y", loc.getY());
            cfg.set("jail.z", loc.getZ());
            cfg.set("jail.yaw", loc.getYaw());
            cfg.set("jail.pitch", loc.getPitch());
            PropertiesHelper.plugin.saveConfig();
            s.sendMessage(
                MessagesHelper.translateMessage(
                    "command.setjail",
                    "Set the jail location to your location",
                    s.getName(),
                    null,
                    null,
                    null
                )
            );
            return null;
        });
    }
}
