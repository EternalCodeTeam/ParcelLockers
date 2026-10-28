# ParcelLockers API Module Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Split ParcelLockers into a published API module and a deployable Paper plugin module, exposing stable parcel and locker services through a lifecycle-safe provider.

**Architecture:** `parcellockers-api` owns the facade, provider, service contracts, models, events, shared API values, and public exceptions. `parcellockers-plugin` depends on that module and owns every implementation, repository, integration, command, and GUI; its main class implements the API facade and initializes the provider only after startup succeeds.

**Tech Stack:** Java 21, Gradle Kotlin DSL, Paper API 1.21.11, JSpecify 1.0.0, JUnit 6, Mockito 5, Gradle Maven Publish, Shadow 9.6.

## Global Constraints

- Work only on branch `codex/issue-176-api-module` in `D:\Projekty\ParcelLockers\.worktrees\issue-176-api-module`.
- Keep the package prefix `com.eternalcode.parcellockers` unchanged.
- Keep Java source and target compatibility at Java 21.
- Use `parcellockers-api` and `parcellockers-plugin` as the two module names.
- Keep the deployable shadow JAR under root `build/libs` and preserve `./gradlew shadowJar`.
- The API may depend only on Paper through `compileOnlyApi` and JSpecify `1.0.0` through `api`, plus test-only dependencies.
- Expose parcel and locker operations only; do not expose users, content, delivery, repositories, database, configuration, GUI, commands, Vault, or Discord.
- Keep all service operations asynchronous through their existing `CompletableFuture` results.
- Preserve existing event cancellation and thread semantics.
- Publish only `com.eternalcode:parcellockers-api:0.5.0-BETA`, with sources and Javadoc JARs.
- Use `ETERNAL_CODE_MAVEN_USERNAME` and `ETERNAL_CODE_MAVEN_PASSWORD` only for the remote EternalCode repository.
- Apply TDD to every new behavior: write the test, observe the expected failure, implement the minimum, and rerun the focused and surrounding suites.

---

## File Structure

### Root build

- Modify `settings.gradle.kts`: include `parcellockers-api` and `parcellockers-plugin`.
- Replace `build.gradle.kts`: keep shared plugin versions, coordinates, repositories, Java 21 compilation, and JUnit configuration only.
- Modify `buildSrc/src/main/kotlin/Versions.kt`: add `JSPECIFY = "1.0.0"`.

### API module

- Create `parcellockers-api/build.gradle.kts`: Java library dependencies and Maven publication.
- Create `parcellockers-api/src/main/java/com/eternalcode/parcellockers/ParcelLockersApi.java`: facade for the two public services.
- Create `parcellockers-api/src/main/java/com/eternalcode/parcellockers/ParcelLockersProvider.java`: guarded static lifecycle.
- Create `parcellockers-api/src/main/java/com/eternalcode/parcellockers/locker/LockerService.java`: public locker contract.
- Move parcel/locker models, parcel service contract, events, pagination, position, event base, and public exceptions from the plugin source tree into the matching API packages.
- Create `package-info.java` for each public API package to apply JSpecify `@NullMarked`.
- Create `parcellockers-api/src/test/java/com/eternalcode/parcellockers/ApiSurfaceTest.java`: guards public service method names.
- Create `parcellockers-api/src/test/java/com/eternalcode/parcellockers/ParcelLockersProviderTest.java`: guards provider lifecycle.

### Plugin module

- Move the current root `src` tree to `parcellockers-plugin/src` and the current plugin build to `parcellockers-plugin/build.gradle.kts`.
- Create `parcellockers-plugin/src/main/java/com/eternalcode/parcellockers/parcel/service/PluginParcelService.java`: internal extension for rollback, invalidation, and administrative operations.
- Modify `ParcelServiceImpl`, `ParcelDispatchService`, `ParcelReturnService`, `GuiManager`, `DebugCommand`, and `ParcelLockers` to use the correct public or internal service type.
- Modify `LockerManager` to implement `LockerService` and use the API `ValidationException`.
- Create `parcellockers-plugin/src/test/java/com/eternalcode/parcellockers/locker/LockerManagerTest.java`: proves the public validation exception.
- Modify `ParcelReturnServiceTest` to mock `PluginParcelService` because it exercises cache invalidation.

