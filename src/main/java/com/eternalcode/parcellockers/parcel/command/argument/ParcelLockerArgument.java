package com.eternalcode.parcellockers.parcel.command.argument;

import com.eternalcode.parcellockers.locker.Locker;
import com.eternalcode.parcellockers.locker.repository.LockerRepositoryImpl;
import dev.rollczi.litecommands.argument.Argument;
import dev.rollczi.litecommands.argument.parser.ParseResult;
import dev.rollczi.litecommands.argument.resolver.ArgumentResolver;
import dev.rollczi.litecommands.invocation.Invocation;
import dev.rollczi.litecommands.suggestion.SuggestionContext;
import dev.rollczi.litecommands.suggestion.SuggestionResult;
import org.bukkit.command.CommandSender;

public class ParcelLockerArgument extends ArgumentResolver<CommandSender, Locker> {

    private final LockerRepositoryImpl lockerRepository;

    public ParcelLockerArgument(LockerRepositoryImpl lockerRepository) {
        this.lockerRepository = lockerRepository;
    }

    @Override
    public SuggestionResult suggest(Invocation<CommandSender> invocation, Argument<Locker> argument, SuggestionContext context) {
        return this.lockerRepository.cache()
            .values()
            .stream()
            .map(Locker::description)
            .collect(SuggestionResult.collector());
    }

    @Override
    protected ParseResult<Locker> parse(Invocation<CommandSender> invocation, Argument<Locker> argument, String s) {
        return ParseResult.success(this.lockerRepository.cache().values()
            .stream()
            .filter(locker -> locker.description().equalsIgnoreCase(s))
            .findFirst()
            .orElse(null));
    }
}
