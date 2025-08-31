package com.eternalcode.parcellockers.parcel.util;

import com.eternalcode.multification.shared.Formatter;
import com.eternalcode.parcellockers.gui.GuiManager;
import com.eternalcode.parcellockers.locker.Locker;
import com.eternalcode.parcellockers.parcel.Parcel;
import com.eternalcode.parcellockers.user.User;
import com.spotify.futures.CompletableFutures;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Blocking;

public class PlaceholderUtil {

    public static CompletableFuture<List<String>> replaceParcelPlaceholdersAsync(
        Parcel parcel,
        List<String> lore,
        GuiManager guiManager
    ) {

        if (lore == null || lore.isEmpty()) {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }

        CompletableFuture<String> senderNameFuture = getName(parcel.sender(), guiManager);
        CompletableFuture<String> receiverNameFuture = getName(parcel.receiver(), guiManager);
        CompletableFuture<Optional<Locker>> lockerFuture = getLockerInfo(parcel.destinationLocker(), guiManager);

        return CompletableFutures.combine(
                senderNameFuture, receiverNameFuture, lockerFuture,
                (senderName, receiverName, lockerOptional) -> {

                    Formatter formatter = new Formatter()
                        .register("{UUID}", parcel.uuid().toString())
                        .register("{NAME}", parcel.name())
                        .register("{SENDER}", senderName)
                        .register("{RECEIVER}", receiverName)
                        .register("{SIZE}", StringUtils.capitalize(parcel.size().toString().toLowerCase()))
                        .register("{PRIORITY}", parcel.priority() ? "&aYes" : "&cNo")
                        .register("{DESCRIPTION}", parcel.description() != null ? parcel.description() : "-");

                    if (lockerOptional.isPresent()) {
                        Locker locker = lockerOptional.get();
                        formatter.register("{POSITION_X}", String.valueOf(locker.position().x()))
                            .register("{POSITION_Y}", String.valueOf(locker.position().y()))
                            .register("{POSITION_Z}", String.valueOf(locker.position().z()));
                    } else {
                        formatter.register("{POSITION_X}", "-")
                            .register("{POSITION_Y}", "-")
                            .register("{POSITION_Z}", "-");
                    }

                    List<String> newLore = new ArrayList<>();
                    for (String line : lore) {
                        newLore.add(formatter.format(line));
                    }

                    return newLore;
                })
            .toCompletableFuture()
            .orTimeout(5, TimeUnit.SECONDS)
            .exceptionally(throwable -> {
                throwable.printStackTrace();
                return createFallbackLore(parcel, lore);
            });
    }

    private static CompletableFuture<String> getName(UUID userUuid, GuiManager guiManager) {
        return guiManager.getUser(userUuid)
            .orTimeout(3, TimeUnit.SECONDS)
            .thenApply(userOptional -> userOptional
                .map(User::name)
                .orElse("Unknown"))
            .exceptionally(throwable -> {
                System.err.println("Failed to get user name for " + userUuid + ": " + throwable.getMessage());
                return "Unknown";
            });
    }

    private static CompletableFuture<Optional<Locker>> getLockerInfo(UUID lockerId, GuiManager guiManager) {
        if (lockerId == null) {
            return CompletableFuture.completedFuture(Optional.empty());
        }

        return guiManager.getLocker(lockerId)
            .orTimeout(3, TimeUnit.SECONDS)
            .exceptionally(throwable -> {
                System.err.println("Failed to get locker info for " + lockerId + ": " + throwable.getMessage());
                return Optional.empty();
            });
    }

    private static List<String> createFallbackLore(Parcel parcel, List<String> originalLore) {
        // Podstawowe placeholdery bez asynchronicznych wywołań
        Formatter fallbackFormatter = new Formatter()
            .register("{UUID}", parcel.uuid().toString())
            .register("{NAME}", parcel.name())
            .register("{SENDER}", "Loading...")
            .register("{RECEIVER}", "Loading...")
            .register("{SIZE}", StringUtils.capitalize(parcel.size().toString().toLowerCase()))
            .register("{PRIORITY}", parcel.priority() ? "&aYes" : "&cNo")
            .register("{DESCRIPTION}", parcel.description() != null ? parcel.description() : "-")
            .register("{POSITION_X}", "-")
            .register("{POSITION_Y}", "-")
            .register("{POSITION_Z}", "-");

        return originalLore.stream()
            .map(fallbackFormatter::format)
            .toList();
    }

    // Zachowaj starą metodę dla kompatybilności wstecznej, ale z lepszym error handlingiem
    @Blocking
    @Deprecated
    public static List<String> replaceParcelPlaceholders(Parcel parcel, List<String> lore, GuiManager guiManager) {
        try {
            return replaceParcelPlaceholdersAsync(parcel, lore, guiManager).join();
        } catch (Exception e) {
            System.err.println("Timeout or error in replaceParcelPlaceholders: " + e.getMessage());
            return createFallbackLore(parcel, lore);
        }
    }
}


