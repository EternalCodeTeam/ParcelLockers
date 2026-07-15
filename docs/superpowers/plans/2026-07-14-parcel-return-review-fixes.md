# Parcel Return Review Fixes Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make parcel returns concurrency-safe, durably scheduled, locker-valid, and main-thread-safe.

**Architecture:** A return-specific ORMLite repository commits the conditional parcel transition, deposited content, delivery row, and collected-row cleanup in one transaction. The service validates locker existence and captures Bukkit permission state before async work, invalidates affected caches after commit, and schedules only after persistence. Return GUI data remains asynchronous while all GUI mutation is dispatched through the primary-thread scheduler.

**Tech Stack:** Java 21, Paper 1.21, ORMLite 6.1, CompletableFuture, Triumph GUI, JUnit 5, Mockito, Testcontainers MySQL.

## Global Constraints

- Preserve the existing parcel-return behavior and player-facing configuration.
- Add no dependencies or schema columns.
- Repository work remains asynchronous through the existing `Scheduler`.
- Bukkit player, inventory, event, and GUI APIs run on the primary thread.
- Use a failing regression test before each production behavior change.
- Docker-gated integration tests may skip only when Docker is unavailable; report that explicitly.
- Do not commit or modify unrelated user-owned files.

---

### Task 1: Atomic Return Commit

**Files:**
- Create: `src/main/java/com/eternalcode/parcellockers/returns/repository/ParcelReturnRepository.java`
- Create: `src/main/java/com/eternalcode/parcellockers/returns/repository/ParcelReturnRepositoryOrmLite.java`
- Modify: `src/main/java/com/eternalcode/parcellockers/content/repository/ParcelContentTable.java`
- Modify: `src/main/java/com/eternalcode/parcellockers/delivery/repository/DeliveryTable.java`
- Modify: `src/main/java/com/eternalcode/parcellockers/parcel/repository/ParcelTable.java`
- Modify: `src/main/java/com/eternalcode/parcellockers/returns/repository/CollectedParcelTable.java`
- Test: `src/test/java/com/eternalcode/parcellockers/database/ParcelReturnCommitRepositoryIntegrationTest.java`

**Interfaces:**
- Consumes: `Parcel`, `ParcelContent`, `Delivery`, and `DatabaseManager.connectionSource()`.
- Produces: `CompletableFuture<Boolean> ParcelReturnRepository.commit(Parcel, ParcelContent, Delivery)`.

- [ ] **Step 1: Write the failing concurrent-winner integration test**

Create two return commits for the same `COLLECTED` parcel with different material content and delivery timestamps:

```java
CompletableFuture<Boolean> first = repository.commit(returned,
    new ParcelContent(parcel.uuid(), List.of(new ItemStack(Material.DIAMOND))),
    new Delivery(parcel.uuid(), firstDelivery));
CompletableFuture<Boolean> second = repository.commit(returned,
    new ParcelContent(parcel.uuid(), List.of(new ItemStack(Material.EMERALD))),
    new Delivery(parcel.uuid(), secondDelivery));

boolean firstWon = await(this.first);
boolean secondWon = await(this.second);
assertNotEquals(firstWon, secondWon);
Material expectedMaterial = this.firstWon ? Material.DIAMOND : Material.EMERALD;
Instant expectedDelivery = this.firstWon ? firstDelivery : secondDelivery;
assertEquals(expectedMaterial, await(contentRepository.find(parcel.uuid()))
    .orElseThrow().items().getFirst().getType());
assertEquals(expectedDelivery, await(deliveryRepository.find(parcel.uuid()))
    .orElseThrow().deliveryTimestamp());
assertEquals(ParcelStatus.SENT, await(parcelRepository.findById(parcel.uuid()))
    .orElseThrow().status());
assertTrue(await(collectedRepository.find(parcel.uuid())).isEmpty());
```

- [ ] **Step 2: Run the new test to verify RED**

Run:
```powershell
.\gradlew.bat test --tests "com.eternalcode.parcellockers.database.ParcelReturnCommitRepositoryIntegrationTest"
```

Expected: compilation fails because `ParcelReturnRepository` does not exist.

- [ ] **Step 3: Implement the transaction**

Define:

```java
public interface ParcelReturnRepository {
    CompletableFuture<Boolean> commit(Parcel returned, ParcelContent content, Delivery delivery);
}
```

The ORMLite implementation runs on `action(ParcelTable.class, ...)` and calls:

