package com.eternalcode.parcellockers.locker;

import com.eternalcode.parcellockers.configuration.implementation.PluginConfig;
import com.eternalcode.parcellockers.locker.event.LockerCreateEvent;
import com.eternalcode.parcellockers.locker.event.LockerDeleteEvent;
import com.eternalcode.parcellockers.locker.repository.LockerRepository;
import com.eternalcode.parcellockers.locker.validation.LockerValidationService;
import com.eternalcode.parcellockers.notification.NoticeService;
import com.eternalcode.parcellockers.parcel.repository.ParcelRepository;
import com.eternalcode.parcellockers.shared.Page;
import com.eternalcode.parcellockers.shared.PageResult;
import com.eternalcode.parcellockers.shared.Position;
import com.eternalcode.parcellockers.shared.validation.ValidationResult;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import eu.okaeri.configs.exception.ValidationException;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;

public class LockerManager {

    private final PluginConfig config;
    private final LockerRepository lockerRepository;
    private final LockerValidationService validationService;
    private final ParcelRepository parcelRepository;
    private final Server server;

    private final Cache<UUID, Locker> lockersByUUID;
    private final Cache<Position, Locker> lockersByPosition;

    public LockerManager(
        PluginConfig config,
        LockerRepository lockerRepository,
        LockerValidationService validationService,
        ParcelRepository parcelRepository,
        Server server
    ) {
        this.config = config;
        this.lockerRepository = lockerRepository;
        this.validationService = validationService;
        this.parcelRepository = parcelRepository;
        this.server = server;

        this.lockersByUUID = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofHours(2))
            .maximumSize(10_000)
            .build();
        this.lockersByPosition = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofHours(2))
            .maximumSize(10_000)
            .build();
    }

    public CompletableFuture<Optional<Locker>> get(UUID uniqueId) {
        if (uniqueId == null) {
            return CompletableFuture.completedFuture(Optional.empty());
        }

        Locker locker = this.lockersByUUID.getIfPresent(uniqueId);

        if (locker != null) {
            return CompletableFuture.completedFuture(Optional.of(locker));
        }

        return this.lockerRepository.find(uniqueId).thenApply(optionalLocker -> {
            optionalLocker.ifPresent(locker1 -> {
                this.lockersByUUID.put(locker1.uuid(), locker1);
                this.lockersByPosition.put(locker1.position(), locker1);
            });
            return optionalLocker;
        });
    }

    public CompletableFuture<Optional<Locker>> get(Position position) {
        Locker locker = this.lockersByPosition.getIfPresent(position);

        if (locker != null) {
            return CompletableFuture.completedFuture(Optional.of(locker));
        }

        return this.lockerRepository.find(position).thenApply(optionalLocker -> {
            optionalLocker.ifPresent(locker1 -> {
                this.lockersByUUID.put(locker1.uuid(), locker1);
                this.lockersByPosition.put(locker1.position(), locker1);
            });
            return optionalLocker;
        });
    }

    public CompletableFuture<PageResult<Locker>> get(Page page) {
        List<Locker> cached = List.copyOf(this.lockersByUUID.asMap().values());
        boolean hasNextPage = cached.size() > page.getLimit();
        if (!cached.isEmpty() && page.getOffset() == 0 && !hasNextPage) {
            return CompletableFuture.completedFuture(new PageResult<>(cached, false));
        }
        return this.lockerRepository.findPage(page);
    }

    public CompletableFuture<Locker> create(UUID uniqueId, String name, Position position, UUID playerUUID) {
        return CompletableFuture.supplyAsync(() -> {

            ValidationResult validation = this.validationService.validateCreateParameters(uniqueId, name, position);
            if (!validation.isValid()) {
                throw new ValidationException("Invalid locker parameters: " + validation.errorMessage());
            }

            Optional<Locker> existingByUUID = Optional.ofNullable(this.lockersByUUID.get(uniqueId, uuid -> null));
            Optional<Locker> existingByPosition = Optional.ofNullable(this.lockersByPosition.get(position, pos -> null));

            ValidationResult conflictCheck = this.validationService.validateNoConflicts(
                uniqueId, position, existingByUUID, existingByPosition);

            if (!conflictCheck.isValid()) {
                throw new ValidationException(conflictCheck.errorMessage());
            }

            Locker locker = new Locker(uniqueId, name, position);

            // Fire LockerCreateEvent
            LockerCreateEvent event = new LockerCreateEvent(locker, playerUUID);
            this.server.getPluginManager().callEvent(event);

            if (event.isCancelled()) {
                throw new ValidationException("Locker creation cancelled by event");
            }

            this.lockersByUUID.put(uniqueId, locker);
            this.lockersByPosition.put(position, locker);

            return this.lockerRepository.save(locker);
        }).thenCompose(Function.identity());
    }

    public CompletableFuture<Void> delete(UUID uniqueId, UUID playerUUID) {
        Locker cachedLocker = this.lockersByUUID.getIfPresent(uniqueId);

        CompletableFuture<Locker> lockerFuture = cachedLocker != null
            ? CompletableFuture.completedFuture(cachedLocker)
            : this.lockerRepository.find(uniqueId).thenApply(opt -> opt.orElse(null));

        return lockerFuture.thenCompose(locker -> {
            if (locker == null) {
                return CompletableFuture.completedFuture(null);
            }

            LockerDeleteEvent event = new LockerDeleteEvent(locker, playerUUID);
            this.server.getPluginManager().callEvent(event);

            if (event.isCancelled()) {
                return CompletableFuture.completedFuture(null);
            }

            return this.deleteLocker(uniqueId);
        });
    }

    public CompletableFuture<Void> deleteAll(CommandSender sender, NoticeService noticeService) {
        return this.lockerRepository.deleteAll().thenAccept(deleted -> {
            noticeService.create()
                .notice(messages -> messages.admin.deletedLockers)
                .placeholder("{COUNT}", deleted.toString())
                .viewer(sender)
                .send();

            this.lockersByUUID.invalidateAll();
            this.lockersByPosition.invalidateAll();
        });
    }

    public CompletableFuture<Boolean> isLockerFull(UUID uniqueId) {
        return this.parcelRepository.countDeliveredParcelsByDestinationLocker(uniqueId)
            .thenApply(count -> count > 0 && count >= this.config.settings.maxParcelsPerLocker);
    }

    private CompletableFuture<Void> deleteLocker(UUID uniqueId) {
        return this.lockerRepository.delete(uniqueId).thenAccept(deleted -> {
            if (deleted > 0) {
                this.lockersByUUID.invalidate(uniqueId);
                this.lockersByPosition.asMap().values().removeIf(l -> l.uuid().equals(uniqueId));
            }
        });
    }
}
