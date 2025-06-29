package io.github.goober0013.moderationplus;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.Plugin;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.BanList;
import org.bukkit.entity.Player;
import org.bukkit.entity.Entity;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.Location;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import com.sk89q.worldguard.protection.ApplicableRegionSet;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.Duration;
import java.util.Date;
import java.util.UUID;
import java.util.List;
import java.util.Arrays;
import java.util.stream.Collectors;

public class ModerationPlus extends JavaPlugin implements Listener {

@Override
public void onEnable() {
    saveDefaultConfig();
    ensureFolders();
    getLogger().info("ModerationPlus enabled");

    // Register Brigadier commands
    LifecycleEventManager<org.bukkit.plugin.Plugin> manager = this.getLifecycleManager();
    manager.registerEventHandler(LifecycleEvents.COMMANDS, event -> {
        Commands commands = event.registrar();

        // /ban
        commands.register(
            "ban",
            List.of(),
            new BasicCommand() {
                @Override public void execute(CommandSourceStack src, String[] args) { handleBan(src, args); }
                @Override public String permission() { return "moderationplus.ban"; }
            }
        );

        // /unban alias /pardon
        commands.register(
            "unban",
            List.of("pardon"),
            new BasicCommand() {
                @Override public void execute(CommandSourceStack src, String[] args) { handleUnban(src, args); }
                @Override public String permission() { return "moderationplus.unban"; }
            }
        );

        // /mute
        commands.register(
            "mute",
            List.of(),
            new BasicCommand() {
                @Override public void execute(CommandSourceStack src, String[] args) { handleMute(src, args); }
                @Override public String permission() { return "moderationplus.mute"; }
            }
        );

        // /unmute alias /pardon
        commands.register(
            "unmute",
            List.of("pardon"),
            new BasicCommand() {
                @Override public void execute(CommandSourceStack src, String[] args) { handleUnmute(src, args); }
                @Override public String permission() { return "moderationplus.unmute"; }
            }
        );

        // /warn
        commands.register(
            "warn",
            List.of(),
            new BasicCommand() {
                @Override public void execute(CommandSourceStack src, String[] args) { handleWarn(src, args); }
                @Override public String permission() { return "moderationplus.warn"; }
            }
        );

        // /kick
        commands.register(
            "kick",
            List.of(),
            new BasicCommand() {
                @Override public void execute(CommandSourceStack src, String[] args) { handleKick(src, args); }
                @Override public String permission() { return "moderationplus.kick"; }
            }
        );

        // /jail
        commands.register(
            "jail",
            List.of(),
            new BasicCommand() {
                @Override public void execute(CommandSourceStack src, String[] args) { handleJail(src, args); }
                @Override public String permission() { return "moderationplus.jail"; }
            }
        );

        // /unjail
        commands.register(
            "unjail",
            List.of(),
            new BasicCommand() {
                @Override public void execute(CommandSourceStack src, String[] args) { handleUnjail(src, args); }
                @Override public String permission() { return "moderationplus.unjail"; }
            }
        );

        // /setjail
        commands.register(
            "setjail",
            List.of(),
            new BasicCommand() {
                @Override public void execute(CommandSourceStack src, String[] args) { handleSetJail(src, args); }
                @Override public String permission() { return "moderationplus.setjail"; }
            }
        );

        // /actions alias /logs
        commands.register(
            "actions",
            List.of("logs"),
            new BasicCommand() {
                @Override public void execute(CommandSourceStack src, String[] args) { handleActions(src, args); }
                @Override public String permission() { return "moderationplus.actions"; }
            }
        );
    });

    // Register listeners
    getServer().getPluginManager().registerEvents(this, this);
}

private void handleBan(CommandSourceStack src, String[] args) {
    CommandSender s = src.getSender();
    if (!s.hasPermission("moderationplus.ban")) {
        s.sendMessage("§cYou don’t have permission");
        return;
    }
    if (args.length < 1) {
        s.sendMessage("§cSpecify a player");
        return;
    }
    String name = args[0];
    OfflinePlayer t = Bukkit.getOfflinePlayer(name);
    if (!t.hasPlayedBefore() && !t.isOnline()) {
        s.sendMessage("§cThat player does not exist");
        return;
    }
    if (t.isOnline() && ((Player) t).hasPermission("moderationplus.exempt") && !(s instanceof ConsoleCommandSender)) {
        s.sendMessage("§cThe player cannot be banned");
        return;
    }
    List<Player> notify = notifyOnline(s);

    Date expires = null;
    String reason = null;
    if (args.length >= 2) {
        String second = args[1];
        if (second.matches("\\d+[smhd]")) {
            Duration d = parseDuration(second);
            if (d.getSeconds() < 1) {
                s.sendMessage("§cBan time must be longer than 1 second");
                return;
            }
            expires = Date.from(Instant.now().plus(d));
            if (args.length > 2) {
                reason = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
            }
        } else {
            reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        }
    }

    Bukkit.getBanList(BanList.Type.NAME).addBan(name, reason, expires, s.getName());
    if (t.isOnline()) {
        ((Player) t).kick(Component.text(reason != null ? reason : ""));
    }

    String msg = "Banned " + name
        + (expires != null ? " for " + args[1] : "")
        + (reason != null ? ": " + reason : "");
    s.sendMessage(msg);
    notify.forEach(p -> p.sendMessage(s.getName() + " " + msg));
    logAction(t.getUniqueId(), s.getName() + " " + msg);
}

private void handleUnban(CommandSourceStack src, String[] args) {
    CommandSender s = src.getSender();
    if (!s.hasPermission("moderationplus.unban")) {
        s.sendMessage("§cYou don’t have permission");
        return;
    }
    if (args.length < 1) {
        s.sendMessage("§cSpecify a player");
        return;
    }
    String name = args[0];
    if (!Bukkit.getBanList(BanList.Type.NAME).isBanned(name)) {
        s.sendMessage("§cNothing changed. The player isn't banned");
        return;
    }
    Bukkit.getBanList(BanList.Type.NAME).pardon(name);
    String msg = "Unbanned " + name;
    s.sendMessage(msg);
    notifyOnline(s).forEach(p -> p.sendMessage(s.getName() + " " + msg));
    logAction(Bukkit.getOfflinePlayer(name).getUniqueId(), s.getName() + " " + msg);
}

private void handleMute(CommandSourceStack src, String[] args) {
    CommandSender s = src.getSender();
    if (!s.hasPermission("moderationplus.mute")) {
        s.sendMessage("§cYou don’t have permission");
        return;
    }
    if (args.length < 1) {
        s.sendMessage("§cSpecify a player");
        return;
    }
    List<Player> notify = notifyOnline(s);
    for (String name : selectTargets(src.getSender(), args[0])) {
        OfflinePlayer t = Bukkit.getOfflinePlayer(name);
        if (!t.hasPlayedBefore() && !t.isOnline()) {
            s.sendMessage("§cThat player does not exist");
            continue;
        }
        if (t.isOnline() && ((Player) t).hasPermission("moderationplus.exempt") && !(s instanceof ConsoleCommandSender)) {
            s.sendMessage("§cThe player cannot be muted");
            continue;
        }
        File f = new File(getDataFolder(), "playermutes/" + t.getUniqueId());
        f.getParentFile().mkdirs();
        long expiry = writeExpiry(f, args);
        String reason = writeReason(args);
        if (t.isOnline()) {
            ((Player) t).sendMessage("§cYou were muted"
                + (expiry > 0 ? " for " + args[1] : "")
                + (reason != null ? ": " + reason : ""));
        }
        String msg = "Muted " + name
            + (expiry > 0 ? " for " + args[1] : "")
            + (reason != null ? ": " + reason : "");
        s.sendMessage(msg);
        notify.forEach(p -> p.sendMessage(s.getName() + " " + msg));
        logAction(t.getUniqueId(), s.getName() + " " + msg);
    }
}

private boolean hasExpired(File f) {
    try {
        String content = Files.readString(f.toPath()).trim();
        if ("forever".equals(content)) return false;
        long exp = Long.parseLong(content);
        return Instant.ofEpochSecond(exp).isBefore(Instant.now());
    } catch (IOException | NumberFormatException e) {
        getLogger().severe("Error checking expiry for " + f.getName() + ": " + e.getMessage());
        return true; // treat as expired if error
    }
}

private void handleUnmute(CommandSourceStack src, String[] args) {
    CommandSender s = src.getSender();
    if (!s.hasPermission("moderationplus.unmute")) {
        s.sendMessage("§cYou don’t have permission");
        return;
    }
    if (args.length < 1) {
        s.sendMessage("§cSpecify a player");
        return;
    }
    for (String name : args) {
        File f = new File(getDataFolder(), "playermutes/" + Bukkit.getOfflinePlayer(name).getUniqueId());
        if (!f.exists() || hasExpired(f)) {
            s.sendMessage("§cNothing changed. The player isn't muted");
            continue;
        }
        f.delete();
        String msg = "Unmuted " + name;
        s.sendMessage(msg);
        notifyOnline(s).forEach(p -> p.sendMessage(s.getName() + " " + msg));
        logAction(Bukkit.getOfflinePlayer(name).getUniqueId(), s.getName() + " " + msg);
    }
}

private void handleWarn(CommandSourceStack src, String[] args) {
    CommandSender s = src.getSender();
    if (!s.hasPermission("moderationplus.warn")) {
        s.sendMessage("§cYou don’t have permission");
        return;
    }
    if (args.length < 1) {
        s.sendMessage("§cSpecify a player");
        return;
    }
    String name = args[0];
    OfflinePlayer t = Bukkit.getOfflinePlayer(name);
    if (!t.hasPlayedBefore() && !t.isOnline()) {
        s.sendMessage("§cThat player does not exist");
        return;
    }
    if (t.isOnline() && ((Player) t).hasPermission("moderationplus.exempt") && !(s instanceof ConsoleCommandSender)) {
        s.sendMessage("§cThe player cannot be warned");
        return;
    }
    List<Player> notify = notifyOnline(s);
    String reason = args.length > 1 ? String.join(" ", Arrays.copyOfRange(args, 1, args.length)) : null;
    if (t.isOnline()) {
        ((Player) t).sendMessage("§cYou were warned" + (reason != null ? ": " + reason : ""));
    }
    String msg = "Warned " + name + (reason != null ? ": " + reason : "");
    s.sendMessage(msg);
    notify.forEach(p -> p.sendMessage(s.getName() + " " + msg));
    logAction(t.getUniqueId(), s.getName() + " " + msg);
}

private void handleKick(CommandSourceStack src, String[] args) {
    CommandSender s = src.getSender();
    if (!s.hasPermission("moderationplus.kick")) {
        s.sendMessage("§cYou don’t have permission");
        return;
    }
    if (args.length < 1) {
        s.sendMessage("§cSpecify a player");
        return;
    }
    Player t = Bukkit.getPlayer(args[0]);
    if (t == null) {
        s.sendMessage("§cNo player was found");
        return;
    }
    if (t.hasPermission("moderationplus.exempt") && !(s instanceof ConsoleCommandSender)) {
        s.sendMessage("§cThe player cannot be kicked");
        return;
    }
    String reason = args.length > 1 ? String.join(" ", Arrays.copyOfRange(args, 1, args.length)) : null;
    t.kick(Component.text(reason != null ? reason : "Kicked"));
    String msg = "Kicked " + t.getName() + (reason != null ? ": " + reason : "");
    s.sendMessage(msg);
    notifyOnline(s).forEach(p -> p.sendMessage(s.getName() + " " + msg));
    logAction(t.getUniqueId(), s.getName() + " " + msg);
}

private void handleJail(CommandSourceStack src, String[] args) {
    CommandSender s = src.getSender();
    if (!s.hasPermission("moderationplus.jail")) {
        s.sendMessage("§cYou don’t have permission");
        return;
    }
    if (args.length < 1) {
        s.sendMessage("§cSpecify a player");
        return;
    }

    // ─── Lookup & validate ────────────────────────────────────────────────────────
    String name = args[0];
    OfflinePlayer t = Bukkit.getOfflinePlayer(name);
    if (!t.hasPlayedBefore() && !t.isOnline()) {
        s.sendMessage("§cThat player does not exist");
        return;
    }
    if (t.isOnline()
        && ((Player) t).hasPermission("moderationplus.exempt")
        && !(s instanceof ConsoleCommandSender)) {
        s.sendMessage("§cThe player cannot be jailed");
        return;
    }

    List<Player> notify = notifyOnline(s);

    // ─── Write expiry + reason via your helpers ───────────────────────────────────
    File jailFile = new File(getDataFolder(), "playerjails/" + t.getUniqueId());
    long expiry = writeExpiry(jailFile, args);      // returns -1 for forever
    String reason = writeReason(args);              // null if none

    // ─── Build text pieces ───────────────────────────────────────────────────────
    String durationText = expiry < 0
        ? ""
        : " for " + args[1];
    String reasonText = (reason != null && !reason.isBlank())
        ? ": " + reason
        : "";

    String fullMsg = "Jailed " + name + durationText + reasonText;

    // ─── Teleport & inform the jailed player ─────────────────────────────────────
    if (t.isOnline()) {
        Player target = (Player) t;
        Location jailLoc = getJailLocation();
        if (jailLoc != null) {
            target.teleport(jailLoc);
            target.sendMessage("§cYou have been jailed"
                + durationText + reasonText);
        } else {
            s.sendMessage("§cJail location not set in config");
            return;
        }
    }

    // ─── Broadcast to command sender + staff ─────────────────────────────────────
    s.sendMessage(fullMsg);
    notify.forEach(p -> p.sendMessage(s.getName() + " " + fullMsg));

    // ─── Log the action ──────────────────────────────────────────────────────────
    logAction(t.getUniqueId(), s.getName() + " " + fullMsg);
}

private void handleUnjail(CommandSourceStack src, String[] args) {
    CommandSender s = src.getSender();
    if (!s.hasPermission("moderationplus.unjail")) {
        s.sendMessage("§cYou don’t have permission");
        return;
    }
    if (args.length < 1) {
        s.sendMessage("§cSpecify a player");
        return;
    }
    boolean essSpawn = getServer().getPluginManager().isPluginEnabled("EssentialsSpawn");
    getLogger().info("EssentialsSpawn enabled? " + essSpawn);

    for (String name : args) {
        UUID uuid = Bukkit.getOfflinePlayer(name).getUniqueId();
        File f = new File(getDataFolder(), "playerjails/" + uuid);
        if (f.exists() && f.delete()) {
            String msg = "Unjailed " + name;
            s.sendMessage(msg);
            notifyOnline(s).forEach(p -> p.sendMessage(s.getName() + " " + msg));
            logAction(uuid, s.getName() + " " + msg);

            // Teleport the player if online
            Player target = Bukkit.getPlayer(uuid);
            if (target != null) {
                if (essSpawn) {
                    getLogger().info("Using EssentialsSpawn /spawn for " + name);
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "spawn " + name);
                } else {
                    Location spawn = target.getWorld().getSpawnLocation();
                    getLogger().info("Teleporting " + name + " to vanilla spawn: " + spawn);
                    target.teleport(spawn);
                }
                getLogger().info(name + " teleported to " + target.getLocation());
            }
        } else {
            s.sendMessage("§cNothing changed. The player isn't jailed");
        }
    }
}

