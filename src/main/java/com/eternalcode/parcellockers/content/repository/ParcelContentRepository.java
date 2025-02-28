package com.eternalcode.parcellockers.content.repository;

import com.eternalcode.parcellockers.content.ParcelContent;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface ParcelContentRepository {

    CompletableFuture<Void> save(ParcelContent parcelContent);

    CompletableFuture<Optional<ParcelContent>> findByUUID(UUID uniqueId);

    CompletableFuture<Integer> remove(UUID uniqueId);

    CompletableFuture<Integer> removeAll();

}
