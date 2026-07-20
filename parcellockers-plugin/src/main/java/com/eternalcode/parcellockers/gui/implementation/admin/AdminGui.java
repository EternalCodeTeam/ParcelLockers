package com.eternalcode.parcellockers.gui.implementation.admin;

import static com.eternalcode.commons.adventure.AdventureUtil.resetItalic;

import com.eternalcode.commons.concurrent.FutureHandler;
import com.eternalcode.commons.scheduler.Scheduler;
import com.eternalcode.parcellockers.configuration.implementation.MessageConfig;
import com.eternalcode.parcellockers.configuration.implementation.PluginConfig.GuiSettings;
import com.eternalcode.parcellockers.gui.GuiManager;
import com.eternalcode.parcellockers.gui.GuiView;
import com.eternalcode.parcellockers.notification.NoticeService;
import com.eternalcode.parcellockers.parcel.service.AdminParcelService;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;

@SuppressWarnings("UnstableApiUsage")
public class AdminGui implements GuiView {

    private static final int PARCELS_SLOT = 20;
    private static final int LOCKERS_SLOT = 22;
    private static final int USERS_SLOT = 24;
    private static final int DELETE_PARCELS_SLOT = 38;
    private static final int DELETE_LOCKERS_SLOT = 42;
    private static final int CLOSE_SLOT = 49;

    private final Scheduler scheduler;
    private final MiniMessage miniMessage;
    private final GuiSettings guiSettings;
    private final MessageConfig messageConfig;
    private final NoticeService noticeService;
    private final GuiManager guiManager;
    private final AdminParcelService adminParcelService;
    private final ConfirmationDialogFactory confirmationDialogFactory;

    public AdminGui(Scheduler scheduler, MiniMessage miniMessage, GuiSettings guiSettings, MessageConfig messageConfig,
            NoticeService noticeService, GuiManager guiManager, AdminParcelService adminParcelService) {
        this.scheduler = scheduler;
        this.miniMessage = miniMessage;
        this.guiSettings = guiSettings;
        this.messageConfig = messageConfig;
        this.noticeService = noticeService;
        this.guiManager = guiManager;
        this.adminParcelService = adminParcelService;
        this.confirmationDialogFactory = new ConfirmationDialogFactory(miniMessage);
    }

    @Override
    public void show(Player player) {
        Gui gui = Gui.gui()
            .title(resetItalic(this.miniMessage.deserialize(this.guiSettings.adminGuiTitle)))
            .rows(6)
            .disableAllInteractions()
            .create();

        GuiItem background = this.guiSettings.mainGuiBackgroundItem.toGuiItem();
        GuiItem corner = this.guiSettings.cornerItem.toGuiItem();
        for (int i = 0; i < gui.getRows() * 9; i++) {
            gui.setItem(i, background);
        }
        for (int slot : CORNER_SLOTS) {
            gui.setItem(slot, corner);
        }

        gui.setItem(PARCELS_SLOT, this.guiSettings.adminParcelsButton.toGuiItem(event ->
            new AdminParcelListGui(this.scheduler, this.miniMessage, this.guiSettings, this.messageConfig,
                this.noticeService, this.guiManager, this.adminParcelService, this).show(player)));

        gui.setItem(LOCKERS_SLOT, this.guiSettings.adminLockersButton.toGuiItem(event ->
            new AdminLockerListGui(this.scheduler, this.miniMessage, this.guiSettings, this.messageConfig,
                this.noticeService, this.guiManager, this).show(player)));

        gui.setItem(USERS_SLOT, this.guiSettings.adminUsersButton.toGuiItem(event ->
            new AdminUserListGui(this.scheduler, this.miniMessage, this.guiSettings, this.guiManager, this).show(player)));

        gui.setItem(DELETE_PARCELS_SLOT, this.guiSettings.adminDeleteAllParcelsButton.toGuiItem(event ->
            player.showDialog(this.confirmationDialogFactory.create(
                "<red>Delete ALL parcels? This cannot be undone.",
                "<dark_red>Delete all", "<gray>Cancel",
                () -> this.guiManager.deleteAllParcels(player, this.noticeService)
                    .thenRun(() -> this.scheduler.run(() -> this.show(player)))
                    .exceptionally(FutureHandler::handleException),
                () -> this.scheduler.run(() -> this.show(player))))));

        gui.setItem(DELETE_LOCKERS_SLOT, this.guiSettings.adminDeleteAllLockersButton.toGuiItem(event ->
            player.showDialog(this.confirmationDialogFactory.create(
                "<red>Delete ALL lockers? This cannot be undone.",
                "<dark_red>Delete all", "<gray>Cancel",
                () -> this.guiManager.deleteAllLockers(player, this.noticeService)
                    .thenRun(() -> this.scheduler.run(() -> this.show(player)))
                    .exceptionally(FutureHandler::handleException),
                () -> this.scheduler.run(() -> this.show(player))))));

        gui.setItem(CLOSE_SLOT, this.guiSettings.closeItem.toGuiItem(event -> gui.close(player)));
        gui.setDefaultClickAction(event -> event.setCancelled(true));
        this.scheduler.run(() -> gui.open(player));
    }
}
