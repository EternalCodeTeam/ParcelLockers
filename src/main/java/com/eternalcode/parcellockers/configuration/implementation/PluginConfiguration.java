package com.eternalcode.parcellockers.configuration.implementation;

import com.eternalcode.parcellockers.configuration.ReloadableConfig;
import com.eternalcode.parcellockers.database.DatabaseType;
import net.dzikoysk.cdn.entity.Contextual;
import net.dzikoysk.cdn.entity.Description;
import net.dzikoysk.cdn.source.Resource;
import net.dzikoysk.cdn.source.Source;
import org.bukkit.Material;

import java.io.File;
import java.util.Collections;
import java.util.List;

public class PluginConfiguration implements ReloadableConfig {

    @Description({ " ", "# Parcel Lockers plugin configuration file." })
    public Settings settings = new Settings();

    @Description({ " ", "# The plugin messages." })
    public Messages messages = new Messages();

    @Description({ " ", "# The plugin GUI settings." })
    public GuiSettings guiSettings = new GuiSettings();

    @Override
    public Resource resource(File folder) {
        return Source.of(folder, "config.yml");
    }

    @Contextual
    public static class Settings {

        @Description({ " ",
            "# Should we enable Sentry integration?",
            "# Sentry is a service that collects errors and other information about the plugin.",
            "# It is used to improve the plugin and fix bugs.",
            "# It is also strongly recommended to enable this option.",
            "# You can learn more about Sentry here: https://sentry.io/" })
        public boolean enableSentry = true;

        @Description({ " ", "# Whether the player after entering the server should receive information about the new version of the plugin?" })
        public boolean receiveUpdates = true;

        @Description({ " ", "# The database type. (MYSQL, SQLITE)" })
        public DatabaseType databaseType = DatabaseType.SQLITE;

        @Description({ " ", "# The URL to the database." })
        public String host = "localhost";

        @Description({ " ", "# The database name." })
        public String databaseName = "parcellockers";

        @Description({ " ", "# The database user." })
        public String user = "root";

        @Description({ " ", "# The database port." })
        public String port = "3306";

        @Description({ " ", "# Turn this on to enable SSL (Secure Sockets Layer)" })
        public boolean useSSL = false;

        @Description({ " ", "# The database password." })
        public String password = "";

        @Description({ " ", "# The parcel locker item." })
        public ConfigItem parcelLockerItem = new ConfigItem()
            .setName("&3Parcel locker")
            .setType(Material.CHEST)
            .setLore(List.of("&bPlace to create a parcel locker."));

        @Description({ " ", "# Small parcel cost." })
        public double smallParcelCost = 5.0;

        @Description({ " ", "# Medium parcel cost." })
        public double mediumParcelCost = 10.0;

        @Description({ " ", "# Large parcel cost." })
        public double largeParcelCost = 15.0;
    }

    @Contextual
    public static class Messages {

