package com.eternalcode.parcellockers.command.handler;

import com.eternalcode.multification.notice.Notice;
import com.eternalcode.parcellockers.notification.NoticeService;
import dev.rollczi.litecommands.handler.result.ResultHandler;
import dev.rollczi.litecommands.handler.result.ResultHandlerChain;
import dev.rollczi.litecommands.invocation.Invocation;
import org.bukkit.command.CommandSender;

public class NoticeHandler implements ResultHandler<CommandSender, Notice> {

    private final NoticeService noticeService;

    public NoticeHandler(NoticeService noticeService) {
        this.noticeService = noticeService;
    }

    @Override
    public void handle(Invocation<CommandSender> invocation, Notice result, ResultHandlerChain<CommandSender> chain) {
        this.noticeService.create()
            .viewer(invocation.sender())
            .notice(result)
            .send();
    }

}
