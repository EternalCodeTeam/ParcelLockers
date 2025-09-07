package com.eternalcode.parcellockers.gui.implementation.locker;

import static com.eternalcode.commons.adventure.AdventureUtil.resetItalic;

import com.eternalcode.commons.scheduler.Scheduler;
import com.eternalcode.parcellockers.configuration.implementation.PluginConfig.GuiSettings;
import com.eternalcode.parcellockers.configuration.serializable.ConfigItem;
import com.eternalcode.parcellockers.gui.GuiManager;
import com.eternalcode.parcellockers.gui.GuiView;
import com.eternalcode.parcellockers.notification.NoticeService;
import com.eternalcode.parcellockers.parcel.Parcel;
import com.eternalcode.parcellockers.parcel.ParcelSize;
import com.eternalcode.parcellockers.shared.ScheduledGuiAction;
import com.eternalcode.parcellockers.util.MaterialUtil;
import de.rapha149.signgui.SignGUI;
import de.rapha149.signgui.exception.SignGUIVersionException;
import dev.rollczi.liteskullapi.SkullAPI;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class SendingGui implements GuiView {

    private static final int RECEIVER_ITEM_SLOT = 23;
    private static final int SMALL_BUTTON_SLOT = 12;
    private static final int MEDIUM_BUTTON_SLOT = 13;
    private static final int LARGE_BUTTON_SLOT = 14;
    private static final int NAME_ITEM_SLOT = 21;
    private static final int DESCRIPTION_ITEM_SLOT = 22;
    private static final int DESTINATION_ITEM_SLOT = 30;
    private static final int STORAGE_ITEM_SLOT = 37;
    private static final int PRIORITY_BUTTON_SLOT = 42;
    private static final int SUBMIT_ITEM_SLOT = 43;
    private static final int CLOSE_ITEM_SLOT = 49;
    private final Scheduler scheduler;
    private final GuiSettings guiSettings;
    private final MiniMessage miniMessage;
    private final NoticeService noticeService;
    private final GuiManager guiManager;
    private final SkullAPI skullAPI;

    private final SendingGuiState state;

    private Gui gui;

    public SendingGui(
        Scheduler scheduler,
        GuiSettings guiSettings,
        MiniMessage miniMessage,
        NoticeService noticeService,
        GuiManager guiManager,
        SkullAPI skullAPI,
        SendingGuiState state
    ) {
        this.scheduler = scheduler;
        this.guiSettings = guiSettings;
        this.miniMessage = miniMessage;
        this.noticeService = noticeService;
        this.guiManager = guiManager;
        this.skullAPI = skullAPI;
        this.state = state;
    }

    public void show(Player player, UUID entryLocker) {
        this.state.entryLocker(entryLocker);
        this.show(player);
    }

    @Override
    public void show(Player player) {
        Component guiTitle = this.miniMessage.deserialize(this.guiSettings.parcelLockerSendingGuiTitle);

        this.gui = Gui.gui()
            .rows(6)
            .disableAllInteractions()
            .title(guiTitle)
            .create();

        GuiItem backgroundItem = this.guiSettings.mainGuiBackgroundItem.toGuiItem();
        GuiItem cornerItem = this.guiSettings.cornerItem.toGuiItem();
        ConfigItem nameItem = this.guiSettings.parcelNameItem.clone();
        GuiItem nameGuiItem = nameItem.toGuiItem(event -> {
            try {
                SignGUI nameSignGui = SignGUI.builder()
                    .setColor(DyeColor.BLACK)
                    .setType(Material.OAK_SIGN)
                    .setLine(0, "Enter parcel name:")
                    .setHandler((p, result) -> {
                        String name = result.getLineWithoutColor(1);
                        if (name.isBlank()) {
                            this.noticeService.create()
                                .notice(messages -> messages.parcel.emptyName)
                                .player(player.getUniqueId())
                                .send();
                            return Collections.emptyList();
                        }

                        this.state.parcelName(name);
                        this.noticeService.create()
                            .notice(messages -> messages.parcel.nameSet)
                            .player(player.getUniqueId())
                            .send();

                        List<String> lore = nameItem.lore();
                        if (lore.size() > 1) {
                            lore.remove(1);
                        }

                        lore.add(
                            this.guiSettings.parcelNameSetLine.replace("{NAME}",
                            this.state.parcelName() == null ? "None" : this.state.parcelName())
                        );

                        this.gui.updateItem(NAME_ITEM_SLOT, nameItem
                            .lore(lore)
                            .toItemStack());
                        return List.of(new ScheduledGuiAction(this.scheduler, () -> this.gui.open(player)));
                    })
                    .build();
                nameSignGui.open(player);
            } catch (SignGUIVersionException e) {
                Bukkit.getLogger().severe("The server version is unsupported by SignGUI API!");
            }
        });

        ConfigItem descriptionItem = this.guiSettings.parcelDescriptionItem.clone();
        GuiItem descriptionGuiItem = descriptionItem.toGuiItem(event -> {
            SignGUI descriptionSignGui = null;
            try {
                descriptionSignGui = SignGUI.builder()
                    .setColor(DyeColor.BLACK)
                    .setType(Material.OAK_SIGN)
                    .setLine(0, "Enter parcel description:")
                    .setHandler((p, result) -> {
                        String description = result.getLineWithoutColor(1);

                        this.state.parcelDescription(description);
                        this.noticeService.create()
                            .notice(messages -> messages.parcel.descriptionSet)
                            .player(player.getUniqueId())
                            .send();

                        List<String> lore = descriptionItem.clone().lore();
                        if (lore.size() > 1) {
                            lore.remove(1);
                        }

                        lore.add(this.guiSettings.parcelDescriptionSetLine.replace("{DESCRIPTION}", description));

                        this.gui.updateItem(DESCRIPTION_ITEM_SLOT, descriptionItem
                            .lore(lore)
                            .toItemStack());
                        return List.of(new ScheduledGuiAction(this.scheduler, () -> this.gui.open(player)));
                    })
                    .build();
            } catch (SignGUIVersionException e) {
                Bukkit.getLogger().severe("The server version is unsupported by SignGUI API!");
            }

            if (descriptionSignGui != null) {
                descriptionSignGui.open(player);
            }
        });

        ConfigItem parcelStorageItem = this.guiSettings.parcelStorageItem.clone();
        GuiItem storageItem = parcelStorageItem.toGuiItem(event -> {
            ItemStorageGui storageGUI = new ItemStorageGui(
                this.scheduler,
                this.guiSettings,
                this.miniMessage,
                this.guiManager,
                this.noticeService,
                this.skullAPI,
                this.state
            );
            this.guiManager.getItemStorage(player.getUniqueId()).thenAccept(result -> {
                    int slotsSize = result.items().size();
                    if (slotsSize <= 9) {
                        this.scheduler.run(() -> storageGUI.show(player, this.state.size()));
                    } else if (slotsSize <= 18 && this.state.size() == ParcelSize.SMALL) {
                        this.scheduler.run(() -> storageGUI.show(player, ParcelSize.MEDIUM));
                    } else {
                        this.scheduler.run(() -> storageGUI.show(player, ParcelSize.LARGE));
                    }
                }
            ).orTimeout(2, TimeUnit.SECONDS);
        });
        GuiItem submitItem = this.guiSettings.submitParcelItem.toGuiItem(event ->
            this.guiManager.getItemStorage(player.getUniqueId()).thenAccept(result -> {
                if (result.items().isEmpty()) {
                    this.noticeService.create()
                        .notice(messages -> messages.parcel.empty)
                        .player(player.getUniqueId())
                        .send();
                    return;
                }

                if (this.state.receiver() == null) {
                    this.noticeService.create()
                        .notice(messages -> messages.parcel.receiverNotSet)
                        .player(player.getUniqueId())
                        .send();
                    return;
                }

                Parcel parcel = new Parcel(
                    UUID.randomUUID(),
                    player.getUniqueId(),
                    this.state.parcelName(),
                    this.state.parcelDescription(),
                    this.state.priority(),
                    this.state.receiver(),
                    this.state.size(),
                    this.state.entryLocker(),
                    this.state.destinationLocker(),
                    this.state.status()
                );

                this.guiManager.sendParcel(player, parcel, result.items());
                this.guiManager.deleteItemStorage(player.getUniqueId());

                this.gui.close(player);
            }).orTimeout(5, TimeUnit.SECONDS));

        GuiItem closeItem = this.guiSettings.closeItem.toGuiItem(event ->
            new LockerGui(
                this.miniMessage,
                this.scheduler,
                this.guiSettings,
                this.guiManager,
                this.noticeService,
                this.skullAPI
            ).show(player, this.state.entryLocker()));

        ConfigItem smallButton = this.guiSettings.smallParcelSizeItem;
        ConfigItem mediumButton = this.guiSettings.mediumParcelSizeItem;
        ConfigItem largeButton = this.guiSettings.largeParcelSizeItem;
        ConfigItem priorityItem = this.guiSettings.priorityItem;


        int size = this.gui.getRows() * 9;
        for (int i = 0; i < size; i++) {
            this.gui.setItem(i, backgroundItem);
        }
        for (int slot : CORNER_SLOTS) {
            this.gui.setItem(slot, cornerItem);
        }

        this.gui.setItem(SMALL_BUTTON_SLOT, smallButton.toGuiItem(event -> this.setSelected(this.gui, ParcelSize.SMALL)));
        this.gui.setItem(MEDIUM_BUTTON_SLOT, mediumButton.toGuiItem(event -> this.setSelected(this.gui, ParcelSize.MEDIUM)));
        this.gui.setItem(LARGE_BUTTON_SLOT, largeButton.toGuiItem(event -> this.setSelected(this.gui, ParcelSize.LARGE)));
        this.gui.setItem(NAME_ITEM_SLOT, nameGuiItem);
        this.gui.setItem(DESCRIPTION_ITEM_SLOT, descriptionGuiItem);
        this.gui.setItem(RECEIVER_ITEM_SLOT, this.guiSettings.parcelReceiverItem.toGuiItem(event -> new ReceiverGui(
            this.scheduler,
            this.guiSettings,
            this.miniMessage,
            this.guiManager,
            this,
            this.skullAPI,
            this.state
        ).show(player)));

        this.gui.setItem(DESTINATION_ITEM_SLOT, this.guiSettings.parcelDestinationLockerItem.toGuiItem(event -> new DestinationGui(
            this.scheduler,
            this.guiSettings,
            this.miniMessage,
            this.guiManager,
            this,
            this.state
        ).show(player)));

        this.gui.setItem(STORAGE_ITEM_SLOT, storageItem);
        this.gui.setItem(SUBMIT_ITEM_SLOT, submitItem);
        this.gui.setItem(PRIORITY_BUTTON_SLOT, priorityItem.toGuiItem(event -> this.setSelected(this.gui, !this.state.priority())));
        this.gui.setItem(CLOSE_ITEM_SLOT, closeItem);

        this.setSelected(this.gui, this.state.size() == null ? ParcelSize.SMALL : this.state.size());

        this.updateNameItem();
        this.updateDescriptionItem();
        this.updateStorageItem(player);
        if (this.state.receiver() != null) {
            this.guiManager.getUser(this.state.receiver()).thenAccept(userOptional ->
                userOptional.ifPresent(user -> this.updateReceiverItem(player, user.name(), false)));
        }

        if (this.state.destinationLocker() != null) {
            this.guiManager.getLocker(this.state.destinationLocker()).thenAccept(lockerOptional ->
                lockerOptional.ifPresent(locker -> this.updateDestinationItem(player, locker.name(), false)));
        }

        this.scheduler.run(() -> this.gui.open(player));
    }

    private void setSelected(Gui gui, ParcelSize size) {
        this.state.size(size);

        ConfigItem smallButton = size == ParcelSize.SMALL ? this.guiSettings.selectedSmallParcelSizeItem : this.guiSettings.smallParcelSizeItem;
        ConfigItem mediumButton = size == ParcelSize.MEDIUM ? this.guiSettings.selectedMediumParcelSizeItem : this.guiSettings.mediumParcelSizeItem;
        ConfigItem largeButton = size == ParcelSize.LARGE ? this.guiSettings.selectedLargeParcelSizeItem : this.guiSettings.largeParcelSizeItem;
        ConfigItem priorityButton = this.state.priority() ? this.guiSettings.selectedPriorityItem : this.guiSettings.priorityItem;

        gui.updateItem(SMALL_BUTTON_SLOT, smallButton.toItemStack());
        gui.updateItem(MEDIUM_BUTTON_SLOT, mediumButton.toItemStack());
        gui.updateItem(LARGE_BUTTON_SLOT, largeButton.toItemStack());
        gui.updateItem(PRIORITY_BUTTON_SLOT, priorityButton.toItemStack());
    }

    private void setSelected(Gui gui, boolean priority) {
        this.state.priority(priority);

        ConfigItem priorityButton = priority ? this.guiSettings.selectedPriorityItem : this.guiSettings.priorityItem;

        gui.updateItem(PRIORITY_BUTTON_SLOT, priorityButton.toItemStack());
    }

    public void updateNameItem() {
        if (this.state.parcelName() == null || this.state.parcelName().isEmpty()) {
            this.gui.updateItem(NAME_ITEM_SLOT, this.guiSettings.parcelNameItem.toItemStack());
            return;
        }

        String line = this.guiSettings.parcelNameSetLine.replace("{NAME}", this.state.parcelName());
        this.gui.updateItem(NAME_ITEM_SLOT, this.createActiveItem(this.guiSettings.parcelNameItem, line));
    }

    public void updateDescriptionItem() {
        if (this.state.parcelDescription() == null || this.state.parcelDescription().isEmpty()) {
            this.gui.updateItem(DESCRIPTION_ITEM_SLOT, this.guiSettings.parcelDescriptionItem.toItemStack());
            return;
        }

        String line = this.guiSettings.parcelDescriptionSetLine.replace("{DESCRIPTION}", this.state.parcelDescription());
        this.gui.updateItem(DESCRIPTION_ITEM_SLOT, this.createActiveItem(this.guiSettings.parcelDescriptionItem, line));
    }

    public void updateReceiverItem(Player player, String receiverName, boolean sendNotice) {
        if (sendNotice) {
            this.noticeService.create()
                .notice(messages -> messages.parcel.receiverSet)
                .player(player.getUniqueId())
                .send();
        }

        if (receiverName == null || receiverName.isEmpty()) {
            this.gui.updateItem(RECEIVER_ITEM_SLOT, this.createActiveItem(this.guiSettings.parcelReceiverItem, this.guiSettings.parcelReceiverNotSetLine));
            return;
        }

        String line = this.guiSettings.parcelReceiverGuiSetLine.replace("{RECEIVER}", receiverName);
        this.gui.updateItem(RECEIVER_ITEM_SLOT, this.createActiveItem(this.guiSettings.parcelReceiverItem, line));
    }

    public void updateDestinationItem(Player player, String destinationLockerDesc, boolean sendNotice) {
        if (sendNotice){
            this.noticeService.create()
                .notice(messages -> messages.parcel.destinationSet)
                .player(player.getUniqueId())
                .send();
        }

        if (destinationLockerDesc == null || destinationLockerDesc.isEmpty()) {
            this.gui.setItem(DESTINATION_ITEM_SLOT, this.guiSettings.parcelDestinationLockerItem.toGuiItem(event -> new DestinationGui(
                this.scheduler,
                this.guiSettings,
                this.miniMessage,
                this.guiManager,
                this,
                this.state
            ).show(player)));
            return;
        }

        String line = this.guiSettings.parcelDestinationLockerSetLine.replace("{DESCRIPTION}", destinationLockerDesc);
        this.gui.updateItem(DESTINATION_ITEM_SLOT, this.createActiveItem(this.guiSettings.parcelDestinationLockerItem, line));
    }

    public void updateStorageItem(Player player) {
        this.guiManager.getItemStorage(player.getUniqueId()).thenAccept(result -> {
            List<ItemStack> items = result.items();
            if (items.isEmpty()) {
                this.gui.updateItem(STORAGE_ITEM_SLOT, this.createActiveItem(this.guiSettings.parcelStorageItem, List.of()));
                return;
            }

            List<String> lore = new ArrayList<>();
            lore.add(this.guiSettings.parcelStorageItemsSetLine);

            for (ItemStack item : items) {
                lore.add(this.guiSettings.parcelStorageItemLine
                    .replace("{ITEM}", MaterialUtil.format(item.getType()))
                    .replace("{AMOUNT}", Integer.toString(item.getAmount()))
                );
            }
            this.gui.updateItem(STORAGE_ITEM_SLOT, this.createActiveItem(this.guiSettings.parcelStorageItem, lore));
        });
    }

    private ItemStack createActiveItem(ConfigItem item, String appendLore) {
        List<String> itemLore = new ArrayList<>(item.lore());
        itemLore.add(appendLore);

        return item.toBuilder()
            .lore(itemLore.stream().map(element -> resetItalic(this.miniMessage.deserialize(element))).toList())
            .glow(true)
            .build();
    }

    private ItemStack createActiveItem(ConfigItem item, List<String> appendLore) {
        List<String> itemLore = new ArrayList<>(item.lore());
        itemLore.addAll(appendLore);

        return item.toBuilder()
            .lore(itemLore.stream().map(element -> resetItalic(this.miniMessage.deserialize(element))).toList())
            .glow(true)
            .build();
    }

}