---

### Task 1: Convert the root build into two modules without changing runtime behavior

**Files:**
- Modify: `settings.gradle.kts`
- Replace: `build.gradle.kts`
- Move: `src` to `parcellockers-plugin/src`
- Move: existing `build.gradle.kts` to `parcellockers-plugin/build.gradle.kts` before creating the new root build
- Create: `parcellockers-api/build.gradle.kts`
- Modify: `parcellockers-plugin/build.gradle.kts`
- Modify: `buildSrc/src/main/kotlin/Versions.kt`

**Interfaces:**
- Consumes: the existing single-project Gradle build and unchanged source tree.
- Produces: Gradle projects `:parcellockers-api` and `:parcellockers-plugin`; the plugin project still builds and tests all existing code.

- [ ] **Step 1: Record the pre-move baseline**

Run:

```powershell
.\gradlew.bat test
```

Expected: `BUILD SUCCESSFUL`. This was true before planning; rerun it immediately before source movement so the execution log contains fresh evidence.

- [ ] **Step 2: Move the existing build and source tree mechanically**

Run:

```powershell
New-Item -ItemType Directory -Force -Path parcellockers-plugin | Out-Null
git mv build.gradle.kts parcellockers-plugin/build.gradle.kts
git mv src parcellockers-plugin/src
New-Item -ItemType Directory -Force -Path parcellockers-api | Out-Null
```

Expected: Git records renames; no Java package declarations change.

- [ ] **Step 3: Replace `settings.gradle.kts` with the two-project definition**

```kotlin
rootProject.name = "parcellockers"

include("parcellockers-api")
include("parcellockers-plugin")

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
```

- [ ] **Step 4: Create the shared root `build.gradle.kts`**

```kotlin
plugins {
    id("de.eldoria.plugin-yml.paper") version "0.9.0" apply false
    id("xyz.jpenilla.run-paper") version "3.0.2" apply false
    id("com.gradleup.shadow") version "9.6.0" apply false
    id("com.modrinth.minotaur") version "2.+" apply false
}

allprojects {
    group = "com.eternalcode"
    version = "0.5.0-BETA"

    repositories {
        maven("https://maven-central.storage-download.googleapis.com/maven2/")
        maven("https://repo.triumphteam.dev/snapshots/")
        maven("https://jitpack.io")
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://repo.eternalcode.pl/releases")
        maven("https://storehouse.okaeri.eu/repository/maven-public/")
        maven("https://nexus.scarsz.me/content/groups/public/")
    }
}

subprojects {
    apply(plugin = "java")

    extensions.configure<JavaPluginExtension> {
        toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.isIncremental = true
        options.compilerArgs.add("-parameters")
        options.release = 21
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }
}
```

- [ ] **Step 5: Create the initial API build**

Create `parcellockers-api/build.gradle.kts`:

```kotlin
plugins {
    `java-library`
}

dependencies {
    compileOnlyApi("io.papermc.paper:paper-api:${Versions.PAPER_API}")
    api("org.jspecify:jspecify:${Versions.JSPECIFY}")

    testImplementation("org.junit.jupiter:junit-jupiter-api:${Versions.JUNIT}")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${Versions.JUNIT}")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
```

Add to `buildSrc/src/main/kotlin/Versions.kt` next to the annotation versions:

```kotlin
const val JSPECIFY = "1.0.0"
```

- [ ] **Step 6: Convert the moved plugin build to a subproject build**

In `parcellockers-plugin/build.gradle.kts`:

1. Remove `id("java")`; the root applies Java to every subproject.
2. Remove versions from the four external plugin declarations so the block is exactly:

```kotlin
plugins {
    id("de.eldoria.plugin-yml.paper")
    id("xyz.jpenilla.run-paper")
    id("com.gradleup.shadow")
    id("com.modrinth.minotaur")
}
```

3. Remove the local `group`, `version`, `repositories`, `java`, `tasks.withType<JavaCompile>`, and `tasks.test` declarations now supplied by the root.
4. Add the API project as the first dependency:

