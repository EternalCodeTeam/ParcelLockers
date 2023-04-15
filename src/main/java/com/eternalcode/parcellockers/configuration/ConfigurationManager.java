package com.eternalcode.parcellockers.configuration;

import com.eternalcode.parcellockers.configuration.composer.PositionComposer;
import net.dzikoysk.cdn.Cdn;
import net.dzikoysk.cdn.CdnFactory;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

public class ConfigurationManager {

    private static final Cdn CDN = CdnFactory
            .createYamlLike()
            .getSettings()
            .withComposer(PositionComposer.class, new PositionComposer())
            .build();

    private final Set<ReloadableConfig> configs = new HashSet<>();
    private final File dataFolder;

    public ConfigurationManager(File dataFolder) {
        this.dataFolder = dataFolder;
    }

    public <T extends ReloadableConfig> T load(T config) {
        CDN.load(config.resource(this.dataFolder), config)
                .orThrow(RuntimeException::new);

        CDN.render(config, config.resource(this.dataFolder))
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