private void handleSetJail(CommandSourceStack src, String[] args) {
    CommandSender s = src.getSender();
    if (!s.hasPermission("moderationplus.setjail")) {
        s.sendMessage("§cYou don’t have permission");
        return;
    }
    if (!(s instanceof Player)) {
        s.sendMessage("§cOnly players can set jail");
        return;
    }
    Player p = (Player) s;
    var cfg = getConfig();
    var loc = p.getLocation();
    cfg.set("jail.world", loc.getWorld().getName());
    cfg.set("jail.x", loc.getX());
    cfg.set("jail.y", loc.getY());
    cfg.set("jail.z", loc.getZ());
    cfg.set("jail.yaw", loc.getYaw());
    cfg.set("jail.pitch", loc.getPitch());
    saveConfig();
    s.sendMessage("§aJail location set");
}

private void handleActions(CommandSourceStack src, String[] args) {
    CommandSender s = src.getSender();
    if (!s.hasPermission("moderationplus.viewactions")) {
        s.sendMessage("§cYou don’t have permission");
        return;
    }
    if (args.length < 1) {
        s.sendMessage("§cSpecify a player");
        return;
    }
    String name = args[0];
    File lf = new File(getDataFolder(), "playerlogs/" + Bukkit.getOfflinePlayer(name).getUniqueId() + ".log");
    if (!lf.exists()) {
        s.sendMessage(name + " has no recorded moderator actions");
        return;
    }
    List<String> lines;
    try {
        lines = Files.readAllLines(lf.toPath());
    } catch (IOException ex) {
        s.sendMessage("§cError reading actions");
        return;
    }
    int start = Math.max(0, lines.size() - 5);
    for (int i = start; i < lines.size(); i++) {
        s.sendMessage(lines.get(i));
    }
}

