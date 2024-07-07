package com.eternalcode.parcellockers.parcel.command;

import com.eternalcode.parcellockers.configuration.implementation.PluginConfiguration;
import com.eternalcode.parcellockers.gui.implementation.remote.MainGUI;
import com.eternalcode.parcellockers.gui.implementation.remote.ParcelListGUI;
import com.eternalcode.parcellockers.locker.repository.LockerRepository;
import com.eternalcode.parcellockers.notification.NotificationAnnouncer;
import com.eternalcode.parcellockers.parcel.Parcel;
import com.eternalcode.parcellockers.parcel.ParcelManager;
import com.eternalcode.parcellockers.parcel.ParcelSize;
import com.eternalcode.parcellockers.user.UserManager;
import com.eternalcode.parcellockers.util.ItemUtil;
import com.eternalcode.parcellockers.util.RandomUtil;
import dev.rollczi.litecommands.annotations.argument.Arg;
import dev.rollczi.litecommands.annotations.command.Command;
import dev.rollczi.litecommands.annotations.context.Context;
import dev.rollczi.litecommands.annotations.execute.Execute;
import dev.rollczi.litecommands.annotations.permission.Permission;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.TestOnly;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Command(name = "parcel")
@Permission("parcellockers.command.parcel")
public class ParcelCommand {

    private final LockerRepository lockerRepository;
    private final NotificationAnnouncer announcer;
    private final PluginConfiguration config;
    private final MainGUI mainGUI;
    private final ParcelListGUI parcelListGUI;
    private final ParcelManager parcelManager;
    private final UserManager userManager;

    public ParcelCommand(LockerRepository lockerRepository, NotificationAnnouncer announcer, PluginConfiguration config, MainGUI mainGUI, ParcelListGUI parcelListGUI, ParcelManager parcelManager, UserManager userManager) {
        this.lockerRepository = lockerRepository;
        this.announcer = announcer;
        this.config = config;
        this.mainGUI = mainGUI;
        this.parcelListGUI = parcelListGUI;
        this.parcelManager = parcelManager;
        this.userManager = userManager;
    }

    @Execute(name = "list")
    void list(@Context Player player) {
        this.parcelListGUI.show(player);
    }

    @Execute(name = "info")
    void info(@Context Player player, @Arg Parcel parcel) {
        List<String> messagesToSend = ItemUtil.replaceParcelPlaceholders(parcel, this.config.messages.parcelInfoMessages, this.userManager, this.lockerRepository);
        messagesToSend.forEach(message -> this.announcer.sendMessage(player, message));
    }

    @Execute(name = "delete")
    void delete(@Context Player player, @Arg Parcel parcel) {
        this.parcelManager.deleteParcel(player, parcel);
    }

    @Execute(name = "gui")
    void gui(@Context Player player) {
        this.mainGUI.show(player);
    }

    @TestOnly
    @Execute(name = "createrandom")
    void createRandomParcel(@Context Player player) {
        Parcel parcel = Parcel.builder()
            .uuid(UUID.randomUUID())
            .name("Random Parcel")
            .sender(player.getUniqueId())
            .receiver(player.getUniqueId())
            .priority(false)
            .size(RandomUtil.randomEnum(ParcelSize.class))
            .entryLocker(UUID.randomUUID())
            .destinationLocker(UUID.randomUUID())
            .description(RandomUtil.randomParcelDescription())
            .recipients(Set.of(player.getUniqueId()))
            .build();

        this.parcelManager.createParcel(player, parcel);
    }
}
