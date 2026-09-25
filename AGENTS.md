# AGENTS.md

This file provides guidance to AI agents when working with code in this repository.

## Philosophy and Code Style

- You can communicate all of your views and report submit objections to the user's way of thinking.
- Use 'this' wherever possible (this.field, this.method(), etc.).
- Use SOLID, KISS, Clean Code principles, DRY (Don't Repeat Yourself), no copy-paste coding, Defensive copying for safety
- Prefer meaningful variable names
- Use fail-fast approach, validate early, throw meaningful exceptions 
- Create immutable objects where possible, use builder pattern for complex objects, no magic numbers/strings - prefer constants
- If asked by user for a simple task/edit - do not invoke unnecessary skills such as TDD or planning.

## Build & Run Commands

```bash
# Build the plugin JAR (output: build/libs/ParcelLockers v<version>.jar)
./gradlew shadowJar

# Run tests
./gradlew test

# Run a single test class
./gradlew test --tests "com.eternalcode.parcellockers.database.ParcelRepositoryIntegrationTest"

# Start a local Paper server with the plugin loaded (downloads Paper + dependencies automatically)
./gradlew runServer --console=plain
```

Requires JDK 21+. The `runServer` task uses a Java 25 toolchain and auto-downloads LuckPerms, VaultUnlocked, and EssentialsX. Uncomment the DiscordSRV line in `build.gradle.kts` to test that integration locally.

## Architecture Overview

ParcelLockers is a Paper plugin (Minecraft 1.21+) that lets players transfer items between parcel locker blocks across the world, with optional Discord notifications.

### Entry Point & Wiring

The project consists of two Gradle modules: `parcellockers-api`, which contains the API the plugin depends on. Other plugins' developers can hook into this API and integrate with our plugin.
API Implementation and the code of the plugin itself is in the `parcellockers-plugin` directory.

`ParcelLockers.java` (`onEnable`) is the manual DI root — all components are instantiated and wired there in order: config → database → repositories → managers/services → GUIs → commands → event controllers. There is no DI framework; dependencies are passed via constructors.

### Domain Layers

Each domain (`locker`, `parcel`, `content`, `delivery`, `itemstorage`, `user`, `discord`) follows a consistent layered structure:

- **Model** — plain record/class (e.g. `Locker`, `Parcel`, `User`)
- **Repository** — interface + `*OrmLite` implementation backed by H2/PostgreSQL via ORMLite
- **Manager/Service** — business logic, coordinates repository calls; repositories return `CompletableFuture<T>` for all async DB operations
- **Controller** — Bukkit `Listener` handling in-game events (block place/break/interact, player join/quit)

### Key Components

| Component | Purpose |
|---|---|
| `DatabaseManager` | Manages HikariCP connection pool; supports H2 (default, embedded) and PostgreSQL |
| `ConfigService` + okaeri-configs | Loads `config.yml` and `messages.yml` via YAML; `PluginConfig` and `MessageConfig` are the config POJOs |
| `NoticeService` + multification | Sends MiniMessage-formatted notices to players; all user-facing text goes through `MessageConfig` |
| `ParcelDispatchService` | Orchestrates sending a parcel: validates, charges economy (Vault), schedules `ParcelSendTask` |
| `ParcelSendTask` | Runs async after a configurable delay; marks parcel DELIVERED and fires the deliver notification event |
| `GuiManager` + triumph-gui | Factory for all inventory GUIs; `LockerGui` and `MainGui` are the two root GUI entry points |
| `DiscordProviderPicker` | Selects between Discord4J (standalone bot) and DiscordSRV (delegation) at startup based on detected plugins |
| `LockerPlaceController` | Uses Paper's Dialog API (unstable) to prompt for a locker description when a player places the locker item |

### Optional Integrations

- **DiscordSRV** — when present, account linking and DM notifications are delegated to it; otherwise, the plugin manages its own Discord bot via Discord4J
- **Vault** — required; used to charge players an economy fee when sending parcels

### Testing

Tests live in `parcellockers-plugin/src/test/java/` and `parcellockers-api/src/test/java/`. Integration tests (e.g. `LockerRepositoryIntegrationTest`) extend `IntegrationTestSpec` and use Testcontainers (MySQL) to test repository implementations against a real database. `ParcelPageTest` is a unit test with no container dependency.
