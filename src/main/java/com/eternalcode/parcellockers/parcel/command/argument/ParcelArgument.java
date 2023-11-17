package com.eternalcode.parcellockers.parcel.command.argument;

import com.eternalcode.parcellockers.parcel.Parcel;
import com.eternalcode.parcellockers.parcel.repository.ParcelRepositoryImpl;
import dev.rollczi.litecommands.argument.Argument;
import dev.rollczi.litecommands.argument.parser.ParseResult;
import dev.rollczi.litecommands.argument.resolver.ArgumentResolver;
import dev.rollczi.litecommands.invalidusage.InvalidUsage;
import dev.rollczi.litecommands.invocation.Invocation;
import dev.rollczi.litecommands.suggestion.SuggestionContext;
import dev.rollczi.litecommands.suggestion.SuggestionResult;
import org.bukkit.command.CommandSender;

import java.util.UUID;

public class ParcelArgument extends ArgumentResolver<CommandSender, Parcel> {

    private final ParcelRepositoryImpl databaseService;

    public ParcelArgument(ParcelRepositoryImpl cache) {
        this.databaseService = cache;
    }

    @Override
    protected ParseResult<Parcel> parse(Invocation<CommandSender> invocation, Argument<Parcel> context, String argument) {
        Parcel parcel = this.databaseService.findParcel(UUID.fromString(argument)).orElse(null);

        if (parcel == null) {
            return ParseResult.failure(InvalidUsage.Cause.MISSING_ARGUMENT);
        }
        return ParseResult.success(parcel);
    }

    @Override
    public SuggestionResult suggest(Invocation<CommandSender> invocation, Argument<Parcel> argument, SuggestionContext context) {
        return this.databaseService.cache().values().stream()
                .map(Parcel::name)
                .collect(SuggestionResult.collector());
    }
}
