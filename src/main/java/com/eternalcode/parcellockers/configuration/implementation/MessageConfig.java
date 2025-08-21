package com.eternalcode.parcellockers.configuration.implementation;

import com.eternalcode.multification.notice.Notice;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;

public class MessageConfig extends OkaeriConfig {

    public Notice playerOnlyCommand = Notice.chat("&4✘ &cThis command is only available to players!");
    public Notice noPermission = Notice.chat("&4✘ &cYou don't have permission to perform this command! &6(&c{PERMISSION}&6)");
    public Notice playerNotFound = Notice.chat("&4✘ &cThe specified player could not be found!");
    public Notice invalidUsage = Notice.chat("&4❣ &cCorrect usage: &6{USAGE}");
    public Notice reload = Notice.chat("&3❣ &bConfiguration has been successfully reloaded!");
    public Notice parcelSuccessfullyDeleted = Notice.chat("&2✔ &aParcel deleted successfully.");
    public Notice failedToDeleteParcel = Notice.chat("&4✘ &cAn error occurred while deleting the parcel.");
    public Notice failedToCreateParcelLocker = Notice.chat("&4✘ &cCould not create the parcel locker.");
    public Notice parcelLockerSuccessfullyCreated = Notice.chat("&7» &aParcel locker created successfully.");
    public Notice enterDescriptionPrompt = Notice.chat("&6↵ &eEnter a description for the parcel locker:");
    public Notice cannotBreakParcelLocker = Notice.chat("&4✘ &cYou have no permission to break the parcel locker.");
    public Notice parcelLockerSuccessfullyDeleted = Notice.chat("&2✔ &aParcel locker deleted successfully.");
    public Notice broadcastParcelLockerRemoved = Notice.chat("&4❣ &cThe parcel locker at &4{X} {Y} {Z} &cin &4{WORLD} &chas been removed by &4{PLAYER}!");
    public Notice parcelSent = Notice.chat("&2✔ &aParcel sent successfully.");
    public Notice parcelFailedToSend = Notice.chat("&4✘ &cAn error occurred while sending the parcel. Check the console for more information.");
    public Notice illegalItemFailedToSend = Notice.chat("&4✘ &cThe parcel contains illegal items that cannot be sent. ({ITEMS})");
    public Notice parcelCannotBeEmpty = Notice.chat("&4✘ &cThe parcel cannot be empty!");
    public Notice parcelNameCannotBeEmpty = Notice.chat("&4✘ &cThe parcel name cannot be empty!");
    public Notice parcelNameSet = Notice.chat("&2✔ &aParcel name set successfully.");
    public Notice parcelDescriptionSet = Notice.chat("&2✔ &aParcel description set successfully.");
    public Notice parcelReceiverSet = Notice.chat("&2✔ &aParcel receiver set successfully.");
    public Notice parcelDestinationSet = Notice.chat("&2✔ &aParcel destination locker set successfully.");
    public Notice alreadyCreatingLocker = Notice.chat("&4✘ &cYou are already creating a parcel locker!");
    public Notice receiverNotSet = Notice.chat("&4✘ &cThe parcel receiver is not set!");
    public Notice parcelSuccessfullyCollected = Notice.chat("&2✔ &aParcel collected successfully.");
    public Notice failedToCollectParcel = Notice.chat("&4✘ &cAn error occurred while collecting the parcel.");
    public Notice notEnoughInventorySpace = Notice.chat("&4✘ &cYou don't have enough space in your inventory to collect the parcel!");

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
