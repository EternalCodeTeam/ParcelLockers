package com.eternalcode.parcellockers.locker.controller;

import com.eternalcode.parcellockers.configuration.implementation.PluginConfig;
import com.eternalcode.parcellockers.conversation.ParcelLockerPlacePrompt;
import com.eternalcode.parcellockers.locker.Locker;
import com.eternalcode.parcellockers.locker.repository.LockerRepository;
import com.eternalcode.parcellockers.notification.NotificationAnnouncer;
import com.eternalcode.parcellockers.shared.PositionAdapter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
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
    private final NotificationAnnouncer announcer;
    private final Map<UUID, Boolean> lockerCreators = new HashMap<>();

    public LockerPlaceController(
            PluginConfig config,
            Plugin plugin,
            LockerRepository databaseService,
            NotificationAnnouncer announcer
    ) {
        this.config = config;
        this.plugin = plugin;
        this.databaseService = databaseService;
        this.announcer = announcer;
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

        if (this.lockerCreators.containsKey(player.getUniqueId())) {
            event.setCancelled(true);
            this.announcer.sendMessage(player, this.config.messages.alreadyCreatingLocker);
            return;
        }
        this.lockerCreators.put(player.getUniqueId(), true);

        ConversationFactory conversationFactory = new ConversationFactory(this.plugin)
                .addConversationAbandonedListener(e -> {
                    if (e.gracefulExit()) {
                        String description = (String) e.getContext().getSessionData("description");
                        Location location = event.getBlockPlaced().getLocation();

                        this.databaseService.save(new Locker(UUID.randomUUID(), description, PositionAdapter.convert(location))).whenComplete((parcelLocker, throwable) -> {
                            if (throwable != null) {
                                throwable.printStackTrace();
                                this.announcer.sendMessage(player, this.config.messages.failedToCreateParcelLocker);
                                return;
                            }
                            this.announcer.sendMessage(player, this.config.messages.parcelLockerSuccessfullyCreated);
                        });
                    } else {
                        event.setCancelled(true);
                    }
                    this.lockerCreators.remove(player.getUniqueId());
                })
                .withPrefix(new NullConversationPrefix())
                .withModality(false)
                .withLocalEcho(false)
                .withTimeout(60)
                .withFirstPrompt(new ParcelLockerPlacePrompt(this.config));

        player.beginConversation(conversationFactory.buildConversation(player));
    }
}
