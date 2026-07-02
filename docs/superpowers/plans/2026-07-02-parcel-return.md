# Parcel Return (GH-69) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Players return collected parcels at any locker by depositing the original items back; the parcel ships in reverse to the original sender, with configurable attribute-match strictness and per-size return fees.

**Architecture:** Collecting a parcel now marks it `COLLECTED` (keeping the parcel + content rows as a validation snapshot) and records the collection time in a new `collected_parcels` table. A new `ParcelReturnService` validates deposited items against the snapshot via a config-driven `ReturnItemEquivalence`, charges the fee, flips the same parcel row into a reverse `SENT` shipment, and schedules the normal `ParcelSendTask`. A periodic purge task deletes collected parcels whose return window expired. Two new GUIs (`ReturnGui` list + `ReturnDepositGui` deposit) hang off a third button in `LockerGui`.

**Tech Stack:** Java 21, Paper API 1.21, ORMLite (H2/MySQL/PostgreSQL via `DatabaseManager`), okaeri-configs, triumph-gui, Vault Economy, JUnit 5 + Mockito (unit), Testcontainers MySQL (integration, Docker-gated).

**Spec:** `docs/superpowers/specs/2026-07-02-parcel-return-design.md`

## Global Constraints

- Branch: `feat/parcel-return-gh-69` (based on `origin/fix/issue-222` — PR #230 must merge before this).
- JDK 21; build with `./gradlew`; run unit tests with `./gradlew test`. Integration tests are `@Testcontainers(disabledWithoutDocker = true)` — they silently skip without Docker; run them when Docker is available.
- No new dependencies.
- No schema migrations exist in this codebase — only `TableUtils.createTableIfNotExists`. Never add columns to existing tables; new persistent data goes in new tables.
- Unit tests must not bootstrap a server: mock `ItemStack`/`ItemMeta` with Mockito; ormlite is off the test classpath, so unit tests must not touch `*OrmLite`/`*Table` classes.
- All user-facing text goes through `MessageConfig` notices / `PluginConfig.GuiSettings` items; никогда hardcode player-visible strings (follow the `&`-color + MiniMessage conventions of neighboring entries).
- Repositories return `CompletableFuture`; Bukkit API (inventory, events to players) on the main thread via `scheduler.run(...)`.
- Commit after every task with a conventional-commit message ending in the Claude trailer used below.

**Commit trailer for every commit in this plan:**

```
Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>
Claude-Session: https://claude.ai/code/session_01UWGThsFyex5LiEyN63L3Wu
```

---

### Task 1: `ParcelStatus.COLLECTED` + `ParcelSendTask` re-delivery guard

A `COLLECTED` parcel with a stale `deliveries` row must never be re-delivered (that would flip it back to `DELIVERED` and allow collecting the items twice). `ParcelSendTask.decide` currently aborts only for missing/`DELIVERED` parcels; tighten it to "deliver only `SENT` parcels".

**Files:**
- Modify: `src/main/java/com/eternalcode/parcellockers/parcel/ParcelStatus.java`
- Modify: `src/main/java/com/eternalcode/parcellockers/parcel/task/ParcelSendTask.java:37-45`
- Test: `src/test/java/com/eternalcode/parcellockers/parcel/task/ParcelSendTaskTest.java`

**Interfaces:**
- Consumes: existing `ParcelSendTask.decide(Optional<Parcel>, Optional<Delivery>, Instant)`.
- Produces: `ParcelStatus.COLLECTED` enum constant used by every later task.

- [ ] **Step 1: Add the failing test**

Add to `ParcelSendTaskTest`:

```java
@Test
void abortsWhenAlreadyCollected() {
    // A COLLECTED parcel sits in the receiver's return window; a stale delivery row
    // must not re-deliver it, or the items could be collected twice.
    Parcel collected = parcel(ParcelStatus.COLLECTED);
    Delivery due = new Delivery(collected.uuid(), Instant.parse("2026-06-21T11:00:00Z"));
    assertEquals(Decision.ABORT, ParcelSendTask.decide(
        Optional.of(collected), Optional.of(due), Instant.parse("2026-06-21T12:00:00Z")));
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew test --tests "com.eternalcode.parcellockers.parcel.task.ParcelSendTaskTest"`
Expected: compile error — `COLLECTED` does not exist.

- [ ] **Step 3: Implement**

`ParcelStatus.java`:

```java
package com.eternalcode.parcellockers.parcel;

public enum ParcelStatus {

    SENT,
    DELIVERED,
    COLLECTED
}
```

In `ParcelSendTask.decide`, replace the first `if` with:

```java
if (currentParcel.isEmpty() || currentParcel.get().status() != ParcelStatus.SENT) {
    return Decision.ABORT;
}
```

- [ ] **Step 4: Run the whole test class**

Run: `./gradlew test --tests "com.eternalcode.parcellockers.parcel.task.ParcelSendTaskTest"`
Expected: all 6 tests PASS (the existing 5 still pass because they only use SENT/DELIVERED/missing).

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/eternalcode/parcellockers/parcel/ParcelStatus.java src/main/java/com/eternalcode/parcellockers/parcel/task/ParcelSendTask.java src/test/java/com/eternalcode/parcellockers/parcel/task/ParcelSendTaskTest.java
git commit -m "feat: add COLLECTED parcel status and guard ParcelSendTask against re-delivery"
```

---

### Task 2: `CollectedParcel` domain + repository

New `returns` domain package mirroring the `delivery` domain: a record and an ORMLite repository over a new `collected_parcels` table. No migration needed — new table.

**Files:**
- Create: `src/main/java/com/eternalcode/parcellockers/returns/CollectedParcel.java`
- Create: `src/main/java/com/eternalcode/parcellockers/returns/repository/CollectedParcelTable.java`
- Create: `src/main/java/com/eternalcode/parcellockers/returns/repository/CollectedParcelRepository.java`
- Create: `src/main/java/com/eternalcode/parcellockers/returns/repository/CollectedParcelRepositoryOrmLite.java`
- Test: `src/test/java/com/eternalcode/parcellockers/database/CollectedParcelRepositoryIntegrationTest.java`

**Interfaces:**
- Produces: `CollectedParcel(UUID parcel, Instant collectedAt)` record; `CollectedParcelRepository` with `save`, `find`, `findExpired(Instant cutoff)` (rows with `collectedAt <= cutoff`), `delete`.

- [ ] **Step 1: Write the record and repository interface**

`CollectedParcel.java`:

```java
package com.eternalcode.parcellockers.returns;

import java.time.Instant;
import java.util.UUID;

public record CollectedParcel(UUID parcel, Instant collectedAt) {
}
```

`CollectedParcelRepository.java`:

```java
package com.eternalcode.parcellockers.returns.repository;

import com.eternalcode.parcellockers.returns.CollectedParcel;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface CollectedParcelRepository {

    CompletableFuture<Void> save(CollectedParcel collectedParcel);

    CompletableFuture<Optional<CollectedParcel>> find(UUID parcel);

    /** Returns rows collected at or before the given cutoff (i.e. whose return window expired). */
    CompletableFuture<List<CollectedParcel>> findExpired(Instant cutoff);

    CompletableFuture<Boolean> delete(UUID parcel);
}
```

- [ ] **Step 2: Write the table and ORMLite implementation**

`CollectedParcelTable.java`:

```java
package com.eternalcode.parcellockers.returns.repository;

import com.eternalcode.parcellockers.database.persister.InstantPersister;
import com.eternalcode.parcellockers.returns.CollectedParcel;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import java.time.Instant;
import java.util.UUID;

@DatabaseTable(tableName = "collected_parcels")
class CollectedParcelTable {

    static final String COLLECTED_AT_COLUMN = "collected_at";

    @DatabaseField(id = true)
    private UUID parcel;

    @DatabaseField(columnName = COLLECTED_AT_COLUMN, persisterClass = InstantPersister.class)
    private Instant collectedAt;

    CollectedParcelTable() {
    }

    CollectedParcelTable(UUID parcel, Instant collectedAt) {
        this.parcel = parcel;
        this.collectedAt = collectedAt;
    }

    static CollectedParcelTable from(CollectedParcel collectedParcel) {
        return new CollectedParcelTable(collectedParcel.parcel(), collectedParcel.collectedAt());
    }

    CollectedParcel toCollectedParcel() {
        return new CollectedParcel(this.parcel, this.collectedAt);
    }
}
```

`CollectedParcelRepositoryOrmLite.java`:

```java
package com.eternalcode.parcellockers.returns.repository;

import com.eternalcode.commons.scheduler.Scheduler;
import com.eternalcode.parcellockers.database.DatabaseManager;
import com.eternalcode.parcellockers.database.wrapper.AbstractRepositoryOrmLite;
import com.eternalcode.parcellockers.returns.CollectedParcel;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class CollectedParcelRepositoryOrmLite extends AbstractRepositoryOrmLite implements CollectedParcelRepository {

    public CollectedParcelRepositoryOrmLite(DatabaseManager databaseManager, Scheduler scheduler) {
        super(databaseManager, scheduler);
        this.createTable(CollectedParcelTable.class);
    }

    @Override
    public CompletableFuture<Void> save(CollectedParcel collectedParcel) {
        Objects.requireNonNull(collectedParcel, "CollectedParcel cannot be null");
        return this.insertIfAbsent(CollectedParcelTable.class, CollectedParcelTable.from(collectedParcel))
            .thenApply(dao -> null);
    }

    @Override
    public CompletableFuture<Optional<CollectedParcel>> find(UUID parcel) {
        Objects.requireNonNull(parcel, "Parcel UUID cannot be null");
        return this.selectSafe(CollectedParcelTable.class, parcel)
            .thenApply(optional -> optional.map(CollectedParcelTable::toCollectedParcel));
    }

    @Override
    public CompletableFuture<List<CollectedParcel>> findExpired(Instant cutoff) {
        Objects.requireNonNull(cutoff, "Cutoff cannot be null");
        return this.action(CollectedParcelTable.class, dao -> dao.queryBuilder()
            .where()
            .le(CollectedParcelTable.COLLECTED_AT_COLUMN, cutoff)
            .query()
            .stream()
            .map(CollectedParcelTable::toCollectedParcel)
            .toList());
    }

    @Override
    public CompletableFuture<Boolean> delete(UUID parcel) {
        Objects.requireNonNull(parcel, "Parcel UUID cannot be null");
        return this.deleteById(CollectedParcelTable.class, parcel).thenApply(rows -> rows > 0);
    }
}
```

- [ ] **Step 3: Write the integration test**

`CollectedParcelRepositoryIntegrationTest.java` (copy the container boilerplate style of `ParcelFindCollectibleIntegrationTest`):

```java
package com.eternalcode.parcellockers.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.eternalcode.parcellockers.TestScheduler;
import com.eternalcode.parcellockers.configuration.implementation.PluginConfig;
import com.eternalcode.parcellockers.returns.CollectedParcel;
import com.eternalcode.parcellockers.returns.repository.CollectedParcelRepository;
import com.eternalcode.parcellockers.returns.repository.CollectedParcelRepositoryOrmLite;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers(disabledWithoutDocker = true)
class CollectedParcelRepositoryIntegrationTest extends IntegrationTestSpec {

    @Container
    private static final MySQLContainer<?> mySQLContainer = new MySQLContainer<>(DockerImageName.parse("mysql:latest"));

    @TempDir
    private Path tempDir;

    private DatabaseManager databaseManager;

    private CollectedParcelRepository repository() throws SQLException {
        PluginConfig config = new PluginConfig();
        config.settings.databaseType = DatabaseType.MYSQL;
        config.settings.host = mySQLContainer.getHost();
        config.settings.port = String.valueOf(mySQLContainer.getFirstMappedPort());
        config.settings.databaseName = mySQLContainer.getDatabaseName();
        config.settings.user = mySQLContainer.getUsername();
        config.settings.password = mySQLContainer.getPassword();

        DatabaseManager databaseManager = new DatabaseManager(config, Logger.getLogger("ParcelLockers"), this.tempDir.toFile());
        databaseManager.connect();
        this.databaseManager = databaseManager;

        return new CollectedParcelRepositoryOrmLite(databaseManager, new TestScheduler());
    }

    @Test
    void savesAndFindsCollectedParcel() throws SQLException {
        CollectedParcelRepository repository = this.repository();
        UUID parcel = UUID.randomUUID();
        // MySQL TIMESTAMP columns don't keep nanos; truncate so the round-trip compares equal.
        Instant collectedAt = Instant.now().truncatedTo(ChronoUnit.SECONDS);

        this.await(repository.save(new CollectedParcel(parcel, collectedAt)));

        Optional<CollectedParcel> found = this.await(repository.find(parcel));
        assertTrue(found.isPresent());
        assertEquals(collectedAt, found.get().collectedAt());
    }

    @Test
    void findExpiredReturnsOnlyRowsAtOrBeforeCutoff() throws SQLException {
        CollectedParcelRepository repository = this.repository();
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);

        UUID expired = UUID.randomUUID();
        UUID fresh = UUID.randomUUID();
        this.await(repository.save(new CollectedParcel(expired, now.minusSeconds(3600))));
        this.await(repository.save(new CollectedParcel(fresh, now)));

        List<CollectedParcel> result = this.await(repository.findExpired(now.minusSeconds(60)));
        assertEquals(1, result.size());
        assertEquals(expired, result.get(0).parcel());
    }

    @Test
    void deleteRemovesRow() throws SQLException {
        CollectedParcelRepository repository = this.repository();
        UUID parcel = UUID.randomUUID();
        this.await(repository.save(new CollectedParcel(parcel, Instant.now().truncatedTo(ChronoUnit.SECONDS))));

        assertTrue(this.await(repository.delete(parcel)));
        assertFalse(this.await(repository.find(parcel)).isPresent());
        assertFalse(this.await(repository.delete(parcel)));
    }

    @AfterEach
    void tearDown() {
        if (this.databaseManager != null) {
            this.databaseManager.disconnect();
        }
    }
}
```

- [ ] **Step 4: Verify**

Run: `./gradlew test --tests "com.eternalcode.parcellockers.database.CollectedParcelRepositoryIntegrationTest"`
Expected: 3 tests PASS with Docker running (SKIPPED without Docker — then at minimum confirm `./gradlew compileJava compileTestJava` succeeds).

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/eternalcode/parcellockers/returns src/test/java/com/eternalcode/parcellockers/database/CollectedParcelRepositoryIntegrationTest.java
git commit -m "feat: add CollectedParcel domain and collected_parcels repository"
```

