package com.eternalcode.parcellockers.gui.implementation.remote;

import static com.eternalcode.commons.adventure.AdventureUtil.resetItalic;

import com.eternalcode.commons.scheduler.Scheduler;
import com.eternalcode.parcellockers.configuration.implementation.PluginConfig.GuiSettings;
import com.eternalcode.parcellockers.gui.GuiManager;
import com.eternalcode.parcellockers.gui.GuiView;
import com.eternalcode.parcellockers.parcel.repository.ParcelRepository;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;

@SuppressWarnings("ClassCanBeRecord")
public class MainGui implements GuiView {

    private final Scheduler scheduler;
    private final MiniMessage miniMessage;
    private final GuiSettings guiSettings;
    private final GuiManager guiManager;
    private final ParcelRepository parcelRepository;

    public MainGui(
        Scheduler scheduler,
        MiniMessage miniMessage,
        GuiSettings guiSettings, GuiManager guiManager,
        ParcelRepository parcelRepository
    ) {
        this.scheduler = scheduler;
        this.miniMessage = miniMessage;
        this.guiSettings = guiSettings;
        this.guiManager = guiManager;
        this.parcelRepository = parcelRepository;
    }

    @Override
    public void show(Player player) {

        Gui gui = Gui.gui()
            .title(resetItalic(this.miniMessage.deserialize(guiSettings.mainGuiTitle)))
            .rows(6)
            .disableAllInteractions()
            .create();

        GuiItem backgroundItem = this.guiSettings.mainGuiBackgroundItem.toGuiItem();
        GuiItem myParcelsItem = guiSettings.myParcelsItem.toGuiItem();
        GuiItem sentParcelsItem = guiSettings.sentParcelsItem.toGuiItem();
        GuiItem parcelArchiveItem = guiSettings.parcelArchiveItem.toGuiItem();
        GuiItem closeItem = guiSettings.closeItem.toGuiItem(event -> gui.close(player));
        GuiItem cornerItem = guiSettings.cornerItem.toGuiItem();

        int size = gui.getRows() * 9;
        for (int i = 0; i < size; i++) {
            gui.setItem(i, backgroundItem);
        }

        for (int slot : CORNER_SLOTS) {
            gui.setItem(slot, cornerItem);
        }

        gui.setItem(20, myParcelsItem);
        gui.setItem(22, sentParcelsItem);
        gui.setItem(24, parcelArchiveItem);
        gui.setItem(40, closeItem);

        gui.addSlotAction(20, event -> new ParcelListGui(this.scheduler, this.miniMessage, this.guiSettings, this.parcelRepository, this.guiManager, this).show(player));
        gui.addSlotAction(22, event -> new SentParcelsGui(this.scheduler, this.miniMessage, this.guiSettings, this, this.guiManager).show(player));
        gui.setDefaultClickAction(event -> event.setCancelled(true));
        gui.open(player);
    }
}
