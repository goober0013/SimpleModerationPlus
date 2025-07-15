package io.github.goober0013.simplemoderationplus.api;

import com.destroystokyo.paper.profile.PlayerProfile;
import io.github.goober0013.simplemoderationplus.SimpleModerationPlus;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.stream.Stream;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.translation.GlobalTranslator;
import net.kyori.adventure.translation.TranslationStore;
import net.kyori.adventure.translation.TranslationStore.StringBased;
import org.apache.commons.lang3.LocaleUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class MessageProperties {

    private static final Properties systemMessages = new Properties();

    /**
     * Loads all key/value pairs from the given file into this class's internal properties.
     * Clears any previously loaded messages.
     *
     * @param file  the .properties file to load
     * @throws IOException if the file can't be read or parsed
     */
    public static final void load() {
        final Path langDir = SimpleModerationPlus.dataFolder
            .toPath()
            .resolve("lang");
        try (Stream<Path> paths = Files.list(langDir)) {
            // Create a message-format translation store under "simplemoderationplus:messages"
            Key namespace = Key.key("simplemoderationplus", "messages");
            TranslationStore.StringBased<MessageFormat> store =
                TranslationStore.messageFormat(namespace);

            paths
                .filter(Files::isRegularFile)
                .filter(path ->
                    path
                        .getFileName()
                        .toString()
                        .matches("^messages\\..*\\.properties$")
                )
                .forEach(path -> {
                    try {
                        final String fileName = path.getFileName().toString();
                        final String localeStr = fileName.substring(
                            fileName.indexOf('.') + 1,
                            fileName.lastIndexOf('.')
                        );

                        // Parse the locale
                        final Locale locale = LocaleUtils.toLocale(localeStr);

                        // Load the properties file from disk
                        final InputStream in = Files.newInputStream(path);
                        final ResourceBundle bundle =
                            new PropertyResourceBundle(in);

                        // Register all entries
                        store.registerAll(locale, bundle, true);
                    } catch (Exception e) {}
                });

            // Add to the GlobalTranslator
            GlobalTranslator.translator().addSource(store);

            final Path systemMessagesFile = langDir.resolve(
                "messages.properties"
            );
            if (!Files.exists(systemMessagesFile)) {
                throw new IOException(
                    "/plugins/SimpleModerationPlus/lang/messages.properties not found"
                );
            }
            final FileInputStream in = new FileInputStream(
                systemMessagesFile.toFile()
            );
            systemMessages.load(in);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Retrieves a system message template by key:
     *
     * @param key       the message key
     * @return the formatted message
     */
    public static final String get(String key) {
        return systemMessages.getProperty(key, key);
    }

    /**
     * Retrieves a player message template by key, then replaces placeholders:
     *  - {0} → admin.getName()
     *  - {1} → player.getName()
     *  - {2} → duration.toString()
     *  - {3} → reason
     *
     * Any of the last four args may be null; in that case the placeholder is left alone
     * @param key       the message key
     * @param admin     a String of the admin's name (replaces {0}), or null
     * @param player    the PlayerProfile (replaces {1}), or null
     * @param duration  the Duration (replaces {2}), or null
     * @param reason    the reason string (replaces {3}), or null
     * @return the formatted message
     */
    public static final Component get(
        String key,
        String admin,
        PlayerProfile player,
        Duration duration,
        String reason
    ) {
        String msg = systemMessages.getProperty(key, key);

        final String adminArg = admin != null ? admin : "{0}";
        final String playerArg = player != null ? player.getName() : "{1}";
        final String durationArg = duration != null
            ? DurationFormatUtils.formatDurationWords(
                duration.toMillis(),
                true,
                true
            )
            : "{2}";
        final String reasonArg = reason != null ? reason : "{3}";

        msg = msg
            .replace("{0}", adminArg)
            .replace("{1}", playerArg)
            .replace("{2}", durationArg)
            .replace("{3}", reasonArg);

        return Component.translatable(
            key,
            msg,
            List.of(
                Component.text(adminArg),
                Component.text(playerArg),
                Component.text(durationArg),
                Component.text(reasonArg)
            )
        );
    }

    /**
     * Retrieves a message template by key, then replaces placeholders:
     *  - {0} → admin.getName()
     *  - {1} → player.getName()
     *  - {2} → duration.toString()
     *  - {3} → reason
     *
     * Any of the last four args may be null; in that case the placeholder is left alone
     * @param key       the message key
     * @param admin     the CommandSender (replaces {0}), or null
     * @param player    the PlayerProfile (replaces {1}), or null
     * @param duration  the Duration (replaces {2}), or null
     * @param reason    the reason string (replaces {3}), or null
     * @return the formatted message
     */
    public static final Component get(
        String key,
        CommandSender admin,
        PlayerProfile player,
        Duration duration,
        String reason
    ) {
        return get(key, admin.getName(), player, duration, reason);
    }

    /**
     * Makes the output colored red. For errors.
     * Retrieves a message template by key, then replaces placeholders:
     *  - {0} → admin.getName()
     *  - {1} → player.getName()
     *  - {2} → duration.toString()
     *  - {3} → reason
     *
     * Any of the last four args may be null; in that case the placeholder is left alone
     * @param key       the message key
     * @param admin     the CommandSender (replaces {0}), or null
     * @param player    the PlayerProfile (replaces {1}), or null
     * @param duration  the Duration (replaces {2}), or null
     * @param reason    the reason string (replaces {3}), or null
     * @return the formatted message
     */
    public static final Component getRed(
        String key,
        String admin,
        PlayerProfile player,
        Duration duration,
        String reason
    ) {
        return get(key, admin, player, duration, reason).color(
            NamedTextColor.RED
        );
    }

    /**
     * Makes the output colored red. For errors.
     * Retrieves a message template by key, then replaces placeholders:
     *  - {0} → admin.getName()
     *  - {1} → player.getName()
     *  - {2} → duration.toString()
     *  - {3} → reason
     *
     * Any of the last four args may be null; in that case the placeholder is left alone
     * @param key       the message key
     * @param admin     the CommandSender (replaces {0}), or null
     * @param player    the PlayerProfile (replaces {1}), or null
     * @param duration  the Duration (replaces {2}), or null
     * @param reason    the reason string (replaces {3}), or null
     * @return the formatted message
     */
    public static final Component getRed(
        String key,
        CommandSender admin,
        PlayerProfile player,
        Duration duration,
        String reason
    ) {
        return get(key, admin, player, duration, reason).color(
            NamedTextColor.RED
        );
    }
}
