package com.eternalcode.parcellockers.command.argument;

import com.eternalcode.parcellockers.parcel.repository.ParcelRepositoryImpl;
import dev.rollczi.litecommands.argument.Argument;
import dev.rollczi.litecommands.argument.parser.ParseResult;
import dev.rollczi.litecommands.argument.resolver.ArgumentResolver;
import dev.rollczi.litecommands.invocation.Invocation;
import dev.rollczi.litecommands.suggestion.SuggestionContext;
import dev.rollczi.litecommands.suggestion.SuggestionResult;
import dev.rollczi.litecommands.suggestion.SuggestionStream;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

public class UUIDArgument extends ArgumentResolver<CommandSender, UUID> {

    private final ParcelRepositoryImpl parcelRepository;

    public UUIDArgument(ParcelRepositoryImpl parcelRepository) {
        this.parcelRepository = parcelRepository;
    }

    @Override
    protected ParseResult<UUID> parse(Invocation<CommandSender> invocation, Argument<UUID> context, String argument) {
        return ParseResult.success(UUID.fromString(argument));
    }

    @Override
    public SuggestionResult suggest(Invocation<CommandSender> invocation, Argument<UUID> argument, SuggestionContext context) {
        if (!this.parcelRepository.cache().isEmpty()) {
            return SuggestionStream.of(this.parcelRepository.cache().keySet())
                    .map(UUID::toString)
                    .collect(s -> s);
        }
        List<UUID> uuids = new ArrayList<>();
        IntStream.rangeClosed(1, 10).forEach(i -> uuids.add(UUID.randomUUID()));
        return SuggestionStream.of(uuids)
                .map(UUID::toString)
                .collect(s -> s);
    }
}
