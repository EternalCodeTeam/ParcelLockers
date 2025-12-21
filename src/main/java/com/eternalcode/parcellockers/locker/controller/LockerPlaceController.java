package com.eternalcode.parcellockers.locker.controller;

import com.eternalcode.parcellockers.configuration.implementation.MessageConfig;
import com.eternalcode.parcellockers.configuration.implementation.PluginConfig;
import com.eternalcode.parcellockers.locker.LockerManager;
import com.eternalcode.parcellockers.notification.NoticeService;
import com.eternalcode.parcellockers.shared.PositionAdapter;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.dialog.DialogResponseView;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

@SuppressWarnings("UnstableApiUsage")
public class LockerPlaceController implements Listener {

    private final PluginConfig config;
    private final MessageConfig messages;
    private final MiniMessage miniMessage;
    private final LockerManager lockerManager;
    private final NoticeService noticeService;
    private final Cache<UUID, Boolean> lockerCreators = Caffeine.newBuilder()
        .expireAfterWrite(2, TimeUnit.MINUTES)
        .build();

    public LockerPlaceController(
        PluginConfig config, MessageConfig messages, MiniMessage miniMessage,
        LockerManager lockerManager,
        NoticeService noticeService
    ) {
        this.config = config;
        this.messages = messages;
        this.miniMessage = miniMessage;
        this.lockerManager = lockerManager;
        this.noticeService = noticeService;
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        PlayerInventory playerInventory = player.getInventory();

        ItemStack itemInMainHand = playerInventory.getItemInMainHand();
        ItemStack itemInOffHand = playerInventory.getItemInOffHand();

        ItemStack parcelLockerItem = this.config.settings.parcelLockerItem.toRawItemStack();

        if (!parcelLockerItem.isSimilar(itemInMainHand) && !parcelLockerItem.isSimilar(itemInOffHand)) {
            return;
        }

        Block block = event.getBlockPlaced();
        Material type = block.getType();
        BlockData data = block.getBlockData();
        Location location = block.getLocation();
        event.setCancelled(true);

        if (this.lockerCreators.getIfPresent(player.getUniqueId()) != null) {
            this.noticeService.create()
                .player(player.getUniqueId())
                .notice(messages -> messages.locker.alreadyCreating)
                .send();
            return;
        }

        this.lockerCreators.put(player.getUniqueId(), true);

        Component promptMessage = this.miniMessage.deserialize(this.messages.locker.descriptionPrompt); // Replace with actual config message

        Dialog dialog = Dialog.create(builder -> builder.empty()
            .base(DialogBase.builder(promptMessage)
                .inputs(List.of(
                    DialogInput.text("description", this.miniMessage.deserialize("<yellow>Parcel locker description"))
                        .build()
                ))
                .build()
            )
            .type(DialogType.confirmation(
                ActionButton.create(
                    this.miniMessage.deserialize("<dark_green>Create"),
                    this.miniMessage.deserialize("<green>Click to create the locker"),
                    200,
                    DialogAction.customClick((DialogResponseView view, Audience audience) -> {
                        String description = view.getText("description");
                        if (description == null || description.isEmpty()) {
                            this.lockerCreators.invalidate(player.getUniqueId());
                            return;
                        }

                        // Re-validate before creating to prevent race conditions
                        this.lockerManager.get(PositionAdapter.convert(location)).thenAccept(existingLocker -> {
                            if (existingLocker.isPresent()) {
                                this.noticeService.create()
                                    .player(player.getUniqueId())
                                    .notice(messages -> messages.locker.alreadyExists)
                                    .send();
                                this.lockerCreators.invalidate(player.getUniqueId());
                                return;
                            }

                            location.getWorld().getBlockAt(location).setType(type);
                            location.getWorld().getBlockAt(location).setBlockData(data);
                            this.lockerManager.create(UUID.randomUUID(), description, PositionAdapter.convert(location))
                                .thenAccept(locker -> {
                                    this.noticeService.create()
                                        .player(player.getUniqueId())
                                        .notice(messages -> messages.locker.created)
                                        .send();

                                    this.lockerCreators.invalidate(player.getUniqueId());
                                })
                                .exceptionally(ex -> {
                                    this.noticeService.create()
                                        .player(player.getUniqueId())
                                        .notice(messages -> messages.locker.cannotCreate)
                                        .send();
                                    this.lockerCreators.invalidate(player.getUniqueId());
                                    return null;
                                });
                        });
                    }, ClickCallback.Options.builder()
                        .uses(1)
                        .lifetime(ClickCallback.DEFAULT_LIFETIME)
                        .build())
                ),
                ActionButton.create(
                    this.miniMessage.deserialize("<dark_red>Cancel"),
                    this.miniMessage.deserialize("<red>Click to cancel"),
                    200,
                    DialogAction.customClick(
                        (DialogResponseView view, Audience audience) ->
                            this.lockerCreators.invalidate(player.getUniqueId()), ClickCallback.Options.builder()
                            .uses(1)
                            .lifetime(ClickCallback.DEFAULT_LIFETIME)
                            .build())
                )
            ))
        );

        player.showDialog(dialog);
    }
}
