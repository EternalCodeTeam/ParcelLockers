# README Feature Emoji Design

## Goal

Improve the scanability of the "What makes ParcelLockers worth trying?" feature list by adding one descriptive emoji to the beginning of every feature bullet.

## Design

Keep all existing feature copy, order, links, and Markdown structure unchanged. Prefix the ten bullets with the approved mapping:

- `🏤` physical locker network
- `🖥️` inventory GUIs
- `📦` parcel sizes
- `⚡` standard and priority delivery
- `💰` economy integration
- `📍` destination-based collection
- `↩️` parcel returns
- `🔔` Discord notifications
- `🛡️` administration tools
- `🎨` customization

## Constraints

- Add exactly one emoji per feature bullet.
- Do not change the feature claims, punctuation, heading, or any other README section.
- Use standard Unicode emoji that render on GitHub and Modrinth.

## Validation

- Confirm the ten bullets begin with the approved emoji in the approved order.
- Run `git diff --check` and verify no unrelated README lines changed.
