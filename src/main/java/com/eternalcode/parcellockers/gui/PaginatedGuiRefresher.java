package com.eternalcode.parcellockers.gui;

import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.PaginatedGui;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class PaginatedGuiRefresher {

    private final PaginatedGui gui;
    private final List<Supplier<GuiItem>> items = new ArrayList<>();

    public PaginatedGuiRefresher(PaginatedGui gui) {
        this.gui = gui;
    }

    public void addItem(Supplier<GuiItem> item) {
        this.items.add(item);
        this.gui.addItem(item.get());
    }

    /**
     * Deletes item by its absolute slot in the inventory
     */
    public void removeItemBySlot(int pageSlot) {
        Map<Integer, GuiItem> currentPageItems = this.gui.getCurrentPageItems();

        if (!currentPageItems.containsKey(pageSlot)) {
            return;
        }

        List<Integer> sortedSlots = currentPageItems.keySet().stream()
            .sorted()
            .toList();

        int indexInList = sortedSlots.indexOf(pageSlot);

        if (indexInList >= 0 && indexInList < this.items.size()) {
            this.items.remove(indexInList);
            this.refresh();
        }
    }

    public void refresh() {
        this.gui.clearPageItems(false);

        for (Supplier<GuiItem> item : this.items) {
            this.gui.addItem(item.get());
        }

        this.gui.update();
    }
}