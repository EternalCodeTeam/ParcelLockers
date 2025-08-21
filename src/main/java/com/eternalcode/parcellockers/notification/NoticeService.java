package com.eternalcode.parcellockers.notification;

import com.eternalcode.multification.adventure.AudienceConverter;
import com.eternalcode.multification.bukkit.BukkitMultification;
import com.eternalcode.multification.translation.TranslationProvider;
import com.eternalcode.parcellockers.configuration.implementation.MessagesConfig;
import net.kyori.adventure.platform.AudienceProvider;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.ComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class NoticeService extends BukkitMultification<MessagesConfig> {

    private final MessagesConfig messages;
    private final MiniMessage miniMessage;
    private final AudienceProvider audienceProvider;

    public NoticeService(MessagesConfig messages, MiniMessage miniMessage, AudienceProvider audienceProvider) {
        this.messages = messages;
        this.miniMessage = miniMessage;
        this.audienceProvider = audienceProvider;
    }

    @Override
    protected @NotNull TranslationProvider<MessagesConfig> translationProvider() {
        return locale -> this.messages;
    }

    @Override
    protected @NotNull AudienceConverter<CommandSender> audienceConverter() {
        return  commandSender -> {
            if (commandSender instanceof Player player) {
                return this.audienceProvider.player(player.getUniqueId());
            }

            return this.audienceProvider.console();
        };
    }

    @Override
    protected @NotNull ComponentSerializer<Component, Component, String> serializer() {
        return this.miniMessage;
    }
}