        public String onlyForPlayers = "&4✘ &cThis command is only available to players!";
        public String noPermission = "&4✘ &cYou don't have permission to perform this command! &6(&c{PERMISSION}&6)";
        public String cantFindPlayer = "&4✘ &cThe specified player could not be found!";
        public String invalidUsage = "&4❣ &cCorrect usage: &6{USAGE}";
        public String reload = "&3❣ &bConfiguration has been successfully reloaded!";
        public String parcelCommandUsage = "&9ⓘ Correct usage: &3/parcel &b<list|info|send|cancel> &3[parcel]";
        public String parcelSuccessfullyCreated = "&2✔ &aParcel created successfully.";
        public String failedToCreateParcel = "&4✘ &cAn error occurred while creating the parcel.";
        public String parcelSuccessfullyDeleted = "&2✔ &aParcel deleted successfully.";
        public String failedToDeleteParcel = "&4✘ &cAn error occurred while deleting the parcel.";
        public String failedToCreateParcelLocker = "&4✘ &cCould not create the parcel locker.";
        public String parcelLockerSuccessfullyCreated = "&7» &aParcel locker created successfully.";
        public String enterDescriptionPrompt = "&6❀ &eEnter a description for the parcel locker:";
        public String cannotBreakParcelLocker = "&4✘ &cYou have no permission to break the parcel locker.";
        public String parcelLockerSuccessfullyDeleted = "&2✔ &aParcel locker deleted successfully.";
        public String broadcastParcelLockerRemoved = "&4❣ &cThe parcel locker at &4{X} {Y} {Z} &cin &4{WORLD} &chas been removed by &4{PLAYER}!";
        public String parcelSent = "&2✔ &aParcel sent successfully.";
        public String parcelFailedToSend = "&4✘ &cAn error occurred while sending the parcel. Check the console for more information.";
        public String parcelCannotBeEmpty = "&4✘ &cThe parcel cannot be empty!";
        public String parcelNameCannotBeEmpty = "&4✘ &cThe parcel name cannot be empty!";
        public String parcelNameSet = "&2✔ &aParcel name set successfully.";
        public String parcelDescriptionSet = "&2✔ &aParcel description set successfully.";
        public String parcelReceiverSet = "&2✔ &aParcel receiver set successfully.";
        public String alreadyCreatingLocker = "&4✘ &cYou are already creating a parcel locker!";
        @Description({ " ", "# The parcel info message." })
        public List<String> parcelInfoMessages = List.of(
            "&7» &6Parcel info:",
            "&6UUID: &e{UUID}",
            "&6Sender: &e{SENDER}",
            "&6Receiver: &e{RECEIVER}",
            "&6Size: &e{SIZE}",
            "&6Position: &6X: &e{POSITION_X}, &6Y: &e{POSITION_Y}, &6Z: &e{POSITION_Z}",
            "&6Priority: &e{PRIORITY}",
            "&6Description: &e{DESCRIPTION}",
            "&6Recipients: &e{RECIPIENTS}"
        );
    }

    @Contextual
    public static class GuiSettings {

        @Description({ " ", "# The title of the main GUI" })
        public String mainGuiTitle = "&3Main menu";

        @Description({ " ", "# The title of the parcel list GUI" })
        public String parcelListGuiTitle = "&aMy parcels";

        @Description({ " ", "# The item of the sent parcels GUI" })
        public String sentParcelsTitle = "&6Sent parcels";

        @Description({ " ", "# The item of the parcel locker sending GUI" })
        public String parcelLockerSendingGuiTitle = "&3Parcel sending";

        @Description({ " ", "# The item of the parcel recipient pick GUI" })
        public String parcelReceiverSelectionGuiTitle = "&5Select recipient";

        @Description({ " ", "# The item of the small parcel size button" })
        public ConfigItem smallParcelSizeItem = new ConfigItem()
            .setName("&a✧ Small")
            .setLore(List.of("&bClick to select the small parcel size."))
            .setType(Material.LIME_WOOL);

        @Description({ " ", "# The item of the medium parcel size button" })
        public ConfigItem mediumParcelSizeItem = new ConfigItem()
            .setName("&e✧ Medium")
            .setLore(List.of("&bClick to select the medium parcel size."))
            .setType(Material.YELLOW_WOOL);

        @Description({ " ", "# The item of the large parcel size button" })
        public ConfigItem largeParcelSizeItem = new ConfigItem()
            .setName("&c✧ Large")
            .setLore(List.of("&bClick to select the large parcel size."))
            .setType(Material.RED_WOOL);

        @Description({ " ", "# The item represents selected small parcel size." })
        public ConfigItem selectedSmallParcelSizeItem = new ConfigItem()
            .setName("&a✧ Small")
            .setLore(List.of("&a✧ Currently selected!"))
            .setGlow(true)
            .setType(Material.OAK_CHEST_BOAT);

        @Description({ " ", "# The item represents selected medium parcel size." })
        public ConfigItem selectedMediumParcelSizeItem = new ConfigItem()
            .setName("&e✧ Medium")
            .setLore(List.of("&aCurrently selected!"))
            .setGlow(true)
            .setType(Material.CHEST_MINECART);

        @Description({ " ", "# The item represents selected large parcel size." })
        public ConfigItem selectedLargeParcelSizeItem = new ConfigItem()
            .setName("&c✧ Large")
            .setLore(List.of("&aCurrently selected!"))
            .setGlow(true)
            .setType(Material.TNT_MINECART);

