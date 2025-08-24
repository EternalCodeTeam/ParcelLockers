package com.eternalcode.parcellockers.gui;

import com.eternalcode.parcellockers.configuration.implementation.PluginConfig;
import com.eternalcode.parcellockers.shared.Page;
import com.eternalcode.parcellockers.shared.PageResult;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.PaginatedGui;
import org.bukkit.entity.Player;


public interface GuiView {
    int[] CORNER_SLOTS = { 0, 8, 45, 53 };
    int[] BORDER_SLOTS = { 1, 2, 3, 4, 5, 6, 7, 9, 17, 18, 26, 27, 35, 36, 44, 46, 47, 48, 49, 50, 51, 52 };

    default void show(Player player) {
        throw new UnsupportedOperationException("This GUI requires additional context");
    }

    default void show(Player player, Page page) {
        throw new UnsupportedOperationException("This GUI requires additional context");
    }

    default <T> void setupNavigation(PaginatedGui gui, Page page, PageResult<T> result, Player player, PluginConfig.GuiSettings guiSettings) {
        if (result.hasNextPage()) {
            GuiItem nextPageItem = guiSettings.nextPageItem.toGuiItem(event -> this.show(player, page.next()));
            gui.setItem(51, nextPageItem);
        }

        if (page.hasPrevious()) {
            GuiItem previousPageItem = guiSettings.previousPageItem.toGuiItem(event -> this.show(player, page.previous()));
            gui.setItem(47, previousPageItem);
        }
    }
}
