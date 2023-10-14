package com.eternalcode.parcellockers.conversation;

import com.eternalcode.parcellockers.configuration.implementation.PluginConfiguration;
import com.eternalcode.parcellockers.notification.NotificationAnnouncer;
import org.bukkit.command.CommandSender;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.Prompt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import panda.utilities.StringUtils;

public class ParcelLockerPlacePrompt implements Prompt {

    private final NotificationAnnouncer announcer;
    private final PluginConfiguration config;

    public ParcelLockerPlacePrompt(NotificationAnnouncer announcer, PluginConfiguration config) {
        this.announcer = announcer;
        this.config = config;
    }

    @Override
    public Prompt acceptInput(@NotNull ConversationContext context, @Nullable String input) {
        context.setSessionData("description", input);
        return END_OF_CONVERSATION;
    }

    @NotNull
    @Override
    public String getPromptText(@NotNull ConversationContext context) {
        this.announcer.sendMessage((CommandSender) context.getForWhom(), this.config.messages.enterDescriptionPrompt);
        return StringUtils.EMPTY;
    }

    @Override
    public boolean blocksForInput(@NotNull ConversationContext context) {
        return true;
    }
}