---

### Task 3: `ParcelRepository` — conditional status flips, returnable page, fullness fix

Three additions to `ParcelRepository`/`ParcelRepositoryOrmLite`:
1. `markCollected(UUID)` — conditional `UPDATE ... SET status = COLLECTED WHERE uuid = ? AND status = DELIVERED`; the row-count result is the double-collect guard (the old code got this guarantee from `delete`).
2. `markReturned(Parcel returned)` — conditional swap-update `WHERE uuid = ? AND status = COLLECTED`; guards double-return races.
3. `findReturnable(UUID receiver, Page page)` — paged `receiver = ? AND status = COLLECTED`.
4. Fullness fix: `countParcelsByDestinationLocker` must exclude `COLLECTED` parcels (they are no longer physically in the locker and must not consume `maxParcelsPerLocker` capacity).

**Files:**
- Modify: `src/main/java/com/eternalcode/parcellockers/parcel/repository/ParcelRepository.java`
- Modify: `src/main/java/com/eternalcode/parcellockers/parcel/repository/ParcelRepositoryOrmLite.java`
- Test: `src/test/java/com/eternalcode/parcellockers/database/ParcelReturnRepositoryIntegrationTest.java` (new)

**Interfaces:**
- Consumes: `ParcelStatus.COLLECTED` (Task 1).
- Produces: `CompletableFuture<Boolean> markCollected(UUID uuid)`, `CompletableFuture<Boolean> markReturned(Parcel returned)`, `CompletableFuture<PageResult<Parcel>> findReturnable(UUID receiver, Page page)`.

- [ ] **Step 1: Write the failing integration test**

`ParcelReturnRepositoryIntegrationTest.java` (same boilerplate as `ParcelFindCollectibleIntegrationTest`, including the `repository()` and `save(...)` helpers — copy them verbatim, plus this `parcel` helper):

```java
package com.eternalcode.parcellockers.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.eternalcode.parcellockers.TestScheduler;
import com.eternalcode.parcellockers.configuration.implementation.PluginConfig;
import com.eternalcode.parcellockers.parcel.Parcel;
import com.eternalcode.parcellockers.parcel.ParcelSize;
import com.eternalcode.parcellockers.parcel.ParcelStatus;
import com.eternalcode.parcellockers.parcel.repository.ParcelRepository;
import com.eternalcode.parcellockers.parcel.repository.ParcelRepositoryOrmLite;
import com.eternalcode.parcellockers.shared.Page;
import com.eternalcode.parcellockers.shared.PageResult;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.UUID;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers(disabledWithoutDocker = true)
class ParcelReturnRepositoryIntegrationTest extends IntegrationTestSpec {

    @Container
    private static final MySQLContainer<?> mySQLContainer = new MySQLContainer<>(DockerImageName.parse("mysql:latest"));

    @TempDir
    private Path tempDir;

    private DatabaseManager databaseManager;

    private ParcelRepository repository() throws SQLException {
        PluginConfig config = new PluginConfig();
        config.settings.databaseType = DatabaseType.MYSQL;
        config.settings.host = mySQLContainer.getHost();
        config.settings.port = String.valueOf(mySQLContainer.getFirstMappedPort());
        config.settings.databaseName = mySQLContainer.getDatabaseName();
        config.settings.user = mySQLContainer.getUsername();
        config.settings.password = mySQLContainer.getPassword();

        DatabaseManager databaseManager = new DatabaseManager(config, Logger.getLogger("ParcelLockers"), this.tempDir.toFile());
        databaseManager.connect();
        this.databaseManager = databaseManager;

        return new ParcelRepositoryOrmLite(databaseManager, new TestScheduler());
    }

    private static Parcel parcel(UUID receiver, UUID destinationLocker, ParcelStatus status) {
        return new Parcel(UUID.randomUUID(), UUID.randomUUID(), "p", "d", false,
            receiver, ParcelSize.SMALL, UUID.randomUUID(), destinationLocker, status);
    }

    @Test
    void markCollectedFlipsOnlyDeliveredParcels() throws SQLException {
        ParcelRepository repository = this.repository();
        Parcel delivered = parcel(UUID.randomUUID(), UUID.randomUUID(), ParcelStatus.DELIVERED);
        this.await(repository.save(delivered));

        assertTrue(this.await(repository.markCollected(delivered.uuid())));
        assertEquals(ParcelStatus.COLLECTED, this.await(repository.findById(delivered.uuid())).orElseThrow().status());

        // Second collect attempt must not report success — this is the double-collect guard.
        assertFalse(this.await(repository.markCollected(delivered.uuid())));

        Parcel sent = parcel(UUID.randomUUID(), UUID.randomUUID(), ParcelStatus.SENT);
        this.await(repository.save(sent));
        assertFalse(this.await(repository.markCollected(sent.uuid())));
    }

    @Test
    void markReturnedSwapsPartiesAndLockersOnlyWhenCollected() throws SQLException {
        ParcelRepository repository = this.repository();
        Parcel collected = parcel(UUID.randomUUID(), UUID.randomUUID(), ParcelStatus.COLLECTED);
        this.await(repository.save(collected));

        Parcel returned = new Parcel(collected.uuid(), collected.receiver(), collected.name(),
            collected.description(), collected.priority(), collected.sender(), collected.size(),
            collected.destinationLocker(), collected.entryLocker(), ParcelStatus.SENT);

        assertTrue(this.await(repository.markReturned(returned)));

        Parcel stored = this.await(repository.findById(collected.uuid())).orElseThrow();
        assertEquals(collected.receiver(), stored.sender());
        assertEquals(collected.sender(), stored.receiver());
        assertEquals(collected.destinationLocker(), stored.entryLocker());
        assertEquals(collected.entryLocker(), stored.destinationLocker());
        assertEquals(ParcelStatus.SENT, stored.status());

        // A second return of the same parcel must fail (status is no longer COLLECTED).
        assertFalse(this.await(repository.markReturned(returned)));
    }

    @Test
    void findReturnableReturnsOnlyCollectedParcelsOfReceiver() throws SQLException {
        ParcelRepository repository = this.repository();
        UUID receiver = UUID.randomUUID();

        this.await(repository.save(parcel(receiver, UUID.randomUUID(), ParcelStatus.COLLECTED)));
        this.await(repository.save(parcel(receiver, UUID.randomUUID(), ParcelStatus.COLLECTED)));
        this.await(repository.save(parcel(receiver, UUID.randomUUID(), ParcelStatus.DELIVERED)));
        this.await(repository.save(parcel(UUID.randomUUID(), UUID.randomUUID(), ParcelStatus.COLLECTED)));

        PageResult<Parcel> page = this.await(repository.findReturnable(receiver, new Page(0, 10)));
        assertEquals(2, page.items().size());
        assertTrue(page.items().stream().allMatch(item -> item.status() == ParcelStatus.COLLECTED));
        assertTrue(page.items().stream().allMatch(item -> item.receiver().equals(receiver)));
    }

    @Test
    void collectedParcelsDoNotCountTowardsLockerFullness() throws SQLException {
        ParcelRepository repository = this.repository();
        UUID locker = UUID.randomUUID();

        this.await(repository.save(parcel(UUID.randomUUID(), locker, ParcelStatus.SENT)));
        this.await(repository.save(parcel(UUID.randomUUID(), locker, ParcelStatus.DELIVERED)));
        this.await(repository.save(parcel(UUID.randomUUID(), locker, ParcelStatus.COLLECTED)));

        assertEquals(2, this.await(repository.countParcelsByDestinationLocker(locker)));
    }

    @AfterEach
    void tearDown() {
        if (this.databaseManager != null) {
            this.databaseManager.disconnect();
        }
    }
}
```

- [ ] **Step 2: Run to verify failure**

Run: `./gradlew test --tests "com.eternalcode.parcellockers.database.ParcelReturnRepositoryIntegrationTest"`
Expected: compile error — `markCollected`, `markReturned`, `findReturnable` do not exist.

- [ ] **Step 3: Implement the interface additions**

