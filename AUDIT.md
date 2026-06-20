# ParcelLockers — Code Audit

Audit of `src/` covering duplication, race conditions, optimization opportunities,
and database / unintended-behavior bugs.

Scope: 134 Java source files. Findings are grouped by severity. Each item lists the
file and the relevant lines so they can be jumped to directly.

Severity legend: **🔴 Critical** (item/money loss, duplication exploits) ·
**🟠 High** (data-integrity races, broken correctness) · **🟡 Medium** (latent bugs,
inconsistency) · **🔵 Low** (style, minor optimization).

---

## Resolution status (`audit-fixes` branch)

All findings below are fixed on the `audit-fixes` branch, one commit each, except where noted:

- **Critical:** C1, C2, C3, C4 — fixed.
- **High:** H1, H2, H3, H4, H5, H6 — fixed.
- **Medium:** M1–M8 — fixed.
- **Low:** L1, L3, L4, L5 — fixed. **L2** was folded into the H2 fix (the redundant
  `count > 0` clause was removed there).
- **Duplication:** D1, D2, D4, D5 — fixed (D1 also fixed the same swallowed table-creation
  bug in the Delivery and User repos; D2 also fixed a latent always-false "has next page" in
  `UserRepository.fetchPage`). **D3** (generic cache-or-fetch helper) was intentionally left:
  the five managers use their caches differently enough that one abstraction would risk
  masking per-cache nuance, and the concrete inconsistency it could hide was already fixed by H6.

`./gradlew compileJava` and `./gradlew test` pass on the branch.

---

## 🔴 Critical

### C1 — Economy fee is charged before persistence and never refunded on failure
`parcel/service/ParcelServiceImpl.java:102` (withdraw) vs `:122-138` (save + rollback)

`send()` withdraws the fee at line 102, then saves the parcel/content. If
`parcelRepository.save(...)` or content save fails, the `exceptionally`/rollback
branches delete the parcel and notify the player, **but never refund the money**.
The player is charged for a parcel that was never created.

Fix: only withdraw after a successful save, or `depositPlayer` in every failure path
(content-save failure at `:129`, parcel-save failure at `:135`).

### C2 — Dispatch rollback loses the fee and orphans parcel content
`parcel/service/ParcelDispatchService.java:68-74`

After `parcelService.send(...)` succeeds (parcel **and** content persisted, fee
charged), `itemStorageManager.delete(...)` is awaited. If it returns `false`, the code
calls `parcelService.delete(parcel.uuid())` — which deletes only the **parcel row**, not
the `ParcelContent` row saved in `send()`. Result: orphaned content in the DB, the
sender's item-storage still present, and the fee is not refunded.

Fix: refund on this path and also delete the parcel content; ideally make
"charge → save parcel → save content → clear storage → schedule" a single rollback unit.

### C3 — Locker can be duplicated: place is cancelled but the item is never consumed
`locker/controller/LockerPlaceController.java:80, 126-129`

`onBlockPlace` calls `event.setCancelled(true)` (so vanilla never consumes the item from
the hand), then on dialog confirm it manually re-places the block via
`setType`/`setBlockData`. The locker item is **never removed from the player's
inventory**, so a single locker item creates unlimited lockers.

Fix: decrement the used item stack by one (main hand or off hand, matching the
`isSimilar` check at `:72`) when the locker is actually created.

### C4 — Breaking a locker is never cancelled → chest + contents drop, then block is restored
`locker/controller/LockerBreakController.java:38-78`

`onBlockBreak` never calls `event.setCancelled(true)`. For a non-admin player the block
is physically broken first (the chest item, and in survival its drops, spawn into the
world), and only on the next tick is the block restored via `setType`/`setBlockData`
(`:53-54`). The already-spawned drops remain → free chests / item duplication. For the
admin path the block is also left in vanilla-broken state before `delete` runs.

Fix: cancel the `BlockBreakEvent` for non-admins immediately and return; for admins,
cancel and then run the managed delete + broadcast.

---

## 🟠 High

### H1 — `collect()` deletes the parcel before giving items; inventory check is a TOCTOU
`parcel/service/ParcelServiceImpl.java:171-215`

`freeSlotsInInventory(player)` is evaluated on the async DB thread (`:191`). The parcel
and content rows are then deleted (`:196-197`), and only afterwards are items handed back
on the main thread, one scheduler task per item (`:204`). Between the slot check and the
delivery the inventory can change, and items are given **after** the only copy in the DB
is destroyed. Any failure (full inventory, server stop, exception in `giveItem`) loses the
items permanently.

