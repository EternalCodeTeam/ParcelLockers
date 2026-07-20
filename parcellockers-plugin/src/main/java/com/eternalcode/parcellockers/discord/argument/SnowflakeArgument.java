package com.eternalcode.parcellockers.discord.argument;

import com.eternalcode.parcellockers.configuration.implementation.MessageConfig;
import dev.rollczi.litecommands.argument.Argument;
import dev.rollczi.litecommands.argument.parser.ParseResult;
import dev.rollczi.litecommands.argument.resolver.ArgumentResolver;
import dev.rollczi.litecommands.invocation.Invocation;
import discord4j.common.util.Snowflake;
import org.bukkit.command.CommandSender;

public class SnowflakeArgument extends ArgumentResolver<CommandSender, Snowflake> {

    private final MessageConfig messageConfig;

    public SnowflakeArgument(MessageConfig messageConfig) {
        this.messageConfig = messageConfig;
    }

    @Override
    protected ParseResult<Snowflake> parse(
        Invocation<CommandSender> invocation,
        Argument<Snowflake> context,
        String argument) {
        try {
            Snowflake snowflake = Snowflake.of(argument);
            return ParseResult.success(snowflake);
        }
        catch (NumberFormatException exception) {
            return ParseResult.failure(this.messageConfig.discord.invalidDiscordId);
        }
    }
}
