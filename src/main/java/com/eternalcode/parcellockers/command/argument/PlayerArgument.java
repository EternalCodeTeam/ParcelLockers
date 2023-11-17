package com.eternalcode.parcellockers.command.argument;

import com.eternalcode.parcellockers.configuration.implementation.PluginConfiguration;
import dev.rollczi.litecommands.argument.Argument;
import dev.rollczi.litecommands.argument.parser.ParseResult;
import dev.rollczi.litecommands.argument.resolver.ArgumentResolver;
import dev.rollczi.litecommands.invocation.Invocation;
import dev.rollczi.litecommands.suggestion.SuggestionContext;
import dev.rollczi.litecommands.suggestion.SuggestionResult;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;

public class PlayerArgument extends ArgumentResolver<CommandSender, Player> {

    private final Server server;
    private final PluginConfiguration config;

    public PlayerArgument(Server server, PluginConfiguration config) {
        this.server = server;
        this.config = config;
    }

    @Override
    protected ParseResult<Player> parse(Invocation<CommandSender> invocation, Argument<Player> context, String argument) {
        Player player = this.server.getPlayer(argument);

        if (player == null) {
            return ParseResult.failure(this.config.messages.cantFindPlayer);
        }

        return ParseResult.success(player);
    }

    @Override
    public SuggestionResult suggest(Invocation<CommandSender> invocation, Argument<Player> argument, SuggestionContext context) {
        return this.server.getOnlinePlayers().stream()
                .map(HumanEntity::getName)
                .collect(SuggestionResult.collector());
    }

}
