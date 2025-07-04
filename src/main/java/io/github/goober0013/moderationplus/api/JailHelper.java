package io.github.goober0013.moderationplus.api;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import io.github.goober0013.moderationplus.api.AsyncHelper;
import io.github.goober0013.moderationplus.api.CommandDurationHelper;
import io.github.goober0013.moderationplus.api.ExpiryHelper;
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

public class JailHelper {

    public static boolean isJailed(Player p) {
        File f = new File(
            PropertiesHelper.dataFolder,
            "playerjails/" + p.getUniqueId()
        );
        return f.exists() && !ExpiryHelper.hasExpired(f);
    }

    public static boolean isInJailRegion(Player player) {
        if (
            !PropertiesHelper.worldEditExists ||
            !PropertiesHelper.worldGuardExists
        ) {
            return false;
        }

        RegionContainer container = WorldGuard.getInstance()
            .getPlatform()
            .getRegionContainer();
        RegionQuery query = container.createQuery();

        // fully qualify the WorldEdit Location so Java knows which one you mean
        com.sk89q.worldedit.util.Location wgLoc = BukkitAdapter.adapt(
            player.getLocation()
        );

        ApplicableRegionSet set = query.getApplicableRegions(wgLoc);

        for (ProtectedRegion region : set) {
            if ("jail".equalsIgnoreCase(region.getId())) {
                return true;
            }
        }
        return false;
    }

    public static Location getJailLocation() {
        var cfg = PropertiesHelper.config;
        var world = PropertiesHelper.server.getWorld(
            cfg.getString("jail.world")
        );
        if (world == null) return null;
        return new Location(
            world,
            cfg.getDouble("jail.x"),
            cfg.getDouble("jail.y"),
            cfg.getDouble("jail.z"),
            (float) cfg.getDouble("jail.yaw"),
            (float) cfg.getDouble("jail.pitch")
        );
    }

    public static void preventJailEscape(Player p) {
        Location jailLocation = getJailLocation();
        if (jailLocation != null) {
            p.teleport(jailLocation);
        }
    }

    public static void preventJailOverstay(Player p) {
        if (PropertiesHelper.essentialsSpawnExists) {
            Bukkit.dispatchCommand(
                Bukkit.getConsoleSender(),
                "spawn " + p.getName()
            );
        } else {
            p.teleport(p.getWorld().getSpawnLocation());
        }
    }
}
