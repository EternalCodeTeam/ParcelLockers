package com.eternalcode.parcellockers;

import com.eternalcode.parcellockers.command.DebugCommand;
import com.eternalcode.parcellockers.command.ParcelCommand;
import com.eternalcode.parcellockers.command.argument.ParcelArgument;
import com.eternalcode.parcellockers.command.argument.PlayerArgument;
import com.eternalcode.parcellockers.command.handler.InvalidUsage;
import com.eternalcode.parcellockers.command.handler.PermissionMessage;
import com.eternalcode.parcellockers.configuration.ConfigurationManager;
import com.eternalcode.parcellockers.configuration.implementation.PluginConfiguration;
import com.eternalcode.parcellockers.controller.ParcelLockerBreakController;
import com.eternalcode.parcellockers.controller.ParcelLockerInteractionController;
import com.eternalcode.parcellockers.controller.ParcelLockerPlaceController;
import com.eternalcode.parcellockers.database.DataSourceFactory;
import com.eternalcode.parcellockers.database.ParcelDatabaseService;
import com.eternalcode.parcellockers.database.ParcelLockerDatabaseService;
import com.eternalcode.parcellockers.gui.MainGUI;
import com.eternalcode.parcellockers.gui.ParcelListGUI;
import com.eternalcode.parcellockers.notification.NotificationAnnouncer;
import com.eternalcode.parcellockers.parcel.Parcel;
import com.eternalcode.parcellockers.parcel.ParcelManager;
import com.eternalcode.parcellockers.parcellocker.ParcelLockerManager;
import com.eternalcode.parcellockers.updater.UpdaterService;
import com.eternalcode.parcellockers.util.legacy.LegacyColorProcessor;
import com.google.common.base.Stopwatch;
import com.zaxxer.hikari.HikariDataSource;
import dev.rollczi.litecommands.LiteCommands;
import dev.rollczi.litecommands.bukkit.adventure.platform.LiteBukkitAdventurePlatformFactory;
import dev.rollczi.litecommands.bukkit.tools.BukkitOnlyPlayerContextual;
import io.papermc.lib.PaperLib;
import io.papermc.lib.environments.Environment;
import io.sentry.Sentry;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.milkbowl.vault.economy.Economy;
import org.bstats.bukkit.Metrics;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
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
        if (config.settings.enableSentry) {
            Sentry.init(options -> {
                this.getLogger().info("Initializing Sentry...");
                options.setDsn("https://1dffb5bec4484aaaaca5fcb4c3157a99@o4505014505177088.ingest.sentry.io/4505019784888320");
                options.setTracesSampleRate(1.0);
                options.setRelease(this.getDescription().getVersion());
                options.setTag("serverVersion", this.getServer().getVersion());
                options.setTag("serverSoftware", PaperLib.getEnvironment().getName());
                options.setTag("plugins", Arrays.stream(this.getServer().getPluginManager().getPlugins()).toList().toString());
                this.getLogger().info("Sentry initialized successfully!");
            });
        }

        HikariDataSource dataSource = DataSourceFactory.buildHikariDataSource(config, this.getDataFolder());

        ParcelLockerDatabaseService parcelLockerDatabaseService = new ParcelLockerDatabaseService(dataSource);
        ParcelDatabaseService parcelRepository = new ParcelDatabaseService(dataSource);

        ParcelManager parcelManager = new ParcelManager(this, config, announcer, parcelRepository, parcelLockerDatabaseService);
        ParcelLockerManager parcelLockerManager = new ParcelLockerManager(parcelLockerDatabaseService);

        MainGUI mainGUI = new MainGUI(this, this.getServer(), miniMessage, config, parcelRepository, parcelLockerDatabaseService);
        ParcelListGUI parcelListGUI = new ParcelListGUI(this, miniMessage, config, parcelRepository, parcelLockerDatabaseService, mainGUI);
        this.liteCommands = LiteBukkitAdventurePlatformFactory.builder(this.getServer(), "parcellockers", false, this.audiences, true)
                .argument(Parcel.class, new ParcelArgument(parcelRepository))
                .argument(Player.class, new PlayerArgument(this.getServer(), config))
                .contextualBind(Player.class, new BukkitOnlyPlayerContextual<>(config.messages.onlyForPlayers))
                .commandInstance(
                        new ParcelCommand(this.getServer(), parcelLockerDatabaseService, announcer, config, mainGUI, parcelListGUI, parcelManager),
                        new ParcelLockerCommand(configManager, config, announcer, miniMessage),
                        new DebugCommand(parcelLockerDatabaseService)
                )
                .invalidUsageHandler(new InvalidUsage(announcer, config))
                .permissionHandler(new PermissionMessage(announcer, config))
                .register();

//        if (!this.setupEconomy()) {
//            this.getLogger().severe("Disabling due to no Vault dependency found!");
//            this.getServer().getPluginManager().disablePlugin(this);
//            return;
//        }

        Stream.of(
            new ParcelLockerInteractionController(parcelLockerDatabaseService, miniMessage, config),
            new ParcelLockerPlaceController(config, miniMessage, this, parcelLockerDatabaseService, announcer),
            new ParcelLockerBreakController(parcelLockerDatabaseService, announcer, config.messages)
        ).forEach(controller -> this.getServer().getPluginManager().registerEvents(controller, this));

        new Metrics(this, 17677);
        new UpdaterService(this.getDescription());
        parcelLockerDatabaseService.updatePositionCache();

        long millis = started.stop().elapsed(TimeUnit.MILLISECONDS);
        this.getLogger().log(Level.INFO, "Successfully enabled ParcelLockers in {0}ms", millis);
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

//    private boolean setupEconomy() {
//        if (this.getServer().getPluginManager().getPlugin("Vault") == null) {
//            return false;
//        }
//        RegisteredServiceProvider<Economy> rsp = this.getServer().getServicesManager().getRegistration(Economy.class);
//        if (rsp == null) {
//            return false;
//        }
//        this.economy = rsp.getProvider();
//        return true;
//    }

    public Economy getEconomy() {
        return this.economy;
    }
}


