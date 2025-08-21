package com.eternalcode.parcellockers.gui.implementation.locker;

import com.eternalcode.commons.adventure.AdventureUtil;
import com.eternalcode.commons.scheduler.Scheduler;
import com.eternalcode.parcellockers.configuration.implementation.PluginConfig;
import com.eternalcode.parcellockers.configuration.serializable.ConfigItem;
import com.eternalcode.parcellockers.content.repository.ParcelContentRepository;
import com.eternalcode.parcellockers.gui.GuiView;
import com.eternalcode.parcellockers.itemstorage.repository.ItemStorageRepository;
import com.eternalcode.parcellockers.locker.repository.LockerRepository;
import com.eternalcode.parcellockers.notification.NoticeService;
import com.eternalcode.parcellockers.parcel.Parcel;
import com.eternalcode.parcellockers.parcel.ParcelService;
import com.eternalcode.parcellockers.parcel.ParcelSize;
import com.eternalcode.parcellockers.parcel.repository.ParcelRepository;
import com.eternalcode.parcellockers.user.repository.UserRepository;
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
import org.jetbrains.annotations.NotNull;

public class ParcelSendingGui implements GuiView {

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
    private final PluginConfig config;
    private final MiniMessage miniMessage;
    private final ItemStorageRepository itemStorageRepository;
    private final ParcelRepository parcelRepository;
    private final LockerRepository lockerRepository;
    private final NoticeService noticeService;
    private final ParcelContentRepository parcelContentRepository;
    private final UserRepository userRepository;
    private final SkullAPI skullAPI;
    private final ParcelService parcelService;

    private final ParcelSendingGuiState state;

    private Gui gui;

    public ParcelSendingGui(
        Scheduler scheduler, PluginConfig config,
            MiniMessage miniMessage,
            ItemStorageRepository itemStorageRepository,
            ParcelRepository parcelRepository,
            LockerRepository lockerRepository,
            NoticeService noticeService,
            ParcelContentRepository parcelContentRepository,
            UserRepository userRepository,
            SkullAPI skullAPI,
            ParcelService parcelService,
            ParcelSendingGuiState state
    ) {
        this.scheduler = scheduler;
        this.config = config;
        this.miniMessage = miniMessage;
        this.itemStorageRepository = itemStorageRepository;
        this.parcelRepository = parcelRepository;
        this.lockerRepository = lockerRepository;
        this.noticeService = noticeService;
        this.parcelContentRepository = parcelContentRepository;
        this.userRepository = userRepository;
        this.skullAPI = skullAPI;
        this.parcelService = parcelService;
        this.state = state;
    }

    @Override
    public void show(Player player) {
        PluginConfig.GuiSettings guiSettings = this.config.guiSettings;

        Component guiTitle = this.miniMessage.deserialize(guiSettings.parcelLockerSendingGuiTitle);

        this.gui = Gui.gui()
            .rows(6)
            .disableAllInteractions()
            .title(guiTitle)
            .create();

        GuiItem backgroundItem = guiSettings.mainGuiBackgroundItem.toGuiItem();
        GuiItem cornerItem = guiSettings.cornerItem.toGuiItem();
        ConfigItem nameItem = guiSettings.parcelNameItem.clone();
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
                            this.config.guiSettings.parcelNameSetLine.replace("{NAME}",
                            this.state.parcelName() == null ? "None" : this.state.parcelName())
                        );

