package com.eternalcode.parcellockers.gui.implementation.locker;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eternalcode.commons.scheduler.Scheduler;
import com.eternalcode.parcellockers.configuration.implementation.PluginConfig.GuiSettings;
import com.eternalcode.parcellockers.configuration.serializable.ConfigItem;
import com.eternalcode.parcellockers.gui.GuiManager;
import com.eternalcode.parcellockers.notification.NoticeService;
import com.eternalcode.parcellockers.parcel.Parcel;
import com.eternalcode.parcellockers.shared.PageResult;
import dev.triumphteam.gui.TriumphGui;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.PaginatedGui;
import java.lang.reflect.Field;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ReturnGuiTest {

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

    @Test
    void emptyResultMutatesGuiOnlyInsideScheduledMainThreadCallback() throws ReflectiveOperationException {
        GuiSettings settings = mock(GuiSettings.class);
        settings.parcelReturnGuiTitle = "Return parcels";
        settings.parcelReturnRowItem = mock(ConfigItem.class);
        settings.closeItem = mock(ConfigItem.class);
        settings.cornerItem = mock(ConfigItem.class);
        settings.mainGuiBackgroundItem = mock(ConfigItem.class);
        settings.noReturnableParcelsItem = mock(ConfigItem.class);
        GuiItem emptyItem = mock(GuiItem.class);
        when(settings.noReturnableParcelsItem.toGuiItem()).thenReturn(emptyItem);

        Scheduler scheduler = mock(Scheduler.class);
        GuiManager guiManager = mock(GuiManager.class);
        MiniMessage miniMessage = mock(MiniMessage.class);
        NoticeService noticeService = mock(NoticeService.class);
        PaginatedGui gui = mock(PaginatedGui.class);
        Player player = mock(Player.class);
        UUID playerId = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(playerId);
        when(miniMessage.deserialize("Return parcels")).thenReturn(Component.empty());
        CompletableFuture<PageResult<Parcel>> result = new CompletableFuture<>();
        when(guiManager.getReturnableParcels(any(), any())).thenReturn(result);

        Function<Component, PaginatedGui> guiFactory = ignored -> gui;
        ReturnGui returnGui = new ReturnGui(
            settings, scheduler, guiManager, miniMessage, guiFactory);
        returnGui.show(player);
        clearInvocations(gui, scheduler);

        result.complete(PageResult.empty());

        verify(gui, never()).setItem(anyInt(), any(GuiItem.class));
        verify(gui, never()).open(player);
        ArgumentCaptor<Runnable> callback = ArgumentCaptor.forClass(Runnable.class);
        verify(scheduler).run(callback.capture());

        callback.getValue().run();

        verify(gui).setItem(22, emptyItem);
        verify(gui).open(player);
    }
}
