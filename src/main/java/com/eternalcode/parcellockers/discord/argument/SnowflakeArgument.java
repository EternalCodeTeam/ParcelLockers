package com.eternalcode.parcellockers.discord.argument;

import dev.rollczi.litecommands.argument.Argument;
import dev.rollczi.litecommands.argument.parser.ParseResult;
import dev.rollczi.litecommands.argument.resolver.ArgumentResolver;
import dev.rollczi.litecommands.invocation.Invocation;
import dev.rollczi.litecommands.suggestion.SuggestionContext;
import dev.rollczi.litecommands.suggestion.SuggestionResult;
import discord4j.common.util.Snowflake;
import org.bukkit.command.CommandSender;

public class SnowflakeArgument extends ArgumentResolver<CommandSender, Snowflake> {

    @Override
    protected ParseResult<Snowflake> parse(
        Invocation<CommandSender> invocation,
        Argument<Snowflake> context,
        String argument) {
        try {
            // Try to parse the string as a Snowflake
            Snowflake snowflake = Snowflake.of(argument);
            return ParseResult.success(snowflake);
        }
        catch (NumberFormatException exception) {
            // If parsing fails, return an error
            return ParseResult.failure("&4✘ &cInvalid Discord ID format! Please provide a valid Discord ID.");
        }
    }

    @Override
    public SuggestionResult suggest(
        Invocation<CommandSender> invocation,
        Argument<Snowflake> argument,
        SuggestionContext context) {
        // No suggestions for Discord IDs (they are unique numeric values)
        return SuggestionResult.empty();
    }
}
