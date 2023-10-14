package com.eternalcode.parcellockers.parcel.command.argument;

import com.eternalcode.parcellockers.parcel.Parcel;
import com.eternalcode.parcellockers.parcel.repository.ParcelRepositoryImpl;
import dev.rollczi.litecommands.argument.ArgumentName;
import dev.rollczi.litecommands.argument.simple.OneArgument;
import dev.rollczi.litecommands.command.LiteInvocation;
import dev.rollczi.litecommands.suggestion.Suggestion;
import panda.std.Result;

import java.util.List;
import java.util.UUID;

@ArgumentName("parcel")
public class ParcelArgument implements OneArgument<Parcel> {

    private final ParcelRepositoryImpl databaseService;

    public ParcelArgument(ParcelRepositoryImpl cache) {
        this.databaseService = cache;
    }

    @Override
    public Result<Parcel, ?> parse(LiteInvocation invocation, String argument) {
        Parcel parcel = this.databaseService.findParcel(UUID.fromString(argument)).orElse(null);
        
        if (parcel == null) {
            return Result.error();
        }
        return Result.ok(parcel);
    }

    @Override
    public List<Suggestion> suggest(LiteInvocation invocation) {
        return this.databaseService.cache().values().stream()
                .map(Parcel::name)
                .map(Suggestion::of)
                .toList();
    }
}
