package io.github.goober0013.moderationplus.api;

import io.github.goober0013.moderationplus.api.AsyncHelper;
import io.github.goober0013.moderationplus.api.CommandDurationHelper;
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

public class ExpiryHelper {

    public static boolean hasExpired(File f) {
        try {
            String content = Files.readString(f.toPath()).trim();
            if ("forever".equals(content)) return false;
            long exp = Long.parseLong(content);
            return Instant.ofEpochSecond(exp).isBefore(Instant.now());
        } catch (IOException | NumberFormatException e) {
            PropertiesHelper.logger.severe(
                "Error checking expiry for " +
                f.getName() +
                ": " +
                e.getMessage()
            );
            return true; // treat as expired if error
        }
    }

    public static long writeExpiry(File f, String[] args) {
        long expiry = -1;
        f.getParentFile().mkdirs();
        try (FileWriter w = new FileWriter(f)) {
            if (args.length >= 2 && args[1].matches("\\d+[smhd]")) {
                Duration d = CommandDurationHelper.parseDuration(args[1]);
                if (d.getSeconds() > 0) {
                    expiry = Instant.now().plus(d).getEpochSecond();
                }
            }
            w.write(expiry < 0 ? "forever" : Long.toString(expiry));
        } catch (IOException e) {
            PropertiesHelper.logger.severe(
                "Failed writing expiry: " + e.getMessage()
            );
        }
        return expiry;
    }
}
