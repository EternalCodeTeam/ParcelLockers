package com.eternalcode.parcellockers.gui.implementation.remote;

import static com.eternalcode.commons.adventure.AdventureUtil.resetItalic;

import com.eternalcode.commons.scheduler.Scheduler;
import com.eternalcode.parcellockers.configuration.implementation.PluginConfig.GuiSettings;
import com.eternalcode.parcellockers.configuration.serializable.ConfigItem;
import com.eternalcode.parcellockers.gui.GuiManager;
import com.eternalcode.parcellockers.gui.GuiView;
import com.eternalcode.parcellockers.parcel.Parcel;
import com.eternalcode.parcellockers.parcel.repository.ParcelRepository;
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

@SuppressWarnings("ClassCanBeRecord")
public class ParcelListGui implements GuiView {

    private static final int WIDTH = 7;
    private static final int HEIGHT = 4;
    private static final Page FIRST_PAGE = new Page(0, WIDTH * HEIGHT);
    private final Scheduler scheduler;
    private final MiniMessage miniMessage;
    private final GuiSettings guiSettings;
    private final ParcelRepository parcelRepository;
    private final GuiManager guiManager;
    private final MainGui mainGUI;

    public ParcelListGui(
        Scheduler scheduler,
        MiniMessage miniMessage,
        GuiSettings guiSettings,
        ParcelRepository parcelRepository,
        GuiManager guiManager,
        MainGui mainGUI
    ) {
        this.scheduler = scheduler;
        this.miniMessage = miniMessage;
        this.guiSettings = guiSettings;
        this.parcelRepository = parcelRepository;
        this.guiManager = guiManager;
        this.mainGUI = mainGUI;
    }

    @Override
    public void show(Player player) {
        this.show(player, FIRST_PAGE);
    }

    private void show(Player player, Page page) {
        PaginatedGui gui = Gui.paginated()
            .title(resetItalic(this.miniMessage.deserialize(this.guiSettings.parcelListGuiTitle)))
            .rows(6)
            .disableAllInteractions()
            .create();

        GuiItem backgroundItem = this.guiSettings.mainGuiBackgroundItem.toGuiItem();
        GuiItem cornerItem = this.guiSettings.cornerItem.toGuiItem();
        GuiItem closeItem = this.guiSettings.closeItem.toGuiItem(event -> this.mainGUI.show(player));
        GuiItem previousPageItem = this.guiSettings.previousPageItem.toGuiItem(event -> this.show(player, page.previous()));
        GuiItem nextPageItem = this.guiSettings.nextPageItem.toGuiItem(event -> this.show(player, page.next()));

        for (int slot : CORNER_SLOTS) {
            gui.setItem(slot, cornerItem);
        }
        for (int slot : BORDER_SLOTS) {
            gui.setItem(slot, backgroundItem);
        }

        this.parcelRepository.findByReceiver(player.getUniqueId(), page).thenAccept(result -> {
            if (result.items().isEmpty() && page.hasPrevious()) {
                this.show(player, page.previous());
                return;
            }

            for (Parcel parcel : result.items()) {
                ConfigItem item = this.guiSettings.parcelItem;
                PaperItemBuilder parcelItem = item.toBuilder();

                List<Component> newLore = PlaceholderUtil.replaceParcelPlaceholders(parcel, item.lore(), this.guiManager).stream()
                    .map(line -> resetItalic(this.miniMessage.deserialize(line)))
                    .toList();
                parcelItem.lore(newLore);
                parcelItem.name(this.miniMessage.deserialize(item.name().replace("{NAME}", parcel.name())));

                gui.addItem(parcelItem.asGuiItem());
            }

            gui.setItem(49, closeItem);

            if (result.hasNextPage()) {
                gui.setItem(51, nextPageItem);
            }

            if (page.hasPrevious()) {
                gui.setItem(47, previousPageItem);
            }

            this.scheduler.run(() -> gui.open(player));
        });

    }
}
