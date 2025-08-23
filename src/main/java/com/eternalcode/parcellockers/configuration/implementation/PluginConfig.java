package com.eternalcode.parcellockers.configuration.implementation;

import com.eternalcode.parcellockers.configuration.serializable.ConfigItem;
import com.eternalcode.parcellockers.database.DatabaseType;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import eu.okaeri.configs.annotation.Header;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import org.bukkit.Material;

@Header({
    " ",
    "# Parcel Lockers plugin configuration file.",
    "#",
    "# This file is used to configure the ParcelLockers plugin.",
    "# You can change the settings here to customize the plugin behavior.",
    "#",
    "# If you want to change the settings, you can do it in-game using the /parcellockers reload command."
})
public class PluginConfig extends OkaeriConfig {

    @Comment({ "", "# Parcel Lockers plugin configuration file." })
    public Settings settings = new Settings();

    @Comment({ "", "# The plugin GUI settings." })
    public GuiSettings guiSettings = new GuiSettings();

    public static class Settings extends OkaeriConfig {

        @Comment({ "", "# Whether the player after entering the server should receive information about the new version of the plugin?" })
        public boolean receiveUpdates = true;

        @Comment({ "", "# The database type. (MYSQL, SQLITE)" })
        public DatabaseType databaseType = DatabaseType.SQLITE;

        @Comment({ "", "# The URL to the database." })
        public String host = "localhost";

        @Comment({ "", "# The database name." })
        public String databaseName = "parcellockers";

        @Comment({ "", "# The database user." })
        public String user = "root";

        @Comment({ "", "# The database port." })
        public String port = "3306";

        @Comment({ "", "# The database password." })
        public String password = "";

        @Comment({ "", "# The parcel locker item." })
        public ConfigItem parcelLockerItem = new ConfigItem()
            .name("&3Parcel locker")
            .type(Material.CHEST)
            .lore(List.of("&bPlace to create a parcel locker."));

        @Comment({"", "# Standard parcel sending duration"})
        public Duration parcelSendDuration = Duration.ofSeconds(10);

        @Comment({"", "# Parcel sending duration for priority parcels"})
        public Duration priorityParcelSendDuration = Duration.ofSeconds(5);
    }

    public static class GuiSettings extends OkaeriConfig {

        @Comment({ "", "# The title of the main GUI" })
        public String mainGuiTitle = "&6Parcel management";

        @Comment({ "", "# The title of the parcel list GUI" })
        public String parcelListGuiTitle = "&9My parcels";

        @Comment({ "", "# The item of the sent parcels GUI" })
        public String sentParcelsTitle = "&6Sent parcels";

        @Comment({ "", "# The item of the parcel locker sending GUI" })
        public String parcelLockerSendingGuiTitle = "&6Send parcel";

        @Comment({ "", "# The item of the parcel recipient pick GUI" })
        public String parcelReceiverSelectionGuiTitle = "&5Select recipient";

        @Comment({ "", "# The item of the parcel collection GUI" })
        public String parcelCollectionGuiTitle = "&aCollect parcels";

        @Comment({ "", "# The item of the small parcel size button" })
        public ConfigItem smallParcelSizeItem = new ConfigItem()
            .name("&2\uD83C\uDF37 &aSmall")
            .lore(List.of("&aClick to select the small parcel size."))
            .type(Material.OAK_CHEST_BOAT);

        @Comment({ "", "# The item of the medium parcel size button" })
        public ConfigItem mediumParcelSizeItem = new ConfigItem()
            .name("&6\uD83C\uDF39 &eMedium")
            .lore(List.of("&eClick to select the medium parcel size."))
            .type(Material.CHEST_MINECART);

        @Comment({ "", "# The item of the large parcel size button" })
        public ConfigItem largeParcelSizeItem = new ConfigItem()
            .name("&4\uD83C\uDFDD &cLarge")
            .lore(List.of("&cClick to select the large parcel size."))
            .type(Material.TNT_MINECART);

        @Comment({ "", "# The item represents selected small parcel size." })
        public ConfigItem selectedSmallParcelSizeItem = new ConfigItem()
            .name("&2\uD83C\uDF37 &aSmall")
            .lore(List.of("&2✔ Currently selected!"))
            .glow(true)
            .type(Material.OAK_CHEST_BOAT);

        @Comment({ "", "# The item represents selected medium parcel size." })
        public ConfigItem selectedMediumParcelSizeItem = new ConfigItem()
            .name("&6\uD83C\uDF39 &eMedium")
            .lore(List.of("&6✔ &eCurrently selected!"))
            .glow(true)
            .type(Material.CHEST_MINECART);

        @Comment({ "", "# The item represents selected large parcel size." })
        public ConfigItem selectedLargeParcelSizeItem = new ConfigItem()
            .name("&4\uD83C\uDFDD &cLarge")
            .lore(List.of("&4✔ &cCurrently selected!"))
            .glow(true)
            .type(Material.TNT_MINECART);

