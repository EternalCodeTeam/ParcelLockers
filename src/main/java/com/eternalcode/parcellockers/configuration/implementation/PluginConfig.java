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
    "# ParcelLockers plugin configuration file.",
    "",
    "# You can change the settings here to customize the plugin behavior or appearance.",
    "# If you want to reload the configuration, you can do it using the '/parcellockers reload' command.",
    "# Both legacy color codes and MiniMessage formatting are supported in messages and GUI titles."
})
public class PluginConfig extends OkaeriConfig {

    public Settings settings = new Settings();

    @Comment({ "", "# The plugin GUI settings." })
    public GuiSettings guiSettings = new GuiSettings();

    @Comment({ "", "# The plugin Discord integration settings." })
    public DiscordSettings discord = new DiscordSettings();

    public static class Settings extends OkaeriConfig {

        @Comment("# Whether the player after entering the server should receive information about the new version of the plugin?")
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

        @Comment({ "", "# Maximum number of connections held in the database pool." })
        public int connectionPoolSize = 10;

        @Comment({ "", "# How long (in milliseconds) to wait for a free connection before failing." })
        public long connectionTimeoutMillis = 5000;

        @Comment({
            "",
            "# Connection leak detection threshold in milliseconds (0 disables it).",
            "# Set this comfortably above your slowest expected query to avoid false warnings."
        })
        public long leakDetectionThresholdMillis = 30000;

        @Comment({ "", "# The parcel locker item." })
        public ConfigItem parcelLockerItem = new ConfigItem()
            .name("&3Parcel locker")
            .type(Material.CHEST)
            .lore(List.of("&bPlace to create a parcel locker."));

        @Comment({"", "# Standard parcel sending duration"})
        public Duration parcelSendDuration = Duration.ofMinutes(30);

        @Comment({"", "# Parcel sending duration for priority parcels"})
        public Duration priorityParcelSendDuration = Duration.ofSeconds(30);

        @Comment({"", "# Maximum number of parcels that can be stored in a single locker"})
        public int maxParcelsPerLocker = 30;

        @Comment({
            "",
            "# Whether a parcel can be collected from any locker instead of only its destination locker.",
            "# false (default): parcels can only be collected from the locker they were sent to.",
            "# true: keeps the legacy behavior where every parcel can be collected from every locker."
        })
        public boolean allowCollectingFromAnyLocker = false;

        @Comment({"", "# Small parcel fee in in-game currency"})
        public double smallParcelFee = 10.0;

        @Comment({"", "# Medium parcel fee in in-game currency"})
        public double mediumParcelFee = 25.0;

        @Comment({"", "# Large parcel fee in in-game currency"})
        public double largeParcelFee = 50.0;

        @Comment({"", "# How long after collection a parcel can still be returned.", "# Expired collected parcels are purged periodically."})
        public Duration parcelReturnWindow = Duration.ofDays(7);

        @Comment({"", "# Small parcel return fee in in-game currency"})
        public double smallParcelReturnFee = 5.0;

        @Comment({"", "# Medium parcel return fee in in-game currency"})
        public double mediumParcelReturnFee = 12.5;

        @Comment({"", "# Large parcel return fee in in-game currency"})
        public double largeParcelReturnFee = 25.0;

        @Comment({
            "",
            "# Which item attributes must match the original parcel content when a player returns a parcel.",
            "# Material types and total amounts must always match; each flag below relaxes one attribute when set to false."
        })
        public ReturnChecks returnChecks = new ReturnChecks();
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
        public String parcelCollectionGuiTitle = "&2Collect parcels";

        @Comment({ "", "# The item of the small parcel size button" })
        public ConfigItem smallParcelSizeItem = new ConfigItem()
            .name("&2\uD83C\uDF37 &aSmall")
            .lore(List.of("&2» &aClick to select the small parcel size."))
            .type(Material.OAK_CHEST_BOAT);

        @Comment({ "", "# The item of the medium parcel size button" })
        public ConfigItem mediumParcelSizeItem = new ConfigItem()
            .name("&6\uD83C\uDF39 &eMedium")
            .lore(List.of("&6» &eClick to select the medium parcel size."))
            .type(Material.CHEST_MINECART);