Fix: give items first (and let `ItemUtil.giveItem` drop overflow), then delete only after
delivery is confirmed; re-check free slots on the main thread.

### H2 — Locker fullness check is a TOCTOU race
`parcel/service/ParcelDispatchService.java:50-61` + `locker/LockerManager.java:176-178`

`isLockerFull` runs a `COUNT(*)` and `dispatch` proceeds to `send` based on the result.
Two players sending to the same near-full locker concurrently can both pass the check and
both store, exceeding `maxParcelsPerLocker`. There is no atomic guard at the DB level.

Fix: enforce the cap inside a single transaction, or re-check + compensate after insert.

### H3 — `LockerManager.get(Page)` returns wrong/incomplete pages from the cache
`locker/LockerManager.java:96-103`

```java
List<Locker> cached = List.copyOf(this.lockersByUUID.asMap().values());
boolean hasNextPage = cached.size() > page.getLimit();
if (!cached.isEmpty() && page.getOffset() == 0 && !hasNextPage) {
    return CompletableFuture.completedFuture(new PageResult<>(cached, false));
}
```

The cache holds an arbitrary, partially-evicted, unordered subset of lockers — it is **not**
the full dataset. When fewer than `limit` lockers happen to be cached, page 0 returns just
those, hiding every locker that isn't currently cached (and in non-deterministic order).
This silently shows users an incomplete locker list.

Fix: don't serve pagination from a partial cache; query the repository (or maintain a
separate authoritative ordered index).

### H4 — Locker GUI opens on top of the vanilla chest (cancel happens too late)
`locker/controller/LockerInteractionController.java:41-51`

The DB lookup is async; `event.setCancelled(true)` is executed inside
`scheduler.run(...)` on a later tick — long after the `PlayerInteractEvent` has finished
and the vanilla chest inventory has already opened. The player gets both the vanilla chest
and the locker GUI, and the cancel is a no-op.

Fix: resolve locker membership synchronously where possible (e.g. position cache lookup),
cancel the event in the same tick, then open the GUI.

### H5 — `getOrCreate` / `create` only check the cache for conflicts, not the DB
`user/UserManagerImpl.java:65-117`, and the same pattern in
`locker/LockerManager.java:105-138`

`getOrCreate` (`:65`) returns from cache or calls `create`. `create` validates "no
conflict" using **cache-only** lookups (`usersByUUID.get(uuid, k -> null)` at `:95-96`),
so an entity that exists in the DB but not in the cache passes validation and a second
"create" is attempted. Combined with `get(...)` not populating the cache on a miss (see
H6), this is reachable in normal operation.

Fix: consult the repository in the conflict check, or rely on a DB unique constraint and
handle the violation.

### H6 — `UserManagerImpl.get(...)` never populates the cache on a miss
`user/UserManagerImpl.java:42-62`

Both `get(UUID)` and `get(String)` return straight from the repository on a cache miss
without putting the result into `usersByUUID` / `usersByName`. Every lookup for an
uncached user is a DB round-trip, and the cache only ever fills via `create`/`changeName`.
This also feeds H5. (Contrast with `LockerManager.get`, `DeliveryManager.get`,
`ItemStorageManager.get`, which all cache on read.)

Fix: cache the fetched user (by UUID and name) before returning.

---

## 🟡 Medium

### M1 — `ParcelSendTask` updates status and deletes the delivery as independent fire-and-forget calls
`parcel/task/ParcelSendTask.java:54-64`

The "mark DELIVERED" update (`:54`) and the "delete delivery" call (`:60`) are not
chained. If the update fails but the delete succeeds, the parcel is stuck in `SENT`
forever with no delivery row, so it is never re-scheduled on restart
(`ParcelLockers.java:215-228` only reschedules parcels that still have a delivery) and
never becomes collectable.

Fix: chain — delete the delivery only after the status update completes successfully.

### M2 — Async-only Bukkit events are fired from mixed threads (latent `IllegalStateException`)
`itemstorage/ItemStorageManager.java:51-66`, `user/UserManagerImpl.java:87-117,120-143`,
`locker/LockerManager.java:105-138`

