package com.eternalcode.parcellockers.command.argument;

import com.eternalcode.parcellockers.parcel.Parcel;
import com.eternalcode.parcellockers.parcel.ParcelRepositoryJdbcImpl;
import dev.rollczi.litecommands.argument.ArgumentName;
import dev.rollczi.litecommands.argument.simple.OneArgument;
import dev.rollczi.litecommands.command.LiteInvocation;
import dev.rollczi.litecommands.suggestion.Suggestion;
import org.bukkit.ChatColor;
import panda.std.Result;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@ArgumentName("parcel")
public class ParcelArgument implements OneArgument<Parcel> {

    private final ParcelRepositoryJdbcImpl parcelRepository;

    public ParcelArgument(ParcelRepositoryJdbcImpl parcelRepository) {
        this.parcelRepository = parcelRepository;
    }

    @Override
    public Result<Parcel, ?> parse(LiteInvocation invocation, String argument) {
        CompletableFuture<Optional<Parcel>> parcel = this.parcelRepository.findByUuid(UUID.fromString(argument));
        return parcel.whenComplete((optionalParcel, throwable) -> {
            if (!optionalParcel.isPresent()) {
                invocation.sender().sendMessage(ChatColor.RED + "Parcel not found");
            }
            if (throwable != null) {
                throwable.printStackTrace();
            }
        }).thenApply(optionalParcel -> optionalParcel.map(Result::ok)
                .orElse(Result.error("Something went wrong"))).join();
    }


    @Override
    public List<Suggestion> suggest(LiteInvocation invocation) {
        return this.parcelRepository.findAll().thenApply(parcels -> parcels.stream()
                .map(Parcel::uuid)
                .map(UUID::toString)
                .map(Suggestion::of)
                .toList()).join();
    }
}
