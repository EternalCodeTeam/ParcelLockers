# Parcel Return Review Fixes — Design

Date: 2026-07-14
Status: Approved in conversation
Review: `PR_REVIEW_feat-parcel-return-gh-69.md`

## Summary

The parcel-return flow needs four corrections before merge: atomic winner-owned
content, durable delivery scheduling, destination-locker validation, and safe
main-thread boundaries for Bukkit and GUI APIs.

## Accepted findings

All four findings are valid for the current branch:

1. Content replacement happens before the conditional return claim, so a losing
   concurrent attempt can overwrite the winning shipment's content.
2. Locker capacity lookup does not prove that the original entry locker still
   exists, so a returned parcel can be addressed to a deleted locker.
3. The parcel becomes `SENT` before its delivery row is durably persisted, so a
   restart can leave it without anything startup recovery can schedule.
4. Repository futures complete on async scheduler threads, but `ReturnGui` mutates
   GUI state and `ParcelReturnService` checks player permissions in continuations.

## Architecture

### Atomic return commit

Introduce a return-specific repository operation backed by ORMLite's transaction
support. It accepts the prepared reverse `Parcel`, deposited `ParcelContent`, and
`Delivery`, then performs these operations on one database connection and in one
transaction:

1. Conditionally update the parcel only when its current status is `COLLECTED`.
2. If no row was updated, return `false` without changing content or delivery data.
3. Upsert the deposited parcel content.
4. Upsert the delivery timestamp.
5. Delete the corresponding `collected_parcels` row.
6. Commit and return `true`.

Any exception rolls back every operation. Two concurrent attempts therefore have
one winner, and the content and delivery row always belong to that winner. The
transaction also makes every committed `SENT` return discoverable by startup
delivery recovery before the player is told it succeeded.

After a successful commit, the service synchronizes the parcel, content, and
delivery caches without issuing additional database writes. It then schedules the
in-memory delivery task. If scheduling throws during shutdown, the durable delivery
row remains available for startup recovery; deposited items and fees are not
returned because the reverse shipment has already committed.

### Locker validation

Resolve the original entry locker with `LockerManager.get` before the fullness
query. An absent locker rejects the attempt using the generic cannot-return notice,
returns the deposited items, and does not charge a fee. Capacity is checked only for
an existing locker.

### Main-thread boundary

Capture the fee-bypass permission before entering repository continuations. The
return entry point is invoked by a GUI event on the primary thread, while Vault
withdrawal and refund operations remain explicitly scheduled there.

`ReturnGui` continues fetching parcel/content data asynchronously, but every GUI
mutation is placed in one primary-thread callback: navigation setup, empty-state
item placement, `PaginatedGuiRefresher` population, and `gui.open`. Async work may
only prepare immutable/plain data and deferred item suppliers.

## Error handling

- A conditional-claim loss refunds any charged fee and gives the deposited items
  back.
- A transaction failure rolls back all persistent return state, refunds the fee,
  gives the deposited items back, and sends the generic failure notice.
- A post-commit scheduling failure is logged but never triggers refund/item
  give-back, because doing so would duplicate committed shipment contents.
- A missing original locker is handled before charging or committing.

## Testing

- Add a Docker-gated integration test that launches two return commits with
  different deposited content, asserts exactly one succeeds, and verifies parcel,
  content, and delivery rows all belong to the winner.
- Add an integration test proving a failed conditional claim leaves existing
  content and delivery data unchanged.
- Add focused service tests for missing original locker and for permission capture
  before async repository completion where practical with existing mocks.
- Add a focused `ReturnGui` test or extract a small scheduling boundary that proves
  GUI mutation is dispatched through the primary-thread scheduler.
- Run focused tests after each red/green cycle, then the complete Gradle test suite
  and `shadowJar`.

## Out of scope

- Changing return fees, windows, item-equivalence rules, or GUI copy.
- Introducing an intermediate `RETURNING` parcel status.
- Solving cross-request locker-capacity reservation beyond the existing best-effort
  fullness model.
- Refactoring unrelated send/collection persistence paths.
