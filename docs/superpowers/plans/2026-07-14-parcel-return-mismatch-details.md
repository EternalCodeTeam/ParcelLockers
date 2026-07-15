# Detailed Parcel Return Mismatch Messages Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the generic refunded-parcel content mismatch notice with one configurable notice that lists every item-specific mismatch category.

**Architecture:** `ReturnItemEquivalence` becomes a diagnostic comparator while preserving predicate behavior. `ParcelReturnValidator` returns immutable diagnostics, `ReturnMismatchFormatter` renders them through `MessageConfig`, and `ParcelReturnService` sends one formatted notice while retaining the current give-back and atomic-return boundaries.

**Tech Stack:** Java 21, Paper/Bukkit, Okaeri Configs, multification, JUnit 6, Mockito, Gradle.

## Global Constraints

- Material and total amount checks are mandatory.
- Disabled attribute checks neither reject a return nor appear in diagnostics.
- Report all categories with the affected material; include numeric values for amounts and durability.
- Do not expose raw lore or arbitrary NBT in chat.
- Invalid returns give deposited items back and do not withdraw fees, commit, or schedule delivery.
- Do not stage or change the user's `build.gradle.kts` or `PR_REVIEW_feat-parcel-return-gh-69.md`.

## File Structure

- `ReturnItemComparator` and `ReturnItemDifference`: low-level diagnostic comparison.
- `ReturnMismatchType`, `ReturnItemMismatch`, and `ParcelReturnValidationResult`: parcel-level diagnostics.
- `ParcelReturnValidator`: exact-first, closest-same-material reconciliation.
- `ReturnMismatchFormatter` and `MessageConfig`: configurable rendering.
- `ParcelReturnService`, `ParcelLockers`, and `ReturnDepositGui`: runtime integration.

---

### Task 1: Diagnostic Item Comparison

**Files:**
- Create: `src/main/java/com/eternalcode/parcellockers/returns/ReturnItemComparator.java`
- Create: `src/main/java/com/eternalcode/parcellockers/returns/ReturnItemDifference.java`
- Modify: `src/main/java/com/eternalcode/parcellockers/returns/ReturnItemEquivalence.java`
- Test: `src/test/java/com/eternalcode/parcellockers/returns/ReturnItemEquivalenceTest.java`

**Interfaces:**
- Produces `EnumSet<ReturnItemDifference> differences(ItemStack expected, ItemStack deposited)`.
- Retains predicate-compatible `test(expected, deposited)`.

- [ ] **Step 1: Write failing diagnostic tests**

Add exact-set assertions for material, durability, display name, enchantments, lore, residual metadata, and disabled checks:

```java
assertEquals(
    EnumSet.of(ReturnItemDifference.DURABILITY, ReturnItemDifference.ENCHANTMENTS),
    equivalence.differences(expected, deposited)
);
```

For residual metadata, configure mocked clones so normalized `isSimilar` returns false and assert `EnumSet.of(ReturnItemDifference.NBT)`. Also retain the existing boolean assertions to prove compatibility.

- [ ] **Step 2: Run the test and verify RED**

```powershell
./gradlew test --tests "com.eternalcode.parcellockers.returns.ReturnItemEquivalenceTest"
```

Expected: compilation fails because the diagnostic API does not exist.

- [ ] **Step 3: Add the contract and enum**

```java
@FunctionalInterface
public interface ReturnItemComparator extends BiPredicate<ItemStack, ItemStack> {
    EnumSet<ReturnItemDifference> differences(ItemStack expected, ItemStack deposited);

    @Override
    default boolean test(ItemStack expected, ItemStack deposited) {
        return this.differences(expected, deposited).isEmpty();
    }
}
```

```java
public enum ReturnItemDifference {
    MATERIAL, DURABILITY, ITEM_NAME, ENCHANTMENTS, LORE, NBT
}
```

- [ ] **Step 4: Implement diagnostic equivalence**

Make `ReturnItemEquivalence` implement `ReturnItemComparator`. Return `MATERIAL` immediately for different types. Compare each enabled explicit attribute and add its enum value. When `checkNbt` is enabled, clone both items, clear damage, display name, enchantments, and lore on both clones, then compare the normalized clones with `isSimilar`:

```java
if (this.checks.checkNbt && !this.residualDataMatches(expected, deposited)) {
    differences.add(ReturnItemDifference.NBT);
}
```

