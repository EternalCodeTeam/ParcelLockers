package com.eternalcode.parcellockers.gui.implementation.admin;

import static com.eternalcode.commons.adventure.AdventureUtil.resetItalic;

import com.eternalcode.commons.concurrent.FutureHandler;
import com.eternalcode.commons.scheduler.Scheduler;
import com.eternalcode.parcellockers.configuration.implementation.MessageConfig;
import com.eternalcode.parcellockers.configuration.implementation.PluginConfig.GuiSettings;
import com.eternalcode.parcellockers.configuration.serializable.ConfigItem;
import com.eternalcode.parcellockers.gui.GuiManager;
import com.eternalcode.parcellockers.gui.GuiView;
import com.eternalcode.parcellockers.notification.NoticeService;
import com.eternalcode.parcellockers.parcel.Parcel;
import com.eternalcode.parcellockers.parcel.ParcelSize;
import com.eternalcode.parcellockers.parcel.ParcelStatus;
import com.eternalcode.parcellockers.parcel.service.AdminParcelService;
import com.eternalcode.parcellockers.parcel.service.EditResult;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.dialog.DialogResponseView;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;

@SuppressWarnings("UnstableApiUsage")
public class AdminParcelEditGui implements GuiView {

    private static final int NAME_SLOT = 10;
    private static final int DESCRIPTION_SLOT = 11;
    private static final int PRIORITY_SLOT = 12;
    private static final int SIZE_SLOT = 13;
    private static final int STATUS_SLOT = 14;
    private static final int RECEIVER_SLOT = 15;
    private static final int DESTINATION_SLOT = 16;
    private static final int CONTENTS_SLOT = 30;
    private static final int DELETE_SLOT = 32;
    private static final int CLOSE_SLOT = 49;

    private final Scheduler scheduler;
    private final MiniMessage miniMessage;
    private final GuiSettings guiSettings;
    private final MessageConfig messageConfig;
    private final NoticeService noticeService;
    private final GuiManager guiManager;
    private final AdminParcelService adminParcelService;
    private final GuiView parent;
    private final Parcel parcel;
    private final ConfirmationDialogFactory confirmationDialogFactory;

    public AdminParcelEditGui(Scheduler scheduler, MiniMessage miniMessage, GuiSettings guiSettings,
            MessageConfig messageConfig, NoticeService noticeService, GuiManager guiManager,
            AdminParcelService adminParcelService, GuiView parent, Parcel parcel) {
        this.scheduler = scheduler;
        this.miniMessage = miniMessage;
        this.guiSettings = guiSettings;
        this.messageConfig = messageConfig;
        this.noticeService = noticeService;
        this.guiManager = guiManager;
        this.adminParcelService = adminParcelService;
        this.parent = parent;
        this.parcel = parcel;
        this.confirmationDialogFactory = new ConfirmationDialogFactory(miniMessage);
    }

