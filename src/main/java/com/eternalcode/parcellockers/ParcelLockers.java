package com.eternalcode.parcellockers;

import com.eternalcode.parcellockers.command.ParcelCommand;
import com.eternalcode.parcellockers.command.argument.ParcelArgument;
import com.eternalcode.parcellockers.command.handler.InvalidUsage;
import com.eternalcode.parcellockers.command.handler.PermissionMessage;
import com.eternalcode.parcellockers.configuration.ConfigurationManager;
import com.eternalcode.parcellockers.configuration.implementation.PluginConfiguration;
import com.eternalcode.parcellockers.database.JdbcConnectionProvider;
import com.eternalcode.parcellockers.manager.ParcelLockerManager;
import com.eternalcode.parcellockers.manager.ParcelManager;
import com.eternalcode.parcellockers.manager.UserManager;
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


    @Override
    public void onEnable() {
        Stopwatch started = Stopwatch.createStarted();

        this.softwareCheck();

        this.audiences = BukkitAudiences.create(this);
        MiniMessage miniMessage = MiniMessage.builder()
                .postProcessor(new LegacyColorProcessor())
                .build();
        NotificationAnnouncer announcer = new NotificationAnnouncer(this.audiences, miniMessage);

        ConfigurationManager configManager = new ConfigurationManager(this.getDataFolder());
        PluginConfiguration config = configManager.load(new PluginConfiguration());

        JdbcConnectionProvider jdbcConnectionProvider = new JdbcConnectionProvider(config.settings.databaseUrl, config.settings.user, config.settings.password);
        ParcelLockerRepositoryJdbcImpl parcelLockerRepository = ParcelLockerRepositoryJdbcImpl.create(jdbcConnectionProvider);
        ParcelRepositoryJdbcImpl parcelRepository = ParcelRepositoryJdbcImpl.create(jdbcConnectionProvider);
        UserRepositoryJdbcImpl userRepository = UserRepositoryJdbcImpl.create(jdbcConnectionProvider);

        this.liteCommands = LiteBukkitAdventurePlatformFactory.builder(this.getServer(), "parcellockers", false, this.audiences, true)
                .argument(Parcel.class, new ParcelArgument(parcelRepository))
                .contextualBind(Player.class, new BukkitOnlyPlayerContextual<>(config.messages.onlyForPlayers))
                .commandInstance(new ParcelCommand(announcer, config),
                        new ParcelLockerCommand(configManager, config, announcer))
                .invalidUsageHandler(new InvalidUsage(announcer, config))
                .permissionHandler(new PermissionMessage(announcer, config))
                .register();

        ParcelManager parcelManager = new ParcelManager(parcelRepository);
        ParcelLockerManager parcelLockerManager = new ParcelLockerManager(parcelLockerRepository);
        UserManager userManager = new UserManager(userRepository);

        new Metrics(this, 17677);
        UpdaterService updater = new UpdaterService(this.getDescription());

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
}