This residual comparison prevents explicit differences from also being labeled NBT.

- [ ] **Step 5: Run the test and verify GREEN**

Run the Task 1 test command. Expected: PASS.

- [ ] **Step 6: Commit Task 1**

```powershell
git add src/main/java/com/eternalcode/parcellockers/returns/ReturnItemComparator.java src/main/java/com/eternalcode/parcellockers/returns/ReturnItemDifference.java src/main/java/com/eternalcode/parcellockers/returns/ReturnItemEquivalence.java src/test/java/com/eternalcode/parcellockers/returns/ReturnItemEquivalenceTest.java
git commit -m "refactor: expose return item differences"
```

---

### Task 2: Structured Parcel Validation

**Files:**
- Create: `src/main/java/com/eternalcode/parcellockers/returns/ReturnMismatchType.java`
- Create: `src/main/java/com/eternalcode/parcellockers/returns/ReturnItemMismatch.java`
- Create: `src/main/java/com/eternalcode/parcellockers/returns/ParcelReturnValidationResult.java`
- Modify: `src/main/java/com/eternalcode/parcellockers/returns/ParcelReturnValidator.java`
- Test: `src/test/java/com/eternalcode/parcellockers/returns/ParcelReturnValidatorTest.java`

**Interfaces:**
- Consumes `ReturnItemComparator.differences(...)`.
- Produces `ParcelReturnValidationResult validate(List<ItemStack> deposited, List<ItemStack> expected)`.
- Keeps `matches(...)` as a delegate to `validate(...).matches()`.

- [ ] **Step 1: Write failing structured-result tests**

Use a material-only comparator:

```java
private static final ReturnItemComparator BY_MATERIAL = (expected, deposited) ->
    expected.getType() == deposited.getType()
        ? EnumSet.noneOf(ReturnItemDifference.class)
        : EnumSet.of(ReturnItemDifference.MATERIAL);
```

Assert exact ordered results for insufficient and excess amount, unexpected material, empty deposit, simultaneous attributes, exact-before-closest pairing, split/merged stacks, aggregation, and deterministic order:

```java
assertEquals(List.of(
    ReturnItemMismatch.insufficient(Material.DIAMOND, 5, 4),
    ReturnItemMismatch.unexpected(Material.DIRT, 2)
), result.mismatches());
```

- [ ] **Step 2: Run the validator test and verify RED**

```powershell
./gradlew test --tests "com.eternalcode.parcellockers.returns.ParcelReturnValidatorTest"
```

Expected: compilation fails because the result types do not exist.

- [ ] **Step 3: Add immutable result types**

```java
public enum ReturnMismatchType {
    UNEXPECTED_ITEM, INSUFFICIENT_AMOUNT, EXCESS_AMOUNT,
    DURABILITY, ITEM_NAME, ENCHANTMENTS, LORE, NBT
}
```

```java
public record ReturnItemMismatch(
    ReturnMismatchType type,
    Material item,
    int expectedAmount,
    int depositedAmount,
    Integer expectedDamage,
    Integer depositedDamage
) {
    public static ReturnItemMismatch unexpected(Material item, int deposited) {
        return new ReturnItemMismatch(ReturnMismatchType.UNEXPECTED_ITEM, item, 0, deposited, null, null);
    }

    public static ReturnItemMismatch insufficient(Material item, int expected, int deposited) {
        return new ReturnItemMismatch(ReturnMismatchType.INSUFFICIENT_AMOUNT, item, expected, deposited, null, null);
    }

    public static ReturnItemMismatch excess(Material item, int expected, int deposited) {
        return new ReturnItemMismatch(ReturnMismatchType.EXCESS_AMOUNT, item, expected, deposited, null, null);
    }
}
```

Add an `attribute(type, expected, deposited)` factory that stores both damage values only for `DURABILITY`, and otherwise stores null damage values. Its private damage helper reads `Damageable#getDamage`, defaulting to zero.

```java
public record ParcelReturnValidationResult(List<ReturnItemMismatch> mismatches) {
    public ParcelReturnValidationResult {
        mismatches = List.copyOf(mismatches);
    }

    public boolean matches() {
        return this.mismatches.isEmpty();
    }
}
```

- [ ] **Step 4: Implement deterministic reconciliation**

