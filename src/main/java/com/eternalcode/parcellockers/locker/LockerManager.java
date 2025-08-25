package com.eternalcode.parcellockers.locker;

import com.eternalcode.parcellockers.locker.repository.LockerRepository;
import com.eternalcode.parcellockers.notification.NoticeService;
import com.eternalcode.parcellockers.shared.Page;
import com.eternalcode.parcellockers.shared.PageResult;
import com.eternalcode.parcellockers.shared.Position;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.bukkit.command.CommandSender;

public class LockerManager {

    private final LockerRepository lockerRepository;

    private final Cache<UUID, Locker> lockersByUUID = Caffeine.newBuilder()
        .expireAfterAccess(Duration.ofHours(2))
        .maximumSize(10_000)
        .build();

    private final Cache<Position, Locker> lockersByPosition = Caffeine.newBuilder()
        .expireAfterAccess(Duration.ofHours(2))
        .maximumSize(10_000)
        .build();

    public LockerManager(LockerRepository lockerRepository) {
        this.lockerRepository = lockerRepository;

        this.cacheAll();
    }

    public CompletableFuture<Optional<Locker>> get(UUID uniqueId) {
        if (uniqueId == null) {
            return CompletableFuture.completedFuture(Optional.empty());
        }

        Locker locker = this.lockersByUUID.getIfPresent(uniqueId);

        if (locker != null) {
            return CompletableFuture.completedFuture(Optional.of(locker));
        }

        return this.lockerRepository.fetch(uniqueId).thenApply(optionalLocker -> {
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

        return this.lockerRepository.fetch(position).thenApply(optionalLocker -> {
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
        return this.lockerRepository.fetchPage(page);
    }

    public Locker getOrCreate(UUID uniqueId, String name, Position position) {
        Locker lockerByUUID = this.lockersByUUID.getIfPresent(uniqueId);
        if (lockerByUUID != null) {
            return lockerByUUID;
        }

        Locker lockerByPosition = this.lockersByPosition.getIfPresent(position);
        if (lockerByPosition != null) {
            return lockerByPosition;
        }

        return this.create(uniqueId, name, position);
    }

    public Locker create(UUID uniqueId, String name, Position position) {
        if (this.lockersByUUID.getIfPresent(uniqueId) != null) {
            throw new IllegalStateException("Locker with UUID " + uniqueId + " already exists in cache");
        }

        if (this.lockersByPosition.getIfPresent(position) != null) {
            throw new IllegalStateException("Locker at position " + position + " already exists in cache");
        }

        Locker locker = new Locker(uniqueId, name, position);
        this.lockersByUUID.put(uniqueId, locker);
        this.lockersByPosition.put(position, locker);
        this.lockerRepository.save(locker);

        return locker;
    }

    public void delete(UUID uniqueId) {
        this.lockersByUUID.invalidate(uniqueId);
        this.lockerRepository.delete(uniqueId).thenApply(deleted -> {
            if (deleted > 0) {
                this.lockersByPosition.asMap().values().removeIf(locker -> locker.uuid().equals(uniqueId));
            }
            return deleted;
        });
    }

    public void deleteAll(CommandSender sender, NoticeService noticeService) {
        this.lockerRepository.deleteAll().thenAccept(deleted -> {
            noticeService.create()
                .notice(messages -> messages.admin.deletedLockers)
                .placeholder("{COUNT}", deleted.toString())
                .viewer(sender)
                .send();

            this.lockersByUUID.invalidateAll();
            this.lockersByPosition.invalidateAll();
        });
    }

    private void cacheAll() {
        this.lockerRepository.fetchAll()
            .thenAccept(optionalLockers -> optionalLockers.ifPresent(lockers -> lockers.forEach(locker -> {
                this.lockersByUUID.put(locker.uuid(), locker);
                this.lockersByPosition.put(locker.position(), locker);
            })));
    }
}
