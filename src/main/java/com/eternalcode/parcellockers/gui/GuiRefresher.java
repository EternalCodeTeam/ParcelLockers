package com.eternalcode.parcellockers.gui;

import dev.triumphteam.gui.guis.BaseGui;
import dev.triumphteam.gui.guis.GuiItem;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class GuiRefresher<T extends BaseGui> {

    private final T gui;
    private final List<Supplier<GuiItem>> items = new ArrayList<>();

    public GuiRefresher(T gui) {
        this.gui = gui;
    }

    public void addItem(Supplier<GuiItem> item) {
        this.items.add(item);
        this.gui.addItem(item.get());
    }

    public void refresh() {
        for (GuiItem item : this.gui.getGuiItems().values()) {
            this.gui.removeItem(item);
        }

        for (Supplier<GuiItem> item : this.items) {
            this.gui.addItem(item.get());
        }

        this.gui.update();
    }

}