        @Comment({ "", "# The item of the priority button" })
        public ConfigItem priorityItem = new ConfigItem()
            .name("&4\uD83D\uDE80 &cPriority")
            .lore(List.of("&cClick to send the parcel faster."))
            .type(Material.FIREWORK_ROCKET);

        @Comment({ "", "# The item of the selected priority button" })
        public ConfigItem selectedPriorityItem = new ConfigItem()
            .name("&4\uD83D\uDE80 &cPriority")
            .lore(List.of("&4✔ &cCurrently selected!", "&8&oClick to unselect."))
            .type(Material.FIREWORK_ROCKET)
            .glow(true);

        @Comment({ "", "# The close button item" })
        public ConfigItem closeItem = new ConfigItem()
            .name("&4✖ &cClose")
            .lore(List.of("&cClick to close the GUI."))
            .type(Material.BARRIER);

        @Comment({ "", "# The item of the main GUI" })
        public ConfigItem mainGuiBackgroundItem = new ConfigItem()
            .name("")
            .lore(Collections.emptyList())
            .type(Material.GRAY_STAINED_GLASS_PANE);

        @Comment({ "", "# The item of the corner GUI item.", "# Purely for decoration purposes." })
        public ConfigItem cornerItem = new ConfigItem()
            .name("")
            .lore(Collections.emptyList())
            .type(Material.ORANGE_STAINED_GLASS_PANE);

        @Comment({ "", "# The item of the parcel submit button" })
        public ConfigItem submitParcelItem = new ConfigItem()
            .name("&2✔ &aSubmit")
            .lore(List.of("<gradient:#089F49:#32D25E>Click to send your parcel.</gradient>", "<gradient:#CD2D2D:#EC2465>Before submitting, check if everything has been filled correctly!</gradient>"))
            .type(Material.EMERALD_BLOCK)
            .glow(true);

        @Comment({ "", "# The item of the parcel list button" })
        public ConfigItem myParcelsItem = new ConfigItem()
            .name("&9\uD83D\uDD83 My parcels")
            .lore(List.of("&9» Click to open your parcels."))
            .type(Material.ENDER_CHEST);

        @Comment({ "", "# The item of the sent parcels button" })
        public ConfigItem sentParcelsItem = new ConfigItem()
            .name("&6\uD83D\uDD85 &eSent parcels")
            .lore(List.of("&6» &eClick to list parcels that you have sent."))
            .type(Material.YELLOW_SHULKER_BOX)
            .glow(true);

        @Comment({ "", "# The parcel archive item button." })
        public ConfigItem parcelArchiveItem = new ConfigItem()
            .name("&5\uD83D\uDCDA &dParcel archive")
            .lore(List.of("&5» &dClick to show all parcels, which you sent or received in the past.", "&c&oNot implemented yet"))
            .type(Material.BOOKSHELF);

        @Comment({ "", "# The item of the parcel locker collect button" })
        public ConfigItem parcelLockerCollectItem = new ConfigItem()
            .name("&2\uD83C\uDF20 Collect parcels")
            .lore(List.of("&2» &aClick to collect your parcels."))
            .type(Material.LECTERN)
            .glow(true);

        @Comment({ "", "# The item of the parcel locker send button" })
        public ConfigItem parcelLockerSendItem = new ConfigItem()
            .name("&6\uD83D\uDCE6 Send parcels")
            .lore(List.of("&6» &eClick to send parcels."))
            .type(Material.CHEST_MINECART)
            .glow(true);

        @Comment({ "", "# The item of the parcel" })
        public ConfigItem parcelItem = new ConfigItem()
            .name("&6{NAME}")
            .lore(List.of(
                    "&6Sender: &e{SENDER}",
                    "&6Receiver: &e{RECEIVER}",
                    "&6Size: &e{SIZE}",
                    "&6Position: &6X: &e{POSITION_X}, &6Y: &e{POSITION_Y}, &6Z: &e{POSITION_Z}",
                    "&6Priority: &e{PRIORITY}",
                    "&6Description: &e{DESCRIPTION}"
                )
            )
            .type(Material.CHEST_MINECART);

        @Comment({ "", "# The item of the parcel item storage button" })
        public ConfigItem parcelStorageItem = new ConfigItem()
            .name("&6\uD83D\uDCBE Parcel storage")
            .lore(List.of("&6» &eClick to add/remove items from this parcel"))
            .type(Material.CHEST);

        @Comment({ "", "# The item of the parcel name button" })
        public ConfigItem parcelNameItem = new ConfigItem()
            .name("&4✎ &cParcel name")
            .lore(List.of("&4» &cClick to name the parcel."))
            .type(Material.NAME_TAG);

