package com.eternalcode.parcellockers.configuration.implementation;

import com.eternalcode.parcellockers.configuration.ReloadableConfig;
import net.dzikoysk.cdn.source.Resource;
import net.dzikoysk.cdn.source.Source;

import java.io.File;

public class PluginConfiguration implements ReloadableConfig {

    @Override
    public Resource resource(File folder) {
        return Source.of(folder, "config.yml");
    }
}