        @Comment({ "", "# The item of the large parcel size button" })
        public ConfigItem largeParcelSizeItem = new ConfigItem()
            .name("&4\uD83C\uDFDD &cLarge")
            .lore(List.of("&4» &cClick to select the large parcel size."))
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
            .type(Material.BLUE_STAINED_GLASS_PANE);

        @Comment({ "", "# ----- Admin GUI -----" })
        @Comment("# Title of the admin root menu")
        public String adminGuiTitle = "&4Admin panel";
        public String adminParcelListGuiTitle = "&4Admin: parcels";
        public String adminParcelEditGuiTitle = "&4Admin: edit parcel";
        public String adminLockerListGuiTitle = "&4Admin: lockers";
        public String adminLockerEditGuiTitle = "&4Admin: edit locker";
        public String adminUserListGuiTitle = "&4Admin: users";
        public String adminUserInspectGuiTitle = "&4Admin: user parcels";
        public String adminParcelContentGuiTitle = "&4Admin: edit contents";

        @Comment({ "", "# Admin root menu buttons" })
        public ConfigItem adminParcelsButton = new ConfigItem()
            .type(Material.CHEST)
            .name("&6📦 &eParcels")
            .lore(List.of("&7» &fBrowse and edit every parcel."));

        public ConfigItem adminLockersButton = new ConfigItem()
            .type(Material.ENDER_CHEST)
            .name("&6🔒 &eLockers")
            .lore(List.of("&7» &fManage parcel lockers."));

        public ConfigItem adminUsersButton = new ConfigItem()
            .type(Material.PLAYER_HEAD)
            .name("&6👤 &eUsers")
            .lore(List.of("&7» &fInspect users and their parcels."));

        public ConfigItem adminDeleteAllParcelsButton = new ConfigItem()
            .type(Material.TNT)
            .name("&4⚠ &cDelete ALL parcels")
            .lore(List.of("&c» &7Irreversible. Asks for confirmation."));

        public ConfigItem adminDeleteAllLockersButton = new ConfigItem()
            .type(Material.TNT)
            .name("&4⚠ &cDelete ALL lockers")
            .lore(List.of("&c» &7Irreversible. Asks for confirmation."));

        @Comment({ "", "# Admin parcel edit buttons" })
        public ConfigItem adminEditNameButton = new ConfigItem()
            .type(Material.NAME_TAG)
            .name("&eName: &f{NAME}")
            .lore(List.of("&7» &fClick to edit."));
        public ConfigItem adminEditDescriptionButton = new ConfigItem()
            .type(Material.WRITABLE_BOOK)
            .name("&eDescription")
            .lore(List.of("&f{DESCRIPTION}", "&7» &fClick to edit."));
        public ConfigItem adminEditPriorityButton = new ConfigItem()
            .type(Material.BLAZE_POWDER)
            .name("&ePriority: &f{PRIORITY}")
            .lore(List.of("&7» &fClick to toggle."));
        public ConfigItem adminEditSizeButton = new ConfigItem()
            .type(Material.SHULKER_BOX)
            .name("&eSize: &f{SIZE}")
            .lore(List.of("&7» &fClick to cycle."));
        public ConfigItem adminEditStatusButton = new ConfigItem()
            .type(Material.COMPARATOR)
            .name("&eStatus: &f{STATUS}")
            .lore(List.of("&7» &fClick to cycle."));
        public ConfigItem adminEditReceiverButton = new ConfigItem()
            .type(Material.PLAYER_HEAD)
            .name("&eReceiver: &f{RECEIVER}")
            .lore(List.of("&7» &fClick to choose."));
        public ConfigItem adminEditDestinationButton = new ConfigItem()
            .type(Material.ENDER_CHEST)
            .name("&eDestination: &f{DESTINATION}")
            .lore(List.of("&7» &fClick to choose."));
        public ConfigItem adminEditContentsButton = new ConfigItem()
            .type(Material.CHEST_MINECART)
            .name("&eEdit contents")
            .lore(List.of("&7» &fOpen the item editor."));
        public ConfigItem adminDeleteParcelButton = new ConfigItem()
            .type(Material.LAVA_BUCKET)
            .name("&cDelete parcel")
            .lore(List.of("&c» &7Asks for confirmation."));

