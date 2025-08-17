package com.eternalcode.parcellockers.parcel;

import java.util.List;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public interface ParcelService {

    void collect(Player player, Parcel parcel);

    void remove(CommandSender sender, Parcel parcel);

    boolean send(Player sender, Parcel parcel, List<ItemStack> items);
}
