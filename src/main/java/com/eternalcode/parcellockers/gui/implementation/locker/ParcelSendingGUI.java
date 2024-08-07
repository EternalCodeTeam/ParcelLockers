package com.eternalcode.parcellockers.gui.implementation.locker;

import com.eternalcode.parcellockers.configuration.implementation.ConfigItem;
import com.eternalcode.parcellockers.configuration.implementation.PluginConfiguration;
import com.eternalcode.parcellockers.content.ParcelContent;
import com.eternalcode.parcellockers.content.repository.ParcelContentRepository;
import com.eternalcode.parcellockers.gui.GuiView;
import com.eternalcode.parcellockers.itemstorage.repository.ItemStorageRepository;
import com.eternalcode.parcellockers.locker.repository.LockerRepository;
import com.eternalcode.parcellockers.notification.NotificationAnnouncer;
import com.eternalcode.parcellockers.parcel.Parcel;
import com.eternalcode.parcellockers.parcel.ParcelSize;
import com.eternalcode.parcellockers.parcel.repository.ParcelRepository;
import com.eternalcode.parcellockers.shared.ExceptionHandler;
import com.eternalcode.parcellockers.user.UserRepository;
import de.rapha149.signgui.SignGUI;
import de.rapha149.signgui.SignGUIAction;
import dev.rollczi.liteskullapi.SkullAPI;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class ParcelSendingGUI extends GuiView {

    private final Plugin plugin;
    private final BukkitScheduler scheduler;
    private final PluginConfiguration config;
    private final MiniMessage miniMessage;
    private final ItemStorageRepository itemStorageRepository;
    private final ParcelRepository parcelRepository;
    private final LockerRepository lockerRepository;
    private final NotificationAnnouncer announcer;
    private final ParcelContentRepository parcelContentRepository;
    private final UserRepository userRepository;
    private final SkullAPI skullAPI;

    private String parcelName;
    private String parcelDescription;
    private ParcelSize size;
    private UUID receiver;
    private boolean priority = false;
    private UUID entryLocker = UUID.randomUUID();
    private UUID destinationLocker = UUID.randomUUID();

    private ConfigItem receiverItem;

    private Gui gui;

    public ParcelSendingGUI(Plugin plugin,
                            PluginConfiguration config,
                            MiniMessage miniMessage,
                            ItemStorageRepository itemStorageRepository,
                            ParcelRepository parcelRepository, LockerRepository lockerRepository,
                            NotificationAnnouncer announcer,
                            ParcelContentRepository parcelContentRepository,
                            UserRepository userRepository,
                            SkullAPI skullAPI) {
        this.plugin = plugin;
        this.config = config;
        this.miniMessage = miniMessage;
        this.itemStorageRepository = itemStorageRepository;
        this.parcelRepository = parcelRepository;
        this.lockerRepository = lockerRepository;
        this.announcer = announcer;
        this.parcelContentRepository = parcelContentRepository;
        this.userRepository = userRepository;
        this.skullAPI = skullAPI;
        this.scheduler = this.plugin.getServer().getScheduler();
    }

    public void show(Player player, ParcelSendingGUIState guiState) {
        this.parcelName = guiState.getParcelName();
        this.parcelDescription = guiState.getParcelDescription();
        this.size = guiState.getSize();
        this.receiver = guiState.getReceiver();
        this.priority = guiState.isPriority();
        this.entryLocker = UUID.randomUUID(); //guiState.getEntryLocker(); - temp workaround for NPE
        this.destinationLocker = UUID.randomUUID(); //guiState.getDestinationLocker(); - temp workaround for NPE

        this.show(player);
    }

    @Override
    public void show(Player player) {
        PluginConfiguration settings = this.config;
        PluginConfiguration.GuiSettings guiSettings = settings.guiSettings;

        Component guiTitle = this.miniMessage.deserialize(guiSettings.parcelLockerSendingGuiTitle);

        this.gui = Gui.gui()
            .rows(6)
            .disableAllInteractions()
            .title(guiTitle)
            .create();

        GuiItem backgroundItem = guiSettings.mainGuiBackgroundItem.toGuiItem();
        GuiItem cornerItem = guiSettings.cornerItem.toGuiItem();
        ConfigItem nameItem = guiSettings.parcelNameItem;
        GuiItem nameGuiItem = nameItem.toGuiItem(event -> {
            SignGUI nameSignGui = SignGUI.builder()
                .setColor(DyeColor.BLACK)
                .setType(Material.OAK_SIGN)
                .setLine(0, "Enter parcel name:")
                .setHandler((p, result) -> {
                    String name = result.getLineWithoutColor(1);

                    if (name.isEmpty() || name.isBlank()) {
                        this.announcer.sendMessage(player, settings.messages.parcelNameCannotBeEmpty);
                        return Collections.emptyList();
                    }

                    this.parcelName = name;
                    this.announcer.sendMessage(player, settings.messages.parcelNameSet);

                    List<String> lore = nameItem.lore;
                    if (lore.size() > 1) {
                        lore.remove(1);
                    }

                    lore.add(this.config.guiSettings.parcelNameSetLine.replace("{NAME}", this.parcelName == null ? "None" : this.parcelName));

                    this.gui.updateItem(21, nameItem
                        .setLore(lore)
                        .toItemStack());
                    return List.of(SignGUIAction.runSync((JavaPlugin) this.plugin, () -> this.gui.open(player)));
                })
                .build();
            nameSignGui.open(player);
        });

        ConfigItem descriptionItem = guiSettings.parcelDescriptionItem;
        GuiItem descriptionGuiItem = descriptionItem.toGuiItem(event -> {
            SignGUI descriptionSignGui = SignGUI.builder()
                .setColor(DyeColor.BLACK)
                .setType(Material.OAK_SIGN)
                .setLine(0, "Enter parcel description:")
                .setHandler((p, result) -> {
                    String description = result.getLineWithoutColor(1);

                    this.parcelDescription = description;
                    this.announcer.sendMessage(player, settings.messages.parcelDescriptionSet);

                    List<String> lore = descriptionItem.lore;
                    if (lore.size() > 1) {
                        lore.remove(1);
                    }

                    lore.add(this.config.guiSettings.parcelDescriptionSetLine.replace("{DESCRIPTION}", description));

                    this.gui.updateItem(22, descriptionItem
                        .setLore(lore)
                        .toItemStack());
                    return List.of(SignGUIAction.runSync((JavaPlugin) this.plugin, () -> this.gui.open(player)));
                })
                .build();
            descriptionSignGui.open(player);
        });

        this.receiverItem = guiSettings.parcelReceiverItem;
        GuiItem destinationItem = guiSettings.parcelDestinationLockerItem.toGuiItem(event -> new DestinationSelectionGUI(
            this.plugin,
            this.scheduler,
            this.config,
            this.miniMessage,
            this.lockerRepository,
            this,
            new ParcelSendingGUIState(this.parcelName, this.parcelDescription, this.size, this.receiver, this.priority, this.entryLocker, this.destinationLocker)
        ).show(player));

        GuiItem storageItem = guiSettings.parcelStorageItem.toGuiItem(event -> {
            ParcelItemStorageGUI storageGUI = new ParcelItemStorageGUI(
                this.plugin,
                this.config,
                this.miniMessage,
                this.itemStorageRepository,
                this.parcelRepository,
                this.lockerRepository,
                this.announcer,
                this.parcelContentRepository,
                this.userRepository,
                this.skullAPI
            );
            this.itemStorageRepository.find(player.getUniqueId()).thenAccept(result -> {
                    if (result.isPresent()) {
                        int slotsSize = result.get().items().size();
                        if (slotsSize <= 9) {
                            this.scheduler.runTask(this.plugin, () -> storageGUI.show(player, this.size));
                        } else if (slotsSize <= 18 && this.size == ParcelSize.SMALL) {
                            this.scheduler.runTask(this.plugin, () -> storageGUI.show(player, ParcelSize.MEDIUM));
                        } else {
                            this.scheduler.runTask(this.plugin, () -> storageGUI.show(player, ParcelSize.LARGE));
                        }
                    } else {
                        this.scheduler.runTask(this.plugin, () -> storageGUI.show(player, this.size));
                    }
                }
            ).orTimeout(2, TimeUnit.SECONDS);
        });
        GuiItem submitItem = guiSettings.submitParcelItem.toGuiItem(event ->
            this.itemStorageRepository.find(player.getUniqueId()).thenAccept(result -> {
                if (result.isEmpty() || result.get().items().isEmpty()) {
                    this.announcer.sendMessage(player, settings.messages.parcelCannotBeEmpty);
                    this.gui.close(player);
                    return;
                }

                Parcel parcel = Parcel.builder()
                    .size(this.size)
                    .priority(this.priority)
                    .sender(player.getUniqueId())
                    .uuid(UUID.randomUUID())
                    .name(this.parcelName)
                    .description(this.parcelDescription)
                    .entryLocker(this.entryLocker)
                    .destinationLocker(this.destinationLocker)
                    .receiver(this.receiver)
                    .sender(player.getUniqueId())
                    .build();

                this.parcelRepository.save(parcel).thenAccept(unused -> {

                    this.parcelContentRepository.save(new ParcelContent(parcel.uuid(), result.get().items())
                    ).thenAccept(none -> this.itemStorageRepository.remove(player.getUniqueId()));

                    this.announcer.sendMessage(player, settings.messages.parcelSent);
                    this.gui.close(player);
                }).whenComplete(ExceptionHandler.handler()
                    .andThen((unused, throwable) -> {
                            if (throwable != null) {
                                this.announcer.sendMessage(player, settings.messages.parcelFailedToSend);
                            }
                        }
                    ));
            }).orTimeout(5, TimeUnit.SECONDS));

        GuiItem closeItem = guiSettings.closeItem.toGuiItem(event ->
            new LockerMainGUI(this.plugin,
                this.miniMessage,
                this.config,
                this.itemStorageRepository,
                this.parcelRepository,
                this.lockerRepository,
                this.announcer,
                this.parcelContentRepository,
                this.userRepository,
                this.skullAPI
            ).show(player));

        ConfigItem smallButton = guiSettings.smallParcelSizeItem;
        ConfigItem mediumButton = guiSettings.mediumParcelSizeItem;
        ConfigItem largeButton = guiSettings.largeParcelSizeItem;
        ConfigItem priorityItem = guiSettings.priorityItem;

        for (int slot : CORNER_SLOTS) {
            this.gui.setItem(slot, cornerItem);
        }

        for (int slot : BORDER_SLOTS) {
            this.gui.setItem(slot, backgroundItem);
        }

        // TODO: size buttons not persisiting after actions/closing GUI
        // easy fix - cache the selected size and priority in the ParcelSendingGUI class

        this.gui.setItem(12, smallButton.toGuiItem(event -> this.setSelected(this.gui, ParcelSize.SMALL)));
        this.gui.setItem(13, mediumButton.toGuiItem(event -> this.setSelected(this.gui, ParcelSize.MEDIUM)));
        this.gui.setItem(14, largeButton.toGuiItem(event -> this.setSelected(this.gui, ParcelSize.LARGE)));
        this.gui.setItem(21, nameGuiItem);
        this.gui.setItem(22, descriptionGuiItem);
        this.gui.setItem(23, this.receiverItem.toGuiItem(event -> new ReceiverSelectionGui(
            this.plugin,
            this.scheduler,
            this.config,
            this.miniMessage,
            this.userRepository,
            this,
            this.skullAPI,
            new ParcelSendingGUIState(this.parcelName, this.parcelDescription, this.size, this.receiver, this.priority, this.entryLocker, this.destinationLocker),
            this.receiver
        ).show(player)));
        this.gui.setItem(30, destinationItem);
        this.gui.setItem(37, storageItem);
        this.gui.setItem(43, submitItem);
        this.gui.setItem(42, priorityItem.toGuiItem(event -> this.setSelected(this.gui, !this.priority)));
        this.gui.setItem(49, closeItem);

        this.setSelected(this.gui, this.size == null ? ParcelSize.SMALL : this.size);

        this.gui.open(player);
    }

    private void setSelected(Gui gui, ParcelSize size) {
        PluginConfiguration.GuiSettings settings = this.config.guiSettings;
        this.size = size;

        ConfigItem smallButton = size == ParcelSize.SMALL ? settings.selectedSmallParcelSizeItem : settings.smallParcelSizeItem;
        ConfigItem mediumButton = size == ParcelSize.MEDIUM ? settings.selectedMediumParcelSizeItem : settings.mediumParcelSizeItem;
        ConfigItem largeButton = size == ParcelSize.LARGE ? settings.selectedLargeParcelSizeItem : settings.largeParcelSizeItem;
        ConfigItem priorityButton = this.priority ? settings.selectedPriorityItem : settings.priorityItem;

        gui.updateItem(12, smallButton.toItemStack());
        gui.updateItem(13, mediumButton.toItemStack());
        gui.updateItem(14, largeButton.toItemStack());
        gui.updateItem(42, priorityButton.toItemStack());
    }

    private void setSelected(Gui gui, boolean priority) {
        PluginConfiguration.GuiSettings settings = this.config.guiSettings;
        this.priority = priority;

        ConfigItem priorityButton = priority ? settings.selectedPriorityItem : settings.priorityItem;

        gui.updateItem(42, priorityButton.toItemStack());
    }

    public void updateReceiverItem(Player player, UUID receiverUuid, String receiverName) {
        this.receiver = receiverUuid;
        PluginConfiguration settings = this.config;
        PluginConfiguration.GuiSettings guiSettings = settings.guiSettings;

        if (this.receiverItem.lore.size() > 1) {
            this.receiverItem.lore.remove(1);
        }

        if (receiverName != null) {
            this.receiverItem.lore.add(guiSettings.parcelReceiverGuiSetLine.replace("{RECEIVER}", receiverName));
            this.receiverItem.setGlow(true);
        }

        this.gui.updateItem(23, this.receiverItem.toItemStack());
        this.announcer.sendMessage(player, settings.messages.parcelReceiverSet);
    }

    public void updateDestinationItem(Player player, UUID destinationLockerUuid, String destinationLockerDesc) {
        this.destinationLocker = destinationLockerUuid;
        PluginConfiguration settings = this.config;
        PluginConfiguration.GuiSettings guiSettings = settings.guiSettings;

        if (this.receiverItem.lore.size() > 1) {
            this.receiverItem.lore.remove(1);
        }

        if (destinationLockerDesc != null) {
            this.receiverItem.lore.add(guiSettings.parcelDestinationSetLine.replace("{DESCRIPTION}", destinationLockerDesc));
            this.receiverItem.setGlow(true);
        }

        this.gui.updateItem(30, this.receiverItem.toItemStack());
        this.announcer.sendMessage(player, settings.messages.parcelDestinationSet);
    }
}
