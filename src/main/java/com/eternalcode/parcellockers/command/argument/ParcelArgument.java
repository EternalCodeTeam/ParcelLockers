package com.eternalcode.parcellockers.command.argument;

import com.eternalcode.parcellockers.database.ParcelDatabaseService;
import com.eternalcode.parcellockers.parcel.Parcel;
import dev.rollczi.litecommands.argument.ArgumentName;
import dev.rollczi.litecommands.argument.simple.OneArgument;
import dev.rollczi.litecommands.command.LiteInvocation;
import dev.rollczi.litecommands.suggestion.Suggestion;
import panda.std.Result;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@ArgumentName("parcel")
public class ParcelArgument implements OneArgument<Parcel> {

    private final ParcelDatabaseService databaseService;

    public ParcelArgument(ParcelDatabaseService databaseService) {
        this.databaseService = databaseService;
    }

    @Override
    public Result<Parcel, ?> parse(LiteInvocation invocation, String argument) {
        Set<Parcel> emptySet = new HashSet<>();
        this.databaseService.findAll(emptySet);
        return Result.ok(emptySet.stream()
                .filter(parcel -> parcel.uuid().toString().equals(argument))
                .findFirst()
                .get());
    }


    @Override
    public List<Suggestion> suggest(LiteInvocation invocation) {
        Set<Parcel> emptySet = new HashSet<>();
        this.databaseService.findAll(emptySet);
        return emptySet.stream().map(Parcel::uuid)
                .map(UUID::toString)
                .map(Suggestion::of)
                .toList();
    }
}
