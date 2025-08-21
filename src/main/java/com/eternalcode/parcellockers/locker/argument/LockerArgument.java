package com.eternalcode.parcellockers.locker.argument;

import com.eternalcode.parcellockers.locker.Locker;
import com.eternalcode.parcellockers.locker.repository.LockerCache;
import dev.rollczi.litecommands.argument.Argument;
import dev.rollczi.litecommands.argument.parser.ParseResult;
import dev.rollczi.litecommands.argument.resolver.ArgumentResolver;
import dev.rollczi.litecommands.invocation.Invocation;
import dev.rollczi.litecommands.suggestion.SuggestionContext;
import dev.rollczi.litecommands.suggestion.SuggestionResult;
import org.bukkit.command.CommandSender;

public class LockerArgument extends ArgumentResolver<CommandSender, Locker> {

    private final LockerCache cache;

    public LockerArgument(LockerCache cache) {
        this.cache = cache;
    }

    @Override
    protected ParseResult<Locker> parse(Invocation<CommandSender> invocation, Argument<Locker> argument, String s) {
        return this.cache.cache().values()
            .stream()
            .filter(locker -> locker.description().equalsIgnoreCase(s))
            .findFirst()
            .map(ParseResult::success)
            .orElseThrow(() -> new IllegalArgumentException("Locker with description '" + s + "' not found"));
    }

    @Override
    public SuggestionResult suggest(Invocation<CommandSender> invocation, Argument<Locker> argument, SuggestionContext context) {
        return this.cache.cache()
            .values()
            .stream()
            .map(Locker::description)
            .collect(SuggestionResult.collector());
    }
}