```kotlin
dependencies {
    implementation(project(":parcellockers-api"))

    // existing dependencies remain unchanged below
}
```

5. Preserve the current root README lookup in the Modrinth block.
6. Add the root artifact destination to `shadowJar` while preserving the current archive name, exclusions, service merge, and relocation:

```kotlin
shadowJar {
    archiveFileName.set("ParcelLockers v${project.version}.jar")
    destinationDirectory.set(rootProject.layout.buildDirectory.dir("libs"))

    exclude(
        "org/intellij/lang/annotations/**",
        "org/jetbrains/annotations/**",
        "META-INF/**"
    )

    mergeServiceFiles()

    val relocationPrefix = "com.eternalcode.parcellockers.libs"
    listOf("org.bstats").forEach { relocate(it, "$relocationPrefix.$it") }
}
```

- [ ] **Step 7: Verify the mechanical split**

Run:

```powershell
.\gradlew.bat projects
.\gradlew.bat :parcellockers-api:compileJava :parcellockers-plugin:test
```

Expected: both modules are listed; API has `NO-SOURCE`; every pre-existing plugin test passes.

- [ ] **Step 8: Commit the module skeleton**

```powershell
git add settings.gradle.kts build.gradle.kts buildSrc/src/main/kotlin/Versions.kt parcellockers-api parcellockers-plugin
git commit -m "build: split API and plugin modules"
```

---

### Task 2: Extract the public API surface and isolate plugin-only parcel operations

**Files:**
- Move into API: `parcel/Parcel.java`, `parcel/ParcelSize.java`, `parcel/ParcelStatus.java`, `parcel/event/*.java`, `parcel/service/ParcelService.java`
- Move into API: `locker/Locker.java`, `locker/event/*.java`
- Move into API: `shared/Page.java`, `shared/PageResult.java`, `shared/Position.java`, `shared/event/CancellableEvent.java`
- Move into API: `shared/exception/ParcelLockersException.java`, `ParcelOperationException.java`, `ValidationException.java`
- Create: `parcellockers-api/src/main/java/com/eternalcode/parcellockers/locker/LockerService.java`
- Create: API `package-info.java` files
- Create: `parcellockers-api/src/test/java/com/eternalcode/parcellockers/ApiSurfaceTest.java`
- Create: `parcellockers-plugin/src/main/java/com/eternalcode/parcellockers/parcel/service/PluginParcelService.java`
- Modify: `ParcelServiceImpl.java`, `ParcelDispatchService.java`, `ParcelReturnService.java`, `GuiManager.java`, `DebugCommand.java`, `ParcelLockers.java`
- Modify: `parcellockers-plugin/src/test/java/com/eternalcode/parcellockers/returns/ParcelReturnServiceTest.java`

**Interfaces:**
- Consumes: module dependency `parcellockers-plugin -> parcellockers-api` from Task 1.
- Produces: `ParcelService` with only consumer-safe operations; `LockerService`; internal `PluginParcelService extends ParcelService` for rollback, invalidation, and administrative calls.

- [ ] **Step 1: Write a failing API-surface test**

Create `parcellockers-api/src/test/java/com/eternalcode/parcellockers/ApiSurfaceTest.java`:

```java
package com.eternalcode.parcellockers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.eternalcode.parcellockers.locker.LockerService;
import com.eternalcode.parcellockers.parcel.service.ParcelService;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class ApiSurfaceTest {

    @Test
    void exposesOnlySupportedParcelOperations() {
        Set<String> methods = methodNames(ParcelService.class);

        assertEquals(Set.of(
            "send", "update", "updateIfStatus", "collect", "get",
            "getBySender", "getByReceiver", "getCollectible", "getReturnable",
            "getAll", "delete"
        ), methods);
        assertFalse(methods.contains("rollbackSend"));
        assertFalse(methods.contains("invalidate"));
        assertFalse(methods.contains("deleteAll"));
    }

    @Test
    void exposesOnlySupportedLockerOperations() {
        assertEquals(Set.of("get", "create", "delete", "rename", "isLockerFull"),
            methodNames(LockerService.class));
    }

    private static Set<String> methodNames(Class<?> type) {
        return Arrays.stream(type.getDeclaredMethods())
            .map(Method::getName)
            .collect(Collectors.toSet());
    }
}
```

