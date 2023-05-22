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

@ArgumentName("parcel")
public class ParcelArgument implements OneArgument<Parcel> {

    private final ParcelDatabaseService cache;

    public ParcelArgument(ParcelDatabaseService cache) {
        this.cache = cache;
    }

    @Override
    public Result<Parcel, ?> parse(LiteInvocation invocation, String argument) {
        return Result.ok(this.cache.getParcel(UUID.fromString(argument)));
    }


    @Override
    public List<Suggestion> suggest(LiteInvocation invocation) {
        return this.cache.getParcels().stream()
                .map(Parcel::uuid)
                .map(UUID::toString)
                .map(Suggestion::of)
                .toList();
    }
}
