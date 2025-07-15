package io.github.goober0013.simplemoderationplus;

import io.github.goober0013.simplemoderationplus.api.MessageProperties;
import io.github.goober0013.simplemoderationplus.api.ModerationLogger;
import io.github.goober0013.simplemoderationplus.api.ProfileJailList;
import io.github.goober0013.simplemoderationplus.api.ProfileMuteList;
import io.github.goober0013.simplemoderationplus.api.ProfilePermissions;
import io.github.goober0013.simplemoderationplus.api.WorldguardRegionHelper;
import io.github.goober0013.simplemoderationplus.commands.ActionsCommand;
import io.github.goober0013.simplemoderationplus.commands.BanCommand;
import io.github.goober0013.simplemoderationplus.commands.JailCommand;
import io.github.goober0013.simplemoderationplus.commands.KickCommand;
import io.github.goober0013.simplemoderationplus.commands.MuteCommand;
import io.github.goober0013.simplemoderationplus.commands.SetJailCommand;
import io.github.goober0013.simplemoderationplus.commands.UnbanCommand;
import io.github.goober0013.simplemoderationplus.commands.UnjailCommand;
import io.github.goober0013.simplemoderationplus.commands.UnmuteCommand;
import io.github.goober0013.simplemoderationplus.commands.VoteKickCommand;
import io.github.goober0013.simplemoderationplus.commands.WarnCommand;
import io.github.goober0013.simplemoderationplus.listeners.DefaultVoteKickListener;
import io.github.goober0013.simplemoderationplus.listeners.JailListener;
import io.github.goober0013.simplemoderationplus.listeners.MuteListener;
import io.papermc.paper.ban.BanListType;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.ban.ProfileBanList;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.ServicesManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

public final class SimpleModerationPlus extends JavaPlugin {

    public static File dataFolder;
    public static Logger logger;
    public static Server server;
    public static PluginManager pluginManager;
    public static ServicesManager servicesManager;
    public static BukkitScheduler scheduler;
    public static ProfileBanList banList;
    public static SimpleModerationPlus instance;

    @Override
    public void onLoad() {
        instance = this;
        logger = this.getLogger();
        logger.info("ModerationPlus is loading");

        dataFolder = getDataFolder();
        if (!dataFolder.exists()) {
            // Check if on disk
            boolean created = dataFolder.mkdirs(); // Create folder (and any parents)
            if (!created) {
                logger.severe("ModerationPlus cannot create a data folder");
            }
        }

        saveResource("lang/messages.properties", false);
        saveResource("lang/messages.en_US.properties", false);
        saveResource("lang/messages.es_MX.properties", false);
        saveResource("jail.yml", false);

        MessageProperties.load();
        logger.info(MessageProperties.get("log.load_messages"));

        server = this.getServer();
    }

    @Override
    public void onEnable() {
        LifecycleEventManager<Plugin> lm = this.getLifecycleManager();

        pluginManager = server.getPluginManager();
        servicesManager = server.getServicesManager();
        scheduler = server.getScheduler();
        banList = Bukkit.getBanList(BanListType.PROFILE);

        ProfilePermissions.load();
        ModerationLogger.load();
        ProfileMuteList.load();
        ProfileJailList.load();
        WorldguardRegionHelper.load();
        DefaultVoteKickListener.load();

        lm.registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            var registrar = event.registrar();

            logger.info(MessageProperties.get("log.enable_commands"));

            // Ban commands
            registrar.register(
                BanCommand.ban(),
                MessageProperties.get("help.description.ban")
            );
            registrar.register(
                BanCommand.tempban(),
                MessageProperties.get("help.description.tempban")
            );
            registrar.register(
                BanCommand.permban(),
                MessageProperties.get("help.description.permban")
            );
            registrar.register(
                UnbanCommand.unban(),
                MessageProperties.get("help.description.unban"),
                List.of("pardon")
            );

            // Mute commands
            registrar.register(
                MuteCommand.mute(),
                MessageProperties.get("help.description.mute")
            );
            registrar.register(
                MuteCommand.tempmute(),
                MessageProperties.get("help.description.tempmute")
            );
            registrar.register(
                MuteCommand.permmute(),
                MessageProperties.get("help.description.permmute")
            );
            registrar.register(
                UnmuteCommand.unmute(),
                MessageProperties.get("help.description.unmute")
            );

            // Jail commands
            registrar.register(
                JailCommand.jail(),
                MessageProperties.get("help.description.jail")
            );

            registrar.register(
                JailCommand.tempjail(),
                MessageProperties.get("help.description.tempjail")
            );
            registrar.register(
                JailCommand.permjail(),
                MessageProperties.get("help.description.permjail")
            );
            registrar.register(
                UnjailCommand.unjail(),
                MessageProperties.get("help.description.unjail")
            );
            registrar.register(
                SetJailCommand.setJail(),
                MessageProperties.get("help.description.setjail")
            );

            // Actions command
            registrar.register(
                ActionsCommand.actions(),
                MessageProperties.get("help.description.actions")
            );

            // Kick command
            registrar.register(
                KickCommand.kick(),
                MessageProperties.get("help.description.kick")
            );

            // Warn command
            registrar.register(
                WarnCommand.warn(),
                MessageProperties.get("help.description.warn")
            );

            // Votekick commands
            registrar.register(
                VoteKickCommand.vk(),
                MessageProperties.get("help.description.vk")
            );
            registrar.register(
                VoteKickCommand.vkcancel(),
                MessageProperties.get("help.description.vkcancel")
            );
        });

        logger.info(MessageProperties.get("log.enable_listeners"));
        pluginManager.registerEvents(new MuteListener(), this);
        pluginManager.registerEvents(new JailListener(), this);
    }
}
