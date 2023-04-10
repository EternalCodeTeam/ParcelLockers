package com.eternalcode.parcellockers.configuration.implementation;

import com.eternalcode.parcellockers.configuration.ReloadableConfig;
import net.dzikoysk.cdn.entity.Contextual;
import net.dzikoysk.cdn.entity.Description;
import net.dzikoysk.cdn.source.Resource;
import net.dzikoysk.cdn.source.Source;

import java.io.File;

public class PluginConfiguration implements ReloadableConfig {

    @Description("Do you want to change the plugin settings?")
    public Settings settings = new Settings();

    @Description("Do you want to change the plugin messages?")
    public Messages messages = new Messages();

    @Contextual
    public static class Settings {

        @Description({" ", "# Whether the player after entering the server should receive information about the new version of the plugin?"})
        public boolean receiveUpdates = true;

        @Description({" ", "# The URL to the database."})
        public String databaseUrl = "jdbc:mysql://localhost:3306/parcellockers?useSSL=false";

        @Description({" ", "# The database user."})
        public String user = "root";

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

    @Override
    public Resource resource(File folder) {
        return Source.of(folder, "config.yml");
    }
}
