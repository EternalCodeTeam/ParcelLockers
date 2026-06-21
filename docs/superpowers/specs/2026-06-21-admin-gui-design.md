# Admin GUI — Design

**Issue:** #64 — "Admin GUI (deleting, overwriting parcels, parcel lockers management and modifying them)"
**Branch:** `feature/admin-gui`
**Date:** 2026-06-21

## Summary

Add an in-game Admin GUI that lets a holder of `parcellockers.admin` browse and fully
modify every parcel, manage and rename lockers, inspect users, and run guarded bulk
deletions — all from inventory menus consistent with the plugin's existing
triumph-gui screens. Risky edits (parcel size, priority, receiver, destination, status)
are funneled through a dedicated service that enforces invariants (content fits the size,
destination locker not full) and gracefully updates delivery timing.

## Goals

- Single entry point: `/parcellockers admin` (reuses the existing `parcellockers.admin` permission).
- Full parcel modification: name, description, priority, size, status, receiver, destination, delete.
- Locker management: browse, rename, teleport, delete.
- User inspection: browse users and view their received/sent parcels (read-only).
- Bulk actions: delete-all parcels / delete-all lockers, each behind a confirmation dialog.
- Safe edits to **in-transit** parcels (no stale-snapshot overwrite by the delivery task).

## Non-Goals

- Editing parcel **item contents** (only metadata is editable in this version).
- A new top-level command or a separate permission node.
- Web/REST admin surface — in-game GUI only.
- Free-text editing via chat or anvil — input uses the Paper Dialog API only.

## Background / Current State

- Domains are layered: Model → Repository (ORMLite, returns `CompletableFuture`) →
  Manager/Service → Controller. Manual DI in `ParcelLockers#onEnable`.
- GUIs implement `GuiView`, built with triumph-gui, items/titles sourced from
  `PluginConfig.GuiSettings`, data loaded via `CompletableFuture` and reopened on the
  main thread with `Scheduler#run`. `MainGui` / `ParcelListGui` are reference patterns.
- Free-text input already uses the Paper **Dialog API** (`SendingGui`,
  `DiscordVerificationDialogFactory`, `LockerPlaceController`).
- `ParcelService` already exposes `update`, `delete(parcel)`, `deleteAll`, `get`,
  `getBySender`, `getByReceiver`. There is **no** "get all parcels" path yet.
- `LockerManager` exposes `get`, `get(page)`, `create`, `delete`, `deleteAll`, but
  **no rename/update** — `Locker(uuid, name, position)` is an immutable record.
- `UserManager#getPage` and `GuiManager#getUsers` already exist.

### Size → capacity

Content GUIs (`ItemStorageGui`) are 2/3/4 rows for SMALL/MEDIUM/LARGE, with the bottom
row reserved, giving **usable capacities of 9 / 18 / 27 item stacks**. This matches the
size-inference in `SendingGui`. Shrinking a parcel's size must reject when
`content.items().size() > capacity(newSize)`.

### Priority → delivery time, and the stale-snapshot problem

Delivery timing is set once at dispatch: `deliveryTimestamp = now + (priority ?
priorityParcelSendDuration : parcelSendDuration)`, stored in a `Delivery` row.
`ParcelSendTask` is scheduled via `Scheduler#runLaterAsync` with a fixed delay and
**captures a `Parcel` snapshot** at schedule time; on fire it marks that snapshot
`DELIVERED`. It does **not** re-read the parcel or the delivery timestamp, and no
cancellation handle is stored. Consequently, editing an in-transit (`SENT`) parcel today
would be silently reverted when the already-scheduled task fires with its stale snapshot.

## Design

### Entry point

Add to `ParcelLockersCommand` (already `@Permission("parcellockers.admin")`):

```java
@Execute(name = "admin")
void admin(@Sender Player player) { this.adminGui.show(player); }
```

`AdminGui` is constructed in `ParcelLockers#onEnable` alongside the other GUIs and
injected into the command.

### GUI layer — new package `gui/implementation/admin/`

All implement `GuiView`, follow the existing constructor-injection + `CompletableFuture`
+ `Scheduler#run` reopen pattern, hold a back-navigation reference to their parent, and
source every item/title from config.

| Screen | Purpose |
|---|---|
| `AdminGui` | Root menu: Parcels, Lockers, Users, Bulk-delete parcels, Bulk-delete lockers, Close. |
| `AdminParcelListGui` | Paginated list of **all** parcels; click → `AdminParcelEditGui`. |
| `AdminParcelEditGui` | Per-field buttons (see below) + Delete + Back. |
| `AdminLockerListGui` | Paginated list of all lockers; click → `AdminLockerEditGui`. |
| `AdminLockerEditGui` | Rename (Dialog), Teleport, Delete, Back. |
| `AdminUserListGui` | Paginated users; click → `AdminUserInspectGui`. |
| `AdminUserInspectGui` | Read-only: that user's received & sent parcels (reuses existing parcel rendering). |

`AdminParcelEditGui` buttons:

