# Modrinth README Refresh Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the sparse project README with an encouraging, accurate, Modrinth-first page that appeals to players and gives server owners enough information to install the plugin.

**Architecture:** This is a documentation-only change. Replace `README.md` as one cohesive publishing artifact, then validate its claims against the approved design and current project metadata.

**Tech Stack:** GitHub-flavored Markdown, Modrinth project-page Markdown, PowerShell validation commands

## Global Constraints

- The same `README.md` must render sensibly on GitHub and when synchronized to Modrinth.
- The primary reader is a server owner; player-facing benefits lead the page.
- Use confident, friendly, energetic language and restrained emojis.
- Preserve absolute GitHub asset URLs so synchronized images continue to work on Modrinth.
- State Paper or Purpur, Java 21 or newer, and Vault with a compatible economy provider.
- Do not claim Folia support, Nexo integration, or mandatory Discord integration.
- Do not hard-code a Minecraft version list; direct readers to the versions shown on each Modrinth release.
- Do not change plugin code, configuration defaults, build metadata, or publishing automation.
- Do not push any commits.

---

### Task 1: Rewrite and validate the publishing README

**Files:**
- Modify: `README.md`
- Reference: `docs/superpowers/specs/2026-07-15-modrinth-readme-design.md`
- Include in local commit: `docs/superpowers/plans/2026-07-15-modrinth-readme-refresh.md`

**Interfaces:**
- Consumes: the verified plugin behavior and publishing constraints in the approved design specification
- Produces: one self-contained Markdown page used by GitHub and `modrinth.syncBodyFrom`

- [ ] **Step 1: Replace `README.md` with the approved benefit-led copy**

Use this exact content:

````markdown
<div align="center">

