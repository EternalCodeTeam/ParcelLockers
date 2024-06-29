package com.eternalcode.parcellockers.gui.implementation.locker;

import com.eternalcode.parcellockers.configuration.implementation.ConfigItem;
import com.eternalcode.parcellockers.configuration.implementation.PluginConfiguration;
import com.eternalcode.parcellockers.content.ParcelContent;
import com.eternalcode.parcellockers.content.repository.ParcelContentRepository;
import com.eternalcode.parcellockers.gui.GuiView;
import com.eternalcode.parcellockers.itemstorage.repository.ItemStorageRepository;
import com.eternalcode.parcellockers.notification.NotificationAnnouncer;
import com.eternalcode.parcellockers.parcel.Parcel;
import com.eternalcode.parcellockers.parcel.ParcelSize;
import com.eternalcode.parcellockers.parcel.repository.ParcelRepository;
import de.rapha149.signgui.SignGUI;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class ParcelSendingGUI extends GuiView {

    private final Plugin plugin;
    private final BukkitScheduler scheduler;
    private final PluginConfiguration config;
    private final MiniMessage miniMessage;
    private final ItemStorageRepository itemStorageRepository;
    private final ParcelRepository parcelRepository;
    private final NotificationAnnouncer announcer;
    private final ParcelContentRepository parcelContentRepository;
    private final Map<UUID, String> parcelNames = new HashMap<>();
    private ParcelSize size;
    private boolean priority;

    public ParcelSendingGUI(Plugin plugin,
                            PluginConfiguration config,
                            MiniMessage miniMessage,
                            ItemStorageRepository itemStorageRepository,
                            ParcelRepository parcelRepository,
                            NotificationAnnouncer announcer,
                            ParcelContentRepository parcelContentRepository) {
        this.plugin = plugin;
        this.config = config;
        this.miniMessage = miniMessage;
        this.itemStorageRepository = itemStorageRepository;
        this.parcelRepository = parcelRepository;
        this.announcer = announcer;
        this.parcelContentRepository = parcelContentRepository;
        this.scheduler = this.plugin.getServer().getScheduler();
    }

    @Override
    public void show(Player player) {
        PluginConfiguration settings = this.config;
        PluginConfiguration.GuiSettings guiSettings = settings.guiSettings;

        Component guiTitle = this.miniMessage.deserialize(guiSettings.parcelLockerSendingGuiTitle);

        Gui gui = Gui.gui()
            .rows(6)
            .disableAllInteractions()
            .title(guiTitle)
            .create();

        GuiItem backgroundItem = guiSettings.mainGuiBackgroundItem.toGuiItem();
        GuiItem cornerItem = guiSettings.cornerItem.toGuiItem();
        ConfigItem parcelNameItem = guiSettings.parcelNameItem;
        GuiItem nameItem = parcelNameItem.toGuiItem(event -> {
            SignGUI nameSignGui = SignGUI.builder()
                .setColor(DyeColor.BLACK)
                .setType(Material.OAK_SIGN)
                .setLine(0, "Enter the parcel name:")
                .setHandler((p, result) -> {

                    String name = result.getLineWithoutColor(1);

                    if (name.isEmpty() || name.isBlank()) {
                        announcer.sendMessage(player, settings.messages.parcelNameCannotBeEmpty);
                        return Collections.emptyList();
                    }

                    parcelNames.put(player.getUniqueId(), name);
                    announcer.sendMessage(player, settings.messages.parcelNameSet);

                    List<String> lore = parcelNameItem.lore;
                    lore.add(this.config.guiSettings.parcelNameSetLine.replace("{NAME}", this.parcelNames.getOrDefault(player.getUniqueId(), "")));

                    gui.updateItem(21, parcelNameItem
                        .setLore(lore)
                        .toItemStack());
                    return Collections.emptyList();

                })
                .build();
            nameSignGui.open(player);
        });

        GuiItem descriptionItem = guiSettings.parcelDescriptionItem.toGuiItem();
        GuiItem receiverItem = guiSettings.parcelReceiverItem.toGuiItem();
        GuiItem destinationItem = guiSettings.parcelDestinationLockerItem.toGuiItem();

        GuiItem storageItem = guiSettings.parcelStorageItem.toGuiItem(event -> {
            ParcelItemStorageGUI storageGUI = new ParcelItemStorageGUI(
                plugin,
                this.config,
                this.miniMessage,
                itemStorageRepository,
                parcelRepository,
                announcer,
                this.parcelContentRepository
            );
            itemStorageRepository.find(player.getUniqueId()).whenComplete((result, error) -> {
                    if (result.isPresent()) {
                        int slotsSize = result.get().items().size();
                        if (slotsSize <= 9) {
                            scheduler.runTask(this.plugin, () -> storageGUI.show(player, this.size));
                        } else if (slotsSize <= 18 && this.size == ParcelSize.SMALL) {
                            scheduler.runTask(this.plugin, () -> storageGUI.show(player, ParcelSize.MEDIUM));
                        } else {
                            scheduler.runTask(this.plugin, () -> storageGUI.show(player, ParcelSize.LARGE));
                        }
                    } else {
                        scheduler.runTask(this.plugin, () -> storageGUI.show(player, this.size));
                    }
                })
                .orTimeout(2, TimeUnit.SECONDS);
        });
        GuiItem submitItem = guiSettings.submitParcelItem.toGuiItem(event ->
            this.itemStorageRepository.find(player.getUniqueId()).thenAccept(result -> {
                if (result.isEmpty() || result.get().items().isEmpty()) {
                    announcer.sendMessage(player, settings.messages.parcelCannotBeEmpty);
                    gui.close(player);
                    return;
                }

                Parcel parcel = Parcel.builder()
                    .size(this.size)
                    .priority(this.priority)
                    .sender(player.getUniqueId())
                    .uuid(UUID.randomUUID())
                    .name(player.getName() + "'s Parcel")
                    .description("None")
                    .destinationLocker(UUID.randomUUID())
                    .entryLocker(UUID.randomUUID())
                    .receiver(player.getUniqueId())
                    .sender(player.getUniqueId())
                    .build();

                this.parcelRepository.save(parcel).whenComplete((unused, throwable) -> {
                    if (throwable != null) {
                        announcer.sendMessage(player, settings.messages.parcelFailedToSend);
                        throwable.printStackTrace();
                        return;
                    }

                    this.parcelContentRepository.save(new ParcelContent(parcel.uuid(), result.get().items())
                    ).thenAccept(none -> this.itemStorageRepository.remove(player.getUniqueId()));

                    announcer.sendMessage(player, settings.messages.parcelSent);
                    gui.close(player);
                });
            }).orTimeout(5, TimeUnit.SECONDS));

        GuiItem closeItem = guiSettings.closeItem.toGuiItem(event ->
            new LockerMainGUI(plugin,
                this.miniMessage,
                this.config,
                itemStorageRepository,
                parcelRepository,
                announcer,
                parcelContentRepository
            ).show(player));

        ConfigItem smallButton = guiSettings.smallParcelSizeItem;
        ConfigItem mediumButton = guiSettings.mediumParcelSizeItem;
        ConfigItem largeButton = guiSettings.largeParcelSizeItem;
        ConfigItem priorityItem = guiSettings.priorityItem;

        for (int slot : CORNER_SLOTS) {
            gui.setItem(slot, cornerItem);
        }

        for (int slot : BORDER_SLOTS) {
            gui.setItem(slot, backgroundItem);
        }

        this.setSelected(gui, ParcelSize.SMALL);
        this.priority = false;

        gui.setItem(12, smallButton.toGuiItem(event -> this.setSelected(gui, ParcelSize.SMALL)));
        gui.setItem(13, mediumButton.toGuiItem(event -> this.setSelected(gui, ParcelSize.MEDIUM)));
        gui.setItem(14, largeButton.toGuiItem(event -> this.setSelected(gui, ParcelSize.LARGE)));
        gui.setItem(21, nameItem);
        gui.setItem(37, storageItem);
        gui.setItem(43, submitItem);
        gui.setItem(48, priorityItem.toGuiItem(event -> this.setSelected(gui, !this.priority)));
        gui.setItem(49, closeItem);

        gui.open(player);
    }

    private void setSelected(Gui gui, ParcelSize size) {
        PluginConfiguration.GuiSettings settings = config.guiSettings;
        this.size = size;

        ConfigItem smallButton = size == ParcelSize.SMALL ? settings.selectedSmallParcelSizeItem : settings.smallParcelSizeItem;
        ConfigItem mediumButton = size == ParcelSize.MEDIUM ? settings.selectedMediumParcelSizeItem : settings.mediumParcelSizeItem;
        ConfigItem largeButton = size == ParcelSize.LARGE ? settings.selectedLargeParcelSizeItem : settings.largeParcelSizeItem;
        ConfigItem priorityButton = priority ? settings.selectedPriorityItem : settings.priorityItem;

        gui.updateItem(12, smallButton.toItemStack());
        gui.updateItem(13, mediumButton.toItemStack());
        gui.updateItem(14, largeButton.toItemStack());
        gui.updateItem(48, priorityButton.toItemStack());
    }

    private void setSelected(Gui gui, boolean priority) {
        PluginConfiguration.GuiSettings settings = config.guiSettings;
        this.priority = priority;

        ConfigItem priorityButton = priority ? settings.selectedPriorityItem : settings.priorityItem;

        gui.updateItem(48, priorityButton.toItemStack());
    }
}
