<div align="center">

![ParcelLockers](https://raw.githubusercontent.com/EternalCodeTeam/ParcelLockers/master/.github/assets/ParcelLockers.svg)

[![Supports Paper](https://raw.githubusercontent.com/intergrav/devins-badges/v3/assets/cozy/supported/paper_vector.svg)](https://papermc.io)

[![Ko-fi](https://raw.githubusercontent.com/intergrav/devins-badges/v3/assets/cozy/donate/kofi-plural_vector.svg)](https://ko-fi.com/eternalcodeteam)
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
[<img src="https://raw.githubusercontent.com/EternalCodeTeam/ParcelLockers/master/.github/assets/sentry.svg" alt="Sentry logo" width="150" height="150">](https://www.sentry.io)

Thank you to JetBrains for providing [Open Source Licenses](https://www.jetbrains.com/opensource/) for their development tools, and to Sentry for supporting the project with an [Open Source plan](https://sentry.io/for/open-source/).

Locker icon created by [Nikita Golubev — Flaticon](https://www.flaticon.com/free-icons/locker).