- [ ] **Step 2: Run the test and observe the missing API**

Run:

```powershell
.\gradlew.bat :parcellockers-api:test --tests "com.eternalcode.parcellockers.ApiSurfaceTest"
```

Expected: compilation fails because `ParcelService` and `LockerService` do not yet exist in the API module.

- [ ] **Step 3: Move the required existing types into the API source tree**

Create matching destination directories, then use `git mv` for the exact files listed in this task. Move the complete `parcel/event` and `locker/event` directories; move only the three public exception files so `DatabaseException.java` remains in the plugin module.

After the move, remove `org.jetbrains.annotations.NotNull` imports and return annotations from the six moved event classes. Their `getHandlers()` methods become:

```java
@Override
public HandlerList getHandlers() {
    return HANDLER_LIST;
}
```

In `Position.java`, replace the Javadoc reference to plugin-only `PositionAdapter` with:

```java
/**
 * Immutable coordinates suitable for storage without retaining a Bukkit world instance.
 */
```

- [ ] **Step 4: Replace `ParcelService` with the consumer-safe contract**

```java
package com.eternalcode.parcellockers.parcel.service;

import com.eternalcode.parcellockers.parcel.Parcel;
import com.eternalcode.parcellockers.parcel.ParcelStatus;
import com.eternalcode.parcellockers.shared.Page;
import com.eternalcode.parcellockers.shared.PageResult;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.Nullable;

public interface ParcelService {

    CompletableFuture<Boolean> send(Player sender, Parcel parcel, List<ItemStack> items);

    CompletableFuture<Void> update(Parcel parcel);

    CompletableFuture<Boolean> updateIfStatus(Parcel updated, ParcelStatus expectedStatus);

    CompletableFuture<Void> collect(Player player, Parcel parcel);

    CompletableFuture<Optional<Parcel>> get(UUID uuid);

    CompletableFuture<PageResult<Parcel>> getBySender(UUID sender, Page page);

    CompletableFuture<PageResult<Parcel>> getByReceiver(UUID receiver, Page page);

    CompletableFuture<PageResult<Parcel>> getCollectible(
        UUID receiver,
        @Nullable UUID destinationLocker,
        Page page
    );

    CompletableFuture<PageResult<Parcel>> getReturnable(UUID receiver, Page page);

    CompletableFuture<PageResult<Parcel>> getAll(Page page);

    CompletableFuture<Boolean> delete(UUID uuid);

    CompletableFuture<Boolean> delete(Parcel parcel);
}
```

- [ ] **Step 5: Add the public locker contract**

Create `parcellockers-api/src/main/java/com/eternalcode/parcellockers/locker/LockerService.java`:

```java
package com.eternalcode.parcellockers.locker;

import com.eternalcode.parcellockers.shared.Page;
import com.eternalcode.parcellockers.shared.PageResult;
import com.eternalcode.parcellockers.shared.Position;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface LockerService {

    CompletableFuture<Optional<Locker>> get(UUID uniqueId);

    CompletableFuture<Optional<Locker>> get(Position position);

    CompletableFuture<PageResult<Locker>> get(Page page);

    CompletableFuture<Locker> create(UUID uniqueId, String name, Position position, UUID playerUuid);

    CompletableFuture<Void> delete(UUID uniqueId, UUID playerUuid);

    CompletableFuture<Locker> rename(UUID uniqueId, String newName);

    CompletableFuture<Boolean> isLockerFull(UUID uniqueId);
}
```

- [ ] **Step 6: Add JSpecify package defaults**

Create this form of `package-info.java` in each listed API package: root, `parcel`, `parcel.event`, `parcel.service`, `locker`, `locker.event`, `shared`, `shared.event`, and `shared.exception`.

Example for `parcel.service`:

```java
@NullMarked
package com.eternalcode.parcellockers.parcel.service;

import org.jspecify.annotations.NullMarked;
```

