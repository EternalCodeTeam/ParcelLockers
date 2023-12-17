package com.eternalcode.parcellockers;

import com.eternalcode.parcellockers.command.argument.PlayerArgument;
import com.eternalcode.parcellockers.command.handler.InvalidUsageImpl;
import com.eternalcode.parcellockers.command.handler.PermissionMessage;
import com.eternalcode.parcellockers.configuration.ConfigurationManager;
import com.eternalcode.parcellockers.configuration.implementation.PluginConfiguration;
import com.eternalcode.parcellockers.database.DataSourceFactory;
import com.eternalcode.parcellockers.itemstorage.repository.ItemStorageRepositoryImpl;
import com.eternalcode.parcellockers.locker.LockerManager;
import com.eternalcode.parcellockers.locker.controller.LockerBreakController;
import com.eternalcode.parcellockers.locker.controller.LockerInteractionController;
import com.eternalcode.parcellockers.locker.controller.LockerPlaceController;
import com.eternalcode.parcellockers.locker.gui.MainGUI;
import com.eternalcode.parcellockers.locker.repository.LockerRepositoryImpl;
import com.eternalcode.parcellockers.notification.NotificationAnnouncer;
import com.eternalcode.parcellockers.parcel.Parcel;
import com.eternalcode.parcellockers.parcel.ParcelManager;
import com.eternalcode.parcellockers.parcel.command.ParcelCommand;
import com.eternalcode.parcellockers.parcel.command.argument.ParcelArgument;
import com.eternalcode.parcellockers.parcel.gui.ParcelListGUI;
import com.eternalcode.parcellockers.parcel.repository.ParcelRepositoryImpl;
import com.eternalcode.parcellockers.updater.UpdaterService;
import com.eternalcode.parcellockers.util.legacy.LegacyColorProcessor;
import com.google.common.base.Stopwatch;
import com.zaxxer.hikari.HikariDataSource;
import dev.rollczi.litecommands.LiteCommands;
import dev.rollczi.litecommands.annotations.LiteCommandsAnnotations;
import dev.rollczi.litecommands.bukkit.LiteBukkitMessages;
import dev.rollczi.litecommands.bukkit.LiteCommandsBukkit;
import io.papermc.lib.PaperLib;
import io.papermc.lib.environments.Environment;
import io.sentry.Sentry;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.milkbowl.vault.economy.Economy;
import org.bstats.bukkit.Metrics;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

public final class ParcelLockers extends JavaPlugin {

    private LiteCommands<CommandSender> liteCommands;

    private BukkitAudiences audiences;

    private Economy economy;

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
        Server server = this.getServer();

        if (config.settings.enableSentry) {
            Sentry.init(options -> {
                this.getLogger().info("Initializing Sentry...");
                options.setDsn("https://1dffb5bec4484aaaaca5fcb4c3157a99@o4505014505177088.ingest.sentry.io/4505019784888320");
                options.setTracesSampleRate(1.0);
                options.setRelease(this.getDescription().getVersion());
                options.setTag("serverVersion", this.getServer().getVersion());
                options.setTag("serverSoftware", PaperLib.getEnvironment().getName());
                options.setTag("plugins", Arrays.stream(server.getPluginManager().getPlugins()).toList().toString());
                this.getLogger().info("Sentry initialized successfully!");
            });
        }

        HikariDataSource dataSource = DataSourceFactory.buildHikariDataSource(config, this.getDataFolder());

        LockerRepositoryImpl parcelLockerRepositoryImpl = new LockerRepositoryImpl(dataSource);
        parcelLockerRepositoryImpl.updatePositionCache();
        ItemStorageRepositoryImpl itemStorageRepository = new ItemStorageRepositoryImpl(dataSource);

        ParcelRepositoryImpl parcelRepository = new ParcelRepositoryImpl(dataSource);

        ParcelManager parcelManager = new ParcelManager(config, announcer, parcelRepository);
        LockerManager lockerManager = new LockerManager(parcelLockerRepositoryImpl);

        MainGUI mainGUI = new MainGUI(this, server, miniMessage, config, parcelRepository, parcelLockerRepositoryImpl);
        ParcelListGUI parcelListGUI = new ParcelListGUI(this, server, miniMessage, config, parcelRepository, parcelLockerRepositoryImpl, mainGUI);
        this.liteCommands = LiteCommandsBukkit.builder("parcellockers", this)
                .argument(Parcel.class, new ParcelArgument(parcelRepository))
                .argument(Player.class, new PlayerArgument(server, config))
                .message(LiteBukkitMessages.PLAYER_ONLY, config.messages.onlyForPlayers)
                .commands(LiteCommandsAnnotations.of(
                    new ParcelCommand(server, parcelLockerRepositoryImpl, announcer, config, mainGUI, parcelListGUI, parcelManager),
                    new ParcelLockersCommand(configManager, config, announcer, miniMessage, itemStorageRepository)
                ))
                .invalidUsage(new InvalidUsageImpl(announcer, config))
                .missingPermission(new PermissionMessage(announcer, config))
                .build();

        if (!this.setupEconomy()) {
            this.getLogger().severe("Disabling due to no Vault dependency found!");
            server.getPluginManager().disablePlugin(this);
            return;
        }

        Stream.of(
            new LockerInteractionController(this, parcelLockerRepositoryImpl, itemStorageRepository, miniMessage, config),
            new LockerPlaceController(config, miniMessage, this, parcelLockerRepositoryImpl, announcer),
            new LockerBreakController(parcelLockerRepositoryImpl, announcer, config.messages)
        ).forEach(controller -> server.getPluginManager().registerEvents(controller, this));

        new Metrics(this, 17677);
        new UpdaterService(this.getDescription());

        long millis = started.stop().elapsed(TimeUnit.MILLISECONDS);
        this.getLogger().log(Level.INFO, "Successfully enabled ParcelLockers in {0}ms", millis);
    }

    @Override
    public void onDisable() {
        if (this.liteCommands != null) {
            this.liteCommands.unregister();
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
            logger.warning("WARNING: Supported MC versions are 1.17.x-1.19.x");
            return;
        }

        if (!environment.isVersion(17)) {
            logger.warning("ParcelLockers no longer supports your version, be aware that there may be bugs!");
            return;
        }

        logger.info("Your server is running on supported software, congratulations!");
        logger.info("Server version: " + this.getServer().getVersion());
    }

    private boolean setupEconomy() {
        if (this.getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }

        RegisteredServiceProvider<Economy> rsp = this.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false; // !!!MAJK!!! Vault is installed but no economy plugin is registered (e.g. EssentialsX)
        }

        this.economy = rsp.getProvider();
        return true;
    }

    public Economy getEconomy() {
        return this.economy;
    }
}