                        this.gui.updateItem(NAME_ITEM_SLOT, nameItem
                            .lore(lore)
                            .toItemStack());
                        this.gui.open(player);
                        return List.of();
                    })
                    .build();
                nameSignGui.open(player);
            } catch (SignGUIVersionException e) {
                Bukkit.getLogger().severe("The server version is unsupported by SignGUI API!");
            }

        });

        ConfigItem descriptionItem = guiSettings.parcelDescriptionItem.clone();
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

                        lore.add(this.config.guiSettings.parcelDescriptionSetLine.replace("{DESCRIPTION}", description));

                        this.gui.updateItem(DESCRIPTION_ITEM_SLOT, descriptionItem
                            .lore(lore)
                            .toItemStack());
                        this.gui.open(player);
                        return List.of();
                    })
                    .build();
            } catch (SignGUIVersionException e) {
                Bukkit.getLogger().severe("The server version is unsupported by SignGUI API!");
            }
            descriptionSignGui.open(player);
        });

        GuiItem storageItem = guiSettings.parcelStorageItem.toGuiItem(event -> {
            ParcelItemStorageGui storageGUI = new ParcelItemStorageGui(
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
                this.state,
                this.parcelService
            );
            this.itemStorageRepository.find(player.getUniqueId()).thenAccept(result -> {
                    if (result.isPresent()) {
                        int slotsSize = result.get().items().size();
                        if (slotsSize <= 9) {
                            this.scheduler.run(() -> storageGUI.show(player, this.state.size()));
                        } else if (slotsSize <= 18 && this.state.size() == ParcelSize.SMALL) {
                            this.scheduler.run(() -> storageGUI.show(player, ParcelSize.MEDIUM));
                        } else {
                            this.scheduler.run(() -> storageGUI.show(player, ParcelSize.LARGE));
                        }
                    } else {
                        this.scheduler.run(() -> storageGUI.show(player, this.state.size()));
                    }
                }
            ).orTimeout(2, TimeUnit.SECONDS);
        });
        GuiItem submitItem = guiSettings.submitParcelItem.toGuiItem(event ->
            this.itemStorageRepository.find(player.getUniqueId()).thenAccept(result -> {
                if (result.isEmpty() || result.get().items().isEmpty()) {
                    this.noticeService.create()
                        .notice(messages -> messages.parcel.empty)
                        .player(player.getUniqueId())
                        .send();
                    // TODO player.playSound(player, this.config.settings.errorSound, this.config.settings.errorSoundVolume, this.config.settings.errorSoundPitch);
                    return;
                }

                if (this.state.receiver() == null) {
                    this.noticeService.create()
                        .notice(messages -> messages.parcel.receiverNotSet)
                        .player(player.getUniqueId())
                        .send();
                    player.playSound(player, this.config.settings.errorSound, this.config.settings.errorSoundVolume, this.config.settings.errorSoundPitch);
                    return;
                }

                Parcel parcel = new Parcel(UUID.randomUUID(), player.getUniqueId(), this.state.parcelName(),
                    this.state.parcelDescription(), this.state.priority(), this.state.receiver(),
                    this.state.size(), this.state.entryLocker(), this.state.destinationLocker(), this.state.status());

                if (this.parcelService.send(player, parcel, result.get().items())) {
                    this.itemStorageRepository.delete(player.getUniqueId());
                }

                this.gui.close(player);
            }).orTimeout(5, TimeUnit.SECONDS));

        GuiItem closeItem = guiSettings.closeItem.toGuiItem(event ->
            new LockerMainGui(
                this.miniMessage,
                this.scheduler,
                this.config,
                this.itemStorageRepository,
                this.parcelRepository,
                this.lockerRepository,
                this.noticeService,
                this.parcelContentRepository,
                this.userRepository,
                this.skullAPI,
                this.parcelService
            ).show(player));

        ConfigItem smallButton = guiSettings.smallParcelSizeItem;
        ConfigItem mediumButton = guiSettings.mediumParcelSizeItem;
        ConfigItem largeButton = guiSettings.largeParcelSizeItem;
        ConfigItem priorityItem = guiSettings.priorityItem;


        int size = gui.getRows() * 9;
        for (int i = 0; i < size; i++) {
            gui.setItem(i, backgroundItem);
        }
        for (int slot : CORNER_SLOTS) {
            this.gui.setItem(slot, cornerItem);
        }

        this.gui.setItem(SMALL_BUTTON_SLOT, smallButton.toGuiItem(event -> this.setSelected(this.gui, ParcelSize.SMALL)));
        this.gui.setItem(MEDIUM_BUTTON_SLOT, mediumButton.toGuiItem(event -> this.setSelected(this.gui, ParcelSize.MEDIUM)));
        this.gui.setItem(LARGE_BUTTON_SLOT, largeButton.toGuiItem(event -> this.setSelected(this.gui, ParcelSize.LARGE)));
        this.gui.setItem(NAME_ITEM_SLOT, nameGuiItem);
        this.gui.setItem(DESCRIPTION_ITEM_SLOT, descriptionGuiItem);
        this.gui.setItem(RECEIVER_ITEM_SLOT, guiSettings.parcelReceiverItem.toGuiItem(event -> new ReceiverSelectionGui(
            this.scheduler,
            this.config,
            this.miniMessage,
            this.userRepository,
            this,
            this.skullAPI,
            this.state
        ).show(player)));

        this.gui.setItem(DESTINATION_ITEM_SLOT, guiSettings.parcelDestinationLockerItem.toGuiItem(event -> new DestinationSelectionGui(
            this.scheduler,
            this.config,
            this.miniMessage,
            this.lockerRepository,
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
        this.userRepository.find(this.state.receiver()).thenAccept(userOptional ->
            userOptional.ifPresent(user -> this.updateReceiverItem(player, user.name())));

        this.lockerRepository.find(this.state.destinationLocker()).thenAccept(lockerOptional ->
            lockerOptional.ifPresent(locker -> this.updateDestinationItem(player, locker.description())));


        this.gui.open(player);
    }

    private void setSelected(Gui gui, ParcelSize size) {
        PluginConfig.GuiSettings settings = this.config.guiSettings;
        this.state.size(size);

        ConfigItem smallButton = size == ParcelSize.SMALL ? settings.selectedSmallParcelSizeItem : settings.smallParcelSizeItem;
        ConfigItem mediumButton = size == ParcelSize.MEDIUM ? settings.selectedMediumParcelSizeItem : settings.mediumParcelSizeItem;
        ConfigItem largeButton = size == ParcelSize.LARGE ? settings.selectedLargeParcelSizeItem : settings.largeParcelSizeItem;
        ConfigItem priorityButton = this.state.priority() ? settings.selectedPriorityItem : settings.priorityItem;

        gui.updateItem(SMALL_BUTTON_SLOT, smallButton.toItemStack());
        gui.updateItem(MEDIUM_BUTTON_SLOT, mediumButton.toItemStack());
        gui.updateItem(LARGE_BUTTON_SLOT, largeButton.toItemStack());
        gui.updateItem(PRIORITY_BUTTON_SLOT, priorityButton.toItemStack());
    }

    private void setSelected(Gui gui, boolean priority) {
        PluginConfig.GuiSettings settings = this.config.guiSettings;
        this.state.priority(priority);

        ConfigItem priorityButton = priority ? settings.selectedPriorityItem : settings.priorityItem;

        gui.updateItem(PRIORITY_BUTTON_SLOT, priorityButton.toItemStack());
    }

    public void updateNameItem() {
        if (this.state.parcelName() == null || this.state.parcelName().isEmpty()) {
            this.gui.updateItem(NAME_ITEM_SLOT, this.config.guiSettings.parcelNameItem.toItemStack());
            return;
        }

        String line = this.config.guiSettings.parcelNameSetLine.replace("{NAME}", this.state.parcelName());
        this.gui.updateItem(NAME_ITEM_SLOT, this.createActiveItem(this.config.guiSettings.parcelNameItem, line));
    }

    public void updateDescriptionItem() {
        if (this.state.parcelDescription() == null || this.state.parcelDescription().isEmpty()) {
            this.gui.updateItem(DESCRIPTION_ITEM_SLOT, this.config.guiSettings.parcelDescriptionItem.toItemStack());
            return;
        }

        String line = this.config.guiSettings.parcelDescriptionSetLine.replace("{DESCRIPTION}", this.state.parcelDescription());
        this.gui.updateItem(DESCRIPTION_ITEM_SLOT, this.createActiveItem(this.config.guiSettings.parcelDescriptionItem, line));
    }

    public void updateReceiverItem(Player player, String receiverName) {
        this.noticeService.create()
            .notice(messages -> messages.parcel.receiverSet)
            .player(player.getUniqueId())
            .send();

        if (receiverName == null || receiverName.isEmpty()) {
            this.gui.updateItem(RECEIVER_ITEM_SLOT, this.config.guiSettings.parcelReceiverItem.toItemStack());
            return;
        }

        String line = this.config.guiSettings.parcelReceiverGuiSetLine.replace("{RECEIVER}", receiverName);
        this.gui.updateItem(RECEIVER_ITEM_SLOT, this.createActiveItem(this.config.guiSettings.parcelReceiverItem, line));
    }

    public void updateDestinationItem(Player player, String destinationLockerDesc) {
        this.noticeService.create()
            .notice(messages -> messages.parcel.destinationSet)
            .player(player.getUniqueId())
            .send();

        if (destinationLockerDesc == null || destinationLockerDesc.isEmpty()) {
            this.gui.updateItem(DESTINATION_ITEM_SLOT, this.config.guiSettings.parcelDestinationLockerItem.toItemStack());
            return;
        }

        String line = this.config.guiSettings.parcelDestinationLockerSetLine.replace("{DESCRIPTION}", destinationLockerDesc);
        this.gui.updateItem(DESTINATION_ITEM_SLOT, this.createActiveItem(this.config.guiSettings.parcelDestinationLockerItem, line));
    }

    private @NotNull ItemStack createActiveItem(ConfigItem item, String appendLore) {
        List<String> itemLore = new ArrayList<>(item.lore());
        itemLore.add(appendLore);

        return item.toBuilder()
            .lore(itemLore.stream().map(element -> AdventureUtil.resetItalic(this.miniMessage.deserialize(element))).toList())
            .glow(true)
            .build();
    }

}