Use the matching package declaration in each other file. Do not annotate plugin-only packages.

- [ ] **Step 7: Add the internal parcel extension and update implementation consumers**

Create `parcellockers-plugin/src/main/java/com/eternalcode/parcellockers/parcel/service/PluginParcelService.java`:

```java
package com.eternalcode.parcellockers.parcel.service;

import com.eternalcode.parcellockers.notification.NoticeService;
import com.eternalcode.parcellockers.parcel.Parcel;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public interface PluginParcelService extends ParcelService {

    CompletableFuture<Void> rollbackSend(Player sender, Parcel parcel);

    void invalidate(UUID uuid);

    CompletableFuture<Void> delete(CommandSender sender, Parcel parcel);

    CompletableFuture<Void> deleteAll(CommandSender sender, NoticeService noticeService);
}
```

Make these exact type changes:

- `ParcelServiceImpl implements PluginParcelService`.
- `ParcelDispatchService` field and constructor parameter: `PluginParcelService`.
- `ParcelReturnService` field and constructor parameter: `PluginParcelService`.
- `GuiManager` field and constructor parameter: `PluginParcelService`.
- `DebugCommand` field and constructor parameter: `PluginParcelService`.
- `ParcelLockers.onEnable()` local implementation variable: `PluginParcelService parcelService = new ParcelServiceImpl(...)`.
- `ParcelReturnServiceTest` fixture mock: `PluginParcelService`, with the corresponding import.

Leave `AdminParcelService`, `ParcelSendTask`, `ReturnWindowPurgeTask`, and read-only GUI consumers typed as `ParcelService`; they use only the public contract.

- [ ] **Step 8: Run focused and module tests**

Run:

```powershell
.\gradlew.bat :parcellockers-api:test --tests "com.eternalcode.parcellockers.ApiSurfaceTest"
.\gradlew.bat :parcellockers-plugin:test
```

Expected: `ApiSurfaceTest` passes with the exact method-name sets; all moved plugin tests pass.

- [ ] **Step 9: Commit the API surface extraction**

```powershell
git add parcellockers-api parcellockers-plugin
git commit -m "feat: extract parcel and locker API contracts"
```

---

### Task 3: Make locker validation part of the public API contract

**Files:**
- Create: `parcellockers-plugin/src/test/java/com/eternalcode/parcellockers/locker/LockerManagerTest.java`
- Modify: `parcellockers-plugin/src/main/java/com/eternalcode/parcellockers/locker/LockerManager.java`

**Interfaces:**
- Consumes: `LockerService` and API `ValidationException` from Task 2.
- Produces: `LockerManager implements LockerService`; invalid create requests fail with `com.eternalcode.parcellockers.shared.exception.ValidationException`.

- [ ] **Step 1: Write the failing validation test**

```java
package com.eternalcode.parcellockers.locker;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.eternalcode.commons.scheduler.Scheduler;
import com.eternalcode.parcellockers.locker.repository.LockerRepository;
import com.eternalcode.parcellockers.locker.validation.LockerValidationService;
import com.eternalcode.parcellockers.parcel.repository.ParcelRepository;
import com.eternalcode.parcellockers.shared.Position;
import com.eternalcode.parcellockers.shared.exception.ValidationException;
import com.eternalcode.parcellockers.shared.validation.ValidationResult;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import org.bukkit.Server;
import org.junit.jupiter.api.Test;

class LockerManagerTest {

    @Test
    void exposesApiValidationExceptionForInvalidCreateRequest() {
        LockerValidationService validation = mock(LockerValidationService.class);
        UUID lockerId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        Position position = new Position(1, 2, 3, "world");
        when(validation.validateCreateParameters(lockerId, "locker", position))
            .thenReturn(ValidationResult.invalid("invalid locker"));

        LockerManager manager = new LockerManager(
            null,
            mock(LockerRepository.class),
            validation,
            mock(ParcelRepository.class),
            mock(Server.class),
            mock(Scheduler.class)
        );

        CompletionException exception = assertThrows(CompletionException.class,
            () -> manager.create(lockerId, "locker", position, playerId).join());

        assertInstanceOf(ValidationException.class, exception.getCause());
    }
}
```

