package com.eternalcode.parcellockers.configuration;

import com.eternalcode.multification.bukkit.notice.resolver.sound.SoundBukkitResolver;
import com.eternalcode.multification.notice.resolver.NoticeResolverDefaults;
import com.eternalcode.multification.notice.resolver.NoticeResolverRegistry;
import com.eternalcode.multification.okaeri.MultificationSerdesPack;
import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.serdes.commons.SerdesCommons;
import eu.okaeri.configs.yaml.bukkit.YamlBukkitConfigurer;
import eu.okaeri.configs.yaml.bukkit.serdes.SerdesBukkit;
import java.io.File;
import java.util.HashSet;
import java.util.Set;

public class ConfigService {

    private final Set<OkaeriConfig> configs = new HashSet<>();

    public <T extends OkaeriConfig> T create(Class<T> config, File file) {
        T configFile = ConfigManager.create(config);

        YamlBukkitConfigurer configurer = new YamlBukkitConfigurer();
        NoticeResolverRegistry noticeRegistry = NoticeResolverDefaults.createRegistry()
            .registerResolver(new SoundBukkitResolver());

        configFile.withConfigurer(configurer,
            new SerdesCommons(),
            new SerdesBukkit(),
            new MultificationSerdesPack(noticeRegistry)
        );

        configFile.withBindFile(file);
        configFile.withRemoveOrphans(true);
        configFile.saveDefaults();
        configFile.load(true);

        this.configs.add(configFile);

        return configFile;
    }

    public void reload() {
        for (OkaeriConfig config : this.configs) {
            config.load();
        }
    }
}
