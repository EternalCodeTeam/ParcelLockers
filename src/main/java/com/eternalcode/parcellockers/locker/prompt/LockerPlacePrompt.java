package com.eternalcode.parcellockers.locker.prompt;

import com.eternalcode.parcellockers.notification.NoticeService;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.Prompt;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LockerPlacePrompt implements Prompt {

    private final NoticeService noticeService;

    public LockerPlacePrompt(NoticeService noticeService) {
        this.noticeService = noticeService;
    }

    @Override
    public Prompt acceptInput(@NotNull ConversationContext context, @Nullable String input) {
        context.setSessionData("description", input);
        return END_OF_CONVERSATION;
    }

    @NotNull
    @Override
    public String getPromptText(@NotNull ConversationContext context) {
        if (context.getForWhom() instanceof Player player) {
            this.noticeService.create()
                .player(player.getUniqueId())
                .notice(messages -> messages.locker.descriptionPrompt)
                .send();
        }
        return StringUtils.EMPTY;
    }

    @Override
    public boolean blocksForInput(@NotNull ConversationContext context) {
        return true;
    }
}