![ParcelLockers](https://github.com/EternalCodeTeam/ParcelLockers/blob/master/.github/assets/ParcelLockers.svg?raw=true)

[![Supports Paper](https://raw.githubusercontent.com/intergrav/devins-badges/v3/assets/cozy/supported/paper_vector.svg)](https://papermc.io)

[![Ko-fi](https://github.com/intergrav/devins-badges/blob/v3/assets/cozy/donate/kofi-plural_vector.svg?raw=true)](https://ko-fi.com/eternalcodeteam)
[![Chat on Discord](https://raw.githubusercontent.com/vLuckyyy/badges/main//chat-with-us-on-discord.svg)](https://discord.com/invite/FQ7jmGBd6c)
[![Website](https://raw.githubusercontent.com/intergrav/devins-badges/v3/assets/cozy/documentation/website_vector.svg)](https://eternalcode.pl/)

<strong><a href="https://modrinth.com/plugin/parcellockers">📦 Download ParcelLockers on Modrinth</a></strong>

</div>

# Make every delivery feel like part of the world 📮

**ParcelLockers brings immersive player-to-player item delivery to your Minecraft server.** Players can pack items into parcels, choose a recipient and destination locker, then collect deliveries through friendly inventory menus—no awkward meetups or admin hand-offs required.

Whether you run a cozy SMP, a busy economy server, or a role-play community, ParcelLockers turns moving items across the world into a feature your players can actually enjoy.

## ✨ What makes ParcelLockers worth trying?

- **A physical locker network** — place dedicated parcel lockers around your world and give each one a recognizable description.
- **Simple inventory GUIs** — players can send, collect, review, and return parcels without memorizing a maze of commands.
- **Three parcel sizes** — small, medium, and large parcels let you price deliveries around the amount being shipped.
- **Standard and priority delivery** — create a meaningful choice between regular shipping and faster service.
- **Economy integration** — configurable sending and return fees use Vault to fit naturally into your server economy.
- **Destination-based collection** — parcels arrive at the locker selected by the sender, with an optional setting that allows collection from any locker.
- **Fair parcel returns** — collected deliveries can be returned within a configurable window by depositing matching items.
- **Optional Discord notifications** — notify linked players when deliveries arrive using the built-in Discord bot or your existing DiscordSRV setup.
- **Administration tools** — built-in GUIs help staff inspect and manage users, parcels, parcel contents, and lockers.
- **Deep customization** — adjust locker capacity, delivery times, fees, messages, GUI titles, icons, and item appearance.

## 🏡 A natural fit for your server

ParcelLockers adds more than another command—it creates reasons to build post offices, delivery hubs, shops, and community infrastructure. Delivery fees can act as a useful economy sink, priority parcels give players a premium option, and configurable timings let you choose between realism and convenience.

Almost every player-facing detail can be adapted to your server's style, so ParcelLockers can feel at home in a relaxed survival world or a carefully balanced economy.

## 🚀 Quick start

1. Download the latest compatible release from [Modrinth](https://modrinth.com/plugin/parcellockers/versions).
2. Install **Vault** and a compatible economy provider.
3. Place the ParcelLockers JAR in your server's `plugins` directory and restart the server.
4. Review `config.yml` and `messages.yml` to customize fees, delivery times, menus, and messages.
5. Give staff the `parcellockers.admin` permission and use `/parcellockers get` to obtain a locker item.
6. Give players the `parcellockers.command.parcel` permission so they can open their parcel menu with `/parcel gui`.

## ✅ Requirements

- A **Paper or Purpur** server on a version listed as supported by the chosen [Modrinth release](https://modrinth.com/plugin/parcellockers/versions)
- **Java 21 or newer**
- **Vault** with a compatible economy provider

Discord integration is entirely optional. If enabled, ParcelLockers can use either its own bot or an installed DiscordSRV instance for account linking and delivery notifications.

## 🧪 Project status

ParcelLockers is actively developed and currently released as beta software. We are continually improving the experience, and community feedback helps us make every release better.

Need help, have an idea, or are not sure whether something is intended? Join our [Discord community](https://discord.gg/FQ7jmGBd6c). Found a reproducible bug? Please [open an issue](https://github.com/EternalCodeTeam/ParcelLockers/issues).

## 🏗️ Building from source

ParcelLockers requires **JDK 21 or newer** to build:

```shell
./gradlew shadowJar
```

The finished plugin JAR will be created in `build/libs`. The first build may take a little longer while Gradle downloads the required dependencies.

## 🛠️ Development builds

Want to try the newest changes before the next release? Development artifacts are available from [GitHub Actions](https://github.com/EternalCodeTeam/ParcelLockers/actions). These builds may be less stable than published Modrinth releases.

## 🐙 Contributing

Contributions are welcome! Create a [public fork](https://github.com/EternalCodeTeam/ParcelLockers/fork), make your changes, and open a pull request. Read [CONTRIBUTING.md](https://github.com/EternalCodeTeam/ParcelLockers/blob/master/.github/CONTRIBUTING.md) before getting started.

## ❤️ Special thanks

[<img src="https://user-images.githubusercontent.com/65517973/210912946-447a6b9a-2685-4796-9482-a44bffc727ce.png" alt="JetBrains" width="150">](https://www.jetbrains.com)
[<img src="https://github.com/EternalCodeTeam/ParcelLockers/blob/master/.github/assets/sentry.svg?raw=true" alt="Sentry logo" width="150" height="150">](https://www.sentry.io)

Thank you to JetBrains for providing [Open Source Licenses](https://www.jetbrains.com/opensource/) for their development tools, and to Sentry for supporting the project with an [Open Source plan](https://sentry.io/for/open-source/).

Locker icon created by [Nikita Golubev — Flaticon](https://www.flaticon.com/free-icons/locker).
````

- [ ] **Step 2: Run mechanical Markdown and whitespace checks**

Run:

```powershell
git diff --check -- README.md
rg -n "transfering|an very|early stage|use it at your own risk|Nexo|Folia|Minecraft 1\." README.md
```

Expected: `git diff --check` prints nothing. The `rg` command exits with code 1 and prints no matches.

- [ ] **Step 3: Verify required content and Modrinth-safe asset links**

Run:

```powershell
rg -n "Modrinth|Paper or Purpur|Java 21|Vault|DiscordSRV|priority delivery|parcel returns|parcellockers\.admin|parcellockers\.command\.parcel" README.md
rg --pcre2 -n '(?:!\[[^]]*\]\(|<img[^>]+src=")(?!https://)' README.md
```

Expected: the first command finds every required topic. The second command exits with code 1 and prints no relative Markdown or HTML image URLs.

- [ ] **Step 4: Review the final documentation diff**

Run:

```powershell
git diff -- README.md
git status --short
```

Expected: `README.md` contains only the approved rewrite. The plan file is untracked, and the pre-existing `PR_REVIEW_feat-parcel-return-gh-69.md` remains untracked and untouched.

- [ ] **Step 5: Create a local documentation commit without pushing**

Run:

```powershell
git add -- README.md docs/superpowers/plans/2026-07-15-modrinth-readme-refresh.md
git diff --cached --check
git diff --cached --name-only
git commit -m "docs: refresh README for Modrinth"
```

Expected: the staged-name check lists only `README.md` and `docs/superpowers/plans/2026-07-15-modrinth-readme-refresh.md`; the commit succeeds locally. Do not run `git push`.
