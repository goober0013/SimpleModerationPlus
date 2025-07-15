package io.github.goober0013.simplemoderationplus.api;

import static com.mojang.brigadier.arguments.StringArgumentType.word;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.argument.CustomArgumentType;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses durations of the form <number><unit>, where unit ∈ {s,m,h,d,w,M,y}
 * (seconds, minutes, hours, days, weeks, months=30d, years=365d).
 * Also provides tab-completion suggestions for "[typedNumber]s", "[typedNumber]m", etc.
 */
public class DurationArgumentType
    implements CustomArgumentType.Converted<Duration, String> {

    private static final Pattern DURATION_PATTERN = Pattern.compile(
        "^(\\d+)([smhdwMy])$"
    );
    private static final List<String> UNITS = Arrays.asList(
        "s",
        "m",
        "h",
        "d",
        "w",
        "M",
        "y"
    );

    private static final DurationArgumentType INSTANCE =
        new DurationArgumentType();

    /** Returns the underlying native argument type sent to clients */
    @Override
    public ArgumentType<String> getNativeType() {
        return word();
    }

    /** Factory for Brigadier registration */
    public static DurationArgumentType duration() {
        return INSTANCE;
    }

    /** SuggestionProvider you can attach via `.suggests(...)` */
    public static CompletableFuture<Suggestions> suggest(
        CommandContext<CommandSourceStack> ctx,
        SuggestionsBuilder builder
    ) {
        String rem = builder.getRemaining();
        // strip any non-digit suffix to get the current number
        String digits = rem.replaceAll("\\D.*", "");
        if (!digits.matches("\\d+")) {
            return builder.buildFuture(); // nothing to suggest
        }
        for (String unit : UNITS) {
            builder.suggest(digits + unit);
        }
        return builder.buildFuture();
    }

    /** Converts the parsed String into a Duration */
    @Override
    public Duration convert(String raw) throws CommandSyntaxException {
        Matcher m = DURATION_PATTERN.matcher(raw);
        if (!m.matches()) {
            throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.readerInvalidBool().createWithContext(
                new StringReader(raw),
                "Invalid duration"
            );
        }
        long num = Long.parseLong(m.group(1));
        return switch (m.group(2)) {
            case "s" -> Duration.ofSeconds(num);
            case "m" -> Duration.ofMinutes(num);
            case "h" -> Duration.ofHours(num);
            case "d" -> Duration.ofDays(num);
            case "w" -> Duration.ofDays(num * 7);
            case "M" -> Duration.ofDays(num * 30);
            case "y" -> Duration.ofDays(num * 365);
            default -> throw new IllegalStateException("Unreachable unit");
        };
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(
        CommandContext<S> context,
        SuggestionsBuilder builder
    ) {
        String rem = builder.getRemaining();
        // If nothing left to complete (i.e., user typed a valid duration + space), stop suggesting
        if (rem.isEmpty() || !rem.matches("\\d+")) {
            return builder.buildFuture(); // no suggestions, move to next argument branch
        }
        // Otherwise, offer number+unit suggestions
        for (String unit : UNITS) {
            builder.suggest(rem + unit);
        }
        return builder.buildFuture();
    }

    @Override
    public Duration parse(StringReader reader) throws CommandSyntaxException {
        int start = reader.getCursor();
        String txt = reader.readUnquotedString(); // reads until whitespace
        Matcher m = DURATION_PATTERN.matcher(txt);
        if (!m.matches()) {
            reader.setCursor(start);
            throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.readerInvalidBool().createWithContext(
                reader,
                "Invalid duration"
            );
        }
        long num = Long.parseLong(m.group(1));
        switch (m.group(2)) {
            case "s":
                return Duration.ofSeconds(num);
            case "m":
                return Duration.ofMinutes(num);
            case "h":
                return Duration.ofHours(num);
            case "d":
                return Duration.ofDays(num);
            case "w":
                return Duration.ofDays(num * 7);
            case "M":
                return Duration.ofDays(num * 30);
            case "y":
                return Duration.ofDays(num * 365);
            default: // unreachable
                throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.readerInvalidBool().createWithContext(
                    reader,
                    "Unknown unit"
                );
        }
    }
}