`ItemStorageUpdateEvent`, `UserCreateEvent`, `UserChangeNameEvent`, `LockerCreateEvent`,
`ParcelSendEvent`, `ParcelDeliverEvent` are all constructed with `super(true)`
(asynchronous), while `ParcelCollectEvent`, `LockerDeleteEvent`, `UserChangeNameEvent`'s
counterparts use the default sync constructor. Paper throws if an async event is fired
from the primary thread (or a sync event from an async thread). Whether each `callEvent`
is legal currently depends entirely on the calling thread happening to match the flag —
e.g. `ItemStorageManager.getOrCreate` (`:47`) runs its loader synchronously on the caller
thread, and if ever invoked on the main thread it will throw when firing the async
`ItemStorageUpdateEvent`.

Fix: pick one threading model per event deliberately, and document/guarantee the firing
thread; don't let it depend on the call path.

### M3 — `ItemStorageManager.getOrCreate` mutates the cache from inside a cache-loader for the same key
`itemstorage/ItemStorageManager.java:47-66`

`getOrCreate` calls `cache.get(owner, key -> this.create(key, items))`, and `create`
itself calls `this.cache.put(owner, ...)` (`:63`) for the **same key** that is currently
being loaded. Recursive computation/mutation of the same key inside a Caffeine mapping
function is explicitly discouraged and can throw `IllegalStateException` / corrupt the
entry.

Fix: have the loader return the value and let `cache.get` store it; don't `put` inside the
loader.

### M4 — Item-storage save/delete failures are silently swallowed (possible item loss)
`gui/implementation/locker/ItemStorageGui.java:106-117`, `gui/GuiManager.java:90-92`,
`itemstorage/ItemStorageManager.java:64`

On GUI close, items are read from the inventory, the storage row is deleted, then
`saveItemStorage` is called — but `create()`'s `itemStorageRepository.save(...)` return is
ignored (fire-and-forget at `ItemStorageManager.java:64`), and `saveItemStorage` returns
`void`. If the save fails after the delete succeeded, the player's staged items are gone
with no error surfaced.

Fix: chain delete → save, propagate failure, and re-give items on failure.

### M5 — `ItemStorageRepositoryOrmLite` swallows table-creation failure
`itemstorage/repository/ItemStorageRepositoryOrmLite.java:19-23`

```java
} catch (SQLException ex) {
    ex.printStackTrace();
}
```

Every other repository (`ParcelRepositoryOrmLite:34`, `LockerRepositoryOrmLite:26`,
`ParcelContentRepositoryOrmLite:21`) throws `DatabaseException` on a failed
`createTableIfNotExists`. Here it prints and continues, so a missing table only surfaces
later as confusing query errors.

Fix: throw `DatabaseException` for consistency and fail-fast startup.

### M6 — Confusing repository `save`/`update` semantics
`parcel/repository/ParcelRepositoryOrmLite.java:40-49` +
`database/wrapper/AbstractRepositoryOrmLite.java:27-33`

`save(Parcel)` maps to `saveIfNotExist` → `dao.createIfNotExists` (a no-op if the row
exists), while `update(Parcel)` maps to the base `save` → `dao.createOrUpdate`. The
method named `save` cannot persist changes to an existing parcel, and the wrapper method
named `save` actually performs an upsert. This inversion is an easy source of "my update
didn't persist" bugs.

Fix: rename wrapper methods to `upsert`/`insertIfAbsent` and align the repository method
names with their real behavior.

### M7 — `DatabaseManager.getDao` check-then-put is not atomic
`database/DatabaseManager.java:99-112`

`cachedDao.get` followed by `cachedDao.put` on a `ConcurrentHashMap` can run twice
concurrently and create/replace the DAO twice. Harmless today (ORMLite caches DAOs
internally) but it defeats the point of the cache.

Fix: `cachedDao.computeIfAbsent(type, t -> DaoManager.createDao(...))`.

### M8 — Aggressive/duplicated HikariCP tuning
`database/DatabaseManager.java:47-49`

`leakDetectionThreshold` (5000 ms) equals `connectionTimeout` (5000 ms), and the pool is
hard-capped at 5 connections regardless of DB type or server size. Any legitimately slow
query will log a false "connection leak" warning. None of these are configurable.

Fix: raise/relax the leak threshold, make pool size configurable, and consider a larger
default for networked DBs.

---

## 🔵 Low / Optimization

