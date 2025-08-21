package com.eternalcode.parcellockers.configuration.implementation;

import com.eternalcode.multification.notice.Notice;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import eu.okaeri.configs.annotation.Header;

@Header({
    " ",
    "# This file contains messages used by the ParcelLockers plugin.",
    "# You can customize these messages to fit your server's theme or language.",
    " ",
})
public class MessageConfig extends OkaeriConfig {

    @Comment({" ", "# Messages related to general commands can be configured here." })
    public Notice playerOnlyCommand = Notice.chat("&4✘ &cThis command is only available to players!");
    public Notice noPermission = Notice.chat("&4✘ &cYou don't have permission to perform this command! &6(&c{PERMISSION}&6)");
    public Notice playerNotFound = Notice.chat("&4✘ &cThe specified player could not be found!");
    public Notice invalidUsage = Notice.chat("&4❣ &cCorrect usage: &6{USAGE}");
    public Notice reload = Notice.chat("&3❣ &bConfiguration has been successfully reloaded!");

    @Comment({" ", "# Messages related to parcels can be configured here." })
    @Comment({" ", "# These messages are used when sending, collecting, or managing parcels."})
    public ParcelMessages parcel = new ParcelMessages();

    @Comment({" ", "# Messages related to parcel lockers can be configured here." })
    @Comment({" ", "# These messages are used when creating, deleting, or managing parcel lockers."})
    public LockerMessages locker = new LockerMessages();

    public static class ParcelMessages extends OkaeriConfig {
        public Notice sent = Notice.chat("&2✔ &aParcel sent successfully.");
        public Notice cannotSend = Notice.chat("&4✘ &cAn error occurred while sending the parcel. Check the console for more information.");
        public Notice emptyName = Notice.chat("&4✘ &cThe parcel name cannot be empty!");
        public Notice nameSet = Notice.chat("&2✔ &aParcel name set successfully.");
        public Notice empty = Notice.chat("&4✘ &cThe parcel cannot be empty!");
        public Notice receiverSet = Notice.chat("&2✔ &aParcel receiver set successfully.");
        public Notice descriptionSet = Notice.chat("&2✔ &aParcel description set successfully.");
        public Notice destinationSet = Notice.chat("&2✔ &aParcel destination locker set successfully.");
        public Notice cannotCollect = Notice.chat("&4✘ &cAn error occurred while collecting the parcel.");
        public Notice noInventorySpace = Notice.chat("&4✘ &cYou don't have enough space in your inventory to collect the parcel!");
        public Notice collected = Notice.chat("&2✔ &aParcel collected successfully.");
        public Notice receiverNotSet = Notice.chat("&4✘ &cThe parcel receiver is not set!");
        public Notice illegalItem = Notice.chat("&4✘ &cThe parcel contains illegal items that cannot be sent. ({ITEMS})");
        public Notice cannotDelete = Notice.chat("&4✘ &cAn error occurred while deleting the parcel.");
        public Notice deleted = Notice.chat("&2✔ &aParcel deleted successfully.");

        @Comment({" ", "# The parcel info message." })
        public Notice parcelInfoMessages = Notice.chat(
            "&7» &6Parcel info:",
            "&f• &6UUID: &e{UUID}",
            "&f• &6Sender: &e{SENDER}",
            "&f• &6Receiver: &e{RECEIVER}",
            "&f• &6Size: &e{SIZE}",
            "&f• &6Position: &6X: &e{POSITION_X}, &6Y: &e{POSITION_Y}, &6Z: &e{POSITION_Z}",
            "&f• &6Priority: &e{PRIORITY}",
            "&f• &6Description: &e{DESCRIPTION}"
        );
    }

    public static class LockerMessages extends OkaeriConfig {
        public Notice cannotCreate = Notice.chat("&4✘ &cCould not create the parcel locker.");
        public Notice created = Notice.chat("&7» &aParcel locker created successfully.");
        public Notice descriptionPrompt = Notice.chat("&6↵ &eEnter a description for the parcel locker:");
        public Notice cannotBreak = Notice.chat("&4✘ &cYou have no permission to break the parcel locker.");
        public Notice deleted = Notice.chat("&2✔ &aParcel locker deleted successfully.");
        public Notice broadcastRemoved = Notice.chat("&4❣ &cThe parcel locker at &4{X} {Y} {Z} &cin &4{WORLD} &chas been removed by &4{PLAYER}!");
        public Notice alreadyCreating = Notice.chat("&4✘ &cYou are already creating a parcel locker!");
    }
}
