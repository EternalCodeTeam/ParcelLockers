# Parcel Return Name Format Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a configurable template that names returned parcels `[Refund] <original name>` by default.

**Architecture:** Store the template in `PluginConfig.Settings` and apply it when `ParcelReturnService` constructs the reversed `Parcel`. The already-atomic return repository persists the formatted name together with the other return state.

**Tech Stack:** Java 21, Okaeri Configs, CompletableFuture, JUnit 6, Mockito, Gradle

## Global Constraints

- The config field is named `parcelReturnNameFormat` and defaults to `[Refund] {NAME}`.
- Every `{NAME}` occurrence is replaced with the current parcel name.
- A template without `{NAME}` becomes the complete returned-parcel name.
- No GUI, message, notification, fee, timing, or persistence behavior changes.
- Do not commit; the user will create the commit.

---

### Task 1: Configure and apply the returned-parcel name template

**Files:**
- Modify: `src/main/java/com/eternalcode/parcellockers/configuration/implementation/PluginConfig.java:97-110`
- Modify: `src/main/java/com/eternalcode/parcellockers/returns/ParcelReturnService.java:186-203`
- Test: `src/test/java/com/eternalcode/parcellockers/returns/ParcelReturnServiceTest.java`

**Interfaces:**
- Consumes: `PluginConfig.Settings`, `Parcel.name()`, and `ParcelReturnRepository.commit(Parcel, ParcelContent, Delivery)`.
- Produces: `PluginConfig.Settings#parcelReturnNameFormat` as a public `String`; the committed returned `Parcel` contains the formatted name.

- [ ] **Step 1: Write failing behavior tests**

Add the assertion and captor imports:

```java
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.mockito.ArgumentCaptor;
```

Add these tests to `ParcelReturnServiceTest`:

```java
@Test
void appliesDefaultPrefixToReturnedParcelName() {
    Fixture fixture = new Fixture();

    assertEquals("[Refund] parcel", fixture.returnedParcelName());
}

@Test
void appliesCustomReturnedParcelNameFormat() {
    Fixture fixture = new Fixture();
    fixture.config.settings.parcelReturnNameFormat = "Returned: {NAME} / {NAME}";

    assertEquals("Returned: parcel / parcel", fixture.returnedParcelName());
}

@Test
void usesLiteralReturnedParcelNameWhenFormatHasNoPlaceholder() {
    Fixture fixture = new Fixture();
    fixture.config.settings.parcelReturnNameFormat = "Returned parcel";

    assertEquals("Returned parcel", fixture.returnedParcelName());
}
```

Add this helper inside `Fixture`:

```java
private String returnedParcelName() {
    this.validReturn();
    when(this.returnRepository.commit(any(), any(), any()))
        .thenReturn(CompletableFuture.completedFuture(true));

    this.service.returnParcel(this.player, this.parcel, this.deposited).join();

    ArgumentCaptor<Parcel> returnedParcel = ArgumentCaptor.forClass(Parcel.class);
    verify(this.returnRepository).commit(returnedParcel.capture(), any(), any());
    return returnedParcel.getValue().name();
}
```

- [ ] **Step 2: Run the focused test and verify RED**

Run:

```powershell
.\gradlew.bat test --tests "com.eternalcode.parcellockers.returns.ParcelReturnServiceTest" --no-configuration-cache --no-daemon
```

Expected: the new tests fail because returned parcels still contain `parcel`; before adding the config field, the two custom-format tests may first fail compilation because `parcelReturnNameFormat` does not exist. Add only the config field declaration if compilation blocks observing the behavioral failures, then rerun and confirm the name assertions fail before changing `ParcelReturnService`.

- [ ] **Step 3: Add the minimal configuration and formatting implementation**

Add this field beside the parcel-return window and fees in `PluginConfig.Settings`:

```java
@Comment({
    "",
    "# The name format applied when a parcel is returned.",
    "# Placeholder: {NAME} - the original parcel name."
})
public String parcelReturnNameFormat = "[Refund] {NAME}";
```

In `ParcelReturnService.proceedWithReturn`, replace construction with:

```java
String returnedName = this.config.settings.parcelReturnNameFormat.replace("{NAME}", current.name());
Parcel returned = new Parcel(current.uuid(), current.receiver(), returnedName,
    current.description(), current.priority(), current.sender(), current.size(),
    current.destinationLocker(), current.entryLocker(), ParcelStatus.SENT);
```

- [ ] **Step 4: Run the focused test and verify GREEN**

Run:

```powershell
.\gradlew.bat test --tests "com.eternalcode.parcellockers.returns.ParcelReturnServiceTest" --no-configuration-cache --no-daemon
```

Expected: all `ParcelReturnServiceTest` tests pass, including the three format cases.

- [ ] **Step 5: Run deterministic regression tests**

Run:

```powershell
.\gradlew.bat cleanTest test --tests "com.eternalcode.parcellockers.returns.ReturnItemEquivalenceTest" --tests "com.eternalcode.parcellockers.returns.ParcelReturnValidatorTest" --tests "com.eternalcode.parcellockers.returns.ParcelReturnServiceTest" --tests "com.eternalcode.parcellockers.parcel.task.ParcelSendTaskTest" --tests "com.eternalcode.parcellockers.parcel.service.AdminParcelServiceTest" --tests "com.eternalcode.parcellockers.parcel.repository.ParcelPageTest" --tests "com.eternalcode.parcellockers.gui.implementation.locker.ReturnGuiTest" --tests "com.eternalcode.parcellockers.gui.implementation.admin.AdminParcelEditGuiTest" --tests "com.eternalcode.parcellockers.content.ParcelContentTest" --tests "com.eternalcode.parcellockers.database.ParcelReturnCommitRepositoryIntegrationTest" --no-configuration-cache --no-daemon
```

Expected: all deterministic tests pass with zero failures and zero errors.

- [ ] **Step 6: Build and inspect the patch**

Run:

```powershell
.\gradlew.bat shadowJar --no-configuration-cache --no-daemon
git diff --check
git status --short
```

Expected: `shadowJar` succeeds, `git diff --check` reports no whitespace errors, and only the intended source, test, spec, and plan files are newly modified by this task.
