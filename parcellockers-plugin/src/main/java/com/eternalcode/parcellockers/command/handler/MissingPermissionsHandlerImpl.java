package com.eternalcode.parcellockers.command.handler;

import com.eternalcode.parcellockers.notification.NoticeService;
import dev.rollczi.litecommands.handler.result.ResultHandlerChain;
import dev.rollczi.litecommands.invocation.Invocation;
import dev.rollczi.litecommands.permission.MissingPermissions;
import dev.rollczi.litecommands.permission.MissingPermissionsHandler;
import org.bukkit.command.CommandSender;

public class MissingPermissionsHandlerImpl implements MissingPermissionsHandler<CommandSender> {

    private final NoticeService noticeService;

    public MissingPermissionsHandlerImpl(NoticeService noticeService) {
        this.noticeService = noticeService;
    }

    @Override
    public void handle(Invocation<CommandSender> invocation, MissingPermissions missingPermissions, ResultHandlerChain<CommandSender> chain) {
        String joinedText = missingPermissions.asJoinedText();

        this.noticeService.create()
            .viewer(invocation.sender())
            .notice(messages -> messages.noPermission)
            .placeholder("{PERMISSION}", joinedText)
            .send();
    }
}