        @Comment({ "", "# Admin locker edit buttons" })
        public ConfigItem adminRenameLockerButton = new ConfigItem()
            .type(Material.NAME_TAG)
            .name("&eName: &f{NAME}")
            .lore(List.of("&7» &fClick to rename."));
        public ConfigItem adminTeleportLockerButton = new ConfigItem()
            .type(Material.ENDER_PEARL)
            .name("&eTeleport")
            .lore(List.of("&7» &fGo to this locker."));
        public ConfigItem adminDeleteLockerButton = new ConfigItem()
            .type(Material.LAVA_BUCKET)
            .name("&cDelete locker")
            .lore(List.of("&c» &7Asks for confirmation."));

        @Comment({ "", "# Admin list row items" })
        public ConfigItem adminParcelRowItem = new ConfigItem()
            .type(Material.PAPER)
            .name("&e{NAME}")
            .lore(List.of("&7Status: &f{STATUS}", "&7Size: &f{SIZE}", "&7Priority: &f{PRIORITY}", "&8{UUID}", "&7» &fClick to edit."));
        public ConfigItem adminLockerRowItem = new ConfigItem()
            .type(Material.CHEST)
            .name("&e{NAME}")
            .lore(List.of("&7{POSITION}", "&8{UUID}", "&7» &fClick to manage."));
        public ConfigItem adminUserRowItem = new ConfigItem()
            .type(Material.PLAYER_HEAD)
            .name("&e{NAME}")
            .lore(List.of("&8{UUID}", "&7» &fClick to inspect."));
        public ConfigItem adminToggleSentReceivedButton = new ConfigItem()
            .type(Material.COMPASS)
            .name("&eShowing: &f{MODE}")
            .lore(List.of("&7» &fClick to switch between sent and received."));
        public ConfigItem adminEmptyListItem = new ConfigItem()
            .type(Material.BARRIER)
            .name("&cNothing to show");

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

        @Comment({ "", "# The lore line showing when the parcel will arrive. Placeholders: {DURATION} - time remaining, {DATE} - arrival date" })
        public String parcelArrivingLine = "&6Arriving in: &e{DURATION} &7({DATE})";

        @Comment({ "", "# The item of the parcel item storage button" })
        public ConfigItem parcelStorageItem = new ConfigItem()
            .name("&6\uD83D\uDCBE Parcel storage")
            .lore(List.of("&6» &eClick to add/remove items from this parcel."))
            .type(Material.CHEST);

        @Comment({ "", "# The first line of lore when the parcel item storage contains items."})
        public String parcelStorageItemsSetLine = "  &6› &eCurrent items:";

        @Comment({ "", "# The line of lore containing the item name and amount when the parcel item storage contains items."})
        public String parcelStorageItemLine = "   &6- &e{AMOUNT}x {ITEM}";

        @Comment({ "", "# The item of the parcel name button" })
        public ConfigItem parcelNameItem = new ConfigItem()
            .name("&4✎ &cParcel name")
            .lore(List.of("&4» &cClick to name the parcel."))
            .type(Material.NAME_TAG);

        @Comment({ "", "# The value of the GUI line, when parcel name is set" })
        public String parcelNameSetLine = "&4› &cCurrent parcel name: &e{NAME}";

        @Comment({ "", "# The item of the parcel name button" })
        public ConfigItem parcelDescriptionItem = new ConfigItem()
            .name("&2\uD83D\uDDC9 &aParcel description")
            .lore(List.of("&2» &aClick to add parcel description."))
            .type(Material.OAK_SIGN);

        public String parcelDescriptionSetLine = "&2› &aCurrent parcel description: &2{DESCRIPTION}";

        @Comment({ "", "# The item of the parcel receiver button" })
        public ConfigItem parcelReceiverItem = new ConfigItem()
            .name("&5\uD83E\uDDCD &dParcel receiver")
            .lore(List.of("&5» &dClick to choose the parcel receiver."))
            .type(Material.PLAYER_HEAD);

        @Comment({ "", "# The value of the GUI line, when parcel name is set" })
        public String parcelReceiverGuiSetLine = "&5› &dCurrent parcel receiver: &5{RECEIVER}";

        @Comment({ "", "# The value of the player itemlore line, when parcel receiver is not set" })
        public String parcelReceiverNotSetLine = "&9» &bClick to select.";

        @Comment({ "", "# The value of the player item lore line, when parcel receiver is set" })
        public String parcelReceiverSetLine = "&2✔ &aSelected!";