Build insertion-ordered expected and deposited totals per material. Emit one amount or unexpected mismatch per material. Wrap stacks in candidates containing the stack and remaining quantity. Consume exact matches first. Then repeatedly select the equal-material pair with the smallest non-zero difference count, breaking ties by expected index and deposited index. Map differences with an exhaustive switch into `ReturnMismatchType`, add records to a `LinkedHashSet`, consume the smaller quantity, and continue. `MATERIAL` is never mapped because closest pairing considers equal materials only.

- [ ] **Step 5: Run comparison and validator tests and verify GREEN**

```powershell
./gradlew test --tests "com.eternalcode.parcellockers.returns.ParcelReturnValidatorTest" --tests "com.eternalcode.parcellockers.returns.ReturnItemEquivalenceTest"
```

Expected: PASS.

- [ ] **Step 6: Commit Task 2**

```powershell
git add src/main/java/com/eternalcode/parcellockers/returns/ReturnMismatchType.java src/main/java/com/eternalcode/parcellockers/returns/ReturnItemMismatch.java src/main/java/com/eternalcode/parcellockers/returns/ParcelReturnValidationResult.java src/main/java/com/eternalcode/parcellockers/returns/ParcelReturnValidator.java src/test/java/com/eternalcode/parcellockers/returns/ParcelReturnValidatorTest.java
git commit -m "feat: diagnose parcel return mismatches"
```

---

### Task 3: Configurable Formatting

**Files:**
- Create: `src/main/java/com/eternalcode/parcellockers/returns/ReturnMismatchFormatter.java`
- Modify: `src/main/java/com/eternalcode/parcellockers/configuration/implementation/MessageConfig.java`
- Create: `src/test/java/com/eternalcode/parcellockers/returns/ReturnMismatchFormatterTest.java`

**Interfaces:**
- Produces `String ReturnMismatchFormatter.format(ParcelReturnValidationResult result)`.

- [ ] **Step 1: Write failing formatter tests**

Create real `MessageConfig.ParcelMessages`, replace templates with marker strings, and assert every replacement and ordering:

```java
messages.returnMismatchSeparator = " | ";
messages.returnMismatchInsufficientAmount = "{ITEM}:{EXPECTED_AMOUNT}/{DEPOSITED_AMOUNT}";
messages.returnMismatchDurability = "{ITEM}:durability:{EXPECTED_DAMAGE}/{DEPOSITED_DAMAGE}";
```

Expected example: `Diamond:5/4 | Diamond Sword:durability:4/27`. Also assert an empty result throws `IllegalArgumentException`.

- [ ] **Step 2: Run the formatter test and verify RED**

```powershell
./gradlew test --tests "com.eternalcode.parcellockers.returns.ReturnMismatchFormatterTest"
```

Expected: compilation fails because the formatter and message fields do not exist.

- [ ] **Step 3: Add message templates**

Put `{MISMATCHES}` on a second chat line in `returnItemsMismatch`, and add:

```java
public String returnMismatchSeparator = "<newline>";
public String returnMismatchUnexpectedItem = "&8- &f{ITEM}: &cunexpected item (deposited {DEPOSITED_AMOUNT})";
public String returnMismatchInsufficientAmount = "&8- &f{ITEM}: &cinsufficient amount (expected {EXPECTED_AMOUNT}, deposited {DEPOSITED_AMOUNT})";
public String returnMismatchExcessAmount = "&8- &f{ITEM}: &cexcess amount (expected {EXPECTED_AMOUNT}, deposited {DEPOSITED_AMOUNT})";
public String returnMismatchDurability = "&8- &f{ITEM}: &cdurability differs (expected damage {EXPECTED_DAMAGE}, deposited {DEPOSITED_DAMAGE})";
public String returnMismatchItemName = "&8- &f{ITEM}: &ccustom name differs";
public String returnMismatchEnchantments = "&8- &f{ITEM}: &cenchantments differ";
public String returnMismatchLore = "&8- &f{ITEM}: &clore differs";
public String returnMismatchNbt = "&8- &f{ITEM}: &cother item data differs";
```

- [ ] **Step 4: Implement the formatter**

Hold `MessageConfig.ParcelMessages`, select the template with an exhaustive switch, replace `{ITEM}` through `MaterialUtil.format`, replace relevant numeric markers, and join lines with `returnMismatchSeparator`. Reject matching results because they have no diagnostic text.

