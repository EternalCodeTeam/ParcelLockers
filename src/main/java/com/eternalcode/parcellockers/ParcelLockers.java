package com.eternalcode.parcellockers;

import com.eternalcode.commons.adventure.AdventureLegacyColorPostProcessor;
import com.eternalcode.commons.adventure.AdventureLegacyColorPreProcessor;
import com.eternalcode.commons.bukkit.scheduler.BukkitSchedulerImpl;
import com.eternalcode.commons.scheduler.Scheduler;
import com.eternalcode.parcellockers.command.debug.DebugCommand;
import com.eternalcode.parcellockers.command.handler.InvalidUsageHandlerImpl;
import com.eternalcode.parcellockers.command.handler.MissingPermissionsHandlerImpl;
import com.eternalcode.parcellockers.configuration.ConfigService;
import com.eternalcode.parcellockers.configuration.implementation.MessageConfig;
import com.eternalcode.parcellockers.configuration.implementation.PluginConfig;
import com.eternalcode.parcellockers.configuration.implementation.PluginConfig.DiscordSettings;
import com.eternalcode.parcellockers.content.ParcelContentManager;
import com.eternalcode.parcellockers.content.repository.ParcelContentRepository;
import com.eternalcode.parcellockers.content.repository.ParcelContentRepositoryOrmLite;
import com.eternalcode.parcellockers.database.DatabaseManager;
import com.eternalcode.parcellockers.delivery.DeliveryManager;
import com.eternalcode.parcellockers.delivery.repository.DeliveryRepositoryOrmLite;
import com.eternalcode.parcellockers.discord.DiscordClientManager;
import com.eternalcode.parcellockers.discord.DiscordFallbackLinkService;
import com.eternalcode.parcellockers.discord.DiscordLinkService;
import com.eternalcode.parcellockers.discord.DiscordSrvLinkService;
import com.eternalcode.parcellockers.discord.argument.SnowflakeArgument;
import com.eternalcode.parcellockers.discord.command.DiscordLinkCommand;
import com.eternalcode.parcellockers.discord.command.DiscordSrvLinkCommand;
import com.eternalcode.parcellockers.discord.command.DiscordSrvUnlinkCommand;
import com.eternalcode.parcellockers.discord.command.DiscordUnlinkCommand;
import com.eternalcode.parcellockers.discord.controller.DiscordDeliverNotificationController;
import com.eternalcode.parcellockers.discord.notification.Discord4JNotificationService;
import com.eternalcode.parcellockers.discord.notification.DiscordNotificationService;
import com.eternalcode.parcellockers.discord.notification.DiscordSrvNotificationService;
import com.eternalcode.parcellockers.discord.repository.DiscordLinkRepository;
import com.eternalcode.parcellockers.discord.repository.DiscordLinkRepositoryOrmLite;
import com.eternalcode.parcellockers.discord.verification.DiscordLinkValidationService;
import com.eternalcode.parcellockers.discord.verification.DiscordVerificationService;
import com.eternalcode.parcellockers.gui.GuiManager;
import com.eternalcode.parcellockers.gui.implementation.locker.LockerGui;
import com.eternalcode.parcellockers.gui.implementation.remote.MainGui;
import com.eternalcode.parcellockers.itemstorage.ItemStorageManager;
import com.eternalcode.parcellockers.itemstorage.repository.ItemStorageRepository;
import com.eternalcode.parcellockers.itemstorage.repository.ItemStorageRepositoryOrmLite;
import com.eternalcode.parcellockers.locker.LockerManager;
import com.eternalcode.parcellockers.locker.controller.LockerBreakController;
import com.eternalcode.parcellockers.locker.controller.LockerInteractionController;
import com.eternalcode.parcellockers.locker.controller.LockerPlaceController;
import com.eternalcode.parcellockers.locker.repository.LockerRepositoryOrmLite;
import com.eternalcode.parcellockers.locker.validation.LockerValidationService;
import com.eternalcode.parcellockers.locker.validation.LockerValidator;
import com.eternalcode.parcellockers.notification.NoticeService;
import com.eternalcode.parcellockers.parcel.ParcelStatus;
import com.eternalcode.parcellockers.parcel.command.ParcelCommand;
import com.eternalcode.parcellockers.parcel.repository.ParcelRepositoryOrmLite;
import com.eternalcode.parcellockers.parcel.service.ParcelDispatchService;
import com.eternalcode.parcellockers.parcel.service.ParcelService;
import com.eternalcode.parcellockers.parcel.service.ParcelServiceImpl;
import com.eternalcode.parcellockers.parcel.task.ParcelSendTask;
import com.eternalcode.parcellockers.updater.UpdaterService;
import com.eternalcode.parcellockers.user.UserManager;
import com.eternalcode.parcellockers.user.UserManagerImpl;
import com.eternalcode.parcellockers.user.controller.LoadUserController;
import com.eternalcode.parcellockers.user.controller.PrepareUserController;
import com.eternalcode.parcellockers.user.repository.UserRepository;
import com.eternalcode.parcellockers.user.repository.UserRepositoryOrmLite;
import com.eternalcode.parcellockers.user.validation.UserValidationService;
import com.eternalcode.parcellockers.user.validation.UserValidator;
import dev.rollczi.litecommands.LiteCommands;
import dev.rollczi.litecommands.adventure.LiteAdventureExtension;
import dev.rollczi.litecommands.annotations.LiteCommandsAnnotations;
import dev.rollczi.litecommands.bukkit.LiteBukkitFactory;
import dev.rollczi.litecommands.bukkit.LiteBukkitMessages;
import dev.rollczi.liteskullapi.LiteSkullFactory;
import dev.rollczi.liteskullapi.SkullAPI;
import dev.triumphteam.gui.TriumphGui;
import discord4j.common.util.Snowflake;
import java.io.File;
import java.sql.SQLException;
import java.time.Duration;
import java.util.stream.Stream;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.milkbowl.vault.economy.Economy;
import org.bstats.bukkit.Metrics;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class ParcelLockers extends JavaPlugin {

    private LiteCommands<CommandSender> liteCommands;
    private SkullAPI skullAPI;
    private DatabaseManager databaseManager;
    private Economy economy;
    private DiscordClientManager discordClientManager;

    @Override
    public void onEnable() {
        MiniMessage miniMessage = MiniMessage.builder()
            .preProcessor(new AdventureLegacyColorPreProcessor())
            .postProcessor(new AdventureLegacyColorPostProcessor())
            .build();

        ConfigService configService = new ConfigService();
        PluginConfig config = configService.create(PluginConfig.class, new File(this.getDataFolder(), "config.yml"));
        MessageConfig messageConfig =
            configService.create(MessageConfig.class, new File(this.getDataFolder(), "messages.yml"));
        Server server = this.getServer();
        NoticeService noticeService = new NoticeService(messageConfig, miniMessage);
        Scheduler scheduler = new BukkitSchedulerImpl(this);

        if (!this.setupEconomy()) {
            this.getLogger().severe("No economy provider registered! Disabling...");
            server.getPluginManager().disablePlugin(this);
            return;
        }

        DatabaseManager databaseManager = new DatabaseManager(config, this.getLogger(), this.getDataFolder());
        this.databaseManager = databaseManager;

        try {
            databaseManager.connect();
        } catch (SQLException exception) {
            this.getLogger().severe("Could not connect to database! Disabling..." + exception.getMessage());
            this.getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.skullAPI = LiteSkullFactory.builder()
            .cacheExpireAfterWrite(Duration.ofMinutes(45L))
            .bukkitScheduler(this)
            .threadPool(20)
            .build();

        // database repositories
        ParcelRepositoryOrmLite parcelRepository = new ParcelRepositoryOrmLite(databaseManager, scheduler);
        LockerRepositoryOrmLite lockerRepository = new LockerRepositoryOrmLite(databaseManager, scheduler);
        ParcelContentRepository parcelContentRepository =
            new ParcelContentRepositoryOrmLite(databaseManager, scheduler);
        DeliveryRepositoryOrmLite deliveryRepository = new DeliveryRepositoryOrmLite(databaseManager, scheduler);
        ItemStorageRepository itemStorageRepository = new ItemStorageRepositoryOrmLite(databaseManager, scheduler);
        UserRepository userRepository = new UserRepositoryOrmLite(databaseManager, scheduler);

        // service and managers
        ParcelService parcelService = new ParcelServiceImpl(
            noticeService,
            parcelRepository,
            parcelContentRepository,
            scheduler,
            config,
            this.economy,
            server
        );

        UserValidationService userValidationService = new UserValidator();
        UserManager userManager = new UserManagerImpl(userRepository, userValidationService, server);
        LockerValidationService lockerValidationService = new LockerValidator();
        LockerManager lockerManager =
            new LockerManager(config, lockerRepository, lockerValidationService, parcelRepository, server);
        ParcelContentManager parcelContentManager = new ParcelContentManager(parcelContentRepository);
        ItemStorageManager itemStorageManager = new ItemStorageManager(itemStorageRepository, server);
        DeliveryManager deliveryManager = new DeliveryManager(deliveryRepository);

        ParcelDispatchService parcelDispatchService = new ParcelDispatchService(
            lockerManager,
            parcelService,
            deliveryManager,
            itemStorageManager,
            scheduler,
            config,
            noticeService
        );

        // guis
        TriumphGui.init(this);
        GuiManager guiManager = new GuiManager(
            parcelService,
            lockerManager,
            userManager,
            itemStorageManager,
            parcelDispatchService,
            parcelContentManager,
            deliveryManager
        );

        MainGui mainGUI = new MainGui(
            scheduler,
            miniMessage,
            config.guiSettings,
            guiManager
        );

        LockerGui lockerGUI = new LockerGui(
            miniMessage,
            scheduler,
            config.guiSettings,
            guiManager,
            noticeService,
            this.skullAPI
        );

        var liteCommandsBuilder = LiteBukkitFactory.builder(this.getName(), this)
            .extension(new LiteAdventureExtension<>())
            .argument(Snowflake.class, new SnowflakeArgument())
            .message(LiteBukkitMessages.PLAYER_ONLY, messageConfig.playerOnlyCommand)
            .message(LiteBukkitMessages.PLAYER_NOT_FOUND, messageConfig.playerNotFound)
            .commands(LiteCommandsAnnotations.of(
                new ParcelCommand(mainGUI),
                new ParcelLockersCommand(configService, config, noticeService),
                new DebugCommand(
                    parcelService, lockerManager, itemStorageManager, parcelContentManager,
                    noticeService, deliveryManager)
            ))
            .invalidUsage(new InvalidUsageHandlerImpl(noticeService))
            .missingPermission(new MissingPermissionsHandlerImpl(noticeService));

        DiscordSettings discordSettings = config.discord;
        if (discordSettings.enabled) {
            DiscordNotificationService notificationService;
            DiscordLinkService activeLinkService;

            if (server.getPluginManager().isPluginEnabled("DiscordSRV")) {
                this.getLogger().info("DiscordSRV detected! Using DiscordSRV for account linking.");
                DiscordSrvLinkService discordSrvLinkService = new DiscordSrvLinkService(this.getLogger());
                activeLinkService = discordSrvLinkService;
                notificationService = new DiscordSrvNotificationService(this.getLogger());

                liteCommandsBuilder.commands(
                    new DiscordSrvLinkCommand(discordSrvLinkService, noticeService),
                    new DiscordSrvUnlinkCommand(discordSrvLinkService, noticeService)
                );
            } else {
                if (config.discord.botToken == null || config.discord.botToken.isBlank()) {
                    this.getLogger()
                        .severe("Discord integration is enabled but some of the properties are not set! Disabling...");
                    server.getPluginManager().disablePlugin(this);
                    return;
                }

                this.discordClientManager = new DiscordClientManager(
                    discordSettings.botToken,
                    this.getLogger()
                );
                this.discordClientManager.initialize();

                DiscordLinkRepository discordLinkRepository =
                    new DiscordLinkRepositoryOrmLite(databaseManager, scheduler);
                activeLinkService = new DiscordFallbackLinkService(discordLinkRepository);
                notificationService = new Discord4JNotificationService(
                    this.discordClientManager.getClient(),
                    this.getLogger()
                );

                DiscordLinkValidationService validationService = new DiscordLinkValidationService(
                    activeLinkService,
                    this.discordClientManager.getClient()
                );

                DiscordVerificationService verificationService = DiscordVerificationService.create(
                    activeLinkService,
                    noticeService,
                    messageConfig,
                    miniMessage
                );

                liteCommandsBuilder.commands(
                    new DiscordLinkCommand(
                        activeLinkService,
                        validationService,
                        verificationService,
                        noticeService),
                    new DiscordUnlinkCommand(activeLinkService, noticeService)
                );
            }

            server.getPluginManager().registerEvents(
                new DiscordDeliverNotificationController(
                    notificationService,
                    activeLinkService,
                    userManager,
                    messageConfig
                ),
                this
            );
        }

        this.liteCommands = liteCommandsBuilder.build();

        Stream.of(
            new LockerInteractionController(lockerManager, lockerGUI, scheduler),
            new LockerPlaceController(config, messageConfig, miniMessage, lockerManager, noticeService, scheduler),
            new LockerBreakController(lockerManager, noticeService, scheduler),
            new PrepareUserController(userManager),
            new LoadUserController(userManager, server)
        ).forEach(controller -> server.getPluginManager().registerEvents(controller, this));

        Metrics metrics = new Metrics(this, 17677);
        UpdaterService updaterService = new UpdaterService(this.getPluginMeta().getVersion());

        parcelRepository.findAll().thenAccept(optionalParcels -> optionalParcels
            .stream()
            .filter(parcel -> parcel.status() != ParcelStatus.DELIVERED)
            .forEach(parcel -> deliveryRepository.find(parcel.uuid()).thenAccept(optionalDelivery ->
                optionalDelivery.ifPresent(delivery -> {
                    long delay = Math.max(0, delivery.deliveryTimestamp().toEpochMilli() - System.currentTimeMillis());
                    scheduler.runLaterAsync(
                        new ParcelSendTask(parcel, parcelService, deliveryManager),
                        Duration.ofMillis(delay));
                })
            )));
    }

    @Override
    public void onDisable() {
        if (this.databaseManager != null) {
            this.databaseManager.disconnect();
        }

        if (this.liteCommands != null) {
            this.liteCommands.unregister();
        }

        if (this.skullAPI != null) {
            this.skullAPI.shutdown();
        }

        if (this.discordClientManager != null) {
            this.discordClientManager.shutdown();
        }
    }

    private boolean setupEconomy() {
        if (this.getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = this.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        this.economy = rsp.getProvider();
        return this.economy != null;
    }
}
