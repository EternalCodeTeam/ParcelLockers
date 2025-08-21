package com.eternalcode.parcellockers.conversation;

import org.bukkit.ChatColor;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.Prompt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ParcelLockerPlacePrompt implements Prompt {

    @Override
    public Prompt acceptInput(@NotNull ConversationContext context, @Nullable String input) {
        context.setSessionData("description", input);
        return END_OF_CONVERSATION;
    }

    @NotNull
    @Override
    public String getPromptText(@NotNull ConversationContext context) {
        // TODO do not hardcode this
        return ChatColor.translateAlternateColorCodes('&', "&6↵ &eEnter a description for the parcel locker:");
    }

    @Override
    public boolean blocksForInput(@NotNull ConversationContext context) {
        return true;
    }
}