Add to `ParcelRepository.java` (after `findCollectible`):

```java
/**
 * Atomically flips a DELIVERED parcel to COLLECTED. Returns false when the parcel is missing
 * or not DELIVERED — the caller must treat that as "someone else already collected it".
 */
CompletableFuture<Boolean> markCollected(UUID uuid);

/**
 * Atomically turns a COLLECTED parcel into its reverse SENT shipment (parties and lockers
 * swapped as prepared by the caller). Returns false when the parcel is missing or not
 * COLLECTED — the caller must treat that as "already returned or purged".
 */
CompletableFuture<Boolean> markReturned(Parcel returned);

/** Returns the COLLECTED parcels of the given receiver (candidates for a return). */
CompletableFuture<PageResult<Parcel>> findReturnable(UUID receiver, Page page);
```

- [ ] **Step 4: Implement in `ParcelRepositoryOrmLite`**

Add constant next to the existing column constants:

```java
private static final String UUID_COLUMN = "uuid";
```

Add the import `com.j256.ormlite.stmt.UpdateBuilder` and the methods:

```java
@Override
public CompletableFuture<Boolean> markCollected(UUID uuid) {
    Objects.requireNonNull(uuid, "UUID cannot be null");
    return this.action(ParcelTable.class, dao -> {
        UpdateBuilder<ParcelTable, Object> builder = dao.updateBuilder();
        builder.updateColumnValue(STATUS_COLUMN, ParcelStatus.COLLECTED);
        builder.where()
            .eq(UUID_COLUMN, uuid)
            .and()
            .eq(STATUS_COLUMN, ParcelStatus.DELIVERED);
        return builder.update() > 0;
    });
}

@Override
public CompletableFuture<Boolean> markReturned(Parcel returned) {
    Objects.requireNonNull(returned, "Returned parcel cannot be null");
    return this.action(ParcelTable.class, dao -> {
        UpdateBuilder<ParcelTable, Object> builder = dao.updateBuilder();
        builder.updateColumnValue(SENDER_COLUMN, returned.sender());
        builder.updateColumnValue(RECEIVER_COLUMN, returned.receiver());
        builder.updateColumnValue(ENTRY_LOCKER_COLUMN, returned.entryLocker());
        builder.updateColumnValue(DESTINATION_LOCKER_COLUMN, returned.destinationLocker());
        builder.updateColumnValue(STATUS_COLUMN, ParcelStatus.SENT);
        builder.where()
            .eq(UUID_COLUMN, returned.uuid())
            .and()
            .eq(STATUS_COLUMN, ParcelStatus.COLLECTED);
        return builder.update() > 0;
    });
}

@Override
public CompletableFuture<PageResult<Parcel>> findReturnable(UUID receiver, Page page) {
    Objects.requireNonNull(receiver, "Receiver UUID cannot be null");
    Objects.requireNonNull(page, "Page cannot be null");
    return this.queryPage(ParcelTable.class, page, builder -> {
        builder.where()
            .eq(RECEIVER_COLUMN, receiver)
            .and()
            .eq(STATUS_COLUMN, ParcelStatus.COLLECTED);
        return builder;
    }, ParcelTable::toParcel);
}
```

`markReturned` needs an `ENTRY_LOCKER_COLUMN` constant — add next to `DESTINATION_LOCKER_COLUMN`:

```java
private static final String ENTRY_LOCKER_COLUMN = "entry_locker";
```

Fix fullness counting — in `countParcelsByDestinationLocker`, replace the where clause with:

```java
long count = dao.queryBuilder()
    .where()
    .eq(DESTINATION_LOCKER_COLUMN, destinationLocker)
    .and()
    .ne(STATUS_COLUMN, ParcelStatus.COLLECTED)
    .countOf();
```

- [ ] **Step 5: Run the tests**

Run: `./gradlew test --tests "com.eternalcode.parcellockers.database.ParcelReturnRepositoryIntegrationTest" --tests "com.eternalcode.parcellockers.database.ParcelFindCollectibleIntegrationTest"`
Expected: PASS (with Docker; otherwise verify `./gradlew compileJava compileTestJava` succeeds).

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/eternalcode/parcellockers/parcel/repository src/test/java/com/eternalcode/parcellockers/database/ParcelReturnRepositoryIntegrationTest.java
git commit -m "feat: add conditional collect/return status flips and returnable query to ParcelRepository"
```

---

### Task 4: Config + messages

All new configuration and notices in one task, so later tasks can reference them. No tests (config POJOs follow existing untested convention) — verification is compilation.

**Files:**
- Modify: `src/main/java/com/eternalcode/parcellockers/configuration/implementation/PluginConfig.java`
- Modify: `src/main/java/com/eternalcode/parcellockers/configuration/implementation/MessageConfig.java`

**Interfaces:**
- Produces: `config.settings.parcelReturnWindow` (Duration), `config.settings.smallParcelReturnFee` / `mediumParcelReturnFee` / `largeParcelReturnFee` (double), `config.settings.returnChecks` of type `PluginConfig.ReturnChecks` with booleans `checkDurability`, `checkItemName`, `checkEnchantments`, `checkLore`, `checkNbt` (all default `true`); GUI settings `parcelReturnGuiTitle`, `parcelReturnDepositGuiTitle`, `parcelLockerReturnItem`, `parcelReturnRowItem`, `noReturnableParcelsItem`, `confirmReturnItem`, `returnWindowRemainingLine`, `returnWindowExpiredLine`; notices `messages.parcel.returned`, `cannotReturn`, `returnItemsMismatch`, `returnWindowExpired`, `returnFeeWithdrawn`.

- [ ] **Step 1: Add settings to `PluginConfig.Settings`**

Append inside `Settings` (after `largeParcelFee`):

```java
@Comment({"", "# How long after collection a parcel can still be returned.", "# Expired collected parcels are purged periodically."})
public Duration parcelReturnWindow = Duration.ofDays(7);

@Comment({"", "# Small parcel return fee in in-game currency"})
public double smallParcelReturnFee = 5.0;

@Comment({"", "# Medium parcel return fee in in-game currency"})
public double mediumParcelReturnFee = 12.5;

@Comment({"", "# Large parcel return fee in in-game currency"})
public double largeParcelReturnFee = 25.0;

@Comment({
    "",
    "# Which item attributes must match the original parcel content when a player returns a parcel.",
    "# Material types and total amounts must always match; each flag below relaxes one attribute when set to false."
})
public ReturnChecks returnChecks = new ReturnChecks();
```

Add the nested class at `PluginConfig` level (next to `Settings`/`GuiSettings`):

```java
public static class ReturnChecks extends OkaeriConfig {

    @Comment("# Whether durability (damage) must match the original items.")
    public boolean checkDurability = true;

    @Comment("# Whether custom display names must match the original items.")
    public boolean checkItemName = true;

    @Comment("# Whether enchantments must match the original items.")
    public boolean checkEnchantments = true;

    @Comment("# Whether lore must match the original items.")
    public boolean checkLore = true;

    @Comment({"# Whether all remaining item data (NBT) must match the original items.", "# When false, only the attributes enabled above are compared."})
    public boolean checkNbt = true;
}
```

- [ ] **Step 2: Add GUI settings to `PluginConfig.GuiSettings`**

Append at the end of `GuiSettings` (after `parcelArrivedLine`):

```java
@Comment({ "", "# The title of the parcel return GUI" })
public String parcelReturnGuiTitle = "&5Return parcels";

@Comment({ "", "# The title of the return deposit GUI" })
public String parcelReturnDepositGuiTitle = "&5Deposit the parcel items";

@Comment({ "", "# The item of the parcel locker return button" })
public ConfigItem parcelLockerReturnItem = new ConfigItem()
    .name("&5↩ Return parcels")
    .lore(List.of("&5» &dClick to return a collected parcel."))
    .type(Material.HOPPER)
    .glow(true);

@Comment({ "", "# The item of the parcel in the return GUI" })
public ConfigItem parcelReturnRowItem = new ConfigItem()
    .name("&d{NAME}")
    .lore(List.of(
            "&6Sender: &e{SENDER}",
            "&6Size: &e{SIZE}",
            "&6Description: &e{DESCRIPTION}"
        )
    )
    .type(Material.CHEST_MINECART);

@Comment({ "", "# The item displayed in the return GUI when there is nothing to return" })
public ConfigItem noReturnableParcelsItem = new ConfigItem()
    .name("&4✘ &cNo returnable parcels")
    .lore(List.of("&cYou don't have any parcels to return."))
    .type(Material.STRUCTURE_VOID);

@Comment({ "", "# The item of the confirm return button" })
public ConfigItem confirmReturnItem = new ConfigItem()
    .name("&2✔ &aConfirm return")
    .lore(List.of("&2» &aDeposit the original items above, then click to return the parcel."))
    .type(Material.GREEN_DYE);

@Comment({ "", "# The lore line showing how long the parcel can still be returned. Placeholder: {DURATION}" })
public String returnWindowRemainingLine = "&5Return window: &d{DURATION} left";

@Comment({ "", "# The lore line shown when the return window has expired." })
public String returnWindowExpiredLine = "&cReturn window expired";
```

- [ ] **Step 3: Add notices to `MessageConfig.ParcelMessages`**

Append after `feeWithdrawn` (before `parcelInfoMessages`):

```java
public Notice returned = Notice.builder()
    .chat("&2✔ &aParcel returned. It is on its way back to the sender.")
    .sound(SoundEventKeys.ENTITY_PLAYER_LEVELUP)
    .build();
public Notice cannotReturn = Notice.builder()
    .chat("&4✘ &cThis parcel cannot be returned right now. Your items were given back.")
    .sound(SoundEventKeys.ENTITY_VILLAGER_NO)
    .build();
public Notice returnItemsMismatch = Notice.builder()
    .chat("&4✘ &cThe deposited items do not match the original parcel contents!")
    .sound(SoundEventKeys.ENTITY_VILLAGER_NO)
    .build();
public Notice returnWindowExpired = Notice.builder()
    .chat("&4✘ &cThe return window for this parcel has expired!")
    .sound(SoundEventKeys.ENTITY_VILLAGER_NO)
    .build();
public Notice returnFeeWithdrawn = Notice.builder()
    .chat("&2✔ &a${AMOUNT} has been withdrawn from your account to cover the parcel return fee.")
    .sound(SoundEventKeys.ENTITY_EXPERIENCE_ORB_PICKUP)
    .build();
```

- [ ] **Step 4: Verify compilation**

Run: `./gradlew compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/eternalcode/parcellockers/configuration
git commit -m "feat: add return window, fees, attribute-check flags and return notices to config"
```

---

### Task 5: `ReturnItemEquivalence`

Config-driven equivalence between a deposited item and an original item (amounts are NOT compared here — the validator aggregates them). Two modes:
- `checkNbt = true` (default): full-meta comparison. Fast path when every flag is true → plain `ItemStack.isSimilar`. When some attribute flags are false → clone both sides, strip the disabled attributes, then `isSimilar`.
- `checkNbt = false`: compare only material + the enabled attributes, attribute-by-attribute.

The all-strict fast path and the attribute-wise mode are unit-testable with Mockito mocks; the strip-then-isSimilar path needs real `ItemMeta` (server) and is covered by the manual smoke test.

**Files:**
- Create: `src/main/java/com/eternalcode/parcellockers/returns/ReturnItemEquivalence.java`
- Test: `src/test/java/com/eternalcode/parcellockers/returns/ReturnItemEquivalenceTest.java`

**Interfaces:**
- Consumes: `PluginConfig.ReturnChecks` (Task 4).
- Produces: `ReturnItemEquivalence implements BiPredicate<ItemStack, ItemStack>`, constructor `ReturnItemEquivalence(PluginConfig.ReturnChecks checks)`, symmetric `boolean test(ItemStack a, ItemStack b)`.

- [ ] **Step 1: Write the failing tests**

```java
package com.eternalcode.parcellockers.returns;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import com.eternalcode.parcellockers.configuration.implementation.PluginConfig;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.Test;

