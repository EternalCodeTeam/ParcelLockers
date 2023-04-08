package com.eternalcode.parcellockers.parcel;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

public class ParcelBuilder {

    private ParcelBuilder() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static Parcel of(UUID uuid, String senderName, ParcelLocker entryLocker, ParcelLocker destinationLocker, ParcelSize size, boolean priority, String receiver) {
        Player sender = Bukkit.getServer().getPlayer(senderName);

        if (sender == null) {
            return null;
        }

        UUID senderUUID = sender.getUniqueId();
        Player recipient = Bukkit.getServer().getPlayer(receiver);

        if (recipient == null) {
            return null;
        }

        UUID recipientUUID = recipient.getUniqueId();

        return new Parcel(uuid, senderUUID, new ParcelMeta("Parcel", "", priority, recipientUUID, size, entryLocker, destinationLocker));
    }

    public static Parcel of(UUID uuid, String senderName, ParcelLocker entryLocker, ParcelLocker destinationLocker, ParcelSize size, boolean priority, String receiver, String name) {

        Player sender = Bukkit.getServer().getPlayer(senderName);

        if (sender == null) {
            return null;
        }

        UUID senderUUID = sender.getUniqueId();
        Player recipient = Bukkit.getServer().getPlayer(receiver);

        if (recipient == null) {
            return null;
        }

        UUID recipientUUID = recipient.getUniqueId();

        return new Parcel(uuid, senderUUID, new ParcelMeta(name, "", priority, recipientUUID, size, entryLocker, destinationLocker));
    }

    public static Parcel of(UUID uuid, String senderName, ParcelLocker entryLocker, ParcelLocker destinationLocker, ParcelSize size, boolean priority, String receiver, String name, String description) {

        Player sender = Bukkit.getServer().getPlayer(senderName);

        if (sender == null) {
            return null;
        }

        UUID senderUUID = sender.getUniqueId();
        Player recipient = Bukkit.getServer().getPlayer(receiver);

        if (recipient == null) {
            return null;
        }

        UUID recipientUUID = recipient.getUniqueId();

        return new Parcel(uuid, senderUUID, new ParcelMeta(name, description, priority, recipientUUID, size, entryLocker, destinationLocker));
    }
}
