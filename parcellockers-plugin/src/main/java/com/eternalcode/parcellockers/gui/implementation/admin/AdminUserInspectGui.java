package com.eternalcode.parcellockers.gui.implementation.admin;

import static com.eternalcode.commons.adventure.AdventureUtil.resetItalic;

import com.eternalcode.commons.concurrent.FutureHandler;
import com.eternalcode.commons.scheduler.Scheduler;
import com.eternalcode.parcellockers.configuration.implementation.PluginConfig.GuiSettings;
import com.eternalcode.parcellockers.configuration.serializable.ConfigItem;
import com.eternalcode.parcellockers.gui.GuiManager;
import com.eternalcode.parcellockers.gui.GuiView;
import com.eternalcode.parcellockers.parcel.Parcel;
import com.eternalcode.parcellockers.shared.Page;
import com.eternalcode.parcellockers.user.User;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.PaginatedGui;
import java.util.List;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;

public class AdminUserInspectGui implements GuiView {

    private static final Page FIRST_PAGE = new Page(0, 28);

    private final Scheduler scheduler;
    private final MiniMessage miniMessage;
    private final GuiSettings guiSettings;
    private final GuiManager guiManager;
    private final AdminUserListGui parent;
    private final User user;
    private final boolean showSent;

    public AdminUserInspectGui(Scheduler scheduler, MiniMessage miniMessage, GuiSettings guiSettings,
            GuiManager guiManager, AdminUserListGui parent, User user, boolean showSent) {
        this.scheduler = scheduler;
        this.miniMessage = miniMessage;
        this.guiSettings = guiSettings;
        this.guiManager = guiManager;
        this.parent = parent;
        this.user = user;
        this.showSent = showSent;
    }

    @Override
    public void show(Player player) {
        this.show(player, FIRST_PAGE);
    }

    @Override
    public void show(Player player, Page page) {
        PaginatedGui gui = Gui.paginated()
            .title(resetItalic(this.miniMessage.deserialize(this.guiSettings.adminUserInspectGuiTitle)))
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

        String mode = this.showSent ? "Sent" : "Received";
        ConfigItem toggle = this.guiSettings.adminToggleSentReceivedButton.clone();
        gui.setItem(48, toggle.name(toggle.name().replace("{MODE}", mode))
            .lore(toggle.lore().stream().map(line -> line.replace("{MODE}", mode)).toList())
            .toGuiItem(event -> new AdminUserInspectGui(this.scheduler, this.miniMessage, this.guiSettings,
                this.guiManager, this.parent, this.user, !this.showSent).show(player)));

        var future = this.showSent
            ? this.guiManager.getParcelsBySender(this.user.uuid(), page)
            : this.guiManager.getParcelsByReceiver(this.user.uuid(), page);

        future.thenAccept(result -> {
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
                gui.addItem(this.createRow(parcel));
            }
            this.setupNavigation(gui, page, result, player, this.guiSettings);
            this.scheduler.run(() -> gui.open(player));
        }).exceptionally(FutureHandler::handleException);
    }

    private GuiItem createRow(Parcel parcel) {
        ConfigItem row = this.guiSettings.adminParcelRowItem.clone();
        List<String> lore = row.lore().stream()
            .map(line -> line
                .replace("{STATUS}", parcel.status().name())
                .replace("{SIZE}", parcel.size().name())
                .replace("{PRIORITY}", parcel.priority() ? "Yes" : "No")
                .replace("{UUID}", parcel.uuid().toString()))
            .toList();
        return row.name(row.name().replace("{NAME}", parcel.name())).lore(lore).toGuiItem();
    }
}
