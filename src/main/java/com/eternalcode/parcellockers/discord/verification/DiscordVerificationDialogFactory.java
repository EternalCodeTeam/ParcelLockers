package com.eternalcode.parcellockers.discord.verification;

import com.eternalcode.parcellockers.configuration.implementation.MessageConfig;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.dialog.DialogResponseView;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import java.util.List;
import java.util.function.BiConsumer;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.minimessage.MiniMessage;

/**
 * Factory for creating Discord verification dialogs.
 * <p>
 * This implementation relies on Paper's Dialog API ({@code io.papermc.paper.dialog}),
 * which is marked as unstable and may change or be removed in future Paper versions.
 */
class DiscordVerificationDialogFactory {

    private final MiniMessage miniMessage;
    private final MessageConfig messageConfig;

    DiscordVerificationDialogFactory(MiniMessage miniMessage, MessageConfig messageConfig) {
        this.miniMessage = miniMessage;
        this.messageConfig = messageConfig;
    }

    /**
     * Creates a verification dialog for Discord account linking.
     *
     * @param onVerify callback when the user clicks verify, receives the entered code
     * @param onCancel callback when the user clicks cancel
     * @return the created dialog
     */
    Dialog create(BiConsumer<DialogResponseView, String> onVerify, Runnable onCancel) {
        return Dialog.create(builder -> builder.empty()
            .base(DialogBase.builder(this.miniMessage.deserialize(this.messageConfig.discord.verificationDialogTitle))
                .canCloseWithEscape(false)
                .inputs(List.of(DialogInput.text(
                        "code",
                        this.miniMessage.deserialize(this.messageConfig.discord.verificationDialogPlaceholder))
                    .build()))
                .build())
            .type(DialogType.confirmation(this.createVerifyButton(onVerify), this.createCancelButton(onCancel))));
    }

    private ActionButton createVerifyButton(BiConsumer<DialogResponseView, String> onVerify) {
        return ActionButton.create(
            this.miniMessage.deserialize(this.messageConfig.discord.verificationButtonVerifyText),
            this.miniMessage.deserialize(this.messageConfig.discord.verificationButtonVerifyDescription),
            200,
            DialogAction.customClick(
                (DialogResponseView view, Audience audience) -> {
                    String enteredCode = view.getText("code");
                    onVerify.accept(view, enteredCode);
                }, ClickCallback.Options.builder().uses(1).lifetime(ClickCallback.DEFAULT_LIFETIME).build()));
    }

    private ActionButton createCancelButton(Runnable onCancel) {
        return ActionButton.create(
            this.miniMessage.deserialize(this.messageConfig.discord.verificationButtonCancelText),
            this.miniMessage.deserialize(this.messageConfig.discord.verificationButtonCancelDescription),
            200,
            DialogAction.customClick(
                (DialogResponseView view, Audience audience) -> onCancel.run(),
                ClickCallback.Options.builder().uses(1).lifetime(ClickCallback.DEFAULT_LIFETIME).build()));
    }
}
