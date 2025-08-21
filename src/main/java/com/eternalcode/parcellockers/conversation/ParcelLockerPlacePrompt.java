package com.eternalcode.parcellockers.conversation;

import com.eternalcode.parcellockers.configuration.implementation.PluginConfig;
import org.bukkit.ChatColor;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.Prompt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ParcelLockerPlacePrompt implements Prompt {

    private final PluginConfig config;

    public ParcelLockerPlacePrompt(PluginConfig config) {
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
        return ChatColor.translateAlternateColorCodes('&', this.config.messages.enterDescriptionPrompt);
    }

    @Override
    public boolean blocksForInput(@NotNull ConversationContext context) {
        return true;
    }
}