- [ ] **Step 2: Run the test and verify the exception mismatch**

Run:

```powershell
.\gradlew.bat :parcellockers-plugin:test --tests "com.eternalcode.parcellockers.locker.LockerManagerTest"
```

Expected: the assertion fails because the cause is `eu.okaeri.configs.exception.ValidationException`.

- [ ] **Step 3: Implement the public locker contract**

In `LockerManager.java`:

```java
import com.eternalcode.parcellockers.shared.exception.ValidationException;

public class LockerManager implements LockerService {
```

Remove `import eu.okaeri.configs.exception.ValidationException;`. Keep the three existing `new ValidationException(...)` call sites unchanged so they now construct the API exception. Add `@Override` to every method declared by `LockerService`: all three `get` overloads, `create`, `delete`, `rename`, and `isLockerFull`.

- [ ] **Step 4: Run the focused and API tests**

Run:

```powershell
.\gradlew.bat :parcellockers-plugin:test --tests "com.eternalcode.parcellockers.locker.LockerManagerTest"
.\gradlew.bat :parcellockers-api:test :parcellockers-plugin:test
```

Expected: the focused test sees the API `ValidationException`; both module suites pass.

- [ ] **Step 5: Commit the locker implementation**

```powershell
git add parcellockers-plugin/src/main/java/com/eternalcode/parcellockers/locker/LockerManager.java parcellockers-plugin/src/test/java/com/eternalcode/parcellockers/locker/LockerManagerTest.java
git commit -m "feat: expose locker service implementation"
```

---

### Task 4: Add the API provider and wire it to the Paper plugin lifecycle

**Files:**
- Create: `parcellockers-api/src/main/java/com/eternalcode/parcellockers/ParcelLockersApi.java`
- Create: `parcellockers-api/src/main/java/com/eternalcode/parcellockers/ParcelLockersProvider.java`
- Create: `parcellockers-api/src/test/java/com/eternalcode/parcellockers/ParcelLockersProviderTest.java`
- Modify: `parcellockers-plugin/src/main/java/com/eternalcode/parcellockers/ParcelLockers.java`

**Interfaces:**
- Consumes: `ParcelService`, `PluginParcelService`, `LockerService`, and `LockerManager` from Tasks 2–3.
- Produces: `ParcelLockersProvider.provide()` and facade getters `getParcelService()` / `getLockerService()`.

- [ ] **Step 1: Write failing provider lifecycle tests**

```java
package com.eternalcode.parcellockers;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.eternalcode.parcellockers.locker.LockerService;
import com.eternalcode.parcellockers.parcel.service.ParcelService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class ParcelLockersProviderTest {

    @AfterEach
    void resetProvider() {
        try {
            ParcelLockersProvider.deinitialize();
        } catch (IllegalStateException ignored) {
            // The test already left the provider empty.
        }
    }

    @Test
    void rejectsAccessBeforeInitialization() {
        assertThrows(IllegalStateException.class, ParcelLockersProvider::provide);
    }

    @Test
    void returnsInitializedApi() {
        ParcelLockersApi api = stubApi();

        ParcelLockersProvider.initialize(api);

        assertSame(api, ParcelLockersProvider.provide());
    }

    @Test
    void rejectsDuplicateInitialization() {
        ParcelLockersProvider.initialize(stubApi());

        assertThrows(IllegalStateException.class,
            () -> ParcelLockersProvider.initialize(stubApi()));
    }

    @Test
    void rejectsAccessAfterDeinitialization() {
        ParcelLockersProvider.initialize(stubApi());

        ParcelLockersProvider.deinitialize();

        assertThrows(IllegalStateException.class, ParcelLockersProvider::provide);
        assertThrows(IllegalStateException.class, ParcelLockersProvider::deinitialize);
    }

    private static ParcelLockersApi stubApi() {
        return new ParcelLockersApi() {
            @Override
            public ParcelService getParcelService() {
                throw new UnsupportedOperationException();
            }

            @Override
            public LockerService getLockerService() {
                throw new UnsupportedOperationException();
            }
        };
    }
}
```