class ReturnItemEquivalenceTest {

    private static PluginConfig.ReturnChecks checks(boolean durability, boolean name, boolean enchants, boolean lore, boolean nbt) {
        PluginConfig.ReturnChecks checks = new PluginConfig.ReturnChecks();
        checks.checkDurability = durability;
        checks.checkItemName = name;
        checks.checkEnchantments = enchants;
        checks.checkLore = lore;
        checks.checkNbt = nbt;
        return checks;
    }

    private static ItemStack item(Material type) {
        ItemStack item = mock(ItemStack.class);
        when(item.getType()).thenReturn(type);
        return item;
    }

    private static ItemMeta damagedMeta(int damage) {
        ItemMeta meta = mock(ItemMeta.class, withSettings().extraInterfaces(Damageable.class));
        when(((Damageable) meta).getDamage()).thenReturn(damage);
        when(meta.getEnchants()).thenReturn(Map.of());
        return meta;
    }

    @Test
    void fullyStrictChecksDelegateToIsSimilar() {
        ItemStack expected = item(Material.DIAMOND_SWORD);
        ItemStack actual = item(Material.DIAMOND_SWORD);
        when(expected.isSimilar(actual)).thenReturn(true);

        ReturnItemEquivalence equivalence = new ReturnItemEquivalence(checks(true, true, true, true, true));
        assertTrue(equivalence.test(expected, actual));

        when(expected.isSimilar(actual)).thenReturn(false);
        assertFalse(equivalence.test(expected, actual));
    }

    @Test
    void differentMaterialNeverMatches() {
        ItemStack expected = item(Material.DIAMOND_SWORD);
        ItemStack actual = item(Material.IRON_SWORD);

        ReturnItemEquivalence equivalence = new ReturnItemEquivalence(checks(false, false, false, false, false));
        assertFalse(equivalence.test(expected, actual));
    }

    @Test
    void durabilityMismatchFailsWhenChecked() {
        ItemStack expected = item(Material.DIAMOND_SWORD);
        ItemStack actual = item(Material.DIAMOND_SWORD);
        when(expected.getItemMeta()).thenReturn(damagedMeta(0));
        when(actual.getItemMeta()).thenReturn(damagedMeta(100));

        assertFalse(new ReturnItemEquivalence(checks(true, false, false, false, false)).test(expected, actual));
        assertTrue(new ReturnItemEquivalence(checks(false, false, false, false, false)).test(expected, actual));
    }

    @Test
    void enchantmentMismatchFailsWhenChecked() {
        ItemStack expected = item(Material.DIAMOND_SWORD);
        ItemStack actual = item(Material.DIAMOND_SWORD);

        ItemMeta expectedMeta = mock(ItemMeta.class);
        ItemMeta actualMeta = mock(ItemMeta.class);
        Enchantment enchantment = mock(Enchantment.class);
        when(expectedMeta.getEnchants()).thenReturn(Map.of(enchantment, 3));
        when(actualMeta.getEnchants()).thenReturn(Map.of());
        when(expected.getItemMeta()).thenReturn(expectedMeta);
        when(actual.getItemMeta()).thenReturn(actualMeta);

        assertFalse(new ReturnItemEquivalence(checks(false, false, true, false, false)).test(expected, actual));
        assertTrue(new ReturnItemEquivalence(checks(false, false, false, false, false)).test(expected, actual));
    }

    @Test
    void missingMetaOnBothSidesMatches() {
        ItemStack expected = item(Material.COBBLESTONE);
        ItemStack actual = item(Material.COBBLESTONE);
        when(expected.getItemMeta()).thenReturn(null);
        when(actual.getItemMeta()).thenReturn(null);

        assertTrue(new ReturnItemEquivalence(checks(true, true, true, true, false)).test(expected, actual));
    }
}
```

- [ ] **Step 2: Run to verify failure**

Run: `./gradlew test --tests "com.eternalcode.parcellockers.returns.ReturnItemEquivalenceTest"`
Expected: compile error — class does not exist.

- [ ] **Step 3: Implement**

```java
package com.eternalcode.parcellockers.returns;

import com.eternalcode.parcellockers.configuration.implementation.PluginConfig;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiPredicate;
import net.kyori.adventure.text.Component;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Config-driven equivalence between a deposited item and an original parcel item.
 * Material must always match; amounts are compared by {@link ParcelReturnValidator},
 * not here. The relation is symmetric.
 */
public class ReturnItemEquivalence implements BiPredicate<ItemStack, ItemStack> {

    private final PluginConfig.ReturnChecks checks;

    public ReturnItemEquivalence(PluginConfig.ReturnChecks checks) {
        this.checks = checks;
    }

    @Override
    public boolean test(ItemStack expected, ItemStack actual) {
        if (expected.getType() != actual.getType()) {
            return false;
        }
        if (this.checks.checkNbt) {
            if (this.allAttributeChecksEnabled()) {
                return expected.isSimilar(actual);
            }
            return this.normalize(expected).isSimilar(this.normalize(actual));
        }
        return this.attributesMatch(expected.getItemMeta(), actual.getItemMeta());
    }

    private boolean allAttributeChecksEnabled() {
        return this.checks.checkDurability
            && this.checks.checkItemName
            && this.checks.checkEnchantments
            && this.checks.checkLore;
    }

    /** Strips the attributes whose check is disabled so isSimilar ignores them. */
    private ItemStack normalize(ItemStack item) {
        ItemStack copy = item.clone();
        ItemMeta meta = copy.getItemMeta();
        if (meta == null) {
            return copy;
        }
        if (!this.checks.checkDurability && meta instanceof Damageable damageable) {
            damageable.setDamage(0);
        }
        if (!this.checks.checkItemName) {
            meta.displayName(null);
        }
        if (!this.checks.checkEnchantments) {
            meta.getEnchants().keySet().forEach(meta::removeEnchant);
        }
        if (!this.checks.checkLore) {
            meta.lore(null);
        }
        copy.setItemMeta(meta);
        return copy;
    }

    /** checkNbt = false: compare only the enabled attributes. */
    private boolean attributesMatch(ItemMeta expected, ItemMeta actual) {
        if (this.checks.checkDurability && damage(expected) != damage(actual)) {
            return false;
        }
        if (this.checks.checkItemName && !Objects.equals(displayName(expected), displayName(actual))) {
            return false;
        }
        if (this.checks.checkEnchantments && !enchants(expected).equals(enchants(actual))) {
            return false;
        }
        if (this.checks.checkLore && !Objects.equals(lore(expected), lore(actual))) {
            return false;
        }
        return true;
    }

    private static int damage(ItemMeta meta) {
        return meta instanceof Damageable damageable ? damageable.getDamage() : 0;
    }

    private static Component displayName(ItemMeta meta) {
        return meta == null ? null : meta.displayName();
    }

    private static Map<Enchantment, Integer> enchants(ItemMeta meta) {
        return meta == null ? Map.of() : meta.getEnchants();
    }

    private static List<Component> lore(ItemMeta meta) {
        return meta == null ? null : meta.lore();
    }
}
```

- [ ] **Step 4: Run the tests**

Run: `./gradlew test --tests "com.eternalcode.parcellockers.returns.ReturnItemEquivalenceTest"`
Expected: 5 tests PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/eternalcode/parcellockers/returns/ReturnItemEquivalence.java src/test/java/com/eternalcode/parcellockers/returns/ReturnItemEquivalenceTest.java
git commit -m "feat: add config-driven item equivalence for parcel returns"
```

---

### Task 6: `ParcelReturnValidator`

Multiset comparison of deposited items vs. the content snapshot using the equivalence: group both sides by equivalence, total the amounts per group, require identical totals. Stack splitting/merging must not matter.

**Files:**
- Create: `src/main/java/com/eternalcode/parcellockers/returns/ParcelReturnValidator.java`
- Test: `src/test/java/com/eternalcode/parcellockers/returns/ParcelReturnValidatorTest.java`

**Interfaces:**
- Consumes: any `BiPredicate<ItemStack, ItemStack>` (production: `ReturnItemEquivalence` from Task 5).
- Produces: `ParcelReturnValidator(BiPredicate<ItemStack, ItemStack> equivalence)` with `boolean matches(List<ItemStack> deposited, List<ItemStack> expected)`.

- [ ] **Step 1: Write the failing tests**

```java
package com.eternalcode.parcellockers.returns;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.function.BiPredicate;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

class ParcelReturnValidatorTest {

    /** Equivalence by material only — the real attribute logic is tested in ReturnItemEquivalenceTest. */
    private static final BiPredicate<ItemStack, ItemStack> BY_MATERIAL = (a, b) -> a.getType() == b.getType();

    private final ParcelReturnValidator validator = new ParcelReturnValidator(BY_MATERIAL);

    private static ItemStack item(Material type, int amount) {
        ItemStack item = mock(ItemStack.class);
        when(item.getType()).thenReturn(type);
        when(item.getAmount()).thenReturn(amount);
        return item;
    }

    @Test
    void exactSameStacksMatch() {
        List<ItemStack> expected = List.of(item(Material.DIAMOND, 5), item(Material.OAK_LOG, 64));
        List<ItemStack> deposited = List.of(item(Material.DIAMOND, 5), item(Material.OAK_LOG, 64));
        assertTrue(this.validator.matches(deposited, expected));
    }

    @Test
    void splitStacksStillMatch() {
        List<ItemStack> expected = List.of(item(Material.OAK_LOG, 64));
        List<ItemStack> deposited = List.of(item(Material.OAK_LOG, 30), item(Material.OAK_LOG, 34));
        assertTrue(this.validator.matches(deposited, expected));
    }

    @Test
    void mergedStacksStillMatch() {
        List<ItemStack> expected = List.of(item(Material.OAK_LOG, 30), item(Material.OAK_LOG, 34));
        List<ItemStack> deposited = List.of(item(Material.OAK_LOG, 64));
        assertTrue(this.validator.matches(deposited, expected));
    }

    @Test
    void missingAmountFails() {
        List<ItemStack> expected = List.of(item(Material.DIAMOND, 5));
        List<ItemStack> deposited = List.of(item(Material.DIAMOND, 4));
        assertFalse(this.validator.matches(deposited, expected));
    }

    @Test
    void extraAmountFails() {
        List<ItemStack> expected = List.of(item(Material.DIAMOND, 5));
        List<ItemStack> deposited = List.of(item(Material.DIAMOND, 6));
        assertFalse(this.validator.matches(deposited, expected));
    }

    @Test
    void wrongTypeFails() {
        List<ItemStack> expected = List.of(item(Material.DIAMOND, 5));
        List<ItemStack> deposited = List.of(item(Material.EMERALD, 5));
        assertFalse(this.validator.matches(deposited, expected));
    }

    @Test
    void extraForeignItemFails() {
        List<ItemStack> expected = List.of(item(Material.DIAMOND, 5));
        List<ItemStack> deposited = List.of(item(Material.DIAMOND, 5), item(Material.DIRT, 1));
        assertFalse(this.validator.matches(deposited, expected));
    }

    @Test
    void emptyDepositAgainstNonEmptyContentFails() {
        List<ItemStack> expected = List.of(item(Material.DIAMOND, 5));
        assertFalse(this.validator.matches(List.of(), expected));
    }
}
```

