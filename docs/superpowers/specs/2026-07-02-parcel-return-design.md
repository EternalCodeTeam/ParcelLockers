# Parcel Return (GitHub issue #69) — Design

Date: 2026-07-02
Status: Approved by user (brainstorming session)
Issue: https://github.com/EternalCodeTeam/ParcelLockers/issues/69

## Summary

Players can return a parcel they collected by visiting any parcel locker, depositing
the same items that were in the parcel, and paying a configurable per-size return fee.
The parcel then ships back to the original sender at the original entry locker using
the normal delivery pipeline.

## Decisions made during brainstorming

- **Eligibility:** returnable only after collection, within a configurable time window
  (default 7 days). Expired collected parcels are purged.
- **Return flow:** the parcel ships back like a normal parcel (reverse direction,
  normal delivery delay), it is not instantly collectible.
- **Item validation:** strict by default; config flags relax individual attributes:
  `checkDurability`, `checkItemName`, `checkEnchantments`, `checkLore`, `checkNbt`.
  Material types and amounts must always match.
- **Cost:** per-size return fees (`smallParcelReturnFee`, `mediumParcelReturnFee`,
  `largeParcelReturnFee`), consistent with send fees. The existing
  `parcellockers.fee.bypass` permission also bypasses return fees.
- **GUI placement:** a third "Return parcel" button in the physical locker GUI
  (next to Collect/Send). Returns can be initiated at any locker; the parcel ships
  to the original entry locker regardless.

## Data model changes

### `ParcelStatus.COLLECTED` (new enum value)

`ParcelServiceImpl.collect` no longer deletes the parcel and its content. Instead it:

1. Updates the parcel's status to `COLLECTED` (same row, same UUID).
2. Keeps the `parcel_contents` row untouched — it becomes the validation snapshot
   for a later return.
3. Saves a row in a new `collected_parcels` table.

ORMLite persists enums by name, so the new value needs no schema change.

### `collected_parcels` table (new)

Mirrors the existing `deliveries` table pattern:

| column       | type    | notes                          |
|--------------|---------|--------------------------------|
| `parcel`     | UUID    | primary key                    |
| `collected_at` | Instant | via `InstantPersister`       |

New domain package `returns` (or extend `parcel`): `CollectedParcel` record,
`CollectedParcelRepository` interface + `CollectedParcelRepositoryOrmLite`
(extends `AbstractRepositoryOrmLite`, `createTableIfNotExists` on construction —
no migration mechanism required).

### Locker fullness fix

`ParcelRepositoryOrmLite.countParcelsByDestinationLocker` currently counts all
parcels for a locker. COLLECTED parcels are no longer physically in the locker,
so the query gains a `status <> 'COLLECTED'` filter. Without this, kept parcels
would permanently consume `maxParcelsPerLocker` capacity.

## Return eligibility

A parcel is returnable by player P at any locker when all of:

- `parcel.status == COLLECTED`
- `parcel.receiver == P`
- `collectedAt + settings.parcelReturnWindow` is in the future

### Purge task

A repeating async task (runs once at startup, then periodically, e.g. every 30
minutes) deletes parcels whose return window has expired: parcel row, content row,
and `collected_parcels` row. Failures are logged and retried on the next run;
orphaned content rows are tolerated (existing precedent in `ParcelServiceImpl`).

## GUI flow

1. **`LockerGui`** gets a third button, "Return parcel" (slot 22, between the
   collect and send buttons, which move if needed for symmetry).
2. **`ReturnGui`** (new, modeled on `CollectionGui`): paginated list of the
   player's returnable parcels with item lore showing parcel details and its
   items. Clicking a parcel opens the deposit GUI. Shows a "no parcels" item
   when empty.
3. **Deposit GUI** (new, modeled on `ItemStorageGui`): a `StorageGui` sized by
   parcel size (2/3/4 rows) where the player places the items, with a confirm
   button in the bottom row. Confirming triggers validation and the return.
   Closing without confirming hands all placed items back.

## Item validation — `ParcelReturnValidator`

Compares deposited items against the stored `ParcelContent` snapshot:

- **Flags** (all `true` = strict by default) in `settings.returnChecks`:
  - `checkDurability` — damage values must match
  - `checkItemName` — custom display names must match
  - `checkEnchantments` — enchantments must match
  - `checkLore` — lore must match
  - `checkNbt` — all remaining item meta must match
