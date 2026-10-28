package com.eternalcode.parcellockers.gui.implementation.locker;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eternalcode.commons.scheduler.Scheduler;
import com.eternalcode.parcellockers.configuration.implementation.PluginConfig.GuiSettings;
import com.eternalcode.parcellockers.configuration.serializable.ConfigItem;
import com.eternalcode.parcellockers.gui.GuiManager;
import com.eternalcode.parcellockers.parcel.Parcel;
import com.eternalcode.parcellockers.parcel.ParcelSize;
import com.eternalcode.parcellockers.parcel.ParcelStatus;
import dev.triumphteam.gui.TriumphGui;
import dev.triumphteam.gui.components.GuiAction;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.StorageGui;
import java.lang.reflect.Field;
import java.util.List;
import java.util.UUID;
import java.util.function.BiFunction;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ReturnDepositGuiTest {

    private Field serverField;
    private Field triumphPluginField;
    private Object previousServer;
    private Object previousTriumphPlugin;

    @BeforeEach
    void installGuiEnvironment() throws ReflectiveOperationException {
        Server server = mock(Server.class);
        when(server.getPluginManager()).thenReturn(mock(PluginManager.class));
        this.serverField = Bukkit.class.getDeclaredField("server");
        this.serverField.setAccessible(true);
        this.previousServer = this.serverField.get(null);
        this.serverField.set(null, server);

        this.triumphPluginField = TriumphGui.class.getDeclaredField("PLUGIN");
        this.triumphPluginField.setAccessible(true);
        this.previousTriumphPlugin = this.triumphPluginField.get(null);
        TriumphGui.init(mock(Plugin.class));
    }

    @AfterEach
    void restoreGuiEnvironment() throws ReflectiveOperationException {
        this.serverField.set(null, this.previousServer);
        this.triumphPluginField.set(null, this.previousTriumphPlugin);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    void emptyConfirmationUsesNormalReturnValidationFlow() throws ReflectiveOperationException {
        Scheduler scheduler = mock(Scheduler.class);
        GuiSettings settings = mock(GuiSettings.class);
        settings.parcelReturnDepositGuiTitle = "Return items";
        settings.mainGuiBackgroundItem = mock(ConfigItem.class);
        settings.confirmReturnItem = mock(ConfigItem.class);
        MiniMessage miniMessage = mock(MiniMessage.class);
        when(miniMessage.deserialize("Return items")).thenReturn(Component.empty());
        GuiManager guiManager = mock(GuiManager.class);
        Player player = mock(Player.class);
        Parcel parcel = new Parcel(
            UUID.randomUUID(), UUID.randomUUID(), "parcel", "description", false,
            UUID.randomUUID(), ParcelSize.SMALL, UUID.randomUUID(), UUID.randomUUID(), ParcelStatus.COLLECTED
        );

        StorageGui gui = mock(StorageGui.class);
        when(gui.getRows()).thenReturn(2);
        Inventory inventory = mock(Inventory.class);
        when(inventory.getContents()).thenReturn(new ItemStack[18]);
        when(gui.getInventory()).thenReturn(inventory);
        when(settings.mainGuiBackgroundItem.toGuiItem(any())).thenReturn(mock(GuiItem.class));
        ArgumentCaptor<GuiAction<InventoryClickEvent>> confirmAction = ArgumentCaptor.forClass(GuiAction.class);
        when(settings.confirmReturnItem.toGuiItem(confirmAction.capture())).thenReturn(mock(GuiItem.class));
        BiFunction<Component, Integer, StorageGui> guiFactory = (title, rows) -> gui;

        new ReturnDepositGui(scheduler, settings, miniMessage, guiManager, parcel, guiFactory).show(player);
        InventoryClickEvent click = mock(InventoryClickEvent.class);
        confirmAction.getValue().execute(click);

        verify(click).setCancelled(true);
        verify(gui).close(player);
        verify(guiManager).returnParcel(player, parcel, List.of());
    }
}
