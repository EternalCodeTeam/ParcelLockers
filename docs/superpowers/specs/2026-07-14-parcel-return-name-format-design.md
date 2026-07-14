# Configurable Parcel Return Name Format

## Goal

Allow server administrators to configure the name assigned to a returned parcel. Returned parcels should use a configurable template instead of preserving the original name unchanged.

## Configuration

Add `parcelReturnNameFormat` to `PluginConfig.Settings`, next to the existing parcel-return settings.

Default value:

```text
[Refund] {NAME}
```

Okaeri Configs will expose this field in the generated YAML as `parcelReturnNameFormat`, matching the existing camelCase configuration keys.

`{NAME}` represents the parcel name immediately before the return is committed. Every occurrence of the placeholder is replaced. If the configured value contains no `{NAME}` placeholder, the configured value becomes the complete returned-parcel name.

## Return Flow

When `ParcelReturnService` constructs the returned `Parcel`, it formats the name using `config.settings.parcelReturnNameFormat` and the current parcel name. The formatted name is included in the existing atomic return transaction, so no additional persistence operation is introduced.

No other parcel fields, GUI labels, messages, or notification behavior change.

## Compatibility

Existing installations receive the new default during config migration/generation. Therefore, after updating, returned parcels are named `[Refund] <original name>` unless the administrator changes the format to `{NAME}` or another template.

Formatting codes are stored as part of the parcel name exactly as configured; rendering remains the responsibility of the existing GUI/message pipeline.

## Testing

Extend `ParcelReturnServiceTest` using the existing mocked atomic repository boundary:

- verify the default format produces `[Refund] <original name>` in the committed parcel;
- verify a custom format replaces `{NAME}` with the original name;
- verify a format without `{NAME}` becomes the complete returned-parcel name.

Implementation follows test-driven development: add and run a failing behavior test before changing production code, then implement the smallest formatting change and rerun the focused and deterministic suites.
