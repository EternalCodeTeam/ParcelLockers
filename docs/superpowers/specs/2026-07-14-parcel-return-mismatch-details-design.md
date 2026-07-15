# Detailed Parcel Return Mismatch Messages

## Goal

When a player deposits items to return a collected parcel and those items do not match the original contents, report every mismatch category and identify the affected item. Preserve the existing safety behavior: reject the return, give all deposited items back, and perform no fee withdrawal, database commit, or delivery scheduling.

## Validation Result

Replace the validator's boolean-only result with a structured result that contains whether the deposit matches and an ordered list of item-specific mismatches. Keep a boolean convenience method only where it avoids unnecessary caller churn; the structured result is the source of truth used by `ParcelReturnService`.

Mismatch categories are:

- unexpected material;
- insufficient amount;
- excess amount;
- durability;
- custom item name;
- enchantments;
- lore;
- other NBT/item metadata.

Checks disabled in `PluginConfig.ReturnChecks` do not reject a return and do not appear in the mismatch list. Material and total amount remain mandatory checks.

`ReturnItemEquivalence` exposes an attribute comparison in addition to its predicate-compatible boolean result. The attribute comparison isolates explicit attributes from residual metadata so, for example, an enchantment difference is reported as `enchantments`, not redundantly as both `enchantments` and `other item metadata`. The existing equivalence semantics remain unchanged.

## Matching Algorithm

Validation works on remaining quantities rather than physical stack boundaries, preserving the current behavior where split and merged stacks are equivalent.

1. Consume deposited quantities that exactly match an expected equivalence group.
2. For remaining quantities, pair stacks of the same material using the smallest number of enabled attribute differences. Input order breaks ties to keep the result deterministic.
3. Report every attribute category found for each paired item and consume the paired quantity.
4. Report remaining expected quantities as insufficient amounts.
5. Report remaining deposited quantities as excess amounts when that material was expected, or as unexpected items when it was not expected.
6. Aggregate duplicate findings for the same item and category so the notice stays concise.

Pairing same-material items after exact matches prevents an altered sword from being presented as both a missing original sword and an unrelated extra sword. If multiple variants of one material exist, closest-pair matching produces the most useful explanation without changing the acceptance rules.

## Message Configuration

Keep `MessageConfig.ParcelMessages.returnItemsMismatch` as the single failure notice and add a `{MISMATCHES}` placeholder. Add configurable templates for each reason and for joining multiple reasons, ensuring all player-facing wording remains in `MessageConfig`.

Default output lists each aggregated reason on a separate line. Examples:

```text
Diamond Sword: insufficient amount (expected 2, deposited 1)
Diamond Sword: enchantments differ
Diamond Sword: durability differs (expected damage 4, deposited 27)
Dirt: unexpected item (deposited 16)
```

Amounts and durability include expected and deposited numeric values. Material mismatches include readable material names. Custom names, enchantments, lore, and arbitrary metadata identify the item and mismatch category; potentially long or unsafe raw metadata is not dumped into chat.

The empty-deposit shortcut in `ReturnDepositGui` is removed. Confirming an empty deposit invokes the normal return service, allowing the validator to report each missing expected item through the same configured notice.

## Service Flow and Failure Handling

After loading the current parcel content, `ParcelReturnService` asks the validator for a structured result. A valid result continues through the existing locker, fee, and atomic-commit flow. An invalid result formats every mismatch with `MessageConfig`, supplies the combined value as `{MISMATCHES}`, gives the deposited items back, and completes without further return processing.

All other abort paths retain their current notices. Diagnostic formatting is side-effect-free and contains no shared mutable state, so concurrent asynchronous returns cannot leak mismatch details between players.

## Testing

Implementation follows test-driven development. Focused tests are added and observed failing before production changes.

`ReturnItemEquivalenceTest` covers:

- each explicit mismatch category;
- disabled checks;
- residual NBT detection without duplicate explicit categories;
- unchanged boolean equivalence behavior.

`ParcelReturnValidatorTest` covers:

- exact, split, and merged matches;
- insufficient and excess quantities with expected/deposited values;
- unexpected materials;
- several simultaneous categories;
- closest same-material pairing;
- empty deposits;
- aggregation and deterministic ordering.

Service and GUI-level tests cover:

- one configured notice containing all formatted item-specific reasons;
- deposited-item restoration and absence of fee, commit, and scheduling on mismatch;
- empty confirmation reaching normal validation rather than sending the old generic GUI notice.

After focused tests pass, run the complete unit-test suite. Existing user changes in `build.gradle.kts` and `PR_REVIEW_feat-parcel-return-gh-69.md` are outside this work and remain untouched.
