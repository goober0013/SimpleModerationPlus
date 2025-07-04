package io.github.goober0013.moderationplus;

import io.github.goober0013.moderationplus.api.AsyncHelper;
import io.github.goober0013.moderationplus.api.CommandDurationHelper;
import io.github.goober0013.moderationplus.api.ExpiryHelper;
import io.github.goober0013.moderationplus.api.JailHelper;
import io.github.goober0013.moderationplus.api.MessagesHelper;
import io.github.goober0013.moderationplus.api.PermissionsHelper;
import io.github.goober0013.moderationplus.api.PlayerLogHelper;
import io.github.goober0013.moderationplus.api.PropertiesHelper;
import io.github.goober0013.moderationplus.commands.ActionsCommand;
import io.github.goober0013.moderationplus.commands.BanCommand;
import io.github.goober0013.moderationplus.commands.JailCommand;
import io.github.goober0013.moderationplus.commands.KickCommand;
import io.github.goober0013.moderationplus.commands.MuteCommand;
import io.github.goober0013.moderationplus.commands.SetJailCommand;
import io.github.goober0013.moderationplus.commands.UnbanCommand;
import io.github.goober0013.moderationplus.commands.UnjailCommand;
import io.github.goober0013.moderationplus.commands.UnmuteCommand;
import io.github.goober0013.moderationplus.commands.WarnCommand;
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