        @Description({ " ", "# The item of the priority button" })
        public ConfigItem priorityItem = new ConfigItem()
            .setName("&a✧ Priority")
            .setLore(List.of("&bClick to select the priority."))
            .setType(Material.REDSTONE);

        @Description({ " ", "# The item of the selected priority button" })
        public ConfigItem selectedPriorityItem = new ConfigItem()
            .setName("&a✧ Priority")
            .setLore(List.of("&aCurrently selected!", "&c&oClick to unselect."))
            .setType(Material.REDSTONE_BLOCK)
            .setGlow(true);

        @Description({ " ", "# The close button item" })
        public ConfigItem closeItem = new ConfigItem()
            .setName("&c☓ Close")
            .setLore(List.of("&cClick to close the GUI."))
            .setType(Material.BARRIER);

        @Description({ " ", "# The item of the main GUI" })
        public ConfigItem mainGuiBackgroundItem = new ConfigItem()
            .setName("")
            .setLore(Collections.emptyList())
            .setType(Material.GRAY_STAINED_GLASS_PANE);

        @Description({ " ", "# The item of the corner GUI item.", "# Purely for decoration purposes." })
        public ConfigItem cornerItem = new ConfigItem()
            .setName("")
            .setLore(Collections.emptyList())
            .setType(Material.BLUE_STAINED_GLASS_PANE);

        @Description({ " ", "# The item of the parcel submit button" })
        public ConfigItem submitParcelItem = new ConfigItem()
            .setName("&a✔ Submit parcel")
            .setLore(List.of("&aClick to submit the parcel.", "&cProceed with caution! This action is final and cannot be undone."))
            .setType(Material.WAXED_OXIDIZED_COPPER)
            .setGlow(true);

        @Description({ " ", "# The item of the parcel list button" })
        public ConfigItem myParcelsItem = new ConfigItem()
            .setName("&3♪ My parcels")
            .setLore(List.of("&bClick to open your parcels."))
            .setType(Material.ENDER_CHEST);

        @Description({ " ", "# The item of the sent parcels button" })
        public ConfigItem sentParcelsItem = new ConfigItem()
            .setName("&6♚ Sent parcels")
            .setLore(List.of("&eClick to show parcels, which you sent.", "&eYou can also cancel them here, if you want to."))
            .setType(Material.FILLED_MAP)
            .setGlow(true);

        @Description({ " ", "# The parcel archive item button." })
        public ConfigItem parcelArchiveItem = new ConfigItem()
            .setName("&5❄ Parcel archive")
            .setLore(List.of("&eClick to show all parcels, which you sent or received in the past."))
            .setType(Material.WRITTEN_BOOK);

        @Description({ " ", "# The item of the parcel locker collect button" })
        public ConfigItem parcelLockerCollectItem = new ConfigItem()
            .setName("&a✔ Collect parcels")
            .setLore(List.of("&aClick to collect your parcels."))
            .setType(Material.HOPPER)
            .setGlow(true);

        @Description({ " ", "# The item of the parcel locker send button" })
        public ConfigItem parcelLockerSendItem = new ConfigItem()
            .setName("&b☴ Send parcels")
            .setLore(List.of("&bClick to send parcels."))
            .setType(Material.SCULK_SHRIEKER)
            .setGlow(true);

        @Description({ " ", "# The item of the parcel locker status button" })
        public ConfigItem parcelLockerStatusItem = new ConfigItem()
            .setName("&3♖ Parcel locker status")
            .setLore(List.of("&bClick to show the status of your parcel locker."))
            .setType(Material.END_PORTAL_FRAME)
            .setGlow(true);

        @Description({ " ", "# The item of the parcel" })
        public ConfigItem parcelItem = new ConfigItem()
            .setName("&6{NAME}")
            .setLore(List.of(
                    "&6UUID: &e{UUID}",
                    "&6Sender: &e{SENDER}",
                    "&6Receiver: &e{RECEIVER}",
                    "&6Size: &e{SIZE}",
                    "&6Position: &6X: &e{POSITION_X}, &6Y: &e{POSITION_Y}, &6Z: &e{POSITION_Z}",
                    "&6Priority: &e{PRIORITY}",
                    "&6Description: &e{DESCRIPTION}",
                    "&6Recipients: &e{RECIPIENTS}"
                )
            )
            .setType(Material.CHEST_MINECART);