private List<Player> notifyOnline(CommandSender s) {
    return Bukkit.getOnlinePlayers().stream()
        .filter(p -> p.hasPermission("moderationplus.notify") && !p.getName().equals(s.getName()))
        .collect(Collectors.toList());
}

private List<String> selectTargets(CommandSender s, String sel) {
    if (sel.startsWith("@")) {
        return Bukkit.selectEntities(s, sel).stream().map(Entity::getName).toList();
    }
    return List.of(sel);
}

private long writeExpiry(File f, String[] args) {
    long expiry = -1;
    f.getParentFile().mkdirs();
    try (FileWriter w = new FileWriter(f)) {
        if (args.length >= 2 && args[1].matches("\\d+[smhd]")) {
            Duration d = parseDuration(args[1]);
            if (d.getSeconds() > 0) {
                expiry = Instant.now().plus(d).getEpochSecond();
            }
        }
        w.write(expiry < 0 ? "forever" : Long.toString(expiry));
    } catch (IOException e) {
        getLogger().severe("Failed writing expiry: " + e.getMessage());
    }
    return expiry;
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
    File d = getDataFolder();
    if (!d.exists()) d.mkdirs();
    new File(d, "playerlogs").mkdirs();
    new File(d, "playermutes").mkdirs();
    new File(d, "playerjails").mkdirs();
}

