package com.eternalcode.parcellockers.configuration.implementation;

import com.eternalcode.multification.notice.Notice;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import eu.okaeri.configs.annotation.Header;
import io.papermc.paper.registry.keys.SoundEventKeys;

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
        .sound(SoundEventKeys.BLOCK_NOTE_BLOCK_PLING)
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

    @Comment({"", "# Messages related to Discord integration can be configured here." })
    @Comment("# These messages are used for linking Discord accounts with Minecraft accounts.")
    public DiscordMessages discord = new DiscordMessages();

    public static class ParcelMessages extends OkaeriConfig {
        public Notice sent = Notice.builder()
            .chat("&2✔ &aParcel sent successfully.")
            .sound(SoundEventKeys.ENTITY_ITEM_PICKUP)
            .build();
        public Notice cannotSend = Notice.builder()
            .chat("&4✘ &cAn error occurred while sending the parcel. Check the console for more information.")
            .sound(SoundEventKeys.ENTITY_VILLAGER_NO)
            .build();
        public Notice nameCannotBeEmpty = Notice.builder()
            .chat("&4✘ &cThe parcel name cannot be empty!")
            .sound(SoundEventKeys.ENTITY_VILLAGER_NO)
            .build();
        public Notice nameSet = Notice.builder()
            .chat("&2✔ &aParcel name set successfully.")
            .sound(SoundEventKeys.ENTITY_EXPERIENCE_ORB_PICKUP)
            .build();
        public Notice cannotBeEmpty = Notice.builder()
            .chat("&4✘ &cThe parcel cannot be empty!")
            .sound(SoundEventKeys.ENTITY_ENDERMAN_AMBIENT)
            .build();
        public Notice receiverSet = Notice.builder()
            .chat("&2✔ &aParcel receiver set successfully.")
            .sound(SoundEventKeys.ENTITY_EXPERIENCE_ORB_PICKUP)
            .build();
        public Notice descriptionSet = Notice.builder()
            .chat("&2✔ &aParcel description set successfully.")
            .sound(SoundEventKeys.ENTITY_EXPERIENCE_ORB_PICKUP)
            .build();
        public Notice destinationSet = Notice.builder()
            .chat("&2✔ &aParcel destination locker set successfully.")
            .sound(SoundEventKeys.ENTITY_EXPERIENCE_ORB_PICKUP)
            .build();
        public Notice cannotCollect = Notice.builder()
            .chat("&4✘ &cAn error occurred while collecting the parcel.")
            .sound(SoundEventKeys.ENTITY_VILLAGER_NO)
            .build();
        public Notice databaseError = Notice.builder()
            .chat("&4✘ &cA database error occurred. Please contact an administrator.")
            .sound(SoundEventKeys.ENTITY_ITEM_BREAK)
            .build();
        public Notice noInventorySpace = Notice.builder()
            .chat("&4✘ &cYou don't have enough space in your inventory to collect the parcel!")
            .sound(SoundEventKeys.ENTITY_VILLAGER_NO)
            .build();
        public Notice collected = Notice.builder()
            .chat("&2✔ &aParcel collected successfully.")
            .sound(SoundEventKeys.ENTITY_PLAYER_LEVELUP)
            .build();
        public Notice nameNotSet = Notice.builder()
            .chat("&4✘ &cThe parcel name is not set!")
            .sound(SoundEventKeys.ENTITY_ENDERMAN_AMBIENT)
            .build();
        public Notice receiverNotSet = Notice.builder()
            .chat("&4✘ &cThe parcel receiver is not set!")
            .sound(SoundEventKeys.ENTITY_ENDERMAN_AMBIENT)
            .build();
        public Notice destinationNotSet = Notice.builder()
            .chat("&4✘ &cThe parcel destination locker is not set!")
            .sound(SoundEventKeys.ENTITY_ENDERMAN_AMBIENT)
            .build();
        public Notice lockerFull = Notice.builder()
            .chat("&4✘ &cThe destination locker is full! Please select another locker.")
            .sound(SoundEventKeys.ENTITY_VILLAGER_NO)
            .build();
        public Notice illegalItem = Notice.builder()
            .chat("&4✘ &cThe parcel contains illegal items that cannot be sent. ({ITEMS})")
            .build();
        public Notice cannotDelete = Notice.builder()
            .chat("&4✘ &cAn error occurred while deleting the parcel.")
            .sound(SoundEventKeys.ENTITY_VILLAGER_NO)
            .build();
        public Notice deleted = Notice.builder()
            .chat("&2✔ &aParcel deleted successfully.")
            .sound(SoundEventKeys.ENTITY_ITEM_BREAK)
            .build();
        public Notice insufficientFunds = Notice.builder()
            .chat("&4✘ &cYou do not have enough funds to send this parcel! Required: &6${AMOUNT}&c.")
            .sound(SoundEventKeys.ENTITY_VILLAGER_NO)
            .build();
        public Notice feeWithdrawn = Notice.builder()
            .chat("&2✔ &a${AMOUNT} has been withdrawn from your account to cover the parcel sending fee.")
            .sound(SoundEventKeys.ENTITY_EXPERIENCE_ORB_PICKUP)
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
            .sound(SoundEventKeys.BLOCK_NOTE_BLOCK_CHIME)
            .build();
    }

    public static class LockerMessages extends OkaeriConfig {
        public Notice cannotCreate = Notice.builder()
            .chat("&4✘ &cCould not create the parcel locker.")
            .sound(SoundEventKeys.ENTITY_VILLAGER_NO)
            .build();
        public Notice created = Notice.builder()
            .chat("&2✔ &aParcel locker created successfully.")
            .sound(SoundEventKeys.BLOCK_ANVIL_USE)
            .build();
        public String descriptionPrompt = "&6Enter a name for the parcel locker:";
        public Notice cannotBreak = Notice.builder()
            .chat("&4✘ &cYou have no permission to break the parcel locker.")
            .sound(SoundEventKeys.ENTITY_VILLAGER_NO)
            .build();
        public Notice deleted = Notice.builder()
            .chat("&2✔ &aParcel locker deleted successfully.")
            .sound(SoundEventKeys.BLOCK_ANVIL_BREAK)
            .build();
        public Notice broadcastRemoved = Notice.chat("&4❣ &cThe parcel locker at &4{X} {Y} {Z} &cin &4{WORLD} &chas been removed by &4{PLAYER}!");
        public Notice alreadyCreating = Notice.builder()
            .chat("&4✘ &cYou are already creating a parcel locker!")
            .sound(SoundEventKeys.ENTITY_VILLAGER_NO)
            .build();
        public Notice alreadyExists = Notice.builder()
            .chat("&4✘ &cA parcel locker already exists at this location!")
            .sound(SoundEventKeys.ENTITY_VILLAGER_NO)
            .build();
        public Notice addedToInventory = Notice.builder()
            .chat("&2✔ &aParcel locker item added to your inventory.")
            .sound(SoundEventKeys.ENTITY_ITEM_PICKUP)
            .build();
    }

    public static class AdminMessages extends OkaeriConfig {
        public Notice deletedLockers = Notice.chat("&4⚠ &cAll ({COUNT}) parcel lockers have been deleted!");
        public Notice deletedParcels = Notice.chat("&4⚠ &cAll ({COUNT}) parcels have been deleted!");
        public Notice deletedItemStorages = Notice.chat("&4⚠ &cAll ({COUNT}) item storages have been deleted!");
        public Notice deletedContents = Notice.chat("&4⚠ &cAll ({COUNT}) parcel contents have been deleted!");
        public Notice deletedDeliveries = Notice.chat("&4⚠ &cAll ({COUNT}) deliveries have been deleted!");
    }

    public static class DiscordMessages extends OkaeriConfig {
        public Notice verificationAlreadyPending = Notice.builder()
            .chat("&4✘ &cYou already have a pending verification. Please complete it or wait for it to expire.")
            .sound(SoundEventKeys.ENTITY_VILLAGER_NO)
            .build();
        public Notice alreadyLinked = Notice.builder()
            .chat("&4✘ &cYour Minecraft account is already linked to a Discord account!")
            .sound(SoundEventKeys.ENTITY_VILLAGER_NO)
            .build();
        public Notice discordAlreadyLinked = Notice.builder()
            .chat("&4✘ &cThis Discord account is already linked to another Minecraft account!")
            .sound(SoundEventKeys.ENTITY_VILLAGER_NO)
            .build();
        public Notice userNotFound = Notice.builder()
            .chat("&4✘ &cCould not find a Discord user with that ID!")
            .sound(SoundEventKeys.ENTITY_VILLAGER_NO)
            .build();
        public Notice verificationCodeSent = Notice.builder()
            .chat("&2✔ &aA verification code has been sent to your Discord DM. Please check your messages.")
            .sound(SoundEventKeys.ENTITY_EXPERIENCE_ORB_PICKUP)
            .build();
        public Notice cannotSendDm = Notice.builder()
            .chat("&4✘ &cCould not send a DM to your Discord account. Please make sure your DMs are open.")
            .sound(SoundEventKeys.ENTITY_VILLAGER_NO)
            .build();
        public Notice verificationExpired = Notice.builder()
            .chat("&4✘ &cYour verification code has expired. Please run the command again.")
            .sound(SoundEventKeys.ENTITY_VILLAGER_NO)
            .build();
        public Notice invalidCode = Notice.builder()
            .chat("&4✘ &cInvalid verification code. Please try again in 2 minutes.")
            .sound(SoundEventKeys.ENTITY_VILLAGER_NO)
            .build();
        public Notice linkSuccess = Notice.builder()
            .chat("&2✔ &aYour Discord account has been successfully linked!")
            .sound(SoundEventKeys.ENTITY_PLAYER_LEVELUP)
            .build();
        public Notice linkFailed = Notice.builder()
            .chat("&4✘ &cFailed to link your Discord account. Please try again later.")
            .sound(SoundEventKeys.ENTITY_VILLAGER_NO)
            .build();
        public Notice verificationCancelled = Notice.builder()
            .chat("&6⚠ &eVerification cancelled.")
            .sound(SoundEventKeys.BLOCK_NOTE_BLOCK_BASS)
            .build();
        public Notice playerAlreadyLinked = Notice.chat("&4✘ &cThis player already has a linked Discord account!");
        public Notice adminLinkSuccess = Notice.chat("&2✔ &aSuccessfully linked the Discord account to the player.");

        @Comment({"", "# Unlink messages" })
        public Notice notLinked = Notice.builder()
            .chat("&4✘ &cYour Minecraft account is not linked to any Discord account!")
            .sound(SoundEventKeys.ENTITY_VILLAGER_NO)
            .build();
        public Notice unlinkSuccess = Notice.builder()
            .chat("&2✔ &aYour Discord account has been successfully unlinked!")
            .sound(SoundEventKeys.ENTITY_EXPERIENCE_ORB_PICKUP)
            .build();
        public Notice unlinkFailed = Notice.builder()
            .chat("&4✘ &cFailed to unlink the Discord account. Please try again later.")
            .sound(SoundEventKeys.ENTITY_VILLAGER_NO)
            .build();
        public Notice playerNotLinked = Notice.chat("&4✘ &cThis player does not have a linked Discord account!");
        public Notice adminUnlinkSuccess = Notice.chat("&2✔ &aSuccessfully unlinked the Discord account from the player.");
        public Notice discordNotLinked = Notice.chat("&4✘ &cNo Minecraft account is linked to this Discord ID!");
        public Notice adminUnlinkByDiscordSuccess = Notice.chat("&2✔ &aSuccessfully unlinked the Minecraft account from the Discord ID.");

        @Comment({"", "# Dialog configuration for verification" })
        public String verificationDialogTitle = "&6Enter your Discord verification code:";
        public String verificationDialogPlaceholder = "&7Enter 4-digit code";

        @Comment({"", "# The message sent to the Discord user via DM" })
        @Comment("# Placeholders: {CODE} - the verification code, {PLAYER} - the Minecraft player name")
        public String discordDmVerificationMessage = "**📦 ParcelLockers Verification**\n\nPlayer **{PLAYER}** is trying to link their Minecraft account to your Discord account.\n\nYour verification code is: **{CODE}**\n\nThis code will expire in 2 minutes.";

        @Comment({"", "# The message sent to the Discord user when a parcel is delivered" })
        @Comment("# Placeholders: {PARCEL_NAME}, {SENDER}, {RECEIVER}, {DESCRIPTION}, {SIZE}, {PRIORITY}")
        public String parcelDeliveryNotification = "**📦 Parcel Delivered!**\n\nYour parcel **{PARCEL_NAME}** has been delivered!\n\n**From:** {SENDER}\n**Size:** {SIZE}\n**Priority:** {PRIORITY}\n**Description:** {DESCRIPTION}";

        @Comment({"", "# DiscordSRV integration messages" })
        @Comment("# These messages are shown when DiscordSRV is installed and handles account linking")
        public Notice discordSrvLinkRedirect = Notice.builder()
            .chat("&6⚠ &eTo link your Discord account, use the DiscordSRV linking system.")
            .chat("&6⚠ &eYour linking code is: &a{CODE}")
            .chat("&6⚠ &eSend this code to the Discord bot in a private message.")
            .sound(SoundEventKeys.BLOCK_NOTE_BLOCK_CHIME)
            .build();
        public Notice discordSrvAlreadyLinked = Notice.builder()
            .chat("&2✔ &aYour account is already linked via DiscordSRV!")
            .sound(SoundEventKeys.ENTITY_VILLAGER_YES)
            .build();
        public Notice discordSrvUnlinkRedirect = Notice.builder()
            .chat("&6⚠ &eTo unlink your Discord account, please use the DiscordSRV unlinking system.")
            .sound(SoundEventKeys.BLOCK_NOTE_BLOCK_CHIME)
            .build();
    }
}
