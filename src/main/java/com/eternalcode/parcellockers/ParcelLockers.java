package com.eternalcode.parcellockers;

import com.eternalcode.parcellockers.command.ParcelCommand;
import com.eternalcode.parcellockers.command.handler.InvalidUsage;
import com.google.common.base.Stopwatch;
import dev.rollczi.litecommands.LiteCommands;
import dev.rollczi.litecommands.bukkit.adventure.platform.LiteBukkitAdventurePlatformFactory;
import io.papermc.lib.PaperLib;
import io.papermc.lib.environments.Environment;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bstats.bukkit.Metrics;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

public final class ParcelLockers extends JavaPlugin {

    private static ParcelLockers instance;

    private BukkitAudiences audiences;
    private LiteCommands<CommandSender> liteCommands;

    @Override
    public void onEnable() {
        Stopwatch started = Stopwatch.createStarted();
        instance = this;

        this.softwareCheck();
        this.audiences = BukkitAudiences.create(this);
        this.liteCommands = LiteBukkitAdventurePlatformFactory.builder(this.getServer(), "example-plugin", this.audiences)
                .commandInstance(new ParcelCommand())
                .invalidUsageHandler(new InvalidUsage())
                .register();


        Metrics metrics = new Metrics(this, 17677);

        long millis = started.elapsed().toMillis();
        this.getLogger().info(ChatColor.GREEN + String.valueOf(millis) + "ms");
    }

    @Override
    public void onDisable() {
        instance = null;

        if (this.liteCommands != null) {
            this.liteCommands.getPlatform().unregisterAll();
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
            logger.warning("EternalCore no longer supports your version, be aware that there may be bugs!");
            return;
        }

        logger.info("Your server running on supported software, congratulations!");
        logger.info("Server version: " + this.getServer().getVersion());
    }
}