- [ ] **Step 2: Run to verify failure**

Run: `./gradlew test --tests "com.eternalcode.parcellockers.returns.ParcelReturnValidatorTest"`
Expected: compile error — class does not exist.

- [ ] **Step 3: Implement**

```java
package com.eternalcode.parcellockers.returns;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;
import org.bukkit.inventory.ItemStack;

/**
 * Validates that deposited items are, as a multiset, exactly the original parcel content:
 * every deposited stack must be equivalent to some original item, and the total amount per
 * equivalence group must match. Stack splitting or merging is irrelevant.
 */
public class ParcelReturnValidator {

    private final BiPredicate<ItemStack, ItemStack> equivalence;

    public ParcelReturnValidator(BiPredicate<ItemStack, ItemStack> equivalence) {
        this.equivalence = equivalence;
    }

    public boolean matches(List<ItemStack> deposited, List<ItemStack> expected) {
        List<ItemStack> samples = new ArrayList<>();
        List<Integer> expectedTotals = new ArrayList<>();
        List<Integer> depositedTotals = new ArrayList<>();

        for (ItemStack item : expected) {
            int index = this.indexOf(samples, item);
            if (index < 0) {
                samples.add(item);
                expectedTotals.add(item.getAmount());
                depositedTotals.add(0);
                continue;
            }
            expectedTotals.set(index, expectedTotals.get(index) + item.getAmount());
        }

        for (ItemStack item : deposited) {
            int index = this.indexOf(samples, item);
            if (index < 0) {
                return false;
            }
            depositedTotals.set(index, depositedTotals.get(index) + item.getAmount());
        }

        return expectedTotals.equals(depositedTotals);
    }

    private int indexOf(List<ItemStack> samples, ItemStack item) {
        for (int i = 0; i < samples.size(); i++) {
            if (this.equivalence.test(samples.get(i), item)) {
                return i;
            }
        }
        return -1;
    }
}
```

- [ ] **Step 4: Run the tests**

Run: `./gradlew test --tests "com.eternalcode.parcellockers.returns.ParcelReturnValidatorTest"`
Expected: 8 tests PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/eternalcode/parcellockers/returns/ParcelReturnValidator.java src/test/java/com/eternalcode/parcellockers/returns/ParcelReturnValidatorTest.java
git commit -m "feat: add multiset return-content validator"
```

---

### Task 7: Collect keeps the parcel — `ParcelServiceImpl` rework + wiring

Collect stops deleting: it records a `CollectedParcel` row, conditionally flips the status via `markCollected` (double-collect guard), keeps the content row, and only then hands the items over. Also expose `getReturnable`/`markReturned` on `ParcelService` (the impl maintains its cache). `ParcelLockers.onEnable` must construct the new repository and pass it in so everything compiles.

**Files:**
- Modify: `src/main/java/com/eternalcode/parcellockers/parcel/service/ParcelService.java`
- Modify: `src/main/java/com/eternalcode/parcellockers/parcel/service/ParcelServiceImpl.java`
- Modify: `src/main/java/com/eternalcode/parcellockers/ParcelLockers.java`

**Interfaces:**
- Consumes: `CollectedParcelRepository` (Task 2), `ParcelRepository.markCollected`/`markReturned`/`findReturnable` (Task 3).
- Produces: `ParcelService.getReturnable(UUID receiver, Page page)` and `ParcelService.markReturned(Parcel returned)` (cache-aware). `ParcelServiceImpl` constructor gains a `CollectedParcelRepository collectedParcelRepository` parameter (inserted after `parcelContentRepository`).

- [ ] **Step 1: Extend the `ParcelService` interface**

Add after `getCollectible`:

```java
/** Returns the COLLECTED parcels of the given receiver (candidates for a return). */
CompletableFuture<PageResult<Parcel>> getReturnable(UUID receiver, Page page);

/**
 * Atomically turns a COLLECTED parcel into its reverse SENT shipment. Returns false when
 * the parcel was already returned or purged in the meantime.
 */
