package com.eternalcode.parcellockers.configuration.implementation;

import com.eternalcode.multification.notice.Notice;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import eu.okaeri.configs.annotation.Header;
import org.bukkit.Sound;

@SuppressWarnings("ALL")
@Header({
    "# This file contains messages used by the ParcelLockers plugin.",
    "# You can customize these messages to fit your server's theme or language.",
    "# You may use legacy color codes with '&' (e.g., &a for green, &c for red)",
    "# MiniMessage formatting is also supported for advanced text styling."
})
public class MessageConfig extends OkaeriConfig {

    @Comment({"", "# Messages related to general commands can be configured here." })
    public Notice playerOnlyCommand = Notice.chat("&4✘ &cThis command is only available to players!");
    public Notice noPermission = Notice.chat("&4✘ &cYou don't have permission to perform this command! &6(&c{PERMISSION}&6)");
    public Notice playerNotFound = Notice.chat("&4✘ &cThe specified player could not be found!");
    public Notice invalidUsage = Notice.builder()
        .chat("&4» &cCorrect usage: &6{USAGE}")
        .sound(Sound.BLOCK_NOTE_BLOCK_PLING.key())
        .build();

    public Notice reload = Notice.chat("&3❣ &bConfiguration has been successfully reloaded!");

    @Comment({"", "# Messages related to parcels can be configured here." })
    @Comment("# These messages are used when sending, collecting, or managing parcels.")
    public ParcelMessages parcel = new ParcelMessages();

    @Comment({"", "# Messages related to parcel lockers can be configured here." })
    @Comment("# These messages are used when creating, deleting, or managing parcel lockers.")
    public LockerMessages locker = new LockerMessages();

    @Comment({"", "# Messages related to admin commands can be configured here." })
    @Comment("# These messages are used for administrative actions such as deleting all lockers or parcels.")
    public AdminMessages admin = new AdminMessages();

    public static class ParcelMessages extends OkaeriConfig {
        public Notice sent = Notice.builder()
            .chat("&2✔ &aParcel sent successfully.")
            .sound(Sound.ENTITY_ITEM_PICKUP.key())
            .build();

        public Notice cannotSend = Notice.builder()
            .chat("&4✘ &cAn error occurred while sending the parcel. Check the console for more information.")
            .sound(Sound.ENTITY_VILLAGER_NO.key())
            .build();
        public Notice emptyName = Notice.builder()
            .chat("&4✘ &cThe parcel name cannot be empty!")
            .sound(Sound.ENTITY_VILLAGER_NO.key())
            .build();
        public Notice nameSet = Notice.builder()
            .chat("&2✔ &aParcel name set successfully.")
            .sound(Sound.ENTITY_EXPERIENCE_ORB_PICKUP.key())
            .build();
        public Notice empty = Notice.builder()
            .chat("&4✘ &cThe parcel cannot be empty!")
            .sound(Sound.ENTITY_ENDERMAN_AMBIENT.key())
            .build();
        public Notice receiverSet = Notice.builder()
            .chat("&2✔ &aParcel receiver set successfully.")
            .sound(Sound.ENTITY_EXPERIENCE_ORB_PICKUP.key())
            .build();
        public Notice descriptionSet = Notice.builder()
            .chat("&2✔ &aParcel name set successfully.")
            .sound(Sound.ENTITY_EXPERIENCE_ORB_PICKUP.key())
            .build();
        public Notice destinationSet = Notice.builder()
            .chat("&2✔ &aParcel destination locker set successfully.")
            .sound(Sound.ENTITY_EXPERIENCE_ORB_PICKUP.key())
            .build();
        public Notice cannotCollect = Notice.builder()
            .chat("&4✘ &cAn error occurred while collecting the parcel.")
            .sound(Sound.ENTITY_VILLAGER_NO.key())
            .build();
        public Notice noInventorySpace = Notice.builder()
            .chat("&4✘ &cYou don't have enough space in your inventory to collect the parcel!")
            .sound(Sound.ENTITY_VILLAGER_NO.key())
            .build();
        public Notice collected = Notice.builder()
            .chat("&2✔ &aParcel collected successfully.")
            .sound(Sound.ENTITY_PLAYER_LEVELUP.key())
            .build();
        public Notice nameNotSet = Notice.builder()
            .chat("&4✘ &cThe parcel name is not set!")
            .sound(Sound.ENTITY_ENDERMAN_AMBIENT.key())
            .build();
        public Notice receiverNotSet = Notice.builder()
            .chat("&4✘ &cThe parcel receiver is not set!")
            .sound(Sound.ENTITY_ENDERMAN_AMBIENT.key())
            .build();
        public Notice destinationNotSet = Notice.builder()
            .chat("&4✘ &cThe parcel destination locker is not set!")
            .sound(Sound.ENTITY_ENDERMAN_AMBIENT.key())
            .build();
        public Notice illegalItem = Notice.builder()
            .chat("&4✘ &cThe parcel contains illegal items that cannot be sent. ({ITEMS})")
            .build();
        public Notice cannotDelete = Notice.builder()
            .chat("&4✘ &cAn error occurred while deleting the parcel.")
            .sound(Sound.ENTITY_VILLAGER_NO.key())
            .build();
        public Notice deleted = Notice.builder()
            .chat("&2✔ &aParcel deleted successfully.")
            .sound(Sound.ENTITY_ITEM_BREAK.key())
            .build();

