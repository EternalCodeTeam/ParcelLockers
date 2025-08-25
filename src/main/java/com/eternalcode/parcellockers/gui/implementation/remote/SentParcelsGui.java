package com.eternalcode.parcellockers.gui.implementation.remote;

import com.eternalcode.commons.adventure.AdventureUtil;
import com.eternalcode.commons.scheduler.Scheduler;
import com.eternalcode.parcellockers.configuration.implementation.PluginConfig.GuiSettings;
import com.eternalcode.parcellockers.configuration.serializable.ConfigItem;
import com.eternalcode.parcellockers.gui.GuiManager;
import com.eternalcode.parcellockers.gui.GuiView;
import com.eternalcode.parcellockers.parcel.Parcel;
import com.eternalcode.parcellockers.parcel.util.PlaceholderUtil;
import com.eternalcode.parcellockers.shared.Page;
import dev.triumphteam.gui.builder.item.PaperItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.PaginatedGui;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;

public class SentParcelsGui implements GuiView {

    private static final int WIDTH = 7;
    private static final int HEIGHT = 4;
    private static final Page FIRST_PAGE = new Page(0, WIDTH * HEIGHT);

    private final Scheduler scheduler;
    private final MiniMessage miniMessage;
    private final GuiSettings guiSettings;
    private final MainGui mainGUI;
    private final GuiManager guiManager;

    public SentParcelsGui(
        Scheduler scheduler,
        MiniMessage miniMessage,
        GuiSettings guiSettings,
        MainGui mainGUI,
        GuiManager guiManager
    ) {
        this.scheduler = scheduler;
        this.miniMessage = miniMessage;
        this.guiSettings = guiSettings;
        this.mainGUI = mainGUI;
        this.guiManager = guiManager;
    }

    public void show(Player player) {
        this.show(player, FIRST_PAGE);
    }

    @Override
    public void show(Player player, Page page) {
        PaginatedGui gui = Gui.paginated()
            .title(this.miniMessage.deserialize(this.guiSettings.sentParcelsTitle))
            .rows(6)
            .disableAllInteractions()
            .create();

        ConfigItem parcelItem = this.guiSettings.parcelItem;
        this.setupStaticItems(player, gui);

        this.guiManager.getParcelsBySender(player.getUniqueId(), page).thenAccept(result -> {
            List<Parcel> parcels = result.items();

            for (Parcel parcel : parcels) {
                PaperItemBuilder item = parcelItem.toBuilder();

                List<Component> newLore = PlaceholderUtil.replaceParcelPlaceholders(parcel, parcelItem.lore(), this.guiManager)
                    .stream()
                    .map(line -> AdventureUtil.resetItalic(this.miniMessage.deserialize(line)))
                    .toList();
                item.lore(newLore);
                item.name(AdventureUtil.resetItalic(this.miniMessage.deserialize(parcelItem.name().replace("{NAME}", parcel.name()))));

                gui.addItem(item.asGuiItem());
            }
            this.scheduler.run(() -> gui.open(player));
        });
    }

    private void setupStaticItems(Player player, PaginatedGui gui) {
        GuiItem cornerItem = this.guiSettings.cornerItem.toGuiItem();
        GuiItem backgroundItem = this.guiSettings.mainGuiBackgroundItem.toGuiItem();
        GuiItem closeItem = this.guiSettings.closeItem.toGuiItem(event -> this.mainGUI.show(player));

        for (int slot : CORNER_SLOTS) {
            gui.setItem(slot, cornerItem);
        }

        for (int slot : BORDER_SLOTS) {
            gui.setItem(slot, backgroundItem);
        }

        gui.setItem(49, closeItem);
    }
}
