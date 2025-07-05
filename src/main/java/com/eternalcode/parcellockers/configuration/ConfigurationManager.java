package com.eternalcode.parcellockers.configuration;

import com.eternalcode.parcellockers.configuration.composer.DurationComposer;
import com.eternalcode.parcellockers.configuration.composer.PositionComposer;
import com.eternalcode.parcellockers.shared.Position;
import net.dzikoysk.cdn.Cdn;
import net.dzikoysk.cdn.CdnFactory;

import java.io.File;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

public class ConfigurationManager {

    private static final Cdn CDN = CdnFactory
        .createYamlLike()
        .getSettings()
        .withComposer(Position.class, new PositionComposer())
        .withComposer(Duration.class, new DurationComposer())
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