        @Comment({"", "# The parcel info message." })
        public Notice parcelInfoMessages = Notice.builder()
            .chat(
                "&7» &6Parcel info:",
                "&f• &6UUID: &e{UUID}",
                "&f• &6Sender: &e{SENDER}",
                "&f• &6Receiver: &e{RECEIVER}",
                "&f• &6Size: &e{SIZE}",
                "&f• &6Position: &6X: &e{POSITION_X}, &6Y: &e{POSITION_Y}, &6Z: &e{POSITION_Z}",
                "&f• &6Priority: &e{PRIORITY}",
                "&f• &6Description: &e{DESCRIPTION}"
            )
            .sound(Sound.BLOCK_NOTE_BLOCK_CHIME.key())
            .build();
    }

    public static class LockerMessages extends OkaeriConfig {
        public Notice cannotCreate = Notice.builder()
            .chat("&4✘ &cCould not create the parcel locker.")
            .sound(Sound.ENTITY_VILLAGER_NO.key())
            .build();
        public Notice created = Notice.builder()
            .chat("&2✔ &aParcel locker created successfully.")
            .sound(Sound.BLOCK_ANVIL_USE.key())
            .build();
        public Notice descriptionPrompt = Notice.builder()
            .chat("&6\uD83D\uDDC8 &eEnter a name for the parcel locker:")
            .sound(Sound.BLOCK_NOTE_BLOCK_PLING.key())
            .build();
        public Notice cannotBreak = Notice.builder()
            .chat("&4✘ &cYou have no permission to break the parcel locker.")
            .sound(Sound.ENTITY_VILLAGER_NO.key())
            .build();
        public Notice deleted = Notice.builder()
            .chat("&2✔ &aParcel locker deleted successfully.")
            .sound(Sound.BLOCK_ANVIL_BREAK.key())
            .build();
        public Notice broadcastRemoved = Notice.chat("&4❣ &cThe parcel locker at &4{X} {Y} {Z} &cin &4{WORLD} &chas been removed by &4{PLAYER}!");
        public Notice alreadyCreating = Notice.builder()
            .chat("&4✘ &cYou are already creating a parcel locker!")
            .sound(Sound.ENTITY_VILLAGER_NO.key())
            .build();
    }

    public static class AdminMessages extends OkaeriConfig {
        public Notice deletedLockers = Notice.chat("&4⚠ &cAll ({COUNT}) parcel lockers have been deleted!");
        public Notice deletedParcels = Notice.chat("&4⚠ &cAll ({COUNT}) parcels have been deleted!");
        public Notice deletedItemStorages = Notice.chat("&4⚠ &cAll ({COUNT}) item storages have been deleted!");
        public Notice deletedContents = Notice.chat("&4⚠ &cAll ({COUNT}) parcel contents have been deleted!");
        public Notice deletedDeliveries = Notice.chat("&4⚠ &cAll ({COUNT}) deliveries have been deleted!");
        public Notice deletedUsers = Notice.chat("&4⚠ &cAll ({COUNT}) users have been deleted!");
        public Notice invalidNumberOfStacks = Notice.chat("&4✘ &cInvalid number of stacks. Must be between 1 and 36.");
        public Notice invalidItemMaterials = Notice.chat("&4✘ &cItem materials to give are invalid (should never happen, if it does report this on our discord).");
    }
}
