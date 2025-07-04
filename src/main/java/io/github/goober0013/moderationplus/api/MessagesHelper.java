package io.github.goober0013.moderationplus.api;

import io.github.goober0013.moderationplus.api.AsyncHelper;
import io.github.goober0013.moderationplus.api.CommandDurationHelper;
import io.github.goober0013.moderationplus.api.ExpiryHelper;
import io.github.goober0013.moderationplus.api.JailHelper;
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

public class MessagesHelper {

    public static void loadMessages() {
        File msgFile = new File(
            PropertiesHelper.dataFolder,
            "messages.properties"
        );
        try (FileInputStream in = new FileInputStream(msgFile)) {
            PropertiesHelper.messages.clear();
            PropertiesHelper.messages.load(in);
        } catch (IOException e) {
            PropertiesHelper.logger.severe(
                "Failed to load messages.properties: " + e.getMessage()
            );
        }
    }

    public static List<Player> notifyOnline(CommandSender s) {
        return Bukkit.getOnlinePlayers()
            .stream()
            .filter(
                p ->
                    p.hasPermission("moderationplus.notify") &&
                    !p.getName().equals(s.getName())
            )
            .collect(Collectors.toList());
    }

    public static String translateMessage(
        String key,
        String fallback,
        String admin,
        String player,
        String reason,
        String duration
    ) {
        // pick the template: property if present, otherwise fallback
        String template = PropertiesHelper.messages.getProperty(key, fallback);
        if (template == null) return null;

        // apply only the replacements you provided
        String msg = template;
        if (admin != null && !admin.isEmpty()) msg = msg.replace("%a", admin);
        if (player != null && !player.isEmpty()) msg = msg.replace(
            "%p",
            player
        );
        if (reason != null && !reason.isEmpty()) msg = msg.replace(
            "%r",
            reason
        );
        if (duration != null && !duration.isEmpty()) msg = msg.replace(
            "%d",
            duration
        );

        return ChatColor.translateAlternateColorCodes('&', msg);
    }
}