        @Comment({ "", "# The value of the GUI line, when parcel name is set" })
        public String parcelNameSetLine = "&4» &cCurrent parcel name: &e{NAME}";

        @Comment({ "", "# The item of the parcel name button" })
        public ConfigItem parcelDescriptionItem = new ConfigItem()
            .name("&2\uD83D\uDDC9 &aParcel description")
            .lore(List.of("&2» &aClick to add parcel description."))
            .type(Material.OAK_SIGN);

        public String parcelDescriptionSetLine = "&2» &aCurrent parcel name: &2{DESCRIPTION}";

        @Comment({ "", "# The item of the parcel receiver button" })
        public ConfigItem parcelReceiverItem = new ConfigItem()
            .name("&5\uD83E\uDDCD &dParcel receiver")
            .lore(List.of("&5» &dClick to choose the parcel receiver."))
            .type(Material.PLAYER_HEAD);

        @Comment({ "", "# The value of the GUI line, when parcel name is set" })
        public String parcelReceiverGuiSetLine = "&5» &dCurrent parcel receiver: &5{RECEIVER}";

        @Comment({ "", "# The value of the player itemlore line, when parcel receiver is not set" })
        public String parcelReceiverNotSetLine = "&9» &bClick to select.";

        @Comment({ "", "# The value of the player item lore line, when parcel receiver is set" })
        public String parcelReceiverSetLine = "&2✔ &aSelected!";

        @Comment({ "", "# The item of the parcel destination locker button" })
        public ConfigItem parcelDestinationLockerItem = new ConfigItem()
            .name("&3\uD83D\uDEE3 Destination locker")
            .lore(List.of("&3Click to edit the parcel destination locker."))
            .type(Material.COMPASS);

        @Comment({ "", "# The item of the previous page button" })
        public ConfigItem previousPageItem = new ConfigItem()
            .name("&b← Previous page")
            .lore(List.of("&bClick to go to the previous page."))
            .type(Material.ARROW);

        @Comment({ "", "# The item of the next page button" })
        public ConfigItem nextPageItem = new ConfigItem()
            .name("&b→ Next page")
            .lore(List.of("&bClick to go to the next page."))
            .type(Material.ARROW);

        @Comment({ "", "# The item of the confirm items button" })
        public ConfigItem confirmItemsItem = new ConfigItem()
            .name("&2✔ &aConfirm items")
            .lore(List.of("&aClick to confirm the items."))
            .type(Material.GREEN_DYE);

        @Comment({ "", "# The name of the parcel small content GUI" })
        public String parcelSmallContentGuiTitle = "&2Small parcel content";

        @Comment({ "", "# The name of the parcel medium content GUI" })
        public String parcelMediumContentGuiTitle = "&6Medium parcel content";

        @Comment({ "", "# The name of the parcel large content GUI" })
        public String parcelLargeContentGuiTitle = "&4Large parcel content";

        @Comment({ "", "# The title of the parcel destination locker selection GUI" })
        public String parcelDestinationLockerSelectionGuiTitle = "&3Select destination locker";

        @Comment({ "", "# The item of the parcel in destination selection GUI" })
        public ConfigItem destinationLockerItem = new ConfigItem()
            .name("&3{DESCRIPTION}")
            .lore(List.of("&bClick to select this locker."))
            .type(Material.CHISELED_BOOKSHELF);

        @Comment({ "", "# The lore of the button in the sending GUI of the parcel destination locker selection GUI" })
        public String parcelDestinationLockerSetLine = "&6› &eCurrent destination locker: &6{DESCRIPTION}";

        @Comment({ "", "# The destination GUI locker button lore in case the destination locker is set." })
        public String parcelDestinationSetLine = "&2✔ &aSelected!";

        @Comment({ "", "# The destination GUI locker button lore in case the destination locker is not set." })
        public String parcelDestinationNotSetLine = "&9» &bClick to select.";

        @Comment({ "", "# Illegal items list, that cannot be stored in the parcel." })
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
            Material.BEDROCK,
            Material.VAULT,
            Material.END_PORTAL_FRAME
        );

        @Comment({ "", "# The item of the parcel item in the collection GUI" })
        public ConfigItem parcelCollectionItem = new ConfigItem()
            .name("&a{NAME}")
            .lore(List.of(
                    "&6Sender: &e{SENDER}",
                    "&6Size: &e{SIZE}",
                    "&6Description: &e{DESCRIPTION}"
                )
            )
            .type(Material.CHEST_MINECART);

        @Comment({ "", "# The item that is displayed in the collection GUI when no parcels are found" })
        public ConfigItem noParcelsItem = new ConfigItem()
            .name("&4✘ &cNo parcels found")
            .lore(List.of("&cYou don't have any parcels to collect."))
            .type(Material.STRUCTURE_VOID);
    }
}