CompletableFuture<Boolean> markReturned(Parcel returned);
```

- [ ] **Step 2: Rework `ParcelServiceImpl`**

Add the constructor parameter and field (after `parcelContentRepository`):

```java
private final CollectedParcelRepository collectedParcelRepository;
```

```java
public ParcelServiceImpl(
    NoticeService noticeService,
    ParcelRepository parcelRepository,
    ParcelContentRepository parcelContentRepository,
    CollectedParcelRepository collectedParcelRepository,
    Scheduler scheduler,
    PluginConfig config,
    Economy economy,
    Server server
) {
```

(assign `this.collectedParcelRepository = collectedParcelRepository;` with the others; import `com.eternalcode.parcellockers.returns.CollectedParcel`, `com.eternalcode.parcellockers.returns.repository.CollectedParcelRepository`, `com.eternalcode.parcellockers.parcel.ParcelStatus`, `java.time.Instant`).

Replace the body of `collect(Player player, Parcel parcel)` from the `this.scheduler.run(...)` block onward with:

```java
// Re-check inventory space on the main thread (the previous async check was a TOCTOU),
// then flip the status BEFORE handing the items back so the parcel cannot be collected
// twice. The parcel and content rows are kept: they are the snapshot a later return is
// validated against. The collected_parcels row is written first so that a successful
// flip always has a collection timestamp; a stray row from a failed flip is ignored by
// the purge task (it only purges parcels that are actually COLLECTED).
this.scheduler.run(() -> {
    if (!canHold(player, items)) {
        this.noticeService.player(player.getUniqueId(), messages -> messages.parcel.noInventorySpace);
        result.complete(null);
        return;
    }

    this.collectedParcelRepository.save(new CollectedParcel(parcel.uuid(), Instant.now()))
        .thenCompose(saved -> this.parcelRepository.markCollected(parcel.uuid()))
        .thenAccept(marked -> {
            if (!Boolean.TRUE.equals(marked)) {
                // Someone else collected it first (or the status changed under us).
                this.noticeService.player(player.getUniqueId(), messages -> messages.parcel.cannotCollect);
                result.complete(null);
                return;
            }

            this.parcelsByUuid.put(parcel.uuid(), withStatus(parcel, ParcelStatus.COLLECTED));
            this.scheduler.run(() -> {
                items.forEach(item -> ItemUtil.giveItem(player, item));
                this.noticeService.player(player.getUniqueId(), messages -> messages.parcel.collected);
            });
            result.complete(null);
        })
        .exceptionally(throwable -> {
            this.noticeService.player(player.getUniqueId(), messages -> messages.parcel.cannotCollect);
            result.complete(null);
            return null;
        });
});

return result;
```

Add the helper and the two new interface methods (next to the other getters):

```java
private static Parcel withStatus(Parcel parcel, ParcelStatus status) {
    return new Parcel(parcel.uuid(), parcel.sender(), parcel.name(), parcel.description(),
        parcel.priority(), parcel.receiver(), parcel.size(), parcel.entryLocker(),
        parcel.destinationLocker(), status);
}

@Override
public CompletableFuture<PageResult<Parcel>> getReturnable(UUID receiver, Page page) {
    Objects.requireNonNull(receiver, "Receiver UUID cannot be null");
    Objects.requireNonNull(page, "Page cannot be null");

    return this.parcelRepository.findReturnable(receiver, page)
        .thenApply(result -> {
            result.items().forEach(parcel -> this.parcelsByUuid.put(parcel.uuid(), parcel));
            return result;
        });
}

@Override
public CompletableFuture<Boolean> markReturned(Parcel returned) {
    Objects.requireNonNull(returned, "Returned parcel cannot be null");

    return this.parcelRepository.markReturned(returned).thenApply(updated -> {
        if (updated) {
            this.parcelsByUuid.put(returned.uuid(), returned);
        }
        return updated;
    });
}
```

- [ ] **Step 3: Wire the repository in `ParcelLockers.onEnable`**

After the `UserRepository` line add:

```java
CollectedParcelRepositoryOrmLite collectedParcelRepository = new CollectedParcelRepositoryOrmLite(databaseManager, scheduler);
```

and pass `collectedParcelRepository` into `new ParcelServiceImpl(...)` after `parcelContentRepository`. Import `com.eternalcode.parcellockers.returns.repository.CollectedParcelRepositoryOrmLite`.

- [ ] **Step 4: Verify**

Run: `./gradlew compileJava test`
Expected: BUILD SUCCESSFUL, all unit tests pass.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/eternalcode/parcellockers/parcel/service src/main/java/com/eternalcode/parcellockers/ParcelLockers.java
git commit -m "feat: keep collected parcels for the return window instead of deleting them"
```

---

### Task 8: `ParcelReturnEvent` + `ParcelReturnService`

The orchestrator. Every abort path hands the deposited items back on the main thread and sends a specific notice. The status flip is the conditional `markReturned`, so a concurrent double-return (or a return racing the purge) loses cleanly.

**Files:**
- Create: `src/main/java/com/eternalcode/parcellockers/parcel/event/ParcelReturnEvent.java`
- Create: `src/main/java/com/eternalcode/parcellockers/returns/ParcelReturnService.java`
- Test: `src/test/java/com/eternalcode/parcellockers/returns/ParcelReturnServiceTest.java` (window logic only)
- Modify: `src/main/java/com/eternalcode/parcellockers/ParcelLockers.java` (construct service)

**Interfaces:**
- Consumes: `ParcelService.get/markReturned` (Task 7), `ParcelContentManager.get/update`, `CollectedParcelRepository.find/delete` (Task 2), `LockerManager.isLockerFull(UUID)`, `DeliveryManager.create`, `ParcelSendTask`, `ParcelReturnValidator` (Task 6), config fees/window (Task 4).
- Produces: `ParcelReturnService` with `CompletableFuture<Void> returnParcel(Player player, Parcel parcel, List<ItemStack> deposited)`, `CompletableFuture<Optional<CollectedParcel>> getCollectedInfo(UUID parcelId)`, and `static boolean isWithinReturnWindow(CollectedParcel collected, Duration window, Instant now)`.

- [ ] **Step 1: Write the failing window test**

```java
package com.eternalcode.parcellockers.returns;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ParcelReturnServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-02T12:00:00Z");
    private static final Duration WINDOW = Duration.ofDays(7);

    @Test
    void withinWindowJustAfterCollection() {
        CollectedParcel collected = new CollectedParcel(UUID.randomUUID(), NOW.minus(Duration.ofHours(1)));
        assertTrue(ParcelReturnService.isWithinReturnWindow(collected, WINDOW, NOW));
    }

    @Test
    void outsideWindowAfterExpiry() {
        CollectedParcel collected = new CollectedParcel(UUID.randomUUID(), NOW.minus(Duration.ofDays(8)));
        assertFalse(ParcelReturnService.isWithinReturnWindow(collected, WINDOW, NOW));
    }

    @Test
    void exactExpiryInstantIsOutsideWindow() {
        CollectedParcel collected = new CollectedParcel(UUID.randomUUID(), NOW.minus(WINDOW));
        assertFalse(ParcelReturnService.isWithinReturnWindow(collected, WINDOW, NOW));
    }
}
```

- [ ] **Step 2: Run to verify failure**

Run: `./gradlew test --tests "com.eternalcode.parcellockers.returns.ParcelReturnServiceTest"`
Expected: compile error.

- [ ] **Step 3: Create the event**

`ParcelReturnEvent.java` (mirror of `ParcelCollectEvent`):

```java
package com.eternalcode.parcellockers.parcel.event;

import com.eternalcode.parcellockers.parcel.Parcel;
import com.eternalcode.parcellockers.shared.event.CancellableEvent;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class ParcelReturnEvent extends CancellableEvent {

    private static final HandlerList HANDLER_LIST = new HandlerList();

    private final Parcel parcel;

    public ParcelReturnEvent(Parcel parcel) {
        this.parcel = parcel;
    }

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }

    public Parcel getParcel() {
        return this.parcel;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLER_LIST;
    }
}
```

- [ ] **Step 4: Implement the service**

`ParcelReturnService.java`:

```java
package com.eternalcode.parcellockers.returns;

import com.eternalcode.commons.bukkit.ItemUtil;
import com.eternalcode.commons.scheduler.Scheduler;
import com.eternalcode.multification.notice.provider.NoticeProvider;
import com.eternalcode.parcellockers.configuration.implementation.MessageConfig;
import com.eternalcode.parcellockers.configuration.implementation.PluginConfig;
import com.eternalcode.parcellockers.content.ParcelContentManager;
import com.eternalcode.parcellockers.delivery.DeliveryManager;
import com.eternalcode.parcellockers.locker.LockerManager;
import com.eternalcode.parcellockers.notification.NoticeService;
import com.eternalcode.parcellockers.parcel.Parcel;
import com.eternalcode.parcellockers.parcel.ParcelSize;
import com.eternalcode.parcellockers.parcel.ParcelStatus;
import com.eternalcode.parcellockers.parcel.event.ParcelReturnEvent;
import com.eternalcode.parcellockers.parcel.service.ParcelService;
import com.eternalcode.parcellockers.parcel.task.ParcelSendTask;
import com.eternalcode.parcellockers.returns.repository.CollectedParcelRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Orchestrates returning a collected parcel: validates the deposited items against the stored
 * content snapshot, charges the return fee, flips the parcel into a reverse SENT shipment and
 * schedules the normal delivery task. Every abort path hands the deposited items back.
 */
public class ParcelReturnService {

    private static final Logger LOGGER = Logger.getLogger(ParcelReturnService.class.getName());
    private static final String PARCEL_FEE_BYPASS_PERMISSION = "parcellockers.fee.bypass";
    private static final String PLACEHOLDER_AMOUNT = "{AMOUNT}";

    private final ParcelService parcelService;
    private final ParcelContentManager parcelContentManager;
    private final CollectedParcelRepository collectedParcelRepository;
    private final DeliveryManager deliveryManager;
    private final LockerManager lockerManager;
    private final ParcelReturnValidator validator;
    private final Scheduler scheduler;
    private final PluginConfig config;
    private final NoticeService noticeService;
    private final Economy economy;
    private final Server server;

    public ParcelReturnService(
        ParcelService parcelService,
        ParcelContentManager parcelContentManager,
        CollectedParcelRepository collectedParcelRepository,
        DeliveryManager deliveryManager,
        LockerManager lockerManager,
        ParcelReturnValidator validator,
        Scheduler scheduler,
        PluginConfig config,
        NoticeService noticeService,
        Economy economy,
        Server server
    ) {
        this.parcelService = parcelService;
        this.parcelContentManager = parcelContentManager;
        this.collectedParcelRepository = collectedParcelRepository;
        this.deliveryManager = deliveryManager;
        this.lockerManager = lockerManager;
        this.validator = validator;
        this.scheduler = scheduler;
        this.config = config;
        this.noticeService = noticeService;
        this.economy = economy;
        this.server = server;
    }

    public static boolean isWithinReturnWindow(CollectedParcel collected, Duration window, Instant now) {
        return collected.collectedAt().plus(window).isAfter(now);
    }

    public CompletableFuture<Optional<CollectedParcel>> getCollectedInfo(UUID parcelId) {
        Objects.requireNonNull(parcelId, "Parcel UUID cannot be null");
        return this.collectedParcelRepository.find(parcelId);
    }

    public CompletableFuture<Void> returnParcel(Player player, Parcel parcel, List<ItemStack> deposited) {
        Objects.requireNonNull(player, "Player cannot be null");
        Objects.requireNonNull(parcel, "Parcel cannot be null");
        Objects.requireNonNull(deposited, "Deposited items cannot be null");

        ParcelReturnEvent event = new ParcelReturnEvent(parcel);
        this.server.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return this.abort(player, deposited, messages -> messages.parcel.cannotReturn);
        }

        return this.parcelService.get(parcel.uuid()).thenCompose(optionalParcel -> {
            if (optionalParcel.isEmpty()
                || optionalParcel.get().status() != ParcelStatus.COLLECTED
                || !player.getUniqueId().equals(optionalParcel.get().receiver())) {
                return this.abort(player, deposited, messages -> messages.parcel.cannotReturn);
            }
            Parcel current = optionalParcel.get();

            return this.collectedParcelRepository.find(current.uuid()).thenCompose(optionalCollected -> {
                if (optionalCollected.isEmpty()
                    || !isWithinReturnWindow(optionalCollected.get(), this.config.settings.parcelReturnWindow, Instant.now())) {
                    return this.abort(player, deposited, messages -> messages.parcel.returnWindowExpired);
                }

                return this.parcelContentManager.get(current.uuid()).thenCompose(optionalContent -> {
                    if (optionalContent.isEmpty()) {
                        return this.abort(player, deposited, messages -> messages.parcel.cannotReturn);
                    }
                    if (!this.validator.matches(deposited, optionalContent.get().items())) {
                        return this.abort(player, deposited, messages -> messages.parcel.returnItemsMismatch);
                    }

                    // The return ships to the original entry locker.
                    return this.lockerManager.isLockerFull(current.entryLocker()).thenCompose(isFull -> {
                        if (Boolean.TRUE.equals(isFull)) {
                            return this.abort(player, deposited, messages -> messages.parcel.lockerFull);
                        }
                        return this.execute(player, current, deposited);
                    });
                });
            });
        }).exceptionally(throwable -> {
            LOGGER.severe("Failed to return parcel " + parcel.uuid() + " for " + player.getName() + ": " + throwable.getMessage());
            this.giveBack(player, deposited);
            this.noticeService.player(player.getUniqueId(), messages -> messages.parcel.cannotReturn);
            return null;
        });
    }

    private CompletableFuture<Void> execute(Player player, Parcel current, List<ItemStack> deposited) {
        double chargedFee = 0;
        if (!player.hasPermission(PARCEL_FEE_BYPASS_PERMISSION)) {
            double fee = this.returnFeeFor(current.size());
            if (fee > 0) {
                boolean success = this.economy.withdrawPlayer(player, fee).transactionSuccess();
                String formattedFee = String.format("%.2f", fee);
                if (!success) {
                    this.noticeService.create()
                        .notice(messages -> messages.parcel.insufficientFunds)
                        .player(player.getUniqueId())
                        .placeholder(PLACEHOLDER_AMOUNT, formattedFee)
                        .send();
                    this.giveBack(player, deposited);
                    return CompletableFuture.completedFuture(null);
                }
                chargedFee = fee;
                this.noticeService.create()
                    .notice(messages -> messages.parcel.returnFeeWithdrawn)
                    .player(player.getUniqueId())
                    .placeholder(PLACEHOLDER_AMOUNT, formattedFee)
                    .send();
            }
        }
        double refundableFee = chargedFee;

        Parcel returned = new Parcel(current.uuid(), current.receiver(), current.name(),
            current.description(), current.priority(), current.sender(), current.size(),
            current.destinationLocker(), current.entryLocker(), ParcelStatus.SENT);

        List<ItemStack> depositedCopy = deposited.stream().map(ItemStack::clone).toList();

        // Content is overwritten with the actually-deposited items first (they may legitimately
        // differ from the snapshot when check flags are relaxed); only then the status flip makes
        // the parcel a live shipment. markReturned failing means a concurrent return/purge won.
        return this.parcelContentManager.update(current.uuid(), depositedCopy)
            .thenCompose(updated -> this.parcelService.markReturned(returned))
            .thenCompose(marked -> {
                if (!Boolean.TRUE.equals(marked)) {
                    this.refund(player, refundableFee);
                    return this.abort(player, deposited, messages -> messages.parcel.cannotReturn);
                }

                // Best-effort cleanup: a leftover row is ignored by the purge task because the
                // parcel is no longer COLLECTED.
                this.collectedParcelRepository.delete(current.uuid()).exceptionally(throwable -> {
                    LOGGER.warning("Failed to delete collected_parcels row for returned parcel "
                        + current.uuid() + ": " + throwable.getMessage());
                    return false;
                });

                Duration delay = returned.priority()
                    ? this.config.settings.priorityParcelSendDuration
                    : this.config.settings.parcelSendDuration;
                this.deliveryManager.create(returned.uuid(), Instant.now().plus(delay));
                this.scheduler.runLaterAsync(
                    new ParcelSendTask(returned, this.parcelService, this.deliveryManager, this.scheduler),
                    delay);

                this.noticeService.player(player.getUniqueId(), messages -> messages.parcel.returned);
                return CompletableFuture.<Void>completedFuture(null);
            })
            .exceptionally(throwable -> {
                this.refund(player, refundableFee);
                this.giveBack(player, deposited);
                this.noticeService.player(player.getUniqueId(), messages -> messages.parcel.cannotReturn);
                LOGGER.severe("Failed to execute return of parcel " + current.uuid() + ": " + throwable.getMessage());
                return null;
            });
    }

    private double returnFeeFor(ParcelSize size) {
        return switch (size) {
            case SMALL -> this.config.settings.smallParcelReturnFee;
            case MEDIUM -> this.config.settings.mediumParcelReturnFee;
            case LARGE -> this.config.settings.largeParcelReturnFee;
        };
    }

    private void refund(Player player, double fee) {
        if (fee > 0) {
            this.economy.depositPlayer(player, fee);
        }
    }

    private CompletableFuture<Void> abort(Player player, List<ItemStack> deposited, NoticeProvider<MessageConfig> notice) {
        this.giveBack(player, deposited);
        this.noticeService.player(player.getUniqueId(), notice);
        return CompletableFuture.completedFuture(null);
    }

    private void giveBack(Player player, List<ItemStack> items) {
        this.scheduler.run(() -> items.forEach(item -> ItemUtil.giveItem(player, item)));
    }
}
```

Also remove the unused `java.util.function.Function` import if your editor added one — `abort` uses multification's `NoticeProvider<MessageConfig>` (verified signature: `Multification.player(UUID, NoticeProvider<TRANSLATION>, Formatter...)`).

- [ ] **Step 5: Construct in `ParcelLockers.onEnable`**

After `ParcelDispatchService` construction:

```java
ParcelReturnValidator returnValidator = new ParcelReturnValidator(new ReturnItemEquivalence(config.settings.returnChecks));
ParcelReturnService parcelReturnService = new ParcelReturnService(
    parcelService,
    parcelContentManager,
    collectedParcelRepository,
    deliveryManager,
    lockerManager,
    returnValidator,
    scheduler,
    config,
    noticeService,
    this.economy,
    server
);
```

Imports: `com.eternalcode.parcellockers.returns.ParcelReturnService`, `com.eternalcode.parcellockers.returns.ParcelReturnValidator`, `com.eternalcode.parcellockers.returns.ReturnItemEquivalence`.

- [ ] **Step 6: Verify**

Run: `./gradlew test`
Expected: BUILD SUCCESSFUL, `ParcelReturnServiceTest` (3 tests) and all previous tests pass.

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/eternalcode/parcellockers/returns src/main/java/com/eternalcode/parcellockers/parcel/event/ParcelReturnEvent.java src/main/java/com/eternalcode/parcellockers/ParcelLockers.java src/test/java/com/eternalcode/parcellockers/returns/ParcelReturnServiceTest.java
git commit -m "feat: add parcel return service and ParcelReturnEvent"
```

---

### Task 9: Return-window purge task

Periodic async task deleting collected parcels whose window expired. A 5-minute grace period past the window prevents the purge from racing an in-flight return at the expiry boundary. Rows whose parcel is missing or no longer `COLLECTED` (stray rows from failed flips, already-returned parcels) get their row deleted without touching the parcel.

**Files:**
- Create: `src/main/java/com/eternalcode/parcellockers/returns/task/ReturnWindowPurgeTask.java`
- Modify: `src/main/java/com/eternalcode/parcellockers/ParcelLockers.java` (schedule)

**Interfaces:**
- Consumes: `CollectedParcelRepository.findExpired/delete` (Task 2), `ParcelService.get/delete(UUID)` (existing — `delete(UUID)` already removes parcel + content and invalidates the cache).
- Produces: `ReturnWindowPurgeTask implements Runnable`, constructor `(ParcelService, CollectedParcelRepository, PluginConfig)`.

- [ ] **Step 1: Implement the task**

```java
package com.eternalcode.parcellockers.returns.task;

import com.eternalcode.parcellockers.configuration.implementation.PluginConfig;
import com.eternalcode.parcellockers.parcel.ParcelStatus;
import com.eternalcode.parcellockers.parcel.service.ParcelService;
import com.eternalcode.parcellockers.returns.CollectedParcel;
import com.eternalcode.parcellockers.returns.repository.CollectedParcelRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * Deletes collected parcels whose return window expired: the parcel row, its content row and the
 * collected_parcels row. Runs periodically; failures are logged and retried on the next run.
 */
public class ReturnWindowPurgeTask implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(ReturnWindowPurgeTask.class.getName());

    /** Extra slack past the window so an in-flight return at the expiry boundary cannot race the purge. */
    private static final Duration GRACE = Duration.ofMinutes(5);

    private final ParcelService parcelService;
    private final CollectedParcelRepository collectedParcelRepository;
    private final PluginConfig config;

    public ReturnWindowPurgeTask(
        ParcelService parcelService,
        CollectedParcelRepository collectedParcelRepository,
        PluginConfig config
    ) {
        this.parcelService = parcelService;
        this.collectedParcelRepository = collectedParcelRepository;
        this.config = config;
    }

    @Override
    public void run() {
        Instant cutoff = Instant.now().minus(this.config.settings.parcelReturnWindow).minus(GRACE);

        this.collectedParcelRepository.findExpired(cutoff)
            .thenAccept(expired -> expired.forEach(this::purge))
            .exceptionally(throwable -> {
                LOGGER.severe("Failed to query expired collected parcels: " + throwable.getMessage());
                return null;
            });
    }

    private void purge(CollectedParcel collected) {
        this.parcelService.get(collected.parcel())
            .thenCompose(optionalParcel -> {
                if (optionalParcel.isPresent() && optionalParcel.get().status() == ParcelStatus.COLLECTED) {
                    // Delete the parcel (and content) first; the row is only removed once that
                    // succeeded so a failed delete is retried on the next run.
                    return this.parcelService.delete(collected.parcel())
                        .thenCompose(deleted -> Boolean.TRUE.equals(deleted)
                            ? this.collectedParcelRepository.delete(collected.parcel())
                            : CompletableFuture.completedFuture(false));
                }
                // Stray row: the parcel is gone or is a live shipment again — drop only the row.
                return this.collectedParcelRepository.delete(collected.parcel());
            })
            .exceptionally(throwable -> {
                LOGGER.warning("Failed to purge expired collected parcel " + collected.parcel()
                    + " (will retry next run): " + throwable.getMessage());
                return false;
            });
    }
}
```

- [ ] **Step 2: Schedule in `ParcelLockers.onEnable`**

After the `ParcelReturnService` construction:

```java
scheduler.timerAsync(
    new ReturnWindowPurgeTask(parcelService, collectedParcelRepository, config),
    Duration.ofSeconds(30),
    Duration.ofMinutes(30)
);
```

Import `com.eternalcode.parcellockers.returns.task.ReturnWindowPurgeTask` (Duration is already imported).

- [ ] **Step 3: Verify**

Run: `./gradlew compileJava test`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/eternalcode/parcellockers/returns/task src/main/java/com/eternalcode/parcellockers/ParcelLockers.java
git commit -m "feat: purge collected parcels after the return window expires"
```

