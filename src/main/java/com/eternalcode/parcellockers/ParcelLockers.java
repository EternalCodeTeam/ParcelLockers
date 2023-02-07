package com.eternalcode.parcellockers;

import com.google.common.base.Stopwatch;
import dev.rollczi.litecommands.LiteCommands;
import dev.rollczi.litecommands.bukkit.adventure.platform.LiteBukkitAdventurePlatformFactory;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bstats.bukkit.Metrics;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.HelpCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class ParcelLockers extends JavaPlugin {

    private static ParcelLockers instance;

    private BukkitAudiences audiences;
    private LiteCommands<CommandSender> liteCommands;

    @Override
    public void onEnable() {
        Stopwatch started = Stopwatch.createStarted();
        instance = this;

        this.audiences = BukkitAudiences.create(this);
        this.liteCommands = LiteBukkitAdventurePlatformFactory.builder(this.getServer(), "example-plugin", this.audiences)
                .commandInstance(new HelpCommand())
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
}
