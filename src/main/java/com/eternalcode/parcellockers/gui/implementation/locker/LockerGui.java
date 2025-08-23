package com.eternalcode.parcellockers.gui.implementation.locker;

import static com.eternalcode.commons.adventure.AdventureUtil.resetItalic;

import com.eternalcode.commons.scheduler.Scheduler;
import com.eternalcode.parcellockers.configuration.implementation.PluginConfig.GuiSettings;
import com.eternalcode.parcellockers.gui.GuiManager;
import com.eternalcode.parcellockers.gui.GuiView;
import com.eternalcode.parcellockers.notification.NoticeService;
import dev.rollczi.liteskullapi.SkullAPI;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;

public class LockerGui implements GuiView {

    private final MiniMessage miniMessage;
    private final Scheduler scheduler;
    private final GuiSettings guiSettings;
    private final GuiManager guiManager;
    private final NoticeService noticeService;
    private final SkullAPI skullAPI;

    public LockerGui(
        MiniMessage miniMessage, Scheduler scheduler,
        GuiSettings guiSettings, GuiManager guiManager,
        NoticeService noticeService,
        SkullAPI skullAPI
    ) {
        this.miniMessage = miniMessage;
        this.scheduler = scheduler;
        this.guiSettings = guiSettings;
        this.guiManager = guiManager;
        this.noticeService = noticeService;
        this.skullAPI = skullAPI;
    }

    public void show(Player player, UUID entryLocker) {
        Component guiTitle = this.miniMessage.deserialize(this.guiSettings.mainGuiTitle);

        Gui gui = Gui.gui()
            .title(resetItalic(guiTitle))
            .rows(6)
            .disableAllInteractions()
            .create();

        GuiItem backgroundItem = this.guiSettings.mainGuiBackgroundItem.toGuiItem();
        GuiItem cornerItem = this.guiSettings.cornerItem.toGuiItem();
        GuiItem closeItem = this.guiSettings.closeItem.toGuiItem(event -> gui.close(player));

        int size = gui.getRows() * 9;
        for (int i = 0; i < size; i++) {
            gui.setItem(i, backgroundItem);
        }

        for (int slot : CORNER_SLOTS) {
            gui.setItem(slot, cornerItem);
        }

        CollectionGui collectionGui = new CollectionGui(
            this.guiSettings,
            this.scheduler,
            this.guiManager,
            this.miniMessage
        );

        gui.setItem(21, this.guiSettings.parcelLockerCollectItem.toGuiItem(event -> collectionGui.show(player)));
        gui.setItem(23, this.guiSettings.parcelLockerSendItem.toGuiItem(event -> new SendingGui(
            this.scheduler,
            this.guiSettings,
            this.miniMessage,
            this.noticeService,
            this.guiManager,
            this.skullAPI,
            new SendingGuiState().entryLocker(entryLocker)
        ).show(player, entryLocker)));

        gui.setItem(49, closeItem);
        gui.open(player);
    }
}
