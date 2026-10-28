package com.eternalcode.parcellockers.notification;

import com.eternalcode.multification.adventure.AudienceConverter;
import com.eternalcode.multification.bukkit.BukkitMultification;
import com.eternalcode.multification.translation.TranslationProvider;
import com.eternalcode.parcellockers.configuration.implementation.MessageConfig;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.ComponentSerializer;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class NoticeService extends BukkitMultification<MessageConfig> {

    private final MessageConfig messages;
    private final MiniMessage miniMessage;

    public NoticeService(MessageConfig messages, MiniMessage miniMessage) {
        this.messages = messages;
        this.miniMessage = miniMessage;
    }

    @Override
    protected @NotNull TranslationProvider<MessageConfig> translationProvider() {
        return locale -> this.messages;
    }

    @Override
    protected @NotNull AudienceConverter<CommandSender> audienceConverter() {
        return commandSender -> commandSender;
    }

    @Override
    protected @NotNull ComponentSerializer<Component, Component, String> serializer() {
        return this.miniMessage;
    }
}
