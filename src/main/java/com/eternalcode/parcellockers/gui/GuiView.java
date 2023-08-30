package com.eternalcode.parcellockers.gui;

import org.bukkit.entity.Player;

public abstract class GuiView {

    protected static final int[] CORNER_SLOTS = {0, 8, 45, 53};
    protected static final int[] BORDER_SLOTS = {1, 2, 3, 4, 5, 6, 7, 9, 17, 18, 26, 27, 35, 36, 44, 46, 47, 48, 49, 50, 51, 52};

    abstract void show(Player player);
}
