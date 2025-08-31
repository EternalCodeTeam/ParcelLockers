package com.eternalcode.parcellockers.gui;

import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.PaginatedGui;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
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
     * Deletes item by its slot in the current page (not absolute slot in the inventory)
     */
    public void removeItemByPageSlot(int pageSlot) {
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

    /**
     * Deletes items matching the given predicate
     */
    public void removeItem(Predicate<GuiItem> predicate) {
        this.items.removeIf(supplier -> predicate.test(supplier.get()));
        this.refresh();
    }

    /**
     * Deletes item at specific index in the items list
     */
    public void removeItemAt(int index) {
        if (index >= 0 && index < this.items.size()) {
            this.items.remove(index);
            this.refresh();
        }
    }

    public void refresh() {
        this.gui.clearPageItems(false);

        // Dodaj wszystkie itemy z powrotem
        for (Supplier<GuiItem> item : this.items) {
            this.gui.addItem(item.get());
        }

        this.gui.update();
    }

    /**
     * Debug: Pokaż informacje o slotach
     */
    public void debugSlots() {
        System.out.println("=== DEBUG SLOTS ===");
        System.out.println("Static items (setItem): " + this.gui.getGuiItems().keySet());
        System.out.println("Current page items (addItem): " + this.gui.getCurrentPageItems().keySet());
        System.out.println("Items list size: " + this.items.size());
        System.out.println("==================");
    }

    public int size() {
        return this.items.size();
    }

    public boolean isEmpty() {
        return this.items.isEmpty();
    }
}