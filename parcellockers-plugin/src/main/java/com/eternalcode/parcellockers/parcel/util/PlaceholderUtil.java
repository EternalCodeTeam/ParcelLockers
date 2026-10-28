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
import java.util.logging.Logger;
import org.apache.commons.lang3.StringUtils;

public class PlaceholderUtil {

    private static final Logger LOGGER = Logger.getLogger(PlaceholderUtil.class.getName());

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
                        .register("{UUID}", parcel.uuid() != null ? parcel.uuid().toString() : "-")
                        .register("{NAME}", parcel.name() != null ? parcel.name() : "-")
                        .register("{SENDER}", senderName)
                        .register("{RECEIVER}", receiverName)
                        .register("{SIZE}", parcel.size() != null ? StringUtils.capitalize(parcel.size().toString().toLowerCase()) : "-")
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
                LOGGER.log(java.util.logging.Level.WARNING, "Failed to replace parcel placeholders for parcel " + parcel.uuid(), throwable);
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
                LOGGER.log(java.util.logging.Level.WARNING, "Failed to get user name for " + userUuid, throwable);
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
                LOGGER.log(java.util.logging.Level.WARNING, "Failed to get locker info for " + lockerId, throwable);
                return Optional.empty();
            });
    }

    private static List<String> createFallbackLore(Parcel parcel, List<String> originalLore) {
        Formatter fallbackFormatter = new Formatter()
            .register("{UUID}", parcel.uuid() != null ? parcel.uuid().toString() : "-")
            .register("{NAME}", parcel.name() != null ? parcel.name() : "-")
            .register("{SENDER}", "Loading...")
            .register("{RECEIVER}", "Loading...")
            .register("{SIZE}", parcel.size() != null ? StringUtils.capitalize(parcel.size().toString().toLowerCase()) : "-")
            .register("{PRIORITY}", parcel.priority() ? "&aYes" : "&cNo")
            .register("{DESCRIPTION}", parcel.description() != null ? parcel.description() : "-")
            .register("{POSITION_X}", "-")
            .register("{POSITION_Y}", "-")
            .register("{POSITION_Z}", "-");

        return originalLore.stream()
            .map(fallbackFormatter::format)
            .toList();
    }

}