---

### Task 10: GUIs — `ReturnGui`, `ReturnDepositGui`, `LockerGui` button, `GuiManager` plumbing

`ReturnGui` lists the player's COLLECTED parcels (modeled on `CollectionGui`; the lore additionally shows the remaining return window). Clicking opens `ReturnDepositGui`, a `StorageGui` sized by parcel size with a confirm button; confirm snapshots the deposited stacks, clears + closes the GUI and calls the return service (which hands items back on any failure). Closing without confirming gives everything back.

**Files:**
- Modify: `src/main/java/com/eternalcode/parcellockers/gui/GuiManager.java`
- Create: `src/main/java/com/eternalcode/parcellockers/gui/implementation/locker/ReturnGui.java`
- Create: `src/main/java/com/eternalcode/parcellockers/gui/implementation/locker/ReturnDepositGui.java`
- Modify: `src/main/java/com/eternalcode/parcellockers/gui/implementation/locker/LockerGui.java`
- Modify: `src/main/java/com/eternalcode/parcellockers/ParcelLockers.java` (pass service to GuiManager; move construction order if needed)

**Interfaces:**
- Consumes: `ParcelService.getReturnable` (Task 7), `ParcelReturnService.returnParcel/getCollectedInfo` (Task 8), GUI config items (Task 4), `PlaceholderUtil`, `DurationUtil`, `PaginatedGuiRefresher`.
- Produces: `GuiManager.getReturnableParcels(UUID, Page)`, `GuiManager.getCollectedInfo(UUID)`, `GuiManager.returnParcel(Player, Parcel, List<ItemStack>)`; `new ReturnGui(GuiSettings, Scheduler, GuiManager, MiniMessage).show(player)`.

- [ ] **Step 1: Extend `GuiManager`**

Add field + constructor parameter (last position) `ParcelReturnService parcelReturnService` (import `com.eternalcode.parcellockers.returns.CollectedParcel` and `com.eternalcode.parcellockers.returns.ParcelReturnService`), then add:

```java
public CompletableFuture<PageResult<Parcel>> getReturnableParcels(UUID receiver, Page page) {
    return this.parcelService.getReturnable(receiver, page);
}

public CompletableFuture<Optional<CollectedParcel>> getCollectedInfo(UUID parcelId) {
    return this.parcelReturnService.getCollectedInfo(parcelId);
}

public void returnParcel(Player player, Parcel parcel, List<ItemStack> deposited) {
    this.parcelReturnService.returnParcel(player, parcel, deposited);
}
```

In `ParcelLockers.onEnable`, move the `ParcelReturnValidator`/`ParcelReturnService` construction (Task 8) to BEFORE `new GuiManager(...)` and pass `parcelReturnService` as the last GuiManager argument.

- [ ] **Step 2: Create `ReturnGui`**

