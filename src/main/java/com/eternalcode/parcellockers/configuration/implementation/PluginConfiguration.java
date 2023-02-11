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

    }

    @Contextual
    public static class Messages {
        public String onlyForPlayers = "&cThis command is only available to players!";
        public String noPermission = "&cYou don't have permission to perform this command!";
        public String cantFindPlayer = "&cThe specified player could not be found!";
        public String invalidUsage = "&7Correct usage: &e{USAGE}.";
        public String reload = "&aConfiguration has been successfully reloaded!";
    }

    @Override
    public Resource resource(File folder) {
        return Source.of(folder, "config.yml");
    }
}
