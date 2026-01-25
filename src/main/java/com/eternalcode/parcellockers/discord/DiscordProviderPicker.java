package com.eternalcode.parcellockers.discord;

import com.eternalcode.commons.scheduler.Scheduler;
import com.eternalcode.parcellockers.configuration.implementation.MessageConfig;
import com.eternalcode.parcellockers.configuration.implementation.PluginConfig;
import com.eternalcode.parcellockers.database.DatabaseManager;
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
import com.eternalcode.parcellockers.notification.NoticeService;
import com.eternalcode.parcellockers.user.UserManager;
import dev.rollczi.litecommands.LiteCommandsBuilder;
import java.util.logging.Logger;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public class DiscordProviderPicker {

    private final PluginConfig config;
    private final MessageConfig messageConfig;
    private final Server server;
    private final NoticeService noticeService;
    private final Scheduler scheduler;
    private final DatabaseManager databaseManager;
    private final Logger logger;
    private final UserManager userManager;
    private final JavaPlugin plugin;
    private final MiniMessage miniMessage;

    public DiscordProviderPicker(
        PluginConfig config,
        MessageConfig messageConfig,
        Server server,
        NoticeService noticeService,
        Scheduler scheduler,
        DatabaseManager databaseManager,
        Logger logger,
        UserManager userManager,
        JavaPlugin plugin,
        MiniMessage miniMessage
    ) {
        this.config = config;
        this.messageConfig = messageConfig;
        this.server = server;
        this.noticeService = noticeService;
        this.scheduler = scheduler;
        this.databaseManager = databaseManager;
        this.logger = logger;
        this.userManager = userManager;
        this.plugin = plugin;
        this.miniMessage = miniMessage;
    }

    public DiscordClientManager pick(LiteCommandsBuilder<CommandSender, ?, ?> liteCommandsBuilder) {
        if (!this.config.discord.enabled) {
            return null;
        }

        DiscordNotificationService notificationService;
        DiscordLinkService activeLinkService;
        DiscordClientManager discordClientManager = null;

        if (this.server.getPluginManager().isPluginEnabled("DiscordSRV")) {
            this.logger.info("DiscordSRV detected! Using DiscordSRV for account linking.");
            DiscordSrvLinkService discordSrvLinkService = new DiscordSrvLinkService(this.logger);
            activeLinkService = discordSrvLinkService;
            notificationService = new DiscordSrvNotificationService(this.logger);

            liteCommandsBuilder.commands(
                new DiscordSrvLinkCommand(discordSrvLinkService, this.noticeService),
                new DiscordSrvUnlinkCommand(discordSrvLinkService, this.noticeService)
            );
        } else {
            if (this.config.discord.botToken == null || this.config.discord.botToken.isBlank()) {
                this.logger.severe("Discord integration is enabled but some of the properties are not set! Disabling...");
                this.server.getPluginManager().disablePlugin(this.plugin);
                return null;
            }

            discordClientManager = new DiscordClientManager(
                this.config.discord.botToken,
                this.logger
            );

            if (!discordClientManager.initialize()) {
                this.logger.severe("Failed to initialize Discord client! Disabling...");
                this.server.getPluginManager().disablePlugin(this.plugin);
                return null;
            }

            DiscordLinkRepository discordLinkRepository =
                new DiscordLinkRepositoryOrmLite(this.databaseManager, this.scheduler);
            activeLinkService = new DiscordFallbackLinkService(discordLinkRepository);
            notificationService = new Discord4JNotificationService(
                discordClientManager.getClient(),
                this.logger
            );

            DiscordLinkValidationService validationService = new DiscordLinkValidationService(
                activeLinkService,
                discordClientManager.getClient()
            );

            DiscordVerificationService verificationService = DiscordVerificationService.create(
                activeLinkService,
                this.noticeService,
                this.messageConfig,
                this.miniMessage
            );

            liteCommandsBuilder.commands(
                new DiscordLinkCommand(
                    activeLinkService,
                    validationService,
                    verificationService,
                    this.noticeService),
                new DiscordUnlinkCommand(activeLinkService, this.noticeService)
            );
        }

        this.server.getPluginManager().registerEvents(
            new DiscordDeliverNotificationController(
                notificationService,
                activeLinkService,
                this.userManager,
                this.messageConfig
            ),
            this.plugin
        );

        return discordClientManager;
    }
}