- **L1 — Redundant enum round-trip.** `database/DatabaseManager.java:53`
  `DatabaseType.valueOf(databaseType.toString().toUpperCase())` — `databaseType` is already
  a `DatabaseType`; switch on it directly.
- **L2 — Redundant condition.** `locker/LockerManager.java:178`
  `count > 0 && count >= maxParcelsPerLocker` — the first clause is redundant unless
  `maxParcelsPerLocker <= 0`; clarify the intended zero/negative behavior.
- **L3 — `onDisable` ordering.** `ParcelLockers.java:231-244` closes the datasource before
  unregistering anything; in-flight async DB tasks (scheduler pool) will then fail. Consider
  draining/awaiting pending writes before `disconnect()`.
- **L4 — Stale "TODO"/placeholder comments.** `locker/controller/LockerPlaceController.java:92`
  ("Replace with actual config message" — but the config value *is* used) and hardcoded
  English dialog strings throughout `LockerPlaceController` and `SendingGui`
  (`:95-160`) bypass `MessageConfig`, contradicting the "all user-facing text goes through
  MessageConfig" architecture note.
- **L5 — `freeSlotsInInventory` ignores stacking.** `util/InventoryUtil.java:12-20` counts
  only fully empty slots, so `collect()` can wrongly reject a parcel whose items would stack
  into partially-filled slots (or accept-then-drop). Account for partial stacks if precise.
- **L6 — `CollectionGui` removes the item from the GUI before collection is confirmed.**
  `gui/implementation/locker/CollectionGui.java:142-145` calls `collectParcel` (async) and
  `refresher.removeItemBySlot` immediately; on a failed collect the UI desyncs from reality.
  (The double-collect dupe is prevented by the `delete`-returns-false guard in `collect`, but
  the visual desync remains.)

---

## Duplication

The codebase is generally well-layered, but several patterns are copy-pasted across every
domain and are worth extracting:

1. **Repository table bootstrap.** The `try { TableUtils.createTableIfNotExists(...) }
   catch { throw new DatabaseException(...) }` block is duplicated in every `*OrmLite`
   constructor (parcel, locker, content, itemstorage, delivery, user). Pull into
   `AbstractRepositoryOrmLite` as a protected helper (and fix M5 in the process).

2. **Pagination logic.** `ParcelRepositoryOrmLite.findByPaged` (`:108-128`) and
   `LockerRepositoryOrmLite.findPage` (`:61-78`) implement the identical "limit+1, remove
   last, compute hasNext" pattern (and `UserRepository.fetchPage` likely a third copy).
   Extract a shared paged-query helper.

3. **Manager cache-or-fetch.** The "check cache → on miss fetch from repo → populate cache"
   block is reimplemented in `LockerManager`, `DeliveryManager`, `ItemStorageManager`,
   `UserManagerImpl`, and `ParcelServiceImpl` — with subtle inconsistencies (UserManager
   forgets to populate; H6). A small generic cached-loader would unify behavior.

4. **Bukkit event boilerplate.** Every event class (`ParcelSendEvent`, `ParcelCollectEvent`,
   `LockerCreateEvent`, …) repeats the `HandlerList` / `getHandlers` / `isCancelled` /
   `setCancelled` block. A shared abstract `CancellableEvent` base removes ~30 lines each and
   would make the async-flag decision (M2) explicit in one place.

5. **`createActiveItem` overloads.** `SendingGui.java:419-437` has two near-identical
   methods differing only in `add` vs `addAll`; collapse to one taking a `List`.

---

## Summary

| Area | Count | Highest severity |
|---|---|---|
| Item/money loss & duplication | C1–C4 | 🔴 Critical |
| Concurrency / data-integrity races | H1–H6, M1, M3 | 🟠 High |
| Correctness & consistency | M2, M4–M8 | 🟡 Medium |
| Style / minor optimization | L1–L6 | 🔵 Low |
| Duplication | 5 patterns | — |

The most urgent work is the parcel-send/collect money-and-item flows (C1, C2, H1) and the
two block-controller exploits (C3, C4), all of which are reachable in normal gameplay and
cause real loss or duplication. The async-event threading model (M2) is a latent crash that
should be made deliberate rather than incidental.

> Note: this is a static review; none of the findings were reproduced at runtime. Validate
> the critical items (especially C3/C4 duplication and C1/C2 refunds) with targeted tests
> before and after fixing.