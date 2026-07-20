# ParcelLockers API Module Design

**Issue:** [#176 — Add API module](https://github.com/EternalCodeTeam/ParcelLockers/issues/176)

**Objective:** Split ParcelLockers into a published API module and an implementation plugin module so other Paper plugins can listen to ParcelLockers events and manage parcels and lockers through stable service contracts.

## Scope

The first API release exposes parcel and locker operations only. It does not expose users, parcel contents, deliveries, repositories, database types, GUI types, configuration, Discord integration, Vault integration, commands, caches, or administrative bulk operations.

External plugins may perform reads and controlled mutations through services. Every mutation continues to pass through the plugin's validation, persistence, economy, scheduling, and event behavior. No database or repository escape hatch is added.

## Project Structure

The Gradle build becomes a two-module build following the EternalCombat layout:

- `parcellockers-api` is a `java-library` containing the public contracts, public models, events, public exceptions, API facade, and provider.
- `parcellockers-plugin` contains the Paper plugin entry point and every implementation detail. It depends on `implementation(project(":parcellockers-api"))`.
- The root project aggregates shared group, version, repositories, Java 21 settings, and module tasks. It contains no production source set.

The existing package prefix `com.eternalcode.parcellockers` remains unchanged. This avoids unnecessary source incompatibility and lets the plugin entry point call package-private provider lifecycle methods.

The plugin shadow JAR includes the API classes and remains the deployable Paper artifact. The shadow artifact continues to be written under the root `build/libs` directory so the existing `./gradlew shadowJar` workflow remains valid.

## API Module Contents

### Facade and provider

`ParcelLockersApi` exposes:

```java
ParcelService getParcelService();
LockerService getLockerService();
```

`ParcelLockersProvider.provide()` returns the initialized facade. The provider stores the facade in a `volatile` field and rejects invalid lifecycle transitions:

- `provide()` before initialization or after deinitialization throws `IllegalStateException`.
- a second `initialize(...)` call throws `IllegalStateException`.
- `deinitialize()` without an initialized API throws `IllegalStateException`.
- `initialize(...)` and `deinitialize()` remain package-private so external consumers cannot replace the active API.

The main `ParcelLockers` plugin class implements `ParcelLockersApi`. It initializes the provider only after both public services are ready. During `onDisable()`, it deinitializes the provider before unregistering components and closing the datasource.

### Parcel service

The public `ParcelService` supports:

- sending and collecting parcels with the existing `Player` and `ItemStack` inputs;
- updating a parcel, including the conditional status update used for concurrency safety;
- deleting a parcel by object or UUID;
- retrieving a parcel by UUID;
- paginated retrieval by sender, receiver, collectible destination, return eligibility, or all parcels.

Implementation-only methods are removed from the public contract: dispatch rollback, cache invalidation, sender-facing administrative deletion, and bulk deletion with `NoticeService`. The plugin module may define an internal extension contract for these operations where an implementation-facing type is required.

### Locker service

A new `LockerService` interface supports:

- lookup by UUID or `Position`;
- paginated listing;
- creation, deletion, and rename operations;
- locker capacity checks.

`LockerManager` implements `LockerService`. Cache-only lookup and administrative bulk deletion remain implementation details on `LockerManager` and are not part of the published contract.

### Models, events, and exceptions

The API module owns the types required by those contracts:

- parcel types: `Parcel`, `ParcelStatus`, and `ParcelSize`;
- locker types: `Locker` and `Position`;
- pagination types: `Page` and `PageResult`;
- parcel events: `ParcelSendEvent`, `ParcelDeliverEvent`, `ParcelCollectEvent`, and `ParcelReturnEvent`;
- locker events: `LockerCreateEvent` and `LockerDeleteEvent`;
- event base: `CancellableEvent`;
- public exceptions: `ParcelLockersException`, `ParcelOperationException`, and `ValidationException`.

`DatabaseException` remains in the plugin module because persistence is not public API. Locker validation stops exposing Okaeri's `ValidationException` and completes operations exceptionally with the ParcelLockers API `ValidationException` instead.

## Dependencies and Nullness

The API module has only these external compile contracts:

- `io.papermc.paper:paper-api` through `compileOnlyApi`, because service signatures and events use Paper types;
- `org.jspecify:jspecify:1.0.0` through `api`.

Public API packages are marked with JSpecify `@NullMarked` package annotations. Intentionally optional values are marked explicitly with `@Nullable`; this includes the optional destination-locker filter used by collectible-parcel queries. JetBrains Annotations is not an API dependency.

The plugin module keeps all current runtime and integration dependencies. It may retain implementation-only annotations where needed, but public signatures use JSpecify.

## Behavior and Threading

All service operations retain their current `CompletableFuture` model. The module split does not introduce blocking database calls or change the established result types.

Existing event behavior is preserved:

- currently asynchronous parcel and locker events stay asynchronous;
- synchronous events stay synchronous;
- `LockerDeleteEvent` continues to be scheduled onto the main server thread before deletion;
- cancelling an event preserves the current operation outcome.

Validation and operational failures complete returned futures exceptionally with public ParcelLockers exceptions where callers need to distinguish them. Low-level database failures remain implementation details and may be wrapped by the public exception hierarchy rather than exposing ORMLite, HikariCP, or Okaeri types.

## Maven Publication

The API is published with these coordinates:

```text
com.eternalcode:parcellockers-api:<project-version>
```

`parcellockers-api` applies `java-library` and `maven-publish`, and publishes its Java component plus source and Javadoc JARs. Repositories are:

- `mavenLocal()` for local consumer testing;
- `https://repo.eternalcode.pl/releases` for releases.

Remote credentials come only from `ETERNAL_CODE_MAVEN_USERNAME` and `ETERNAL_CODE_MAVEN_PASSWORD`, matching EternalCombat. Publication configuration must not fail local builds when those environment variables are absent; credentials are required only when the remote publish task executes.

## Source Migration

All current production sources and resources first move under `parcellockers-plugin`. The facade, provider, public service interfaces, public models, events, shared pagination/position types, event base, public exceptions, and their package nullness declarations then live under `parcellockers-api`.

All existing tests move under `parcellockers-plugin` unless they test a type owned by the API module. Package names remain stable. No unrelated refactor or API expansion is part of the migration.

## Verification

Provider unit tests cover:

1. `provide()` before initialization;
2. successful initialization and identity of the returned facade;
3. rejection of duplicate initialization;
4. successful deinitialization;
5. `provide()` and repeated deinitialization after shutdown.

The implementation is verified with:

```powershell
.\gradlew.bat :parcellockers-api:test
.\gradlew.bat :parcellockers-plugin:test
.\gradlew.bat test
.\gradlew.bat :parcellockers-api:publishToMavenLocal
.\gradlew.bat :parcellockers-plugin:shadowJar
```

Artifact inspection must confirm that:

- the API JAR contains facade, provider, services, models, events, and public exceptions;
- the API JAR contains no repository implementations, GUI, configuration, database, Discord, or command classes;
- the published POM exposes Paper and JSpecify as consumer compile contracts;
- the deployable shadow JAR contains the API classes and the Paper plugin implementation;
- the existing test suite remains green after its module move.

## Out of Scope

- compatibility shims for unpublished internal classes;
- a user, content, delivery, Discord, GUI, repository, or database API;
- Bukkit `ServicesManager` registration in addition to the static provider;
- changing parcel or locker business rules;
- publishing the plugin module as a library;
- pushing the branch or opening a pull request.
