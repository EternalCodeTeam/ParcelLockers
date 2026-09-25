package com.eternalcode.parcellockers.gui.implementation.admin;

import static com.eternalcode.commons.adventure.AdventureUtil.resetItalic;

import com.eternalcode.commons.concurrent.FutureHandler;
import com.eternalcode.commons.scheduler.Scheduler;
import com.eternalcode.parcellockers.configuration.implementation.MessageConfig;
import com.eternalcode.parcellockers.configuration.implementation.PluginConfig.GuiSettings;
import com.eternalcode.parcellockers.configuration.serializable.ConfigItem;
import com.eternalcode.parcellockers.gui.GuiManager;
import com.eternalcode.parcellockers.gui.GuiView;
import com.eternalcode.parcellockers.notification.NoticeService;
import com.eternalcode.parcellockers.parcel.Parcel;
import com.eternalcode.parcellockers.parcel.service.AdminParcelService;
import com.eternalcode.parcellockers.shared.Page;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.PaginatedGui;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;

public class AdminParcelListGui implements GuiView {

    private static final Page FIRST_PAGE = new Page(0, 28);

    private final Scheduler scheduler;
    private final MiniMessage miniMessage;
    private final GuiSettings guiSettings;
    private final MessageConfig messageConfig;
    private final NoticeService noticeService;
    private final GuiManager guiManager;
    private final AdminParcelService adminParcelService;
    private final GuiView parent;

    public AdminParcelListGui(Scheduler scheduler, MiniMessage miniMessage, GuiSettings guiSettings,
            MessageConfig messageConfig, NoticeService noticeService, GuiManager guiManager,
            AdminParcelService adminParcelService, GuiView parent) {
        this.scheduler = scheduler;
        this.miniMessage = miniMessage;
        this.guiSettings = guiSettings;
        this.messageConfig = messageConfig;
        this.noticeService = noticeService;
        this.guiManager = guiManager;
        this.adminParcelService = adminParcelService;
        this.parent = parent;
    }

    @Override
    public void show(Player player) {
        this.show(player, FIRST_PAGE);
    }

    @Override
    public void show(Player player, Page page) {
        PaginatedGui gui = Gui.paginated()
            .title(resetItalic(this.miniMessage.deserialize(this.guiSettings.adminParcelListGuiTitle)))
            .rows(6)
            .disableAllInteractions()
            .create();

        GuiItem background = this.guiSettings.mainGuiBackgroundItem.toGuiItem();
        GuiItem corner = this.guiSettings.cornerItem.toGuiItem();
        for (int slot : CORNER_SLOTS) {
            gui.setItem(slot, corner);
        }
        for (int slot : BORDER_SLOTS) {
            gui.setItem(slot, background);
        }
        gui.setItem(49, this.guiSettings.closeItem.toGuiItem(event -> this.parent.show(player)));

        this.guiManager.getAllParcels(page).thenAccept(result -> {
            if (result.items().isEmpty() && page.hasPrevious()) {
                this.show(player, page.previous());
                return;
            }
            if (result.items().isEmpty()) {
                gui.setItem(22, this.guiSettings.adminEmptyListItem.toGuiItem());
                this.scheduler.run(() -> gui.open(player));
                return;
            }
            for (Parcel parcel : result.items()) {
                gui.addItem(this.createRow(player, parcel));
            }
            this.setupNavigation(gui, page, result, player, this.guiSettings);
            this.scheduler.run(() -> gui.open(player));
        }).exceptionally(FutureHandler::handleException);
    }

    private GuiItem createRow(Player player, Parcel parcel) {
        ConfigItem row = this.guiSettings.adminParcelRowItem.clone();
        return row.name(row.name().replace("{NAME}", parcel.name()))
            .lore(row.lore().stream().map(line -> line
                .replace("{STATUS}", parcel.status().name())
                .replace("{SIZE}", parcel.size().name())
                .replace("{PRIORITY}", parcel.priority() ? "Yes" : "No")
                .replace("{UUID}", parcel.uuid().toString())).toList())
            .toGuiItem(event -> new AdminParcelEditGui(this.scheduler, this.miniMessage, this.guiSettings,
                this.messageConfig, this.noticeService, this.guiManager, this.adminParcelService, this, parcel).show(player));
    }
}