    @Override
    public void show(Player player) {
        Gui gui = Gui.gui()
            .title(resetItalic(this.miniMessage.deserialize(this.guiSettings.adminParcelEditGuiTitle)))
            .rows(6)
            .disableAllInteractions()
            .create();

        GuiItem background = this.guiSettings.mainGuiBackgroundItem.toGuiItem();
        for (int i = 0; i < gui.getRows() * 9; i++) {
            gui.setItem(i, background);
        }

        gui.setItem(NAME_SLOT, this.button(this.guiSettings.adminEditNameButton, "{NAME}", this.parcel.name(),
            event -> this.openTextDialog(player, "<yellow>Enter parcel name:", "<gray>Name", name -> {
                if (name == null || name.isBlank()) {
                    this.scheduler.run(() -> this.show(player));
                    return;
                }
                this.apply(player, this.adminParcelService.changeName(this.parcel, name));
            })));

        gui.setItem(DESCRIPTION_SLOT, this.button(this.guiSettings.adminEditDescriptionButton, "{DESCRIPTION}", this.parcel.description(),
            event -> this.openTextDialog(player, "<yellow>Enter description:", "<gray>Description", description ->
                this.apply(player, this.adminParcelService.changeDescription(this.parcel, description)))));

        gui.setItem(PRIORITY_SLOT, this.button(this.guiSettings.adminEditPriorityButton, "{PRIORITY}", this.parcel.priority() ? "Yes" : "No",
            event -> this.applyPriority(player, !this.parcel.priority())));

        gui.setItem(SIZE_SLOT, this.button(this.guiSettings.adminEditSizeButton, "{SIZE}", this.parcel.size().name(),
            event -> this.applySize(player, nextSize(this.parcel.size()))));

        gui.setItem(STATUS_SLOT, this.button(this.guiSettings.adminEditStatusButton, "{STATUS}", this.parcel.status().name(),
            event -> this.applyStatus(player, nextStatus(this.parcel.status()))));

        gui.setItem(RECEIVER_SLOT, this.button(this.guiSettings.adminEditReceiverButton, "{RECEIVER}", this.parcel.receiver().toString(),
            event -> new AdminReceiverPickerGui(this.scheduler, this.miniMessage, this.guiSettings, this.guiManager, this,
                (p, user) -> this.apply(p, this.adminParcelService.changeReceiver(this.parcel, user.uuid()))).show(player)));

        gui.setItem(DESTINATION_SLOT, this.button(this.guiSettings.adminEditDestinationButton, "{DESTINATION}", this.parcel.destinationLocker().toString(),
            event -> new AdminDestinationPickerGui(this.scheduler, this.miniMessage, this.guiSettings, this.guiManager, this,
                (p, locker) -> this.applyDestination(p, locker.uuid())).show(player)));

        gui.setItem(CONTENTS_SLOT, this.guiSettings.adminEditContentsButton.toGuiItem(event ->
            new AdminParcelContentGui(this.scheduler, this.guiSettings, this.miniMessage, this.guiManager,
                this.noticeService, this.parcel, this::reopenFresh).show(player)));

        gui.setItem(DELETE_SLOT, this.guiSettings.adminDeleteParcelButton.toGuiItem(event ->
            player.showDialog(this.confirmationDialogFactory.create(
                "<red>Delete parcel '" + this.miniMessage.escapeTags(this.parcel.name()) + "'?",
                "<dark_red>Delete", "<gray>Cancel",
                () -> this.guiManager.deleteParcel(this.parcel).thenRun(() -> {
                    this.noticeService.create().notice(m -> m.admin.parcelDeleted).player(player.getUniqueId()).send();
                    this.scheduler.run(() -> this.parent.show(player));
                }).exceptionally(FutureHandler::handleException),
                () -> this.scheduler.run(() -> this.show(player))))));

        gui.setItem(CLOSE_SLOT, this.guiSettings.closeItem.toGuiItem(event -> this.parent.show(player)));
        gui.setDefaultClickAction(event -> event.setCancelled(true));
        this.scheduler.run(() -> gui.open(player));
    }

    private GuiItem button(ConfigItem template, String placeholder, String value, dev.triumphteam.gui.components.GuiAction<org.bukkit.event.inventory.InventoryClickEvent> action) {
        ConfigItem item = template.clone();
        return item.name(item.name().replace(placeholder, value))
            .lore(item.lore().stream().map(line -> line.replace(placeholder, value)).toList())
            .toGuiItem(action);
    }

    private void apply(Player player, CompletableFuture<EditResult> future) {
        future.thenAccept(result -> {
            this.notifyResult(player, result);
            this.scheduler.run(() -> this.reopenFresh(player));
        }).exceptionally(FutureHandler::handleException);
    }

