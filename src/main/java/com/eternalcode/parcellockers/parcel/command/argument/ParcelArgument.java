package com.eternalcode.parcellockers.parcel.command.argument;

import com.eternalcode.parcellockers.parcel.Parcel;
import com.eternalcode.parcellockers.parcel.repository.ParcelCache;
import dev.rollczi.litecommands.argument.Argument;
import dev.rollczi.litecommands.argument.parser.ParseResult;
import dev.rollczi.litecommands.argument.resolver.ArgumentResolver;
import dev.rollczi.litecommands.invalidusage.InvalidUsage;
import dev.rollczi.litecommands.invocation.Invocation;
import dev.rollczi.litecommands.suggestion.SuggestionContext;
import dev.rollczi.litecommands.suggestion.SuggestionResult;
import org.bukkit.command.CommandSender;

import java.util.Optional;
import java.util.UUID;

public class ParcelArgument extends ArgumentResolver<CommandSender, Parcel> {

    private final ParcelCache cache;

    public ParcelArgument(ParcelCache cache) {
        this.cache = cache;
    }

    @Override
    protected ParseResult<Parcel> parse(Invocation<CommandSender> invocation, Argument<Parcel> context, String argument) {
        try {
            UUID parcelId = UUID.fromString(argument);
            Optional<Parcel> parcel = this.cache.get(parcelId);
            return parcel.map(ParseResult::success)
                .orElseGet(() -> ParseResult.failure(InvalidUsage.Cause.INVALID_ARGUMENT));
        } catch (IllegalArgumentException e) {
            return ParseResult.failure(InvalidUsage.Cause.INVALID_ARGUMENT);
        }
    }

    @Override
    public SuggestionResult suggest(Invocation<CommandSender> invocation, Argument<Parcel> argument, SuggestionContext context) {
        return this.cache.cache().values().stream()
            .map(Parcel::uuid)
            .map(UUID::toString)
            .collect(SuggestionResult.collector());
    }
}
