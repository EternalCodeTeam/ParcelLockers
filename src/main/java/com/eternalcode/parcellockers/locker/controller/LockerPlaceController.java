package com.eternalcode.parcellockers.locker.controller;

import com.eternalcode.parcellockers.configuration.implementation.PluginConfig;
import com.eternalcode.parcellockers.conversation.ParcelLockerPlacePrompt;
import com.eternalcode.parcellockers.locker.Locker;
import com.eternalcode.parcellockers.locker.repository.LockerRepository;
import com.eternalcode.parcellockers.notification.NoticeService;
import com.eternalcode.parcellockers.shared.PositionAdapter;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.bukkit.Location;
import org.bukkit.conversations.ConversationFactory;
import org.bukkit.conversations.NullConversationPrefix;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

public class LockerPlaceController implements Listener {

    private final PluginConfig config;
    private final Plugin plugin;
    private final LockerRepository databaseService;
    private final NoticeService noticeService;
    private final Cache<UUID, Boolean> lockerCreators = Caffeine.newBuilder()
        .expireAfterWrite(2, TimeUnit.MINUTES)
        .build();

    public LockerPlaceController(
            PluginConfig config,
            Plugin plugin,
            LockerRepository databaseService,
            NoticeService noticeService
    ) {
        this.config = config;
        this.plugin = plugin;
        this.databaseService = databaseService;
        this.noticeService = noticeService;
    }

    private static boolean compareMeta(ItemStack first, ItemStack second) {
        ItemMeta firstMeta = first.getItemMeta();
        if (firstMeta == null) {
            return false;
        }

        ItemMeta secondMeta = second.getItemMeta();

        if (secondMeta == null) {
            return false;
        }

        if (first.getType() != second.getType()) {
            return false;
        }

        return new HashSet<>(firstMeta.getLore()).containsAll(secondMeta.getLore())
                && firstMeta.getDisplayName().equals(secondMeta.getDisplayName());
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        PlayerInventory playerInventory = player.getInventory();

        ItemStack itemInMainHand = playerInventory.getItemInMainHand();
        ItemStack itemInOffHand = playerInventory.getItemInOffHand();

        ItemStack parcelLockerItem = this.config.settings.parcelLockerItem.toGuiItem().getItemStack();

        if (!compareMeta(itemInMainHand, parcelLockerItem) && !compareMeta(itemInOffHand, parcelLockerItem)) {
            return;
        }

        if (this.lockerCreators.getIfPresent(player.getUniqueId()) != null) {
            event.setCancelled(true);
            this.noticeService.create()
                .player(player.getUniqueId())
                .notice(messages -> messages.alreadyCreatingLocker)
                .send();
            return;
        }
        this.lockerCreators.put(player.getUniqueId(), true);

        ConversationFactory conversationFactory = new ConversationFactory(this.plugin)
                .addConversationAbandonedListener(e -> {
                    if (!e.gracefulExit()) {
                        event.setCancelled(true);
                        return;
                    }
                    String description = (String) e.getContext().getSessionData("description");
                    Location location = event.getBlockPlaced().getLocation();

                    this.databaseService.save(new Locker(
                        UUID.randomUUID(),
                        description,
                        PositionAdapter.convert(location))).whenComplete((parcelLocker, throwable) -> {
                        if (throwable != null) {
                            throwable.printStackTrace();
                            this.noticeService.create()
                                .player(player.getUniqueId())
                                .notice(messages -> messages.failedToCreateParcelLocker)
                                .send();
                            return;
                        }
                        this.noticeService.create()
                            .player(player.getUniqueId())
                            .notice(messages -> messages.parcelLockerSuccessfullyCreated)
                            .send();
                    });

                    this.lockerCreators.invalidate(player.getUniqueId());
                })
                .withPrefix(new NullConversationPrefix())
                .withModality(false)
                .withLocalEcho(false)
                .withTimeout(60)
                .withFirstPrompt(new ParcelLockerPlacePrompt());

        player.beginConversation(conversationFactory.buildConversation(player));
    }
}
