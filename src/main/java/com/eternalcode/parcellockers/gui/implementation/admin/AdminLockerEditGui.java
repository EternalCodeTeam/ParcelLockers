package com.eternalcode.parcellockers.gui.implementation.admin;

import static com.eternalcode.commons.adventure.AdventureUtil.resetItalic;

import com.eternalcode.commons.concurrent.FutureHandler;
import com.eternalcode.commons.scheduler.Scheduler;
import com.eternalcode.parcellockers.configuration.implementation.MessageConfig;
import com.eternalcode.parcellockers.configuration.implementation.PluginConfig.GuiSettings;
import com.eternalcode.parcellockers.configuration.serializable.ConfigItem;
import com.eternalcode.parcellockers.gui.GuiManager;
import com.eternalcode.parcellockers.gui.GuiView;
import com.eternalcode.parcellockers.locker.Locker;
import com.eternalcode.parcellockers.notification.NoticeService;
import com.eternalcode.parcellockers.shared.Position;
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
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

@SuppressWarnings("UnstableApiUsage")
public class AdminLockerEditGui implements GuiView {

    private static final int RENAME_SLOT = 20;
    private static final int TELEPORT_SLOT = 22;
    private static final int DELETE_SLOT = 24;
    private static final int CLOSE_SLOT = 49;

    private final Scheduler scheduler;
    private final MiniMessage miniMessage;
    private final GuiSettings guiSettings;
    private final MessageConfig messageConfig;
    private final NoticeService noticeService;
    private final GuiManager guiManager;
    private final AdminLockerListGui parent;
    private final Locker locker;
    private final ConfirmationDialogFactory confirmationDialogFactory;

    public AdminLockerEditGui(Scheduler scheduler, MiniMessage miniMessage, GuiSettings guiSettings,
            MessageConfig messageConfig, NoticeService noticeService, GuiManager guiManager,
            AdminLockerListGui parent, Locker locker) {
        this.scheduler = scheduler;
        this.miniMessage = miniMessage;
        this.guiSettings = guiSettings;
        this.messageConfig = messageConfig;
        this.noticeService = noticeService;
        this.guiManager = guiManager;
        this.parent = parent;
        this.locker = locker;
        this.confirmationDialogFactory = new ConfirmationDialogFactory(miniMessage);
    }

    @Override
    public void show(Player player) {
        Gui gui = Gui.gui()
            .title(resetItalic(this.miniMessage.deserialize(this.guiSettings.adminLockerEditGuiTitle)))
            .rows(6)
            .disableAllInteractions()
            .create();

        GuiItem background = this.guiSettings.mainGuiBackgroundItem.toGuiItem();
        for (int i = 0; i < gui.getRows() * 9; i++) {
            gui.setItem(i, background);
        }

        ConfigItem renameItem = this.guiSettings.adminRenameLockerButton.clone();
        renameItem.name(this.guiSettings.adminRenameLockerButton.name().replace("{NAME}", this.locker.name()));
        gui.setItem(RENAME_SLOT, renameItem.toGuiItem(event -> this.openRenameDialog(player)));

        gui.setItem(TELEPORT_SLOT, this.guiSettings.adminTeleportLockerButton.toGuiItem(event -> this.teleport(player, gui)));

        gui.setItem(DELETE_SLOT, this.guiSettings.adminDeleteLockerButton.toGuiItem(event ->
            player.showDialog(this.confirmationDialogFactory.create(
                "<red>Delete locker '" + this.locker.name() + "'?",
                "<dark_red>Delete", "<gray>Cancel",
                () -> this.guiManager.deleteLocker(this.locker.uuid(), player.getUniqueId()).thenRun(() -> {
                    this.noticeService.create().notice(m -> m.admin.lockerDeleted).player(player.getUniqueId()).send();
                    this.scheduler.run(() -> this.parent.show(player));
                }).exceptionally(FutureHandler::handleException),
                () -> this.scheduler.run(() -> this.show(player))))));

        gui.setItem(CLOSE_SLOT, this.guiSettings.closeItem.toGuiItem(event -> this.parent.show(player)));
        gui.setDefaultClickAction(event -> event.setCancelled(true));
        this.scheduler.run(() -> gui.open(player));
    }

    private void teleport(Player player, Gui gui) {
        Position position = this.locker.position();
        World world = Bukkit.getWorld(position.world());
        if (world == null) {
            this.noticeService.create().notice(m -> m.admin.teleportWorldMissing).player(player.getUniqueId()).send();
            return;
        }
        gui.close(player);
        this.scheduler.run(() -> {
            player.teleport(new Location(world, position.x() + 0.5, position.y(), position.z() + 0.5));
            this.noticeService.create().notice(m -> m.admin.teleported).player(player.getUniqueId()).send();
        });
    }

    private void openRenameDialog(Player player) {
        Dialog dialog = Dialog.create(builder -> builder.empty()
            .base(DialogBase.builder(this.miniMessage.deserialize("<yellow>Enter new locker name:"))
                .inputs(List.of(DialogInput.text("name", this.miniMessage.deserialize("<gray>Locker name")).build()))
                .build())
            .type(DialogType.confirmation(
                ActionButton.create(this.miniMessage.deserialize("<dark_green>Confirm"), null, 200,
                    DialogAction.customClick((DialogResponseView view, Audience audience) -> {
                        String name = view.getText("name");
                        if (name == null || name.isBlank()) {
                            this.scheduler.run(() -> this.show(player));
                            return;
                        }
                        this.guiManager.renameLocker(this.locker.uuid(), name).thenAccept(renamed -> {
                            this.noticeService.create().notice(m -> m.admin.lockerRenamed).player(player.getUniqueId()).send();
                            this.scheduler.run(() ->
                                new AdminLockerEditGui(this.scheduler, this.miniMessage, this.guiSettings,
                                    this.messageConfig, this.noticeService, this.guiManager, this.parent, renamed).show(player));
                        }).exceptionally(FutureHandler::handleException);
                    }, ClickCallback.Options.builder().uses(1).lifetime(ClickCallback.DEFAULT_LIFETIME).build())),
                ActionButton.create(this.miniMessage.deserialize("<dark_red>Cancel"), null, 200,
                    DialogAction.customClick((DialogResponseView view, Audience audience) ->
                        this.scheduler.run(() -> this.show(player)),
                        ClickCallback.Options.builder().uses(1).lifetime(ClickCallback.DEFAULT_LIFETIME).build())))));
        player.showDialog(dialog);
    }
}