private void logAction(UUID id, String action) {
    try (FileWriter w = new FileWriter(new File(getDataFolder(), "playerlogs/" + id + ".log"), true)) {
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        w.write("[" + ts + "] " + action + System.lineSeparator());
    } catch (IOException e) {
        getLogger().severe("Log error: " + e.getMessage());
    }
}

private Duration parseDuration(String in) {
    long n = Long.parseLong(in.substring(0, in.length() - 1));
    return switch (in.charAt(in.length() - 1)) {
        case 's' -> Duration.ofSeconds(n);
        case 'm' -> Duration.ofMinutes(n);
        case 'h' -> Duration.ofHours(n);
        case 'd' -> Duration.ofDays(n);
        default -> throw new IllegalArgumentException("Invalid suffix");
    };
}

@EventHandler
public void onChat(AsyncPlayerChatEvent e) {
    Player p = e.getPlayer();
    File f = new File(getDataFolder(), "playermutes/" + p.getUniqueId());
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
            p.sendActionBar(Component.text("§cMuted for " + secs + "s"));
        } else {
            p.sendActionBar(Component.text("§cMuted"));
        }
    } catch (IOException ex) {
        getLogger().severe("Error reading mute file: " + ex.getMessage());
    }
    e.setCancelled(true);
}

private boolean isJailed(Player p) {
    File f = new File(getDataFolder(), "playerjails/" + p.getUniqueId());
    return f.exists() && !hasExpired(f);
}