        @Comment({ "", "# The item of the parcel destination locker button" })
        public ConfigItem parcelDestinationLockerItem = new ConfigItem()
            .name("&9\uD83D\uDEE3 Destination locker")
            .lore(List.of("&9» &3Click to edit the parcel destination locker."))
            .type(Material.END_PORTAL_FRAME);

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
            .lore(List.of("&2» &aClick to confirm the items."))
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
            .lore(List.of("&3» &bClick to select this locker."))
            .type(Material.END_PORTAL_FRAME);

        @Comment({ "", "# The lore of the button in the sending GUI of the parcel destination locker selection GUI" })
        public String parcelDestinationLockerSetLine = "&9› &3Current destination locker: &9{DESCRIPTION}";

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

        @Comment({ "", "# The first line of lore when the parcel contains items in the collection GUI."})
        public String parcelItemsCollectionGui = "&6Items:";

        @Comment({ "", "# The line of lore containing the item name and amount when the parcel contains items in the collection GUI."})
        public String parcelItemCollectionFormat = "&6- <gradient:#f6d14a:#862f51>{AMOUNT}x {ITEM}</gradient>";

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

        @Comment({ "", "# The lore line showing when the parcel has arrived. Placeholders: {DATE} - arrival date" })
        public String parcelArrivedLine = "&aArrived on: &2{DATE}";

        @Comment({ "", "# The title of the parcel return GUI" })
        public String parcelReturnGuiTitle = "&5Return parcels";

        @Comment({ "", "# The title of the return deposit GUI" })
        public String parcelReturnDepositGuiTitle = "&5Deposit the parcel items";

        @Comment({ "", "# The item of the parcel locker return button" })
        public ConfigItem parcelLockerReturnItem = new ConfigItem()
            .name("&5↩ Return parcels")
            .lore(List.of("&5» &dClick to return a collected parcel."))
            .type(Material.HOPPER)
            .glow(true);

        @Comment({ "", "# The item of the parcel in the return GUI" })
        public ConfigItem parcelReturnRowItem = new ConfigItem()
            .name("&d{NAME}")
            .lore(List.of(
                    "&6Sender: &e{SENDER}",
                    "&6Size: &e{SIZE}",
                    "&6Description: &e{DESCRIPTION}"
                )
            )
            .type(Material.CHEST_MINECART);

        @Comment({ "", "# The item displayed in the return GUI when there is nothing to return" })
        public ConfigItem noReturnableParcelsItem = new ConfigItem()
            .name("&4✘ &cNo returnable parcels")
            .lore(List.of("&cYou don't have any parcels to return."))
            .type(Material.STRUCTURE_VOID);

        @Comment({ "", "# The item of the confirm return button" })
        public ConfigItem confirmReturnItem = new ConfigItem()
            .name("&2✔ &aConfirm return")
            .lore(List.of("&2» &aDeposit the original items above, then click to return the parcel."))
            .type(Material.GREEN_DYE);

        @Comment({ "", "# The lore line showing how long the parcel can still be returned. Placeholder: {DURATION}" })
        public String returnWindowRemainingLine = "&5Return window: &d{DURATION} left";

        @Comment({ "", "# The lore line shown when the return window has expired." })
        public String returnWindowExpiredLine = "&cReturn window expired";
    }

    public static class ReturnChecks extends OkaeriConfig {

        @Comment("# Whether durability (damage) must match the original items.")
        public boolean checkDurability = true;

        @Comment("# Whether custom display names must match the original items.")
        public boolean checkItemName = true;

        @Comment("# Whether enchantments must match the original items.")
        public boolean checkEnchantments = true;

        @Comment("# Whether lore must match the original items.")
        public boolean checkLore = true;

        @Comment({"# Whether all remaining item data (NBT) must match the original items.", "# When false, only the attributes enabled above are compared."})
        public boolean checkNbt = true;
    }

    public static class DiscordSettings extends OkaeriConfig {

        @Comment("# Whether Discord integration is enabled.")
        public boolean enabled = true;

        @Comment("# The Discord bot token used by the bot to connect.")
        public String botToken = "";

        @Comment("# The expiration duration of the Discord account linking codes.")
        public Duration linkCodeExpiration = Duration.ofMinutes(2);
    }
}
