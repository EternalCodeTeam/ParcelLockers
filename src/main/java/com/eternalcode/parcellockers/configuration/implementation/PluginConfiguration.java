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

    @Description({" ", "# Parcel Lockers plugin configuration file."})
    public Settings settings = new Settings();

    @Description({" ", "# The plugin messages."})
    public Messages messages = new Messages();

    @Description({" ", "# The plugin GUI settings."})
    public GuiSettings guiSettings = new GuiSettings();

    @Contextual
    public static class Settings {

        @Description({" ",
                "# Should we enable Sentry integration?",
                "# Sentry is a service that collects errors and other information about the plugin.",
                "# It is used to improve the plugin and fix bugs.",
                "# It is also strongly recommended to enable this option.",
                "# You can learn more about Sentry here: https://sentry.io/"})
        public boolean enableSentry = true;

        @Description({" ", "# Whether the player after entering the server should receive information about the new version of the plugin?"})
        public boolean receiveUpdates = true;

        @Description({" ", "# The database type. (MYSQL, SQLITE)"})
        public DatabaseType databaseType = DatabaseType.MYSQL;

        @Description({" ", "# The URL to the database."})
        public String host = "localhost";

        @Description({" ", "# The database name."})
        public String databaseName = "parcellockers";

        @Description({" ", "# The database user."})
        public String user = "root";

        @Description({" ", "# The database port."})
        public String port = "3306";

        @Description({" ", "# Turn this on to enable SSL (Secure Sockets Layer)"})
        public boolean useSSL = false;

        @Description({" ", "# The database password."})
        public String password = "";

        @Description({" ", "# The parcel locker item."})
        public ConfigItem parcelLockerItem = new ConfigItem()
                .setName("&3Parcel locker")
                .setType(Material.CHEST)
                .setLore(List.of("&bPlace to create a parcel locker."));

        @Description({" ", "# Small parcel cost."})
        public double smallParcelCost = 5.0;

        @Description({" ", "# Medium parcel cost."})
        public double mediumParcelCost = 10.0;

        @Description({" ", "# Large parcel cost."})
        public double largeParcelCost = 15.0;
    }

    @Contextual
    public static class Messages {
        public String onlyForPlayers = "&7» &cThis command is only available to players!";
        public String noPermission = "&7» &cYou don't have permission to perform this command! &c(&7{PERMISSION}&c)";
        public String cantFindPlayer = "&7» &cThe specified player could not be found!";
        public String invalidUsage = "&7» &bCorrect usage: &e{USAGE}";
        public String reload = "&7» &bConfiguration has been successfully reloaded!";
        public String parcelCommandUsage = "&7» &3/parcel &b<list|info|send|cancel> [parcel]";
        public String parcelSuccessfullyCreated = "&7» &aParcel created successfully.";
        public String failedToCreateParcel = "&7» &cAn error occurred while creating the parcel.";
        public String parcelSuccessfullyDeleted = "&7» &aParcel deleted successfully.";
        public String failedToDeleteParcel = "&7» &cAn error occurred while deleting the parcel.";
        public String failedToCreateParcelLocker = "&7» &cCould not create the parcel locker.";
        public String parcelLockerSuccessfullyCreated = "&7» &aParcel locker created successfully.";
        public String enterDescriptionPrompt = "&7» &6Enter a description for the parcel locker:";
        public String cannotBreakParcelLocker = "&7» &cYou have no permission to break the parcel locker.";
        public String parcelLockerSuccessfullyDeleted = "&7» &aParcel locker deleted successfully.";
        public String broadcastParcelLockerRemoved = "&7» &a&lWARNING! &r&4The parcel locker at &c{X} {Y} {Z} in world {WORLD} &4has been removed by &4{PLAYER}!";
        @Description({" ", "# The parcel info message."})
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

        @Description({" ", "# The title of the main GUI"})
        public String mainGuiTitle = "&3Main menu";

        @Description({" ", "# The title of the parcel list GUI"})
        public String parcelListGuiTitle = "&aMy parcels";

        @Description({" ", "# The item of the sent parcels GUI"})
        public String sentParcelsTitle = "&6Sent parcels";

        @Description({" ", "# The item of the parcel locker main GUI"})
        public String parcelLockerMainGuiTitle = "&3Parcel locker";

        @Description({" ", "# The item of the parcel locker sending GUI"})
        public String parcelLockerSendingGuiTitle = "&3Parcel sending";

        @Description({" ", "# The item of the small parcel size button"})
        public ConfigItem smallParcelSizeItem = new ConfigItem()
                .setName("&aSmall")
                .setLore(List.of("&bClick to select the small parcel size."))
                .setType(Material.LIME_STAINED_GLASS);

        @Description({" ", "# The item of the medium parcel size button"})
        public ConfigItem mediumParcelSizeItem = new ConfigItem()
                .setName("&eMedium")
                .setLore(List.of("&bClick to select the medium parcel size."))
                .setType(Material.YELLOW_STAINED_GLASS);

        @Description({" ", "# The item of the large parcel size button"})
        public ConfigItem largeParcelSizeItem = new ConfigItem()
                .setName("&cLarge")
                .setLore(List.of("&bClick to select the large parcel size."))
                .setType(Material.RED_STAINED_GLASS);

        @Description({" ", "# The item of the priority button"})
        public ConfigItem priorityItem = new ConfigItem()
                .setName("&aPriority")
                .setLore(List.of("&bClick to select the priority."))
                .setType(Material.REDSTONE);

        @Description({" ", "# The close button item"})
        public ConfigItem closeItem = new ConfigItem()
                .setName("&cClose")
                .setLore(List.of("&cClick to close the GUI."))
                .setType(Material.BARRIER);

        @Description({" ", "# The item of the main GUI"})
        public ConfigItem mainGuiBackgroundItem = new ConfigItem()
                .setName("")
                .setLore(Collections.emptyList())
                .setType(Material.GRAY_STAINED_GLASS_PANE);

        @Description({" ", "# The item of the corner GUI item.", "# Purely for decoration purposes."})
        public ConfigItem cornerItem = new ConfigItem()
                .setName("")
                .setLore(Collections.emptyList())
                .setType(Material.BLUE_STAINED_GLASS_PANE);

        @Description({" ", "# The item of the parcel list button"})
        public ConfigItem myParcelsItem = new ConfigItem()
                .setName("&3My parcels")
                .setLore(List.of("&bClick to open your parcels."))
                .setType(Material.ENDER_CHEST);

        @Description({" ", "# The item of the sent parcels button"})
        public ConfigItem sentParcelsItem = new ConfigItem()
                .setName("&6Sent parcels")
                .setLore(List.of("&eClick to show parcels, which you sent.", "&eYou can also cancel them here, if you want to."))
                .setType(Material.FILLED_MAP)
                .setGlow(true);

        @Description({" ", "# The parcel archive item button."})
        public ConfigItem parcelArchiveItem = new ConfigItem()
                .setName("&5Parcel archive")
                .setLore(List.of("&eClick to show all parcels, which you sent or received in the past."))
                .setType(Material.WRITTEN_BOOK);

        @Description({" ", "# The item of the parcel locker collect button"})
        public ConfigItem parcelLockerCollectItem = new ConfigItem()
                .setName("&aCollect parcels")
                .setLore(List.of("&aClick to collect your parcels."))
                .setType(Material.HOPPER)
                .setGlow(true);

        @Description({" ", "# The item of the parcel locker send button"})
        public ConfigItem parcelLockerSendItem = new ConfigItem()
                .setName("&bSend parcels")
                .setLore(List.of("&bClick to send parcels."))
                .setType(Material.SCULK_SHRIEKER)
                .setGlow(true);

        @Description({" ", "# The item of the parcel locker status button"})
        public ConfigItem parcelLockerStatusItem = new ConfigItem()
                .setName("&3Parcel locker status")
                .setLore(List.of("&bClick to show the status of your parcel locker."))
                .setType(Material.END_PORTAL_FRAME)
                .setGlow(true);

        @Description({" ", "# The item of the parcel"})
        public ConfigItem parcelItem = new ConfigItem()
                .setName("&6{NAME}")
                .setLore(List.of(
                        "&6UUID: &e{UUID}",
                        "&Sender: &e{SENDER}",
                        "&6Receiver: &e{RECEIVER}",
                        "&6Size: &e{SIZE}",
                        "&6Position: &6X: &e{POSITION_X}, &6Y: &e{POSITION_Y}, &6Z: &e{POSITION_Z}",
                        "&6Priority: &e{PRIORITY}",
                        "&6Description: &e{DESCRIPTION}",
                        "&6Recipients: &e{RECIPIENTS}"
                        )
                )
                .setType(Material.CHEST_MINECART);

        @Description({" ", "# The item of the parcel item storage button"})
        public ConfigItem parcelStorageItem = new ConfigItem()
            .setName("&6Parcel storage")
            .setLore(List.of("&eClick to edit the parcel content."))
            .setType(Material.CHEST);

        @Description({" ", "# The item of the previous page button"})
        public ConfigItem previousPageItem = new ConfigItem()
                .setName("&bPrevious page")
                .setLore(List.of("&bClick to go to the previous page."))
                .setType(Material.ARROW);

        @Description({" ", "# The item of the next page button"})
        public ConfigItem nextPageItem = new ConfigItem()
                .setName("&bNext page")
                .setLore(List.of("&bClick to go to the next page."))
                .setType(Material.ARROW);

        @Description({" ", "# The item of the confirm items button"})
        public ConfigItem confirmItemsItem = new ConfigItem()
                .setName("&aConfirm items")
                .setLore(List.of("&aClick to confirm the items."))
                .setType(Material.LIME_STAINED_GLASS_PANE);

        @Description({" ", "# The item of the cancel items button"})
        public ConfigItem cancelItemsItem = new ConfigItem()
                .setName("&cCancel items")
                .setLore(List.of("&cClick to cancel the items."))
                .setType(Material.RED_STAINED_GLASS_PANE);

        @Description({" ", "# The name of the parcel small content GUI"})
        public String parcelSmallContentGuiTitle = "&aSmall parcel content";

        @Description({" ", "# The name of the parcel medium content GUI"})
        public String parcelMediumContentGuiTitle = "&eMedium parcel content";

        @Description({" ", "# The name of the parcel large content GUI"})
        public String parcelLargeContentGuiTitle = "&cLarge parcel content";
    }

    @Override
    public Resource resource(File folder) {
        return Source.of(folder, "config.yml");
    }
}