- Implementation: normalize both sides by cloning each stack and stripping the
  attributes whose flag is disabled (when `checkNbt` is false, meta is reduced to
  only the still-enabled attributes). Then aggregate each side into a map of
  normalized-item → total amount (equality of keys via `ItemStack.isSimilar`)
  and require the two maps to be equal. Aggregation makes stack splitting or
  merging irrelevant; material and total amount therefore always must match,
  even with every flag disabled.
- On mismatch: items are handed back to the player, a mismatch notice is sent,
  nothing is charged, the parcel stays returnable.

## Return execution — `ParcelReturnService`

Orchestrated like `ParcelDispatchService` (constructor-wired in `ParcelLockers.onEnable`).
Steps, in order:

1. Fire cancellable `ParcelReturnEvent` (new, modeled on `ParcelCollectEvent`).
   Cancelled → hand items back, notice.
2. Re-check eligibility (status still COLLECTED, window still open) — guards
   against double-clicks and expiry races.
3. Check the original **entry** locker (the return's destination) is not full
   via `LockerManager.isLockerFull`. Full → hand items back, notice.
4. Charge the per-size return fee unless the player has
   `parcellockers.fee.bypass`. Insufficient funds → hand items back, notice.
5. Overwrite the `parcel_contents` row with the actually-deposited items (they
   may legitimately differ when flags are relaxed).
6. Update the parcel row: swap `sender`/`receiver`, swap
   `entryLocker`/`destinationLocker`, status `SENT`. Same UUID — the parcel keeps
   its identity, name, description, size, and priority flag.
7. Delete the `collected_parcels` row.
8. Create a `Delivery` and schedule `ParcelSendTask` with the normal
   priority-aware send duration.
9. Send the success notice.

Failure after the fee is charged → refund the fee and hand the items back
(mirrors `ParcelServiceImpl.send` rollback). Failures between steps 5–8 are
rolled back best-effort with logging, consistent with existing patterns.

Concurrency: returns to the same destination locker go through the same
per-locker serialization used by `ParcelDispatchService` (reuse or replicate the
`lockerChains` pattern) so fullness checks cannot race.

## Config additions

`PluginConfig.Settings`:

- `Duration parcelReturnWindow = Duration.ofDays(7)`
- `double smallParcelReturnFee = 5.0`, `mediumParcelReturnFee = 12.5`,
  `largeParcelReturnFee = 25.0`
- `ReturnChecks returnChecks` — nested `OkaeriConfig` with the five boolean flags,
  all defaulting to `true`

`PluginConfig.GuiSettings`:

- `parcelReturnGuiTitle`, `parcelReturnDepositGuiTitle`
- `parcelLockerReturnItem` (locker GUI button), `parcelReturnItem` (row item in
  ReturnGui), `noReturnableParcelsItem`, `confirmReturnItem`
- lore line for remaining return window time, e.g.
  `returnWindowRemainingLine` with `{DURATION}` placeholder

`MessageConfig.parcel` (new notices):

- `returned` — success
- `returnItemsMismatch` — deposited items don't match
- `returnFeeWithdrawn` — fee charged (with `{AMOUNT}`)
- `cannotReturn` — generic failure / event cancelled / expired

Existing notices reused: `insufficientFunds`, `lockerFull`, `illegalItem`
(illegal items cannot be deposited in the return GUI either).

## Events

- `ParcelReturnEvent` (cancellable, carries the parcel) — fired before execution.

## Testing

- **Unit:** `ParcelReturnValidatorTest` with mocked `ItemStack`s (per test
  harness constraints: no server bootstrap, ormlite off the test classpath) —
  one case per flag, stack-splitting case, amount mismatch, type mismatch.
- **Integration (Docker/Testcontainers):** `CollectedParcelRepositoryIntegrationTest`
  following `LockerRepositoryIntegrationTest`; extend
  `ParcelRepositoryIntegrationTest` for the fullness-count status filter.
- Manual smoke test via `./gradlew runServer`.

## Out of scope

- Rejecting/returning a parcel before collection.
- Returning an already-returned parcel a second time (a returned parcel becomes a
  normal SENT parcel; once the original sender collects it, it becomes COLLECTED
  and is returnable again by them — acceptable emergent behavior).
- Refunding the original send fee to the returning player.
- The unimplemented "Parcel archive" GUI.
