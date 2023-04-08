package com.eternalcode.parcellockers.command.argument;

import com.eternalcode.parcellockers.parcel.Parcel;
import com.eternalcode.parcellockers.parcel.ParcelRepositoryJdbcImpl;
import dev.rollczi.litecommands.argument.ArgumentName;
import dev.rollczi.litecommands.argument.simple.OneArgument;
import dev.rollczi.litecommands.command.LiteInvocation;
import dev.rollczi.litecommands.suggestion.Suggestion;
import panda.std.Result;

import java.util.List;
import java.util.UUID;

@ArgumentName("parcel")
public class ParcelArgument implements OneArgument<Parcel> {

    private final ParcelRepositoryJdbcImpl parcelRepository;

    public ParcelArgument(ParcelRepositoryJdbcImpl parcelRepository) {
        this.parcelRepository = parcelRepository;
    }

    @Override
    public Result<Parcel, ?> parse(LiteInvocation invocation, String argument) {
        return this.parcelRepository.findByUuid(UUID.fromString(argument))
                .map(Result::ok)
                .orElseGet(() -> Result.error("Parcel not found"));
    }


    @Override
    public List<Suggestion> suggest(LiteInvocation invocation) {
        return this.parcelRepository.findAll().stream()
                .map(Parcel::getUuid)
                .map(UUID::toString)
                .map(Suggestion::of)
                .toList();
    }
}
