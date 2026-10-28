package com.eternalcode.parcellockers.gui.implementation.admin;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.dialog.DialogResponseView;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.minimessage.MiniMessage;

@SuppressWarnings("UnstableApiUsage")
class ConfirmationDialogFactory {

    private final MiniMessage miniMessage;

    ConfirmationDialogFactory(MiniMessage miniMessage) {
        this.miniMessage = miniMessage;
    }

    Dialog create(String title, String confirmLabel, String cancelLabel, Runnable onConfirm, Runnable onCancel) {
        return Dialog.create(builder -> builder.empty()
            .base(DialogBase.builder(this.miniMessage.deserialize(title))
                .canCloseWithEscape(true)
                .build())
            .type(DialogType.confirmation(
                ActionButton.create(
                    this.miniMessage.deserialize(confirmLabel),
                    null,
                    200,
                    DialogAction.customClick((DialogResponseView view, Audience audience) -> onConfirm.run(),
                        ClickCallback.Options.builder().uses(1).lifetime(ClickCallback.DEFAULT_LIFETIME).build())),
                ActionButton.create(
                    this.miniMessage.deserialize(cancelLabel),
                    null,
                    200,
                    DialogAction.customClick((DialogResponseView view, Audience audience) -> onCancel.run(),
                        ClickCallback.Options.builder().uses(1).lifetime(ClickCallback.DEFAULT_LIFETIME).build())))));
    }
}
