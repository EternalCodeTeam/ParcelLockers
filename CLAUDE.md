# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run Commands

```bash
# Build the plugin JAR (output: build/libs/ParcelLockers v<version>.jar)
./gradlew shadowJar

# Run tests
./gradlew test

# Run a single test class
./gradlew test --tests "com.eternalcode.parcellockers.database.ParcelRepositoryIntegrationTest"

# Start a local Paper server with the plugin loaded (downloads Paper + dependencies automatically)
./gradlew runServer
```

Requires JDK 21+. The `runServer` task uses JetBrains JVM and auto-downloads LuckPerms, VaultUnlocked, and EssentialsX. Uncomment the DiscordSRV line in `build.gradle.kts` to test that integration locally.

## Architecture Overview

ParcelLockers is a Paper plugin (Minecraft 1.21) that lets players transfer items between parcel locker blocks across the world, with optional Discord notifications.

### Entry Point & Wiring

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
- **Nexo** — when present, custom item blocks can be used as locker blocks (`NexoIntegration.placeBlock`); guarded by `isPluginEnabled("Nexo")` checks
- **Vault** — required; used to charge players an economy fee when sending parcels

### Testing

Tests live in `src/test/java/`. Integration tests (e.g. `LockerRepositoryIntegrationTest`) extend `IntegrationTestSpec` and use Testcontainers (MySQL) to test repository implementations against a real database. `ParcelPageTest` is a unit test with no container dependency.

## graphify

This project has a knowledge graph at graphify-out/ with god nodes, community structure, and cross-file relationships.

Rules:
- For codebase questions, first run `graphify query "<question>"` when graphify-out/graph.json exists. Use `graphify path "<A>" "<B>"` for relationships and `graphify explain "<concept>"` for focused concepts. These return a scoped subgraph, usually much smaller than GRAPH_REPORT.md or raw grep output.
- If graphify-out/wiki/index.md exists, use it for broad navigation instead of raw source browsing.
- Read graphify-out/GRAPH_REPORT.md only for broad architecture review or when query/path/explain do not surface enough context.
- After modifying code, run `graphify update .` to keep the graph current (AST-only, no API cost).
