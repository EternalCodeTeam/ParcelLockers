package com.eternalcode.parcellockers.gui;

import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.PaginatedGui;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class GuiRefresher {

    private final PaginatedGui gui;
    private final List<Supplier<GuiItem>> items = new ArrayList<>();

    public GuiRefresher(PaginatedGui gui) {
        this.gui = gui;
    }

    public void addItem(Supplier<GuiItem> item) {
        this.items.add(item);
        this.gui.addItem(item.get());
    }

    public void refresh() {
        this.gui.clearPageItems(false);

        for (Supplier<GuiItem> item : this.items) {
            this.gui.addItem(item.get());
        }

        this.gui.update();
    }

}