```java
return TransactionManager.callInTransaction(this.databaseManager.connectionSource(), () -> {
    UpdateBuilder<ParcelTable, Object> update = parcelDao.updateBuilder();
    update.updateColumnValue("sender", returned.sender());
    update.updateColumnValue("receiver", returned.receiver());
    update.updateColumnValue("entry_locker", returned.entryLocker());
    update.updateColumnValue("destination_locker", returned.destinationLocker());
    update.updateColumnValue("status", ParcelStatus.SENT);
    update.where().eq("uuid", returned.uuid()).and().eq("status", ParcelStatus.COLLECTED);
    if (update.update() == 0) {
        return false;
    }
    this.databaseManager.getDao(ParcelContentTable.class)
        .createOrUpdate(ParcelContentTable.from(content));
    this.databaseManager.getDao(DeliveryTable.class)
        .createOrUpdate(DeliveryTable.from(delivery));
    this.databaseManager.getDao(CollectedParcelTable.class).deleteById(returned.uuid());
    return true;
});
```

Expose only the table classes and `from(...)` factories required by the coordinator; keep constructors and domain conversion methods otherwise unchanged.

- [ ] **Step 4: Run the integration test to verify GREEN**

Run the command from Step 2. Expected: both atomicity tests pass when Docker is available; otherwise JUnit reports the Docker-gated class skipped.

---

### Task 2: Use the Atomic Commit and Preserve Cache Correctness

**Files:**
- Modify: `src/main/java/com/eternalcode/parcellockers/returns/ParcelReturnService.java`
- Modify: `src/main/java/com/eternalcode/parcellockers/parcel/service/ParcelService.java`
- Modify: `src/main/java/com/eternalcode/parcellockers/parcel/service/ParcelServiceImpl.java`
- Modify: `src/main/java/com/eternalcode/parcellockers/content/ParcelContentManager.java`
- Modify: `src/main/java/com/eternalcode/parcellockers/delivery/DeliveryManager.java`
- Modify: `src/main/java/com/eternalcode/parcellockers/ParcelLockers.java`
- Test: `src/test/java/com/eternalcode/parcellockers/returns/ParcelReturnServiceTest.java`

**Interfaces:**
- Consumes: `ParcelReturnRepository.commit(...)`.
- Produces: `void invalidate(UUID)` on parcel/content/delivery caches.

- [ ] **Step 1: Write a failing service test for durable commit ordering**

Mock `ParcelReturnRepository.commit` with an incomplete future, call `returnParcel`, and verify that neither `scheduler.runLaterAsync` nor the returned-success notice occurs until that future completes `true`. Complete it `false` in a second test and verify the deposited items are returned and no task is scheduled.

- [ ] **Step 2: Run the focused service test to verify RED**

Run:
```powershell
.\gradlew.bat test --tests "com.eternalcode.parcellockers.returns.ParcelReturnServiceTest"
```

Expected: compilation fails because the service has no return repository dependency or cache invalidation API.

- [ ] **Step 3: Implement commit-first scheduling**

Prepare one delivery timestamp, then:

```java
ParcelContent content = new ParcelContent(returned.uuid(), depositedCopy);
Delivery delivery = new Delivery(returned.uuid(), Instant.now().plus(delay));
return this.parcelReturnRepository.commit(returned, content, delivery).thenCompose(committed -> {
    if (!committed) {
        this.refund(player, refundableFee);
        return this.abort(player, deposited, messages -> messages.parcel.cannotReturn);
    }
    this.parcelService.invalidate(returned.uuid());
    this.parcelContentManager.invalidate(returned.uuid());
    this.deliveryManager.invalidate(returned.uuid());
    this.scheduler.runLaterAsync(
        new ParcelSendTask(returned, this.parcelService, this.deliveryManager, this.scheduler), delay);
    this.noticeService.player(player.getUniqueId(), messages -> messages.parcel.returned);
    return CompletableFuture.completedFuture(null);
});
```

Remove the pre-claim content update, separate `markReturned`, fire-and-forget delivery update, and best-effort collected-row delete from this flow. Wire `ParcelReturnRepositoryOrmLite` in `ParcelLockers.onEnable`.

- [ ] **Step 4: Run the focused service test to verify GREEN**

Run the command from Step 2. Expected: all service tests pass.

---

### Task 3: Validate Locker Existence and Capture Permission on Main Thread

**Files:**
- Modify: `src/main/java/com/eternalcode/parcellockers/returns/ParcelReturnService.java`
- Test: `src/test/java/com/eternalcode/parcellockers/returns/ParcelReturnServiceTest.java`

