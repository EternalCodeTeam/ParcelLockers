package com.eternalcode.parcellockers.util;

import com.eternalcode.parcellockers.exception.ParcelLockersException;
import com.eternalcode.parcellockers.locker.Locker;
import com.eternalcode.parcellockers.locker.repository.LockerRepository;
import com.eternalcode.parcellockers.parcel.Parcel;
import com.eternalcode.parcellockers.user.UserManager;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.spotify.futures.CompletableFutures;
import de.eldoria.jacksonbukkit.JacksonPaper;
import io.sentry.Sentry;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Blocking;
import panda.utilities.text.Formatter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class ItemUtil {

    private static final ObjectMapper JSON = JsonMapper.builder()
        .addModule(JacksonPaper.builder()
            .useLegacyItemStackSerialization()
            .build()
        )
        .build();

    private ItemUtil() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static String serialize(ItemStack stack) {
        try {
            return JSON.writeValueAsString(stack);
        } catch (JsonProcessingException e) {
            Sentry.captureException(e);
            throw new ParcelLockersException("Failed to serialize itemstack", e);
        }
    }

    public static String serializeItems(List<ItemStack> stack) {
        try {
            return JSON.writeValueAsString(stack);
        } catch (JsonProcessingException e) {
            Sentry.captureException(e);
            throw new ParcelLockersException("Failed to serialize itemstack", e);
        }
    }

    public static ItemStack deserialize(String string) {
        try {
            return JSON.readValue(string, ItemStack.class);
        } catch (JsonProcessingException e) {
            Sentry.captureException(e);
            throw new ParcelLockersException("Failed to deserialize itemstack", e);
        }
    }

    public static List<ItemStack> deserializeItems(String string) {
        try {
            return JSON.readValue(string, JSON.getTypeFactory().constructCollectionType(List.class, ItemStack.class));
        } catch (JsonProcessingException e) {
            Sentry.captureException(e);
            throw new ParcelLockersException("Failed to deserialize itemstack", e);
        }
    }

    @Blocking
    public static List<String> replaceParcelPlaceholders(Parcel parcel, List<String> lore, UserManager userManager, LockerRepository lockerRepository) {
        if (lore == null || lore.isEmpty()) {
            return Collections.emptyList();
        }

        String senderName = getName(parcel.sender(), userManager).join();
        String receiver = getName(parcel.receiver(), userManager).join();

        List<String> recipients = parcel.recipients().stream()
            .map(uuid -> getName(uuid, userManager))
            .collect(CompletableFutures.joinList())
            .join();

        Formatter formatter = new Formatter()
            .register("{UUID}", parcel.uuid().toString())
            .register("{NAME}", parcel.name())
            .register("{SENDER}", senderName)
            .register("{RECEIVER}", receiver)
            .register("{SIZE}", parcel.size().toString())
            .register("{PRIORITY}", parcel.priority() ? "&aYes" : "&cNo")
            .register("{DESCRIPTION}", parcel.description())
            .register("{RECIPIENTS}", recipients.toString());

        Optional<Locker> lockerOptional = lockerRepository.findByUUID(parcel.destinationLocker()).join();

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

    @ApiStatus.Internal
    private static CompletableFuture<String> getName(UUID userUuid, UserManager userManager) {
        return userManager.getUser(userUuid).thenApply(userOptional -> userOptional
            .map(user -> user.name())
            .orElse("Unknown")
        );
    }


}