        @Description({ " ", "# The item of the parcel item storage button" })
        public ConfigItem parcelStorageItem = new ConfigItem()
            .setName("&6✉ Parcel storage")
            .setLore(List.of("&eClick to edit the parcel content."))
            .setType(Material.CHEST);

        @Description({ " ", "# The item of the parcel name button" })
        public ConfigItem parcelNameItem = new ConfigItem()
            .setName("&4❁ &cParcel name")
            .setLore(List.of("&cClick to edit the parcel name."))
            .setType(Material.NAME_TAG);

        @Description({ " ", "# The value of the GUI line, when parcel name is set" })
        public String parcelNameSetLine = "&6> Current parcel name: &e{NAME}";

        @Description({ " ", "# The item of the parcel description button" })
        public ConfigItem parcelDescriptionItem = new ConfigItem()
            .setName("&2︙ &aParcel description")
            .setLore(List.of("&aClick to edit the parcel description."))
            .setType(Material.PAPER);

        public String parcelDescriptionSetLine = "&2> &aCurrent parcel description: &2{DESCRIPTION}";

        @Description({ " ", "# The item of the parcel receiver button" })
        public ConfigItem parcelReceiverItem = new ConfigItem()
            .setName("&5✧ &dParcel receiver")
            .setLore(List.of("&dClick to edit the parcel receiver."))
            .setType(Material.PLAYER_HEAD);

        @Description({ " ", "# The value of the GUI line, when parcel name is set" })
        public String parcelReceiverGuiSetLine = "&5> &dCurrent parcel receiver: &5{RECEIVER}";

        @Description({ " ", "# The value of the player itemlore line, when parcel receiver is not set" })
        public String parcelReceiverNotSetLine = "&9ⓘ &bClick to select.";

        @Description({ " ", "# The value of the player item lore line, when parcel receiver is set" })
        public String parcelReceiverSetLine = "&2✔ &aSelected!";

        @Description({ " ", "# The item of the parcel destination locker button" })
        public ConfigItem parcelDestinationLockerItem = new ConfigItem()
            .setName("&3➣ &bDestination locker")
            .setLore(List.of("&bClick to edit the parcel destination locker."))
            .setType(Material.VAULT);

        @Description({ " ", "# The item of the previous page button" })
        public ConfigItem previousPageItem = new ConfigItem()
            .setName("&b✐ Previous page")
            .setLore(List.of("&bClick to go to the previous page."))
            .setType(Material.ARROW);

        @Description({ " ", "# The item of the next page button" })
        public ConfigItem nextPageItem = new ConfigItem()
            .setName("&b✐ Next page")
            .setLore(List.of("&bClick to go to the next page."))
            .setType(Material.ARROW);

        @Description({ " ", "# The item of the confirm items button" })
        public ConfigItem confirmItemsItem = new ConfigItem()
            .setName("&a✉ Confirm items")
            .setLore(List.of("&aClick to confirm the items."))
            .setType(Material.LIME_WOOL);

        @Description({ " ", "# The name of the parcel small content GUI" })
        public String parcelSmallContentGuiTitle = "&aSmall parcel content";

        @Description({ " ", "# The name of the parcel medium content GUI" })
        public String parcelMediumContentGuiTitle = "&eMedium parcel content";

        @Description({ " ", "# The name of the parcel large content GUI" })
        public String parcelLargeContentGuiTitle = "&cLarge parcel content";

        @Description({ " ", "# Illegal items list, that cannot be stored in the parcel." })
        public List<Material> illegalItems = List.of(
            Material.BARRIER,
            Material.COMMAND_BLOCK,
            Material.COMMAND_BLOCK_MINECART,
            Material.CHAIN_COMMAND_BLOCK,
            Material.REPEATING_COMMAND_BLOCK,
            Material.STRUCTURE_BLOCK,
            Material.STRUCTURE_VOID,
            Material.JIGSAW,
            Material.DEBUG_STICK,
            Material.SPAWNER,
            Material.BEDROCK
        );
    }
}
