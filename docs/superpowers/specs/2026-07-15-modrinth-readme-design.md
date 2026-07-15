# Modrinth README Refresh Design

## Goal

Rewrite `README.md` into an encouraging, benefit-led project page that is suitable for both GitHub and Modrinth. The primary reader is a server owner evaluating the plugin, while the copy should also make the player experience feel appealing.

## Positioning

Present ParcelLockers as an immersive way to send items between physical parcel lockers across a Minecraft world. Lead with the gameplay value, then support the promise with concise, verified details about administration, customization, compatibility, and integrations.

The tone should be confident, friendly, and energetic without exaggerating maturity or claiming unsupported features. Emojis should improve scanning rather than appear in every sentence.

## Page Structure

1. Retain the centered project logo, Paper badge, donation link, Discord link, and website link.
2. Replace the existing introduction with a short headline and value proposition focused on convenient, immersive item delivery.
3. Add a scannable feature section covering:
   - inventory GUI workflows for sending, collecting, and returning parcels;
   - small, medium, and large parcel sizes;
   - standard and priority delivery times;
   - configurable Vault economy fees;
   - destination lockers and configurable locker capacity;
   - time-limited returns with configurable validation;
   - optional Discord delivery notifications through the built-in bot or DiscordSRV;
   - configurable messages, GUI items, appearance, fees, and timings.
4. Add a server-owner section explaining the plugin's value for survival, economy, and role-play communities.
5. Add a compact requirements section that states Paper or Purpur, Java 21 or newer, and Vault with a compatible economy provider. Mention that supported game versions are those listed on the Modrinth release rather than hard-coding a potentially stale version list.
6. Replace the discouraging early-development warning with positive beta-stage transparency and clear support links.
7. Keep source compilation, development builds, contributing, issue reporting, credits, and icon attribution below the user-facing sections.

## Accuracy Constraints

- Describe only behavior verified in the current source and build configuration.
- Do not claim Folia support or Nexo integration.
- Do not imply Discord integration is required.
- Distinguish Vault, which is required, from a compatible economy provider.
- Avoid database claims because they are not a primary user benefit and implementation details may change.
- Preserve absolute GitHub asset URLs because Modrinth receives the README body directly.

## Validation

- Review the rendered Markdown structure for clear heading order and readable lists.
- Check every retained link and image URL syntactically.
- Search for outdated wording, spelling mistakes, unsupported claims, and hard-coded Minecraft versions.
- Confirm that only documentation files intended by this task are included in any local commit.

## Out of Scope

- Changing plugin behavior, configuration defaults, build metadata, or publishing automation.
- Adding screenshots or other new visual assets.
- Publishing, pushing, or updating the live Modrinth page directly.