```java
package com.eternalcode.parcellockers.gui.implementation.locker;

import com.eternalcode.commons.concurrent.FutureHandler;
import com.eternalcode.commons.scheduler.Scheduler;
import com.eternalcode.parcellockers.configuration.implementation.PluginConfig.GuiSettings;
import com.eternalcode.parcellockers.configuration.serializable.ConfigItem;
import com.eternalcode.parcellockers.gui.GuiManager;
import com.eternalcode.parcellockers.gui.GuiView;
import com.eternalcode.parcellockers.gui.PaginatedGuiRefresher;
import com.eternalcode.parcellockers.notification.NoticeService;
import com.eternalcode.parcellockers.parcel.Parcel;
import com.eternalcode.parcellockers.parcel.util.PlaceholderUtil;
import com.eternalcode.parcellockers.shared.Page;
import com.eternalcode.parcellockers.util.DurationUtil;
import com.eternalcode.parcellockers.util.MaterialUtil;
import com.spotify.futures.CompletableFutures;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.PaginatedGui;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class ReturnGui implements GuiView {

    private static final int WIDTH = 7;
    private static final int HEIGHT = 4;
    private static final Page FIRST_PAGE = new Page(0, WIDTH * HEIGHT);

    private final GuiSettings guiSettings;
    private final Scheduler scheduler;
    private final GuiManager guiManager;
    private final MiniMessage miniMessage;
    private final NoticeService noticeService;
    private final Duration returnWindow;

    public ReturnGui(
        GuiSettings guiSettings,
        Scheduler scheduler,
        GuiManager guiManager,
        MiniMessage miniMessage,
        NoticeService noticeService,
        Duration returnWindow
    ) {
        this.guiSettings = guiSettings;
        this.scheduler = scheduler;
        this.guiManager = guiManager;
        this.miniMessage = miniMessage;
        this.noticeService = noticeService;
        this.returnWindow = returnWindow;
    }

    @Override
    public void show(Player player) {
        this.show(player, FIRST_PAGE);
    }

    @Override
    public void show(Player player, Page page) {
        Component guiTitle = this.miniMessage.deserialize(this.guiSettings.parcelReturnGuiTitle);
        ConfigItem rowItem = this.guiSettings.parcelReturnRowItem;

        PaginatedGui gui = Gui.paginated()
            .rows(6)
            .disableAllInteractions()
            .title(guiTitle)
            .create();

        this.setupStaticItems(player, gui);

        this.guiManager.getReturnableParcels(player.getUniqueId(), page).thenAccept(result -> {
            if (result == null || result.items().isEmpty()) {
                gui.setItem(22, this.guiSettings.noReturnableParcelsItem.toGuiItem());
                this.scheduler.run(() -> gui.open(player));
                return;
            }

            PaginatedGuiRefresher refresher = new PaginatedGuiRefresher(gui);

            this.setupNavigation(gui, page, result, player, this.guiSettings);

            result.items().stream()
                .map(parcel -> this.createParcelItemAsync(parcel, rowItem, player))
                .collect(CompletableFutures.joinList())
                .thenAccept(suppliers -> {
                    if (suppliers.isEmpty()) {
                        gui.setItem(22, this.guiSettings.noReturnableParcelsItem.toGuiItem());
                        this.scheduler.run(() -> gui.open(player));
                        return;
                    }
                    for (Supplier<GuiItem> supplier : suppliers) {
                        refresher.addItem(supplier);
                    }
                    this.scheduler.run(() -> gui.open(player));
                }).exceptionally(FutureHandler::handleException);
        }).exceptionally(FutureHandler::handleException);
    }

    private void setupStaticItems(Player player, PaginatedGui gui) {
        GuiItem closeItem = this.guiSettings.closeItem.toGuiItem(event -> gui.close(player));
        GuiItem cornerItem = this.guiSettings.cornerItem.toGuiItem();
        GuiItem backgroundItem = this.guiSettings.mainGuiBackgroundItem.toGuiItem();

        for (int cornerSlot : CORNER_SLOTS) {
            gui.setItem(cornerSlot, cornerItem);
        }

        for (int borderSlot : BORDER_SLOTS) {
            gui.setItem(borderSlot, backgroundItem);
        }

        gui.setItem(49, closeItem);
    }

    private CompletableFuture<Supplier<GuiItem>> createParcelItemAsync(
        Parcel parcel,
        ConfigItem rowItem,
        Player player
    ) {
        CompletableFuture<List<String>> loreFuture =
            PlaceholderUtil.replaceParcelPlaceholdersAsync(parcel, rowItem.lore(), this.guiManager);
        CompletableFuture<List<ItemStack>> contentFuture = this.guiManager.getParcelContent(parcel.uuid())
            .thenApply(optional -> optional.map(content -> content.items()).orElse(List.of()));
        CompletableFuture<String> windowLineFuture = this.guiManager.getCollectedInfo(parcel.uuid())
            .thenApply(optional -> optional
                .map(collected -> {
                    Duration remaining = Duration.between(Instant.now(), collected.collectedAt().plus(this.returnWindow));
                    return remaining.isNegative() || remaining.isZero()
                        ? this.guiSettings.returnWindowExpiredLine
                        : this.guiSettings.returnWindowRemainingLine.replace("{DURATION}", DurationUtil.format(remaining));
                })
                .orElse(this.guiSettings.returnWindowExpiredLine));

        return CompletableFutures.combine(loreFuture, contentFuture, windowLineFuture, (processedLore, items, windowLine) -> () -> {
            ConfigItem item = rowItem.clone();
            item.name(item.name().replace("{NAME}", parcel.name()));

            List<String> lore = new ArrayList<>(processedLore);
            lore.add(windowLine);
            if (!items.isEmpty()) {
                lore.add(this.guiSettings.parcelItemsCollectionGui);
                for (ItemStack itemStack : items) {
                    lore.add(this.guiSettings.parcelItemCollectionFormat
                        .replace("{ITEM}", MaterialUtil.format(itemStack.getType()))
                        .replace("{AMOUNT}", Integer.toString(itemStack.getAmount()))
                    );
                }
            }

            item.lore(lore);
            item.glow(true);

            return (GuiItem) item.toGuiItem(event -> new ReturnDepositGui(
                this.scheduler,
                this.guiSettings,
                this.miniMessage,
                this.guiManager,
                this.noticeService,
                parcel
            ).show(player));
        }).toCompletableFuture();
    }
}
```

Note: `CompletableFutures.combine(a, b, c, fn)` is already used in `PlaceholderUtil` — same import. If the 3-arg overload's inference balks at returning a `Supplier<GuiItem>` lambda, assign the lambda to a local `Supplier<GuiItem>` variable inside the combine function and return it.

- [ ] **Step 3: Create `ReturnDepositGui`**

```java
package com.eternalcode.parcellockers.gui.implementation.locker;

import com.eternalcode.commons.bukkit.ItemUtil;
import com.eternalcode.commons.scheduler.Scheduler;
import com.eternalcode.parcellockers.configuration.implementation.PluginConfig.GuiSettings;
import com.eternalcode.parcellockers.gui.GuiManager;
import com.eternalcode.parcellockers.notification.NoticeService;
import com.eternalcode.parcellockers.parcel.Parcel;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.StorageGui;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Deposit GUI for a parcel return: the player places the original items, then confirms.
 * Confirm hands the stacks to the return service (which gives them back on any failure);
 * closing without confirming gives everything back immediately.
 */
public class ReturnDepositGui {

    private final Scheduler scheduler;
    private final GuiSettings guiSettings;
    private final MiniMessage miniMessage;
    private final GuiManager guiManager;
    private final NoticeService noticeService;
    private final Parcel parcel;

    public ReturnDepositGui(
        Scheduler scheduler,
        GuiSettings guiSettings,
        MiniMessage miniMessage,
        GuiManager guiManager,
        NoticeService noticeService,
        Parcel parcel
    ) {
        this.scheduler = scheduler;
        this.guiSettings = guiSettings;
        this.miniMessage = miniMessage;
        this.guiManager = guiManager;
        this.noticeService = noticeService;
        this.parcel = parcel;
    }

    void show(Player player) {
        int rows = switch (this.parcel.size()) {
            case SMALL -> 2;
            case MEDIUM -> 3;
            case LARGE -> 4;
        };

        StorageGui gui = Gui.storage()
            .title(this.miniMessage.deserialize(this.guiSettings.parcelReturnDepositGuiTitle))
            .rows(rows)
            .create();

        GuiItem backgroundItem = this.guiSettings.mainGuiBackgroundItem.toGuiItem(event -> event.setCancelled(true));
        IntStream.rangeClosed(1, 9).forEach(i -> gui.setItem(gui.getRows(), i, backgroundItem));

        AtomicBoolean confirmed = new AtomicBoolean(false);

        GuiItem confirmItem = this.guiSettings.confirmReturnItem.toGuiItem(event -> {
            event.setCancelled(true);

            List<ItemStack> deposited = this.takeDepositedItems(gui);
            if (deposited.isEmpty()) {
                this.noticeService.player(player.getUniqueId(), messages -> messages.parcel.returnItemsMismatch);
                return;
            }

            confirmed.set(true);
            gui.close(player);
            this.guiManager.returnParcel(player, this.parcel, deposited);
        });
        gui.setItem(gui.getRows(), 5, confirmItem);

        gui.setCloseGuiAction(event -> {
            if (confirmed.get()) {
                return;
            }
            // Closed without confirming: give every deposited stack back.
            List<ItemStack> leftovers = this.takeDepositedItems(gui);
            this.scheduler.run(() -> leftovers.forEach(item -> ItemUtil.giveItem(player, item)));
        });

        this.scheduler.run(() -> gui.open(player));
    }

    /** Snapshots and clears the deposit slots (everything above the bottom control row). */
    private List<ItemStack> takeDepositedItems(StorageGui gui) {
        ItemStack[] contents = gui.getInventory().getContents();
        List<ItemStack> items = new ArrayList<>();

        for (int i = 0; i < contents.length - 9; i++) {
            ItemStack item = contents[i];
            if (item == null || item.isEmpty()) {
                continue;
            }
            items.add(item.clone());
            gui.getInventory().setItem(i, null);
        }
        return items;
    }
}
```

- [ ] **Step 4: Add the button to `LockerGui`**

`LockerGui` needs the return window duration; add a `Duration returnWindow` constructor parameter (import `java.time.Duration`) OR fetch it from a `PluginConfig` parameter — prefer passing `PluginConfig config` is NOT done elsewhere in GUIs (they get `GuiSettings`), so pass `Duration returnWindow` explicitly. Update the constructor and field, then in `show(...)` add after the `collectionGui` block:

```java
ReturnGui returnGui = new ReturnGui(
    this.guiSettings,
    this.scheduler,
    this.guiManager,
    this.miniMessage,
    this.noticeService,
    this.returnWindow
);
```

and register the button between collect (21) and send (23):

```java
gui.setItem(22, this.guiSettings.parcelLockerReturnItem.toGuiItem(event -> returnGui.show(player)));
```

In `ParcelLockers.onEnable`, update the `LockerGui` construction:

```java
LockerGui lockerGUI = new LockerGui(
    miniMessage,
    scheduler,
    config.guiSettings,
    guiManager,
    noticeService,
    config.settings.parcelReturnWindow
);
```

- [ ] **Step 5: Verify build + full test suite**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL (checkstyle/compile/tests all green).

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/eternalcode/parcellockers/gui src/main/java/com/eternalcode/parcellockers/ParcelLockers.java
git commit -m "feat: add return button, return list GUI and deposit GUI (GH-69)"
```

---

### Task 11: Verification sweep + smoke test

- [ ] **Step 1: Full build**

Run: `./gradlew clean build`
Expected: BUILD SUCCESSFUL; note the unit-test count increased by ~17 (1 ParcelSendTask + 5 equivalence + 8 validator + 3 window).

- [ ] **Step 2: Integration tests (requires Docker)**

Run: `./gradlew test --tests "com.eternalcode.parcellockers.database.*"`
Expected: all pass, including the two new classes. If Docker is unavailable, state that explicitly in the final report instead of claiming they passed.

- [ ] **Step 3: Manual smoke test (requires a Minecraft client — otherwise verify startup only)**

Run: `./gradlew runServer` and check the server log for a clean plugin enable (table `collected_parcels` created, no stack traces). With a client: place two lockers, send a small parcel between two accounts, collect it at the destination locker, open the locker again → "Return parcels" → parcel listed with window lore → deposit wrong items (expect mismatch notice + items back) → deposit right items (expect fee withdrawn + returned notice) → wait out the priority send duration → collect the return at the original entry locker as the sender.

- [ ] **Step 4: Commit any fixes, then hand off**

Use superpowers:finishing-a-development-branch — the PR should target `master` but note it depends on PR #230 (`fix/issue-222`).

---

## Self-Review Notes (already applied)

- Spec coverage: eligibility (Tasks 2/3/7/8), reverse dispatch (Task 8), validation flags (Tasks 5/6), fees (Tasks 4/8), purge (Task 9), GUIs (Task 10), fullness fix (Task 3), re-delivery guard (Task 1 — discovered during planning, added to spec's safety), events (Task 8), tests (Tasks 1/2/3/5/6/8).
- Deviation from spec: the spec's mention of reusing the `illegalItem` notice in the deposit GUI was dropped — deposited items must match the content snapshot, which already passed the illegal-items filter at send time, so a separate check is redundant.
- Deviation from spec: `ReturnGui` lists all COLLECTED parcels of the player; an expired-but-not-yet-purged parcel shows with an "expired" lore line and its return is rejected by the service re-check. This keeps pagination exact and avoids a cross-table join.
- The spec's "per-locker serialization" concern is satisfied more simply: `markReturned` is a conditional UPDATE (only one racer wins), and locker fullness for returns is best-effort exactly like the existing dispatch path once #230's serialization is bypassed — an over-cap return can momentarily exceed `maxParcelsPerLocker` only by racing a send to the same locker in the same tick; accepted as out of scope.