- **Name**, **Description** — Paper Dialog text input (pattern from `SendingGui`).
- **Priority** — click-to-toggle boolean.
- **Size** — click-to-cycle `SMALL → MEDIUM → LARGE`.
- **Status** — click-to-cycle `ParcelStatus` (`SENT`/`DELIVERED`).
- **Receiver** — opens a user-picker sub-GUI.
- **Destination** — opens a locker-picker sub-GUI.
- **Delete** — confirmation Dialog, then delete.

Bulk actions on `AdminGui` open a confirmation Dialog before calling the existing
`deleteAll` paths.

### Service layer — `AdminParcelService` (new)

Centralizes risky edits and their side effects so GUIs stay thin. All methods return
`CompletableFuture<EditResult>` where `EditResult` carries success or a typed failure
(e.g. `SIZE_TOO_SMALL`, `DESTINATION_FULL`) the GUI maps to a notice.

- `changeName` / `changeDescription` — straight `ParcelService#update`.
- `changeSize(parcel, newSize)` — load `ParcelContent`; if
  `items().size() > capacity(newSize)` → `SIZE_TOO_SMALL` (no write); else `update`.
  `capacity`: SMALL=9, MEDIUM=18, LARGE=27.
- `changePriority(parcel, newPriority)` — `update`; then if the parcel is in-transit
  (`SENT` and a `Delivery` exists), **shift** the timestamp by the duration delta:
  `newTs = oldTs + (durationFor(newPriority) - durationFor(oldPriority))`, **clamped to
  not before `now`** (an already-overdue shift delivers promptly, never retroactively),
  persisted via `DeliveryManager`.
- `changeStatus(parcel, newStatus)` — `update`. (Combined with the task refactor below,
  forcing `DELIVERED` cleanly resolves a stuck parcel.)
- `changeReceiver(parcel, receiver)` — `update`.
- `changeDestination(parcel, lockerUuid)` — re-check `LockerManager#isLockerFull`;
  if full → `DESTINATION_FULL` (no write); else `update`.
- `delete(parcel)` — existing delete path.

`durationFor(priority)` reads `priorityParcelSendDuration` / `parcelSendDuration` from
`PluginConfig`.

### Locker rename

Add `LockerManager#rename(UUID, String newName): CompletableFuture<Locker>` backed by a
repository update, refreshing the `lockersByUUID` / `lockersByPosition` caches. Name is
validated via the existing `LockerValidationService` rules; position is unchanged.

### `ParcelSendTask` refactor (Option A)

At fire time the task re-fetches current state instead of trusting its constructor
snapshot:

1. Load the current `Parcel` by uuid. If absent or already `DELIVERED`, no-op (and clean
   up any stray `Delivery`).
2. Load the current `Delivery`. If its `deliveryTimestamp` is now in the future
   (admin extended it / toggled priority), **reschedule** `this` for the new delay and
   return.
3. Otherwise mark the **current** parcel `DELIVERED` (preserving any admin-edited fields),
   fire `ParcelDeliverEvent`, then delete the delivery — same ordering/retry semantics as
   today.

Call sites (dispatch and the startup reschedule loop in `ParcelLockers#onEnable`) are
unchanged; only the task's internal behavior changes. This makes **every** admin edit to
an in-transit parcel safe — no stale-snapshot overwrite — and is what implements
"gracefully change delivery time."

### Config & messages

- `GuiSettings`: new `ConfigItem`s for every admin button/icon and new GUI titles.
- `MessageConfig.AdminMessages`: `parcelUpdated`, `sizeTooSmall`, `priorityUpdated`,
  `lockerRenamed`, `teleported`, `destinationFull`, plus confirmation prompt strings.
- All user-facing text stays in config (existing convention).

### Error handling

- Repository failures flow through `FutureHandler::handleException`; invariant violations
  (`SIZE_TOO_SMALL`, `DESTINATION_FULL`) abort the edit with no partial write and send a
  notice.
- GUIs always reopen on the main thread via `Scheduler#run`.

## Testing

- **Unit — `AdminParcelService`:** size-capacity boundaries (9/18/27 exact-fit vs.
  over-by-one), priority delta-shift incl. clamp-to-now for overdue, destination-full
  rejection, status change.
- **Unit — `ParcelSendTask`:** reschedule when timestamp moved to the future; no-op when
  parcel missing/already delivered; delivers current (edited) fields.
- **Integration — `LockerManager#rename`:** Testcontainers DB, following
  `LockerRepositoryIntegrationTest`.

## Implementation Phasing (single spec)

1. **Foundation & low-risk areas:** `AdminGui` + command entry + wiring; locker
   browse/rename/teleport/delete (`LockerManager#rename`); user inspect; bulk actions;
   config/message scaffolding.
2. **Parcel editing & safety:** `AdminParcelService` with all invariants;
   `AdminParcelListGui` + all-parcels query; `AdminParcelEditGui`; `ParcelSendTask`
   refactor; tests.

## Open Questions

None outstanding. Decisions locked: reuse `parcellockers.admin`; Dialog API for input;
all parcel fields editable; size-fit + priority-graceful constraints; priority timing via
clamped delta-shift; Option A task refactor; one phased spec.