**Interfaces:**
- Consumes: existing `LockerManager.get(UUID)` and `LockerManager.isLockerFull(UUID)`.
- Produces: no new public API.

- [ ] **Step 1: Write failing missing-locker and permission-timing tests**

For the missing-locker test, return `Optional.empty()` from `lockerManager.get(entryLocker)`, then verify `isLockerFull`, economy withdrawal, and repository commit are never called. For permission timing, leave `parcelService.get` incomplete and verify `player.hasPermission("parcellockers.fee.bypass")` was already called immediately after `returnParcel`.

- [ ] **Step 2: Run the focused service test to verify RED**

Run the Task 2 focused test command. Expected: missing-locker verification fails because `get` is not called, and permission verification fails because it is currently delayed until an async continuation.

- [ ] **Step 3: Implement the main-thread and existence checks**

Capture:

```java
boolean feeBypassed = player.hasPermission(PARCEL_FEE_BYPASS_PERMISSION);
```

before the first repository future, pass it into `execute`, and replace the direct fullness call with:

```java
return this.lockerManager.get(current.entryLocker()).thenCompose(locker -> {
    if (locker.isEmpty()) {
        return this.abort(player, deposited, messages -> messages.parcel.cannotReturn);
    }
    return this.lockerManager.isLockerFull(current.entryLocker()).thenCompose(isFull -> {
        if (Boolean.TRUE.equals(isFull)) {
            return this.abort(player, deposited, messages -> messages.parcel.lockerFull);
        }
        return this.execute(player, current, deposited, feeBypassed);
    });
});
```

- [ ] **Step 4: Run the focused service test to verify GREEN**

Run the Task 2 focused test command. Expected: all service tests pass.

---

### Task 4: Confine Return GUI Mutation to the Primary Thread

**Files:**
- Modify: `src/main/java/com/eternalcode/parcellockers/gui/implementation/locker/ReturnGui.java`
- Create: `src/test/java/com/eternalcode/parcellockers/gui/implementation/locker/ReturnGuiTest.java`

**Interfaces:**
- Consumes: `Scheduler.run(Runnable)`.
- Produces: a package-private constructor accepting `Function<Component, PaginatedGui>` for test-controlled GUI creation.

- [ ] **Step 1: Write the failing empty-result threading test**

Construct `ReturnGui` with a mocked `PaginatedGui), an incomplete returnable-parcels future, and a mocked scheduler that captures but does not run callbacks. After `show`, clear initial synchronous setup interactions, complete the future with an empty page, verify no new GUI mutation occurred, run the captured callback, then verify the empty item was set and the GUI opened.

- [ ] **Step 2: Run the GUI test to verify RED**

Run:
```powershell
.\gradlew.bat test --tests "com.eternalcode.parcellockers.gui.implementation.locker.ReturnGuiTest"
```

Expected: the verification fails because the current async continuation calls `gui.setItem` before scheduling.

- [ ] **Step 3: Implement a single scheduled render boundary**

Keep data futures asynchronous. Move empty-state item placement, navigation setup, `PaginatedGuiRefresher` construction/population, and `gui.open` into `scheduler.run(() -> { ... })`. The public constructor delegates to the package-private factory constructor:

```java
this(guiSettings, scheduler, guiManager, miniMessage, noticeService,
    title -> Gui.paginated().rows(6).disableAllInteractions().title(title).create());
```

- [ ] **Step 4: Run the GUI test to verify GREEN**

Run the command from Step 2. Expected: the test passes.

---

### Task 5: Full Verification

**Files:**
- Review all files changed by Tasks 1-4.

**Interfaces:**
- Consumes: completed fixes.
- Produces: fresh test/build evidence and a clean patch check.

- [ ] **Step 1: Run focused return and GUI tests**

```powershell
.\gradlew.bat test --tests "com.eternalcode.parcellockers.returns.*" --tests "com.eternalcode.parcellockers.gui.implementation.locker.ReturnGuiTest" --tests "com.eternalcode.parcellockers.database.ParcelReturnCommitRepositoryIntegrationTest"
```

- [ ] **Step 2: Run the full suite**

```powershell
.\gradlew.bat test
```

- [ ] **Step 3: Build the plugin JAR**

```powershell
.\gradlew.bat shadowJar
```

- [ ] **Step 4: Inspect the final patch**

```powershell
git diff --check
git status --short
```

Expected: commands exit successfully; report any Docker-skipped Testcontainers tests rather than describing them as executed.
