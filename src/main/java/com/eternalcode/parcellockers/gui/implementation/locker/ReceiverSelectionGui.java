package com.eternalcode.parcellockers.gui.implementation.locker;

import com.eternalcode.parcellockers.configuration.implementation.PluginConfiguration;
import com.eternalcode.parcellockers.gui.GuiView;
import com.eternalcode.parcellockers.shared.Page;
import com.eternalcode.parcellockers.user.User;
import com.eternalcode.parcellockers.user.UserRepository;
import com.spotify.futures.CompletableFutures;
import dev.rollczi.liteskullapi.SkullAPI;
import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.PaginatedGui;
import io.sentry.Sentry;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class ReceiverSelectionGui extends GuiView {

    private static final int WIDTH = 7;
    private static final int HEIGHT = 4;
    private static final Page FIRST_PAGE = new Page(0, WIDTH * HEIGHT);

    private final PluginConfiguration config;
    private final MiniMessage miniMessage;
    private final UserRepository userRepository;
    private final ParcelSendingGUI sendingGUI;
    private final SkullAPI skullAPI;

    private final Map<UUID, UUID> setReceivers = new HashMap<>();

    public ReceiverSelectionGui(PluginConfiguration config, MiniMessage miniMessage, UserRepository userRepository, ParcelSendingGUI sendingGUI, SkullAPI skullAPI) {
        this.config = config;
        this.miniMessage = miniMessage;
        this.userRepository = userRepository;
        this.sendingGUI = sendingGUI;
        this.skullAPI = skullAPI;
    }

    @Override
    public void show(Player player) {
        this.show(player, FIRST_PAGE);
    }

    private void show(Player player, Page page) {
        PaginatedGui gui = Gui.paginated()
            .title(this.miniMessage.deserialize(this.config.guiSettings.parcelReceiverSelectionGuiTitle))
            .rows(6)
            .disableAllInteractions()
            .create();

        GuiItem backgroundItem = this.config.guiSettings.mainGuiBackgroundItem.toGuiItem();
        GuiItem cornerItem = this.config.guiSettings.cornerItem.toGuiItem();
        GuiItem closeItem = this.config.guiSettings.closeItem.toGuiItem(event -> this.sendingGUI.show(player));
        GuiItem previousPageItem = this.config.guiSettings.previousPageItem.toGuiItem(event -> this.show(player, page.previous()));
        GuiItem nextPageItem = this.config.guiSettings.nextPageItem.toGuiItem(event -> this.show(player, page.next()));


        gui.setItem(49, closeItem);

        for (int slot : CORNER_SLOTS) {
            gui.setItem(slot, cornerItem);
        }
        for (int slot : BORDER_SLOTS) {
            gui.setItem(slot, backgroundItem);
        }

        this.userRepository.findPage(page).whenComplete((result, throwable) -> {
            if (throwable != null) {
                Sentry.captureException(throwable);
                throwable.printStackTrace();
                return;
            }

            if (result.hasNextPage()) {
                gui.setItem(51, nextPageItem);
            }

            if (page.hasPrevious()) {
                gui.setItem(47, previousPageItem);
            }

            List<CompletableFuture<ItemStack>> skullFutures = result.users().stream()
                .map(user -> this.skullAPI.getSkull(user.uuid()))
                .toList();

            CompletableFutures.allAsList(skullFutures).whenComplete((skulls, throwable1) -> {
                if (throwable1 != null) {
                    Sentry.captureException(throwable1);
                    throwable1.printStackTrace();
                    return;
                }

                for (int i = 0; i < skulls.size(); i++) {
                    User user = result.users().get(i);
                    UUID receiver = user.uuid();
                    ItemStack skull = skulls.get(i);

                    /*if (receiver.equals(player.getUniqueId())) {
                        continue;
                    }*/


                    GuiItem item = ItemBuilder.from(skull)
                        .name(this.miniMessage.deserialize(user.name()))
                        .lore(this.miniMessage.deserialize(this.config.guiSettings.parcelReceiverNotSetLine))
                        .glow(false)
                        .flags(ItemFlag.HIDE_ATTRIBUTES)
                        .asGuiItem(event -> {
                            this.setReceivers.put(player.getUniqueId(), receiver);
                            this.sendingGUI.show(player);
                        });

                    gui.addItem(item);
                }

                gui.open(player);
            });

            /*for (User user : result.users()) {
                if (user.uuid().equals(player.getUniqueId())) {
                    continue;
                }

                this.skullAPI.acceptAsyncSkullData(user.uuid(), skull -> {
                    GuiItem item = GuiItem.from(skull)
                        .setName(name.replaceText(user.name()))
                        .setLore(lore)
                        .setClick(event -> {
                            this.setReceivers.put(player.getUniqueId(), user.uuid());
                            this.sendingGUI.show(player);
                        });

                    gui.addItem(item);
                }


            }
            gui.open(player);*/
        });


    }
}
