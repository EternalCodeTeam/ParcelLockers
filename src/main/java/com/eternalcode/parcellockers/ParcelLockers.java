package com.eternalcode.parcellockers;

import com.eternalcode.commons.adventure.AdventureLegacyColorPostProcessor;
import com.eternalcode.commons.adventure.AdventureLegacyColorPreProcessor;
import com.eternalcode.commons.bukkit.scheduler.BukkitSchedulerImpl;
import com.eternalcode.commons.scheduler.Scheduler;
import com.eternalcode.parcellockers.command.debug.DebugCommand;
import com.eternalcode.parcellockers.command.handler.InvalidUsageImpl;
import com.eternalcode.parcellockers.command.handler.PermissionMessage;
import com.eternalcode.parcellockers.configuration.ConfigService;
import com.eternalcode.parcellockers.configuration.implementation.MessagesConfig;
import com.eternalcode.parcellockers.configuration.implementation.PluginConfig;
import com.eternalcode.parcellockers.content.repository.ParcelContentRepository;
import com.eternalcode.parcellockers.content.repository.ParcelContentRepositoryOrmLite;
import com.eternalcode.parcellockers.database.DatabaseManager;
import com.eternalcode.parcellockers.delivery.repository.DeliveryRepositoryOrmLite;
import com.eternalcode.parcellockers.gui.implementation.locker.LockerMainGui;
import com.eternalcode.parcellockers.gui.implementation.remote.MainGui;
import com.eternalcode.parcellockers.gui.implementation.remote.ParcelListGui;
import com.eternalcode.parcellockers.itemstorage.repository.ItemStorageRepository;
import com.eternalcode.parcellockers.itemstorage.repository.ItemStorageRepositoryOrmLite;
import com.eternalcode.parcellockers.locker.Locker;
import com.eternalcode.parcellockers.locker.argument.ParcelLockerArgument;
import com.eternalcode.parcellockers.locker.controller.LockerBreakController;
import com.eternalcode.parcellockers.locker.controller.LockerInteractionController;
import com.eternalcode.parcellockers.locker.controller.LockerPlaceController;
import com.eternalcode.parcellockers.locker.repository.LockerCache;
import com.eternalcode.parcellockers.locker.repository.LockerRepositoryOrmLite;
import com.eternalcode.parcellockers.notification.NoticeService;
import com.eternalcode.parcellockers.parcel.Parcel;
import com.eternalcode.parcellockers.parcel.ParcelService;
import com.eternalcode.parcellockers.parcel.ParcelServiceImpl;
import com.eternalcode.parcellockers.parcel.ParcelStatus;
import com.eternalcode.parcellockers.parcel.command.ParcelCommand;
import com.eternalcode.parcellockers.parcel.command.argument.ParcelArgument;
import com.eternalcode.parcellockers.parcel.repository.ParcelCache;
import com.eternalcode.parcellockers.parcel.repository.ParcelRepositoryOrmLite;
import com.eternalcode.parcellockers.parcel.task.ParcelSendTask;
import com.eternalcode.parcellockers.updater.UpdaterService;
import com.eternalcode.parcellockers.user.UserManager;
import com.eternalcode.parcellockers.user.UserManagerImpl;
import com.eternalcode.parcellockers.user.controller.LoadUserController;
import com.eternalcode.parcellockers.user.controller.PrepareUserController;
import com.eternalcode.parcellockers.user.repository.UserRepository;
import com.eternalcode.parcellockers.user.repository.UserRepositoryOrmLite;
import com.google.common.base.Stopwatch;
import dev.rollczi.litecommands.LiteCommands;
import dev.rollczi.litecommands.adventure.LiteAdventureExtension;
import dev.rollczi.litecommands.annotations.LiteCommandsAnnotations;
import dev.rollczi.litecommands.bukkit.LiteBukkitFactory;
import dev.rollczi.litecommands.bukkit.LiteBukkitMessages;
import dev.rollczi.liteskullapi.LiteSkullFactory;
import dev.rollczi.liteskullapi.SkullAPI;
import io.papermc.lib.PaperLib;
import io.papermc.lib.environments.Environment;
import java.io.File;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bstats.bukkit.Metrics;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public final class ParcelLockers extends JavaPlugin {

    private LiteCommands<CommandSender> liteCommands;
    private BukkitAudiences audiences;
    private SkullAPI skullAPI;

    private DatabaseManager databaseManager;

    @Override
    public void onEnable() {
        Stopwatch started = Stopwatch.createStarted();

        this.softwareCheck();

        this.audiences = BukkitAudiences.create(this);
        MiniMessage miniMessage = MiniMessage.builder()
            .preProcessor(new AdventureLegacyColorPreProcessor())
            .postProcessor(new AdventureLegacyColorPostProcessor())
            .build();

        ConfigService configManager = new ConfigService();
        PluginConfig config = configManager.create(PluginConfig.class, new File(this.getDataFolder(), "config.yml"));
        MessagesConfig messagesConfig = configManager.create(MessagesConfig.class, new File(this.getDataFolder(), "messages.yml"));
        Server server = this.getServer();
        NoticeService noticeService = new NoticeService(messagesConfig, miniMessage, this.audiences);
        Scheduler scheduler = new BukkitSchedulerImpl(this);

        DatabaseManager databaseManager = new DatabaseManager(config, this.getLogger(), this.getDataFolder());
        this.databaseManager = databaseManager;

        try {
            databaseManager.connect();
        } catch (SQLException exception) {
            this.getLogger().severe("Could not connect to database! Some functions may not work properly!");
            throw new RuntimeException(exception);
        }

        this.skullAPI = LiteSkullFactory.builder()
            .cacheExpireAfterWrite(Duration.ofMinutes(45L))
            .bukkitScheduler(this)
            .threadPool(20)
            .build();

        LockerCache lockerCache = new LockerCache();
        ParcelCache parcelCache = new ParcelCache();

        LockerRepositoryOrmLite lockerRepository = new LockerRepositoryOrmLite(databaseManager, scheduler, lockerCache);
        lockerRepository.updateCaches();

        ParcelRepositoryOrmLite parcelRepository = new ParcelRepositoryOrmLite(databaseManager, scheduler, parcelCache);
        parcelRepository.updateCaches();

        DeliveryRepositoryOrmLite deliveryRepository = new DeliveryRepositoryOrmLite(databaseManager, scheduler);

        ParcelContentRepository parcelContentRepository = new ParcelContentRepositoryOrmLite(databaseManager, scheduler);
        ParcelService parcelService = new ParcelServiceImpl(config, announcer, parcelRepository, deliveryRepository, parcelContentRepository, scheduler);

        ItemStorageRepository itemStorageRepository = new ItemStorageRepositoryOrmLite(databaseManager, scheduler);

        UserRepository userRepository = new UserRepositoryOrmLite(databaseManager, scheduler);
        UserManager userManager = new UserManagerImpl(userRepository);

        MainGui mainGUI = new MainGui(this, server, miniMessage, config, parcelRepository, lockerRepository,
            userManager);
        ParcelListGui parcelListGUI = new ParcelListGui(this, miniMessage, config, parcelRepository, lockerRepository,
            userManager, mainGUI);

        this.liteCommands = LiteBukkitFactory.builder(this.getName(), this)
            .argument(Parcel.class, new ParcelArgument(parcelCache))
            .argument(Locker.class, new ParcelLockerArgument(lockerCache))
            .extension(new LiteAdventureExtension<>())
            .message(LiteBukkitMessages.PLAYER_ONLY, config.messages.onlyForPlayers)
            .message(LiteBukkitMessages.PLAYER_NOT_FOUND, config.messages.cantFindPlayer)
            .commands(LiteCommandsAnnotations.of(
                new ParcelCommand(lockerRepository, announcer, config, mainGUI, parcelListGUI,
                    parcelService, userManager),
                new ParcelLockersCommand(configManager, config, announcer),
                new DebugCommand(parcelRepository, lockerRepository, itemStorageRepository, parcelContentRepository, announcer)
            ))
            .invalidUsage(new InvalidUsageImpl(announcer, config))
            .missingPermission(new PermissionMessage(announcer, config))
            .build();

        LockerMainGui lockerMainGUI = new LockerMainGui(this, miniMessage, config, itemStorageRepository, parcelRepository, lockerRepository, announcer, parcelContentRepository, userRepository, this.skullAPI,
            parcelService);

        Stream.of(
            new LockerInteractionController(lockerCache, lockerMainGUI),
            new LockerPlaceController(config, this, lockerRepository, announcer),
            new LockerBreakController(lockerRepository, lockerCache, announcer, config.messages),
            new PrepareUserController(userManager),
            new LoadUserController(userManager, server)
        ).forEach(controller -> server.getPluginManager().registerEvents(controller, this));

        new Metrics(this, 17677);
        new UpdaterService(this.getDescription());

        parcelRepository.findAll().thenAccept(optionalParcels -> {
            List<Parcel> parcels = optionalParcels.orElseGet(ArrayList::new).stream()
                .filter(parcel -> parcel.status() != ParcelStatus.DELIVERED)
                .toList();

            parcels.forEach(parcel ->
                deliveryRepository.find(parcel.uuid()).thenAccept(optionalDelivery ->
                    optionalDelivery.ifPresent(delivery -> {
                        long delay = Math.max(0, delivery.deliveryTimestamp().toEpochMilli() - System.currentTimeMillis());
                        scheduler.runLaterAsync(
                            new ParcelSendTask(parcel, delivery, parcelRepository, deliveryRepository, config),
                            Duration.ofMillis(delay)
                        );
                    })
                )
            );
        });

        long millis = started.elapsed(TimeUnit.MILLISECONDS);
        this.getLogger().log(Level.INFO, "Successfully enabled ParcelLockers in {0}ms", millis);
    }

    @Override
    public void onDisable() {
        if (this.databaseManager != null) {
            this.databaseManager.disconnect();
        }

        if (this.liteCommands != null) {
            this.liteCommands.unregister();
        }

        if (this.audiences != null) {
            this.audiences.close();
        }

        this.skullAPI.shutdown();
    }

    private void softwareCheck() {
        Logger logger = this.getLogger();
        Environment environment = PaperLib.getEnvironment();

        if (!environment.isPaper()) {
            logger.warning("Your server running on unsupported software, please use Paper or its forks");
            logger.warning("You can easily download Paper from https://papermc.io/downloads");
        }
    }
}