- [ ] **Step 2: Run the provider test and observe the missing classes**

Run:

```powershell
.\gradlew.bat :parcellockers-api:test --tests "com.eternalcode.parcellockers.ParcelLockersProviderTest"
```

Expected: test compilation fails because `ParcelLockersApi` and `ParcelLockersProvider` do not exist.

- [ ] **Step 3: Add the facade**

Create `ParcelLockersApi.java`:

```java
package com.eternalcode.parcellockers;

import com.eternalcode.parcellockers.locker.LockerService;
import com.eternalcode.parcellockers.parcel.service.ParcelService;

public interface ParcelLockersApi {

    ParcelService getParcelService();

    LockerService getLockerService();
}
```

- [ ] **Step 4: Add the guarded provider**

Create `ParcelLockersProvider.java`:

```java
package com.eternalcode.parcellockers;

import java.util.Objects;

public final class ParcelLockersProvider {

    private static volatile ParcelLockersApi api;

    private ParcelLockersProvider() {
    }

    public static ParcelLockersApi provide() {
        ParcelLockersApi current = api;
        if (current == null) {
            throw new IllegalStateException("ParcelLockersApi has not been initialized");
        }
        return current;
    }

    static synchronized void initialize(ParcelLockersApi parcelLockersApi) {
        Objects.requireNonNull(parcelLockersApi, "parcelLockersApi");
        if (api != null) {
            throw new IllegalStateException("ParcelLockersApi has already been initialized");
        }
        api = parcelLockersApi;
    }

    static synchronized void deinitialize() {
        if (api == null) {
            throw new IllegalStateException("ParcelLockersApi has not been initialized");
        }
        api = null;
    }
}
```

- [ ] **Step 5: Run the provider tests green**

Run:

```powershell
.\gradlew.bat :parcellockers-api:test --tests "com.eternalcode.parcellockers.ParcelLockersProviderTest"
```

Expected: all four tests pass.

- [ ] **Step 6: Wire the provider into `ParcelLockers`**

Change the class declaration and add fields:

```java
public final class ParcelLockers extends JavaPlugin implements ParcelLockersApi {

    private LiteCommands<CommandSender> liteCommands;
    private DatabaseManager databaseManager;
    private Economy economy;
    private DiscordClientManager discordClientManager;
    private ParcelService parcelService;
    private LockerService lockerService;
    private boolean apiInitialized;
```

After constructing `PluginParcelService parcelService` and `LockerManager lockerManager`, assign:

```java
this.parcelService = parcelService;
this.lockerService = lockerManager;
```

At the end of successful `onEnable()` initialization, after listeners and startup tasks have been registered:

```java
ParcelLockersProvider.initialize(this);
this.apiInitialized = true;
```

At the beginning of `onDisable()`, before unregistering listeners or closing dependencies:

```java
if (this.apiInitialized) {
    ParcelLockersProvider.deinitialize();
    this.apiInitialized = false;
}
```

Add the facade getters before `setupEconomy()`:

```java
@Override
public ParcelService getParcelService() {
    return Objects.requireNonNull(this.parcelService, "ParcelService is not initialized");
}

@Override
public LockerService getLockerService() {
    return Objects.requireNonNull(this.lockerService, "LockerService is not initialized");
}
```

Add imports for `LockerService`, `PluginParcelService`, and `java.util.Objects`; keep the implementation construction typed as `PluginParcelService` so internal collaborators can receive rollback and invalidation operations.

- [ ] **Step 7: Verify API and plugin lifecycle compilation**

Run:

```powershell
.\gradlew.bat :parcellockers-api:test
.\gradlew.bat :parcellockers-plugin:test
```

Expected: provider lifecycle and surface tests pass; the complete plugin suite passes.

- [ ] **Step 8: Commit provider integration**

```powershell
git add parcellockers-api/src parcellockers-plugin/src/main/java/com/eternalcode/parcellockers/ParcelLockers.java
git commit -m "feat: expose ParcelLockers API provider"
```

---

### Task 5: Publish and verify the API artifact

