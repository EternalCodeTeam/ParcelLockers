package com.eternalcode.parcellockers.gui;

import org.bukkit.entity.Player;


// TODO INTERFACE
public abstract class GuiView {

    public static final int[] CORNER_SLOTS = { 0, 8, 45, 53 };
    public static final int[] BORDER_SLOTS = { 1, 2, 3, 4, 5, 6, 7, 9, 17, 18, 26, 27, 35, 36, 44, 46, 47, 48, 49, 50, 51, 52 };

    public abstract void show(Player player);
}