- [ ] **Step 5: Run the formatter test and verify GREEN**

Run the Task 3 command. Expected: PASS.

- [ ] **Step 6: Commit Task 3**

```powershell
git add src/main/java/com/eternalcode/parcellockers/returns/ReturnMismatchFormatter.java src/main/java/com/eternalcode/parcellockers/configuration/implementation/MessageConfig.java src/test/java/com/eternalcode/parcellockers/returns/ReturnMismatchFormatterTest.java
git commit -m "feat: format return mismatch reasons"
```

---

### Task 4: Service and Empty-Deposit Integration

**Files:**
- Modify: `src/main/java/com/eternalcode/parcellockers/returns/ParcelReturnService.java`
- Modify: `src/main/java/com/eternalcode/parcellockers/ParcelLockers.java`
- Modify: `src/main/java/com/eternalcode/parcellockers/gui/implementation/locker/ReturnDepositGui.java`
- Modify: `src/test/java/com/eternalcode/parcellockers/returns/ParcelReturnServiceTest.java`
- Create: `src/test/java/com/eternalcode/parcellockers/gui/implementation/locker/ReturnDepositGuiTest.java`

**Interfaces:**
- Consumes `ParcelReturnValidator.validate(...)` and `ReturnMismatchFormatter.format(...)`.
- Sends one configured notice with `{MISMATCHES}` replaced.

- [ ] **Step 1: Write a failing service test**

Return two mismatches from the validator and `first<newline>second` from the formatter. Return a Mockito `NoticeBroadcast` with `RETURNS_SELF` from `noticeService.create()`. Verify:

```java
verify(broadcast).placeholder("{MISMATCHES}", "first<newline>second");
verify(broadcast).send();
verify(fixture.returnRepository, never()).commit(any(), any(), any());
verify(fixture.economy, never()).withdrawPlayer(any(Player.class), anyDouble());
verify(fixture.scheduler, never()).runLaterAsync(any(Runnable.class), any(Duration.class));
```

Capture the give-back scheduler callback to retain restoration coverage.

- [ ] **Step 2: Write a failing empty-confirm GUI test**

Add a package-private constructor accepting `IntFunction<StorageGui>` as a GUI factory. Capture the `GuiAction<InventoryClickEvent>` passed to `confirmReturnItem.toGuiItem(...)`, execute it with no deposited stacks, and assert:

```java
verify(guiManager).returnParcel(player, parcel, List.of());
verify(noticeService, never()).player(any(), any());
```

- [ ] **Step 3: Run focused tests and verify RED**

```powershell
./gradlew test --tests "com.eternalcode.parcellockers.returns.ParcelReturnServiceTest" --tests "com.eternalcode.parcellockers.gui.implementation.locker.ReturnDepositGuiTest"
```

Expected: service and GUI still use the old generic paths.

- [ ] **Step 4: Integrate structured validation**

Inject `ReturnMismatchFormatter` into `ParcelReturnService`. On invalid validation, give items back and send:

```java
this.noticeService.create()
    .notice(messages -> messages.parcel.returnItemsMismatch)
    .player(player.getUniqueId())
    .placeholder("{MISMATCHES}", this.mismatchFormatter.format(validation))
    .send();
```

Return a completed future immediately. Instantiate `new ReturnMismatchFormatter(messageConfig.parcel)` in `ParcelLockers.onEnable()` and pass it into the service.

- [ ] **Step 5: Allow empty submissions**

Remove the empty-list early notice from `ReturnDepositGui`. Always mark confirmed, close, and call `guiManager.returnParcel`. Retain the public Triumph GUI factory and use the injected factory only as the test seam.

- [ ] **Step 6: Run focused tests and verify GREEN**

Run the Task 4 command. Expected: PASS.

- [ ] **Step 7: Run full verification**

```powershell
./gradlew test
```

Expected: PASS. Report unavailable Docker separately if it prevents only Testcontainers execution.

- [ ] **Step 8: Review and commit feature files**

```powershell
git diff --check
git status --short
git diff -- src/main/java src/test/java
```

Stage only files named in Tasks 1-4, then commit:

```powershell
git commit -m "feat: explain parcel return mismatches"
```

Confirm the user's `build.gradle.kts` and `PR_REVIEW_feat-parcel-return-gh-69.md` remain outside every commit.
