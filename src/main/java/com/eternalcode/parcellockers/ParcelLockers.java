package com.eternalcode.parcellockers;

import com.eternalcode.parcellockers.command.ParcelCommand;
import com.eternalcode.parcellockers.command.handler.InvalidUsage;
import com.eternalcode.parcellockers.command.handler.PermissionMessage;
import com.eternalcode.parcellockers.configuration.ConfigurationManager;
import com.eternalcode.parcellockers.configuration.implementation.PluginConfiguration;
import com.eternalcode.parcellockers.notification.NotificationAnnouncer;
import com.eternalcode.parcellockers.updater.Updater;
import com.eternalcode.parcellockers.util.legacy.LegacyColorProcessor;
import com.google.common.base.Stopwatch;
import dev.rollczi.litecommands.LiteCommands;
import dev.rollczi.litecommands.bukkit.adventure.platform.LiteBukkitAdventurePlatformFactory;
import io.papermc.lib.PaperLib;
import io.papermc.lib.environments.Environment;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bstats.bukkit.Metrics;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public final class ParcelLockers extends JavaPlugin {

    private static ParcelLockers instance;

    private LiteCommands<CommandSender> liteCommands;

    private BukkitAudiences audiences;
    private MiniMessage miniMessage;
    private NotificationAnnouncer announcer;

    private ConfigurationManager configManager;
    private PluginConfiguration config;

    @Override
    public void onEnable() {
        Stopwatch started = Stopwatch.createStarted();
        instance = this;
        this.softwareCheck();

        this.audiences = BukkitAudiences.create(this);
        this.miniMessage = MiniMessage.builder()
                .postProcessor(new LegacyColorProcessor())
                .build();
        this.announcer = new NotificationAnnouncer(this.audiences, this.miniMessage);

        this.configManager = new ConfigurationManager(this.getDataFolder());
        this.config = this.configManager.load(new PluginConfiguration());

        this.liteCommands = LiteBukkitAdventurePlatformFactory.builder(this.getServer(), "parcellockers", false, this.audiences, true)
                .commandInstance(new ParcelCommand(), new ParcelLockerCommand(this.configManager, this.config, this.announcer))
                .invalidUsageHandler(new InvalidUsage(this.announcer, this.config))
                .permissionHandler(new PermissionMessage(this.announcer, this.config))
                .register();


        new Metrics(this, 17677);
        new Updater(this.getLogger()).start();

        long millis = started.elapsed(TimeUnit.MILLISECONDS);
        this.getLogger().info("Successfully enabled ParcelLockers in " + millis + "ms");
    }

    @Override
    public void onDisable() {
        instance = null;

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

    public static ParcelLockers getInstance() {
        return instance;
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
}
