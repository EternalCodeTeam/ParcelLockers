package com.eternalcode.parcellockers.parcel.util;

import com.eternalcode.multification.shared.Formatter;
import com.eternalcode.parcellockers.gui.GuiManager;
import com.eternalcode.parcellockers.locker.Locker;
import com.eternalcode.parcellockers.parcel.Parcel;
import com.eternalcode.parcellockers.user.User;
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

    @Blocking
    public static List<String> replaceParcelPlaceholders(Parcel parcel, List<String> lore, GuiManager guiManager) {
        if (lore == null || lore.isEmpty()) {
            return Collections.emptyList();
        }

        String senderName = getName(parcel.sender(), guiManager).join();
        String receiver = getName(parcel.receiver(), guiManager).join();

        Formatter formatter = new Formatter()
            .register("{UUID}", parcel.uuid().toString())
            .register("{NAME}", parcel.name())
            .register("{SENDER}", senderName)
            .register("{RECEIVER}", receiver)
            .register("{SIZE}", StringUtils.capitalize(parcel.size().toString().toLowerCase()))
            .register("{PRIORITY}", parcel.priority() ? "&aYes" : "&cNo")
            .register("{DESCRIPTION}", parcel.description() != null ? parcel.description() : "-");

        Optional<Locker> lockerOptional = guiManager.getLocker(parcel.destinationLocker())
            .orTimeout(3, TimeUnit.SECONDS)
            .join();

        if (lockerOptional.isPresent()) {
            Locker locker = lockerOptional.get();
            formatter.register("{POSITION_X}", locker.position().x())
                .register("{POSITION_Y}", locker.position().y())
                .register("{POSITION_Z}", locker.position().z());
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
    }

    private static CompletableFuture<String> getName(UUID userUuid, GuiManager guiManager) {
        return guiManager.getUser(userUuid).thenApply(userOptional -> userOptional
            .map(User::name)
            .orElse("Unknown")
        );
    }
}
