package com.eternalcode.parcellockers.command.argument;

import com.eternalcode.parcellockers.database.ParcelDatabaseService;
import com.eternalcode.parcellockers.parcel.Parcel;
import dev.rollczi.litecommands.argument.Argument;
import dev.rollczi.litecommands.argument.parser.ParseResult;
import dev.rollczi.litecommands.argument.resolver.ArgumentResolver;
import dev.rollczi.litecommands.invocation.Invocation;
import dev.rollczi.litecommands.suggestion.SuggestionContext;
import dev.rollczi.litecommands.suggestion.SuggestionResult;
import org.bukkit.command.CommandSender;

import java.util.UUID;

public class ParcelArgument extends ArgumentResolver<CommandSender, Parcel> {

    private final ParcelDatabaseService databaseService;

    public ParcelArgument(ParcelDatabaseService cache) {
        this.databaseService = cache;
    }

    @Override
    protected ParseResult<Parcel> parse(Invocation<CommandSender> invocation, Argument<Parcel> context, String argument) {
        Parcel parcel = this.databaseService.findParcel(UUID.fromString(argument)).orElse(null);

        if (parcel == null) {
            return ParseResult.failure();
        }
        return ParseResult.success(parcel);
    }

    @Override
    public SuggestionResult suggest(Invocation<CommandSender> invocation, Argument<Parcel> argument, SuggestionContext context) {
        return this.databaseService.cache().keySet().stream()
                .map(UUID::toString)
                .collect(SuggestionResult.collector());
    }
}
