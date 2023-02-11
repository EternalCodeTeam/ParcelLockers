package com.eternalcode.parcellockers.configuration;

import net.dzikoysk.cdn.Cdn;
import net.dzikoysk.cdn.CdnFactory;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

public class ConfigurationManager {

    private final Cdn cdn = CdnFactory
            .createYamlLike()
            .getSettings()
            .build();

    private final Set<ReloadableConfig> configs = new HashSet<>();
    private final File dataFolder;

    public ConfigurationManager(File dataFolder) {
        this.dataFolder = dataFolder;
    }

    public <T extends ReloadableConfig> T load(T config) {
        this.cdn.load(config.resource(this.dataFolder), config)
                .orThrow(RuntimeException::new);

        this.cdn.render(config, config.resource(this.dataFolder))
                .orThrow(RuntimeException::new);

        this.configs.add(config);

        return config;
    }

    public void reload() {
        for (ReloadableConfig config : this.configs) {
            this.load(config);
        }
    }
}
