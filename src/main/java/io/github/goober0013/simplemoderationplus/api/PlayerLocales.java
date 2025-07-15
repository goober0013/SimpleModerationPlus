package io.github.goober0013.simplemoderationplus.api;

import com.destroystokyo.paper.profile.PlayerProfile;
import java.util.IllformedLocaleException;
import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class PlayerLocales {

    private static final Locale defaultLocale = Locale.of("system");

    private PlayerLocales() {
        // utility class
    }

    /**
     * Get the locale for a CommandSender.
     * @param sender the CommandSender
     * @return the sender's Locale, or a default locale on error/invalid
     */
    public static final Locale get(CommandSender sender) {
        if (sender instanceof Player) {
            return ((Player) sender).locale();
        }
        // else
        return defaultLocale;
    }

    /**
     * Get the locale for a PlayerProfile.
     * @param profile the PlayerProfile
     * @return profile's Locale, or a default locale on error/invalid
     */
    public static final Locale get(PlayerProfile profile) {
        if (profile == null) {
            return defaultLocale;
        }

        // Convert profile to online Player via its UUID :contentReference[oaicite:1]{index=1} and Bukkit#getPlayer(UUID) :contentReference[oaicite:2]{index=2}
        Player player = Bukkit.getPlayer(profile.getId());
        if (player != null) {
            return player.locale();
        }

        return defaultLocale;
    }
}
