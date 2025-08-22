package com.eternalcode.parcellockers.gui.implementation.locker;

import static com.eternalcode.commons.adventure.AdventureUtil.resetItalic;

import com.eternalcode.commons.scheduler.Scheduler;
import com.eternalcode.parcellockers.configuration.implementation.PluginConfig;
import com.eternalcode.parcellockers.content.repository.ParcelContentRepository;
import com.eternalcode.parcellockers.gui.GuiView;
import com.eternalcode.parcellockers.itemstorage.repository.ItemStorageRepository;
import com.eternalcode.parcellockers.locker.repository.LockerRepository;
import com.eternalcode.parcellockers.notification.NoticeService;
import com.eternalcode.parcellockers.parcel.ParcelService;
import com.eternalcode.parcellockers.parcel.repository.ParcelRepository;
import com.eternalcode.parcellockers.user.UserManager;
import com.eternalcode.parcellockers.user.UserManagerImpl;
import com.eternalcode.parcellockers.user.repository.UserRepository;
import dev.rollczi.liteskullapi.SkullAPI;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;

public class LockerMainGui implements GuiView {

    private final MiniMessage miniMessage;
    private final Scheduler scheduler;
    private final PluginConfig config;
    private final ItemStorageRepository itemStorageRepository;
    private final ParcelRepository parcelRepository;
    private final LockerRepository lockerRepository;
    private final NoticeService noticeService;
    private final ParcelContentRepository parcelContentRepository;
    private final UserRepository userRepository;
    private final SkullAPI skullAPI;
    private final ParcelService parcelService;

    private final UserManager userManager;

    public LockerMainGui(
        MiniMessage miniMessage, Scheduler scheduler,
        PluginConfig config,
        ItemStorageRepository itemStorageRepository,
        ParcelRepository parcelRepository,
        LockerRepository lockerRepository,
        NoticeService noticeService,
        ParcelContentRepository parcelContentRepository,
        UserRepository userRepository,
        SkullAPI skullAPI,
        ParcelService parcelService
    ) {
        this.miniMessage = miniMessage;
        this.scheduler = scheduler;
        this.config = config;
        this.itemStorageRepository = itemStorageRepository;
        this.parcelRepository = parcelRepository;
        this.lockerRepository = lockerRepository;
        this.noticeService = noticeService;
        this.parcelContentRepository = parcelContentRepository;
        this.userRepository = userRepository;
        this.skullAPI = skullAPI;
        this.parcelService = parcelService;

        this.userManager = new UserManagerImpl(this.userRepository);
    }

    @Override
    public void show(Player player) {
        Component guiTitle = this.miniMessage.deserialize(this.config.guiSettings.mainGuiTitle);

        Gui gui = Gui.gui()
            .title(resetItalic(guiTitle))
            .rows(6)
            .disableAllInteractions()
            .create();

        GuiItem backgroundItem = this.config.guiSettings.mainGuiBackgroundItem.toGuiItem();
        GuiItem cornerItem = this.config.guiSettings.cornerItem.toGuiItem();
        GuiItem closeItem = this.config.guiSettings.closeItem.toGuiItem(event -> gui.close(player));

        int size = gui.getRows() * 9;
        for (int i = 0; i < size; i++) {
            gui.setItem(i, backgroundItem);
        }

        for (int slot : CORNER_SLOTS) {
            gui.setItem(slot, cornerItem);
        }

        ParcelCollectionGui collectionGui = new ParcelCollectionGui(
            this.config,
            this.scheduler,
            this.parcelRepository,
            this.miniMessage,
            this.parcelService,
            this.userManager,
            this.lockerRepository
        );

        gui.setItem(21, this.config.guiSettings.parcelLockerCollectItem.toGuiItem(event -> collectionGui.show(player)));
        gui.setItem(23, this.config.guiSettings.parcelLockerSendItem.toGuiItem(event -> new ParcelSendingGui(
            this.scheduler,
            this.config,
            this.miniMessage,
            this.itemStorageRepository,
            this.parcelRepository,
            this.lockerRepository,
            this.noticeService,
            this.parcelContentRepository,
            this.userRepository,
            this.skullAPI,
            this.parcelService,
            new ParcelSendingGuiState()
        ).show(player)));

        gui.setItem(49, closeItem);
        gui.open(player);
    }
}