**Files:**
- Modify: `parcellockers-api/build.gradle.kts`
- Verify: `parcellockers-api/build/publications/maven/pom-default.xml`
- Verify: `parcellockers-api/build/libs/parcellockers-api-0.5.0-BETA.jar`
- Verify: `build/libs/ParcelLockers v0.5.0-BETA.jar`

**Interfaces:**
- Consumes: the complete API module and provider from Tasks 2–4.
- Produces: Maven publication `com.eternalcode:parcellockers-api:0.5.0-BETA`, source/Javadoc artifacts, and a deployable plugin shadow JAR containing the API.

- [ ] **Step 1: Add Maven publication configuration**

Replace `parcellockers-api/build.gradle.kts` with:

```kotlin
plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    compileOnlyApi("io.papermc.paper:paper-api:${Versions.PAPER_API}")
    api("org.jspecify:jspecify:${Versions.JSPECIFY}")

    testImplementation("org.junit.jupiter:junit-jupiter-api:${Versions.JUNIT}")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${Versions.JUNIT}")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    withSourcesJar()
    withJavadocJar()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = "parcellockers-api"
            from(components["java"])
        }
    }

    repositories {
        mavenLocal()
        maven {
            name = "eternalcodeReleases"
            url = uri("https://repo.eternalcode.pl/releases")
            credentials {
                username = System.getenv("ETERNAL_CODE_MAVEN_USERNAME")
                password = System.getenv("ETERNAL_CODE_MAVEN_PASSWORD")
            }
        }
    }
}
```

- [ ] **Step 2: Generate and inspect the publication metadata**

Run:

```powershell
.\gradlew.bat :parcellockers-api:generatePomFileForMavenPublication
Select-String -Path 'parcellockers-api\build\publications\maven\pom-default.xml' -Pattern 'paper-api','jspecify','1.0.0'
```

Expected: the POM contains `io.papermc.paper:paper-api` and `org.jspecify:jspecify:1.0.0`; it contains no plugin implementation dependency.

- [ ] **Step 3: Publish locally and build the deployable plugin**

Run:

```powershell
.\gradlew.bat :parcellockers-api:publishToMavenLocal
.\gradlew.bat :parcellockers-plugin:shadowJar
```

Expected: publication and shadow tasks finish with `BUILD SUCCESSFUL`; source and Javadoc JARs are generated for the API.

- [ ] **Step 4: Inspect API inclusion and implementation exclusion**

Run:

```powershell
jar tf 'parcellockers-api\build\libs\parcellockers-api-0.5.0-BETA.jar' | Select-String 'ParcelLockersApi','ParcelLockersProvider','ParcelService','LockerService','ParcelSendEvent','LockerCreateEvent'
jar tf 'parcellockers-api\build\libs\parcellockers-api-0.5.0-BETA.jar' | Select-String -Pattern '/repository/','/gui/','/database/','/discord/','/command/'
jar tf 'build\libs\ParcelLockers v0.5.0-BETA.jar' | Select-String 'com/eternalcode/parcellockers/ParcelLockersApi.class','com/eternalcode/parcellockers/ParcelLockers.class'
```

Expected: the first command finds all public entry points; the second prints nothing; the third finds both the API facade and plugin implementation in the deployable JAR.

- [ ] **Step 5: Run final verification from a clean task graph**

Run:

```powershell
.\gradlew.bat clean test :parcellockers-api:publishToMavenLocal :parcellockers-plugin:shadowJar
git diff --check
git status --short
```

Expected: `BUILD SUCCESSFUL`, no whitespace errors, and only the intended Task 5 build-file change is uncommitted.

- [ ] **Step 6: Commit publication support**

```powershell
git add parcellockers-api/build.gradle.kts
git commit -m "build: publish ParcelLockers API artifact"
```

- [ ] **Step 7: Verify the committed branch state**

Run:

```powershell
.\gradlew.bat test :parcellockers-plugin:shadowJar
git status --short --branch
git log --oneline --decorate -7
```

Expected: all tests and the deployable build pass; the worktree is clean on `codex/issue-176-api-module`; history contains the module, API contracts, locker service, provider, and publication commits.