public class ModerationPlus extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        PropertiesHelper.plugin = this;
        PropertiesHelper.dataFolder = getDataFolder();
        PropertiesHelper.config = getConfig();
        PropertiesHelper.server = getServer();
        PropertiesHelper.logger = getLogger();

        PropertiesHelper.essentialsSpawnExists = PropertiesHelper.server
            .getPluginManager()
            .isPluginEnabled("EssentialsSpawn");
        PropertiesHelper.worldEditExists = PropertiesHelper.server
            .getPluginManager()
            .isPluginEnabled("WorldEdit");
        PropertiesHelper.worldGuardExists = PropertiesHelper.server
            .getPluginManager()
            .isPluginEnabled("WorldGuard");
        PropertiesHelper.vaultExists = PropertiesHelper.server
            .getPluginManager()
            .isPluginEnabled("Vault");

        saveDefaultConfig();
        ensureFolders();

        saveResource("messages.properties", false);
        PropertiesHelper.logger.info(
            MessagesHelper.translateMessage(
                "log.info.enable",
                "ModerationPlus is now enabling",
                null,
                null,
                null,
                null
            )
        );

        MessagesHelper.loadMessages();

        // Try to hook Vault’s Permission service
        if (PropertiesHelper.vaultExists) {
            RegisteredServiceProvider<Permission> rsp = PropertiesHelper.server
                .getServicesManager()
                .getRegistration(Permission.class);
            if (rsp != null) {
                PropertiesHelper.perms = rsp.getProvider();
                PropertiesHelper.logger.info(
                    MessagesHelper.translateMessage(
                        "log.info.vault_enable",
                        "ModerationPlus is now using Vault permissions",
                        null,
                        null,
                        null,
                        null
                    )
                );
            } else {
                PropertiesHelper.perms = null;
                PropertiesHelper.logger.warning(
                    MessagesHelper.translateMessage(
                        "log.info.vault_error",
                        "ModerationPlus failed to setup Vault permissions",
                        null,
                        null,
                        null,
                        null
                    )
                );
            }
        } else {
            PropertiesHelper.perms = null;
            PropertiesHelper.logger.warning(
                MessagesHelper.translateMessage(
                    "log.info.vault_error",
                    "ModerationPlus failed to setup Vault permissions",
                    null,
                    null,
                    null,
                    null
                )
            );
        }

        // Commands
        LifecycleEventManager<org.bukkit.plugin.Plugin> manager =
            PropertiesHelper.plugin.getLifecycleManager();
        manager.registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            Commands commands = event.registrar();

            commands.register(
                "ban",
                List.of(),
                new BasicCommand() {
                    @Override
                    public void execute(CommandSourceStack src, String[] args) {
                        Bukkit.getScheduler().runTaskAsynchronously(
                                PropertiesHelper.plugin,
                                () -> {
                                    BanCommand.handleBan(src, args);
                                }
                            );
                    }

                    @Override
                    public String permission() {
                        return "moderationplus.ban";
                    }
                }
            );

            commands.register(
                "unban",
                List.of("pardon"),
                new BasicCommand() {
                    @Override
                    public void execute(CommandSourceStack src, String[] args) {
                        Bukkit.getScheduler().runTaskAsynchronously(
                                PropertiesHelper.plugin,
                                () -> {
                                    UnbanCommand.handleUnban(src, args);
                                }
                            );
                    }

                    @Override
                    public String permission() {
                        return "moderationplus.unban";
                    }
                }
            );

            commands.register(
                "mute",
                List.of(),
                new BasicCommand() {
                    @Override
                    public void execute(CommandSourceStack src, String[] args) {
                        Bukkit.getScheduler().runTaskAsynchronously(
                                PropertiesHelper.plugin,
                                () -> {
                                    MuteCommand.handleMute(src, args);
                                }
                            );
                    }

                    @Override
                    public String permission() {
                        return "moderationplus.mute";
                    }
                }
            );

            commands.register(
                "unmute",
                List.of("pardon"),
                new BasicCommand() {
                    @Override
                    public void execute(CommandSourceStack src, String[] args) {
                        Bukkit.getScheduler().runTaskAsynchronously(
                                PropertiesHelper.plugin,
                                () -> {
                                    UnmuteCommand.handleUnmute(src, args);
                                }
                            );
                    }

                    @Override
                    public String permission() {
                        return "moderationplus.unmute";
                    }
                }
            );

            commands.register(
                "warn",
                List.of(),
                new BasicCommand() {
                    @Override
                    public void execute(CommandSourceStack src, String[] args) {
                        Bukkit.getScheduler().runTaskAsynchronously(
                                PropertiesHelper.plugin,
                                () -> {
                                    WarnCommand.handleWarn(src, args);
                                }
                            );
                    }

                    @Override
                    public String permission() {
                        return "moderationplus.warn";
                    }
                }
            );

            commands.register(
                "kick",
                List.of(),
                new BasicCommand() {
                    @Override
                    public void execute(CommandSourceStack src, String[] args) {
                        Bukkit.getScheduler().runTaskAsynchronously(
                                PropertiesHelper.plugin,
                                () -> {
                                    KickCommand.handleKick(src, args);
                                }
                            );
                    }

                    @Override
                    public String permission() {
                        return "moderationplus.kick";
                    }
                }
            );

            commands.register(
                "jail",
                List.of(),
                new BasicCommand() {
                    @Override
                    public void execute(CommandSourceStack src, String[] args) {
                        Bukkit.getScheduler().runTaskAsynchronously(
                                PropertiesHelper.plugin,
                                () -> {
                                    JailCommand.handleJail(src, args);
                                }
                            );
                    }

                    @Override
                    public String permission() {
                        return "moderationplus.jail";
                    }
                }
            );

            commands.register(
                "unjail",
                List.of(),
                new BasicCommand() {
                    @Override
                    public void execute(CommandSourceStack src, String[] args) {
                        Bukkit.getScheduler().runTaskAsynchronously(
                                PropertiesHelper.plugin,
                                () -> {
                                    UnjailCommand.handleUnjail(src, args);
                                }
                            );
                    }

                    @Override
                    public String permission() {
                        return "moderationplus.unjail";
                    }
                }
            );

            commands.register(
                "setjail",
                List.of(),
                new BasicCommand() {
                    @Override
                    public void execute(CommandSourceStack src, String[] args) {
                        Bukkit.getScheduler().runTaskAsynchronously(
                                PropertiesHelper.plugin,
                                () -> {
                                    SetJailCommand.handleSetJail(src, args);
                                }
                            );
                    }

                    @Override
                    public String permission() {
                        return "moderationplus.setjail";
                    }
                }
            );

            commands.register(
                "actions",
                List.of("logs"),
                new BasicCommand() {
                    @Override
                    public void execute(CommandSourceStack src, String[] args) {
                        Bukkit.getScheduler().runTaskAsynchronously(
                                PropertiesHelper.plugin,
                                () -> {
                                    ActionsCommand.handleActions(src, args);
                                }
                            );
                    }

                    @Override
                    public String permission() {
                        return "moderationplus.actions";
                    }
                }
            );
        });

        // Register listeners
        getServer().getPluginManager().registerEvents(this, this);
    }

    private String writeReason(String[] args) {
        if (args.length >= 3 && args[1].matches("\\d+[smhd]")) {
            return String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        }
        if (args.length >= 2 && !args[1].matches("\\d+[smhd]")) {
            return String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        }
        return null;
    }

    private void ensureFolders() {
        File d = PropertiesHelper.dataFolder;
        if (!d.exists()) d.mkdirs();
        new File(d, "playerlogs").mkdirs();
        new File(d, "playermutes").mkdirs();
        new File(d, "playerjails").mkdirs();
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        Player p = e.getPlayer();
        String name = p.getName();
        File f = new File(
            PropertiesHelper.dataFolder,
            "playermutes/" + p.getUniqueId()
        );
        if (!f.exists()) return;
        try {
            String content = Files.readString(f.toPath()).trim();
            if (!"forever".equals(content)) {
                long exp = Long.parseLong(content);
                if (Instant.ofEpochSecond(exp).isBefore(Instant.now())) {
                    f.delete();
                    return;
                }
                long secs = exp - Instant.now().getEpochSecond();

                p.sendActionBar(
                    Component.text(
                        MessagesHelper.translateMessage(
                            "player.ismuted.duration",
                            "§cYou are muted for %d",
                            null,
                            name,
                            null,
                            secs + "s"
                        )
                    )
                );
            } else {
                p.sendActionBar(
                    Component.text(
                        MessagesHelper.translateMessage(
                            "player.ismuted",
                            "§cYou are muted for %d",
                            null,
                            name,
                            null,
                            null
                        )
                    )
                );
            }
        } catch (IOException ex) {
            PropertiesHelper.logger.severe(
                "Error reading mute file: " + ex.getMessage()
            );
        }
        e.setCancelled(true);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        if (JailHelper.isJailed(p)) {
            if (!JailHelper.isInJailRegion(p)) {
                JailHelper.preventJailEscape(p);
            }
        } else {
            if (JailHelper.isInJailRegion(p)) {
                JailHelper.preventJailOverstay(p);
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        if (JailHelper.isJailed(p)) {
            if (!JailHelper.isInJailRegion(p)) {
                JailHelper.preventJailEscape(p);
            } else {
                p.sendActionBar(Component.text("§cYou are jailed"));
            }
        } else {
            if (JailHelper.isInJailRegion(p)) {
                JailHelper.preventJailOverstay(p);
            }
        }
    }
}