private boolean isInJailRegion(Player player) {
    RegionContainer container = WorldGuard
        .getInstance()
        .getPlatform()
        .getRegionContainer();
    RegionQuery query = container.createQuery();

    // fully qualify the WorldEdit Location so Java knows which one you mean
    com.sk89q.worldedit.util.Location wgLoc =
        BukkitAdapter.adapt(player.getLocation());

    ApplicableRegionSet set = query.getApplicableRegions(wgLoc);

    for (ProtectedRegion region : set) {
        if ("jail".equalsIgnoreCase(region.getId())) {
            return true;
        }
    }
    return false;
}

private Location getJailLocation() {
    var cfg = getConfig();
    var world = getServer().getWorld(cfg.getString("jail.world"));
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

private void preventJailEscape(Player p) {
    Location jailLocation = getJailLocation();
    if (jailLocation != null) {
        p.teleport(jailLocation);
    }
}

private void preventJailOverstay(Player p) {
    boolean ess = getServer().getPluginManager().isPluginEnabled("EssentialsSpawn");
    if (ess) {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "spawn " + p.getName());
    } else {
        p.teleport(p.getWorld().getSpawnLocation());
    }
    p.sendMessage("§aYou have served your sentence");
}

@EventHandler
public void onPlayerMove(PlayerMoveEvent e) {
    Player p = e.getPlayer();
    if (isJailed(p)) {
        if (!isInJailRegion(p)) {
            preventJailEscape(p);
        }
    } else {
        if (isInJailRegion(p)) {
            preventJailOverstay(p);
        }
    }
}

@EventHandler
public void onPlayerJoin(PlayerJoinEvent e) {
    Player p = e.getPlayer();
    if (isJailed(p)) {
        if (!isInJailRegion(p)) {
            preventJailEscape(p);
        } else {
            p.sendActionBar(Component.text("§cYou are jailed"));
        }
    } else {
        if (isInJailRegion(p)) {
            preventJailOverstay(p);
        }
    }
}

}
