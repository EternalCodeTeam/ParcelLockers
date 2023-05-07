package com.eternalcode.parcellockers.command.argument;

import com.eternalcode.parcellockers.database.ParcelDatabaseService;
import com.eternalcode.parcellockers.parcel.Parcel;
import dev.rollczi.litecommands.argument.ArgumentName;
import dev.rollczi.litecommands.argument.simple.OneArgument;
import dev.rollczi.litecommands.command.LiteInvocation;
import dev.rollczi.litecommands.suggestion.Suggestion;
import panda.std.Result;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

@ArgumentName("parcel")
public class ParcelArgument implements OneArgument<Parcel> {

    private final ParcelDatabaseService databaseService;

    public ParcelArgument(ParcelDatabaseService databaseService) {
        this.databaseService = databaseService;
    }

    @Override
    public Result<Parcel, ?> parse(LiteInvocation invocation, String argument) {
        AtomicReference<Parcel> result = new AtomicReference<>();
        this.databaseService.findAll().whenComplete((parcels, throwable) -> {
            Parcel target = parcels.stream()
                    .filter(parcel -> parcel.uuid().equals(UUID.fromString(argument)))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Parcel not found"));
            result.set(target);
        });
        return Result.ok(result.get());
    }


    @Override
    public List<Suggestion> suggest(LiteInvocation invocation) {
        AtomicReference<List<Suggestion>> suggestions = new AtomicReference<>();
        this.databaseService.findAll().whenComplete((parcels, throwable) ->
            suggestions.set(parcels.stream()
                    .map(Parcel::uuid)
                    .map(UUID::toString)
                    .map(Suggestion::of)
                    .toList()));
        return suggestions.get();
    }
}
