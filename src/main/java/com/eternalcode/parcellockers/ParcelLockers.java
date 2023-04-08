package com.eternalcode.parcellockers;

import com.eternalcode.parcellockers.command.ParcelCommand;
import com.eternalcode.parcellockers.command.argument.ParcelArgument;
import com.eternalcode.parcellockers.command.handler.InvalidUsage;
import com.eternalcode.parcellockers.command.handler.PermissionMessage;
import com.eternalcode.parcellockers.configuration.ConfigurationManager;
import com.eternalcode.parcellockers.configuration.implementation.PluginConfiguration;
import com.eternalcode.parcellockers.database.JdbcConnectionProvider;
import com.eternalcode.parcellockers.notification.NotificationAnnouncer;
import com.eternalcode.parcellockers.parcel.Parcel;
import com.eternalcode.parcellockers.parcel.ParcelLockerRepositoryJdbcImpl;
import com.eternalcode.parcellockers.parcel.ParcelRepositoryJdbcImpl;
import com.eternalcode.parcellockers.updater.UpdaterService;
import com.eternalcode.parcellockers.user.UserRepositoryJdbcImpl;
import com.eternalcode.parcellockers.util.legacy.LegacyColorProcessor;
import com.google.common.base.Stopwatch;
import dev.rollczi.litecommands.LiteCommands;
import dev.rollczi.litecommands.bukkit.adventure.platform.LiteBukkitAdventurePlatformFactory;
import dev.rollczi.litecommands.bukkit.tools.BukkitOnlyPlayerContextual;
import io.papermc.lib.PaperLib;
import io.papermc.lib.environments.Environment;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bstats.bukkit.Metrics;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public final class ParcelLockers extends JavaPlugin {

    private LiteCommands<CommandSender> liteCommands;

    private BukkitAudiences audiences;
    private MiniMessage miniMessage;
    private NotificationAnnouncer announcer;

    private UpdaterService updater;

    private JdbcConnectionProvider jdbcConnectionProvider;
    private ParcelRepositoryJdbcImpl parcelRepository;
    private ParcelLockerRepositoryJdbcImpl parcelLockerRepository;
    private UserRepositoryJdbcImpl userRepository;

    private ConfigurationManager configManager;
    private PluginConfiguration config;

    @Override
    public void onEnable() {
        Stopwatch started = Stopwatch.createStarted();

        this.softwareCheck();

        this.audiences = BukkitAudiences.create(this);
        this.miniMessage = MiniMessage.builder()
                .postProcessor(new LegacyColorProcessor())
                .build();
        this.announcer = new NotificationAnnouncer(this.audiences, this.miniMessage);

        this.configManager = new ConfigurationManager(this.getDataFolder());
        this.config = this.configManager.load(new PluginConfiguration());

        this.liteCommands = LiteBukkitAdventurePlatformFactory.builder(this.getServer(), "parcellockers", false, this.audiences, true)
                .argument(Parcel.class, new ParcelArgument(this.parcelRepository))
                .contextualBind(Player.class, new BukkitOnlyPlayerContextual<>(this.config.messages.onlyForPlayers))
                .commandInstance(new ParcelCommand(this.announcer, this.config),
                        new ParcelLockerCommand(this.configManager, this.config, this.announcer))
                .invalidUsageHandler(new InvalidUsage(this.announcer, this.config))
                .permissionHandler(new PermissionMessage(this.announcer, this.config))
                .register();

        this.jdbcConnectionProvider = new JdbcConnectionProvider(this.config.settings.databaseUrl, this.config.settings.user, this.config.settings.password);
        this.parcelLockerRepository = ParcelLockerRepositoryJdbcImpl.create(this.jdbcConnectionProvider);
        this.parcelRepository = ParcelRepositoryJdbcImpl.create(this.jdbcConnectionProvider);
        this.userRepository = UserRepositoryJdbcImpl.create(this.jdbcConnectionProvider);

        new Metrics(this, 17677);
        this.updater = new UpdaterService(this.getDescription());

        long millis = started.stop().elapsed(TimeUnit.MILLISECONDS);
        this.getLogger().info("Successfully enabled ParcelLockers in " + millis + "ms");
    }

    @Override
    public void onDisable() {

        if (this.liteCommands != null) {
            this.liteCommands.getPlatform().unregisterAll();
        }

        if (this.audiences != null) {
            this.audiences.close();
        }
    }

    private void softwareCheck() {
        Logger logger = this.getLogger();
        Environment environment = PaperLib.getEnvironment();

        if (!environment.isPaper()) {
            logger.warning("Your server running on unsupported software, please use Paper or its forks");
            logger.warning("You can easily download Paper from https://papermc.io/downloads");
            logger.warning("WARNING: Supported MC version is 1.17-1.19.3");
            return;
        }

        if (!environment.isVersion(17)) {
            logger.warning("ParcelLockers no longer supports your version, be aware that there may be bugs!");
            return;
        }

        logger.info("Your server running on supported software, congratulations!");
        logger.info("Server version: " + this.getServer().getVersion());
    }

    public BukkitAudiences getAudiences() {
        return this.audiences;
    }

    public LiteCommands<CommandSender> getLiteCommands() {
        return this.liteCommands;
    }

    public MiniMessage getMiniMessage() {
        return this.miniMessage;
    }

    public NotificationAnnouncer getAnnouncer() {
        return this.announcer;
    }

    public UpdaterService getUpdater() {
        return this.updater;
    }

    public JdbcConnectionProvider getJdbcConnectionProvider() {
        return this.jdbcConnectionProvider;
    }

    public ConfigurationManager getConfigManager() {
        return this.configManager;
    }

    public PluginConfiguration getPluginConfig() {
        return this.config;
    }

    public ParcelRepositoryJdbcImpl getParcelRepository() {
        return this.parcelRepository;
    }

    public ParcelLockerRepositoryJdbcImpl getParcelLockerRepository() {
        return this.parcelLockerRepository;
    }

    public UserRepositoryJdbcImpl getUserRepository() {
        return this.userRepository;
    }
}


