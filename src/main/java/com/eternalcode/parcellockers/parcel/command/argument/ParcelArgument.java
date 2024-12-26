package com.eternalcode.parcellockers.parcel.command.argument;

import com.eternalcode.parcellockers.parcel.Parcel;
import com.eternalcode.parcellockers.parcel.repository.ParcelRepository;
import dev.rollczi.litecommands.argument.Argument;
import dev.rollczi.litecommands.argument.parser.ParseResult;
import dev.rollczi.litecommands.argument.resolver.ArgumentResolver;
import dev.rollczi.litecommands.invalidusage.InvalidUsage;
import dev.rollczi.litecommands.invocation.Invocation;
import dev.rollczi.litecommands.suggestion.SuggestionContext;
import dev.rollczi.litecommands.suggestion.SuggestionResult;
import org.bukkit.command.CommandSender;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class ParcelArgument extends ArgumentResolver<CommandSender, Parcel> {

    private final ParcelRepository databaseService;

    public ParcelArgument(ParcelRepository cache) {
        this.databaseService = cache;
    }

    @Override
    protected ParseResult<Parcel> parse(Invocation<CommandSender> invocation, Argument<Parcel> context, String argument) {
        UUID parcelId = UUID.fromString(argument);
        CompletableFuture<ParseResult<Parcel>> future = this.databaseService.findByUUID(parcelId)
            .thenApply(optional -> optional
                .map(ParseResult::success)
                .orElse(ParseResult.failure(InvalidUsage.Cause.INVALID_ARGUMENT)));
        return ParseResult.completableFuture(future);
    }

    @Override
    public SuggestionResult suggest(Invocation<CommandSender> invocation, Argument<Parcel> argument, SuggestionContext context) {
        return this.databaseService.cache().values().stream()
            .map(Parcel::uuid)
            .map(UUID::toString)
            .collect(SuggestionResult.collector());


    }
}