    private void applyPriority(Player player, boolean priority) {
        this.adminParcelService.changePriority(this.parcel, priority).thenAccept(result -> {
            this.noticeService.create().notice(m -> m.admin.priorityUpdated).player(player.getUniqueId()).send();
            this.scheduler.run(() -> this.reopenFresh(player));
        }).exceptionally(FutureHandler::handleException);
    }

    private void applySize(Player player, ParcelSize size) {
        this.adminParcelService.changeSize(this.parcel, size).thenAccept(result -> {
            this.notifyResult(player, result);
            this.scheduler.run(() -> this.reopenFresh(player));
        }).exceptionally(FutureHandler::handleException);
    }

    private void applyStatus(Player player, ParcelStatus status) {
        this.adminParcelService.changeStatus(this.parcel, status).thenAccept(result -> {
            this.notifyResult(player, result);
            this.scheduler.run(() -> this.reopenFresh(player));
        }).exceptionally(FutureHandler::handleException);
    }

    private void applyDestination(Player player, UUID destination) {
        this.adminParcelService.changeDestination(this.parcel, destination).thenAccept(result -> {
            this.notifyResult(player, result);
            this.scheduler.run(() -> this.reopenFresh(player));
        }).exceptionally(FutureHandler::handleException);
    }

    private void notifyResult(Player player, EditResult result) {
        switch (result.status()) {
            case OK -> this.noticeService.create().notice(m -> m.admin.parcelUpdated).player(player.getUniqueId()).send();
            case SIZE_TOO_SMALL -> this.noticeService.create().notice(m -> m.admin.sizeTooSmall).player(player.getUniqueId()).send();
            case DESTINATION_FULL -> this.noticeService.create().notice(m -> m.admin.destinationFull).player(player.getUniqueId()).send();
        }
    }

    /** Re-fetches the parcel so the editor reflects the just-applied change, then reopens. */
    private void reopenFresh(Player player) {
        this.guiManager.getParcel(this.parcel.uuid()).thenAccept(optional -> {
            if (optional.isEmpty()) {
                this.scheduler.run(() -> this.parent.show(player));
                return;
            }
            this.scheduler.run(() -> new AdminParcelEditGui(this.scheduler, this.miniMessage, this.guiSettings,
                this.messageConfig, this.noticeService, this.guiManager, this.adminParcelService, this.parent, optional.get()).show(player));
        }).exceptionally(FutureHandler::handleException);
    }

    private void openTextDialog(Player player, String title, String placeholder, Consumer<String> onConfirm) {
        Dialog dialog = Dialog.create(builder -> builder.empty()
            .base(DialogBase.builder(this.miniMessage.deserialize(title))
                .inputs(List.of(DialogInput.text("value", this.miniMessage.deserialize(placeholder)).build()))
                .build())
            .type(DialogType.confirmation(
                ActionButton.create(this.miniMessage.deserialize("<dark_green>Confirm"), null, 200,
                    DialogAction.customClick((DialogResponseView view, Audience audience) -> onConfirm.accept(view.getText("value")),
                        ClickCallback.Options.builder().uses(1).lifetime(ClickCallback.DEFAULT_LIFETIME).build())),
                ActionButton.create(this.miniMessage.deserialize("<dark_red>Cancel"), null, 200,
                    DialogAction.customClick((DialogResponseView view, Audience audience) -> this.scheduler.run(() -> this.show(player)),
                        ClickCallback.Options.builder().uses(1).lifetime(ClickCallback.DEFAULT_LIFETIME).build())))));
        player.showDialog(dialog);
    }

    private static ParcelSize nextSize(ParcelSize size) {
        return switch (size) {
            case SMALL -> ParcelSize.MEDIUM;
            case MEDIUM -> ParcelSize.LARGE;
            case LARGE -> ParcelSize.SMALL;
        };
    }

    private static ParcelStatus nextStatus(ParcelStatus status) {
        return status == ParcelStatus.SENT ? ParcelStatus.DELIVERED : ParcelStatus.SENT;
    }
}
