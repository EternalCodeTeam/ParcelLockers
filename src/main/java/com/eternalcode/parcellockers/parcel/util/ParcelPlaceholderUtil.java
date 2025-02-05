package com.eternalcode.parcellockers.parcel.util;

import com.eternalcode.parcellockers.locker.Locker;
import com.eternalcode.parcellockers.locker.repository.LockerRepository;
import com.eternalcode.parcellockers.parcel.Parcel;
import com.eternalcode.parcellockers.user.UserManager;
import org.jetbrains.annotations.Blocking;
import panda.utilities.text.Formatter;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class ParcelPlaceholderUtil {

    @Blocking
    public static List<String> replaceParcelPlaceholders(Parcel parcel, List<String> lore, UserManager userManager, LockerRepository lockerRepository) {
        if (lore == null || lore.isEmpty()) {
            return Collections.emptyList();
        }

        String senderName = getName(parcel.sender(), userManager).join();
        String receiver = getName(parcel.receiver(), userManager).join();

        Formatter formatter = new Formatter()
            .register("{UUID}", parcel.uuid().toString())
            .register("{NAME}", parcel.name())
            .register("{SENDER}", senderName)
            .register("{RECEIVER}", receiver)
            .register("{SIZE}", parcel.size().toString())
            .register("{PRIORITY}", parcel.priority() ? "&aYes" : "&cNo")
            .register("{DESCRIPTION}", parcel.description());

        Optional<Locker> lockerOptional = lockerRepository.findByUUID(parcel.destinationLocker()).join();

        if (lockerOptional.isPresent()) {
            Locker locker = lockerOptional.get();
            formatter.register("{POSITION_X}", locker.position().x())
                .register("{POSITION_Y}", locker.position().y())
                .register("{POSITION_Z}", locker.position().z());
        }
        else {
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

    private static CompletableFuture<String> getName(UUID userUuid, UserManager userManager) {
        return userManager.getUser(userUuid).thenApply(userOptional -> userOptional
            .map(user -> user.name())
            .orElse("Unknown")
        );
    }
}
