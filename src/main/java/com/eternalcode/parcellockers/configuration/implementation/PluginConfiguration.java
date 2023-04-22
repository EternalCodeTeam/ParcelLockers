package com.eternalcode.parcellockers.configuration.implementation;

import com.eternalcode.parcellockers.configuration.ReloadableConfig;
import net.dzikoysk.cdn.entity.Contextual;
import net.dzikoysk.cdn.entity.Description;
import net.dzikoysk.cdn.source.Resource;
import net.dzikoysk.cdn.source.Source;
import org.bukkit.Material;

import java.io.File;
import java.util.Collections;
import java.util.List;

public class PluginConfiguration implements ReloadableConfig {

    @Description({" ", "# Do you want to change the plugin settings?"})
    public Settings settings = new Settings();

    @Description({" ", "# Do you want to change the plugin messages?"})
    public Messages messages = new Messages();

    @Description({" ", "# Do you want to change the plugin GUI settings?"})
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
    }

    @Contextual
    public static class Messages {
        public String onlyForPlayers = "&7› &cThis command is only available to players!";
        public String noPermission = "&7› &cYou don't have permission to perform this command! &c(&7{PERMISSION}&c)";
        public String cantFindPlayer = "&7› &cThe specified player could not be found!";
        public String invalidUsage = "&7› &bCorrect usage: &e{USAGE}.";
        public String reload = "&7› &bConfiguration has been successfully reloaded!";
        public String parcelCommandUsage = "&7› &3/parcel &b<list|info> [parcel]";
    }

    @Contextual
    public static class GuiSettings {

        @Description({" ", "# The title of the main GUI"})
        public String mainGuiTitle = "&3Main menu";

        @Description({" ", "# The title of the parcel list GUI"})
        public String parcelListGuiTitle = "&aMy parcels";

        @Description({" ", "# The item of the sent parcels GUI"})
        public String sentParcelsTitle = "&6Sent parcels";

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
                .setType(Material.BUNDLE)
                .setGlow(true);

        @Description({" ", "# The parcel archive item button."})
        public ConfigItem parcelArchiveItem = new ConfigItem()
                .setName("&5Parcel archive")
                .setLore(List.of("&eClick to show all parcels, which you sent or received in the past."))
                .setType(Material.WRITTEN_BOOK);


    }

    @Override
    public Resource resource(File folder) {
        return Source.of(folder, "config.yml");
    }
}
