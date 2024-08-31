<div align="center">

![ParcelLockers.svg](assets/ParcelLockers.svg)

[![Supports Paper](https://raw.githubusercontent.com/intergrav/devins-badges/v3/assets/cozy/supported/paper_vector.svg)](https://papermc.io)
[![Supports Spigot](https://raw.githubusercontent.com/intergrav/devins-badges/v3/assets/cozy/supported/spigot_vector.svg)](https://spigotmc.org)

[![Patreon](https://raw.githubusercontent.com/intergrav/devins-badges/v3/assets/cozy/donate/patreon-plural_vector.svg)](https://www.patreon.com/eternalcode)
[![Website](https://raw.githubusercontent.com/intergrav/devins-badges/v3/assets/cozy/documentation/website_vector.svg)](https://eternalcode.pl/)
[![Chat on Discord](https://raw.githubusercontent.com/vLuckyyy/badges/main//chat-with-us-on-discord.svg)](https://discord.com/invite/FQ7jmGBd6c)

[![Gradle](https://raw.githubusercontent.com/intergrav/devins-badges/v3/assets/cozy/built-with/gradle_vector.svg)](https://gradle.org/)
[![Java](https://raw.githubusercontent.com/intergrav/devins-badges/v3/assets/cozy/built-with/java17_vector.svg)](https://www.java.com/)

</div>

# Welcome to ParcelLockers! 🚀

ParcelLockers is a minecraft plugin, dedicated for Spigot-based servers.. 💎 Crafted with care, ParcelLockers
is dedicated to incorporating highly practical server function of transfering items across the Minecraft world,
especially on long distances.

## ℹ️ Information

- ParcelLockers fully supports Minecraft's latest minor versions starting from each major version, starting from 1.17
  onward, e.g. `1.17.1`, `1.18.2`, `1.19.4`, `1.20.6`, `1.21.1`.
- Requires **Java 17 or later** to work properly. For older versions of Java, this may affect the functionality of the
  plugin.
- If you have any questions, you can ask us about it on our [Discord server](https://discord.gg/FQ7jmGBd6c).

## ❗Warning

ParcelLockers is at an very early stage of development, and you use it at your own risk.

## 🏗️ Compiling JAR from Source

To build ParcelLockers, follow these steps (Make sure you have **JDK 17 or higher**):

```shell
./gradlew clean shadowJar
```

- The output file will be located at `build/libs`.
- When you build the plugin for the first time, it might take a while to download all the dependencies.

## 🛠️ Development builds

🤫 Want to be a few steps ahead of others? You can gather our freshly squeezed builds from GitHub
Actions [here](https://github.com/EternalCodeTeam/ParcelLockers/actions).

## :octocat: Contributing

🧩 Are you a developer and want to add something?

Create a [public fork of the repository](https://github.com/EternalCodeTeam/ParcelLockers/fork), modify it with
appropriate
changes and then create a Pull Request.
See [CONTRIBUTING.md](https://github.com/EternalCodeTeam/ParcelLockers/blob/master/.github/CONTRIBUTING.md) to find out
more.

## 🐛 Issues

Found a bug? Report it [here](https://github.com/EternalCodeTeam/ParcelLockers/issues). If you are not sure if that's an
intended behavior or not, feel free to ask on our [Discord](https://discord.com/invite/FQ7jmGBd6c) 😅

Additionally, if you enable automatic Sentry bug
reporting in your config.yml file (we strongly advise to do so), errors will be automatically sent to us, which will
greatly accelerate the bug fixing process 🥳

## 📜 License

### ParcelLockers is published under the GNU GPL-v3 license. In particular, the developer:

- Is freely allowed to modify and improve the code
- Can distribute production versions
- He must publish any changes made, ie. the public fork of this repository
- Cannot change the license or copyright

### Additionally:

- It is forbidden to modify or remove the code responsible for the compliance of production versions with the license.
- The authors of this project are not responsible for using the application, modifying and distributing it.
- Trademarks appearing in this project and this document belong to their rightful owners and are used for informational
  purposes only.

## ❤️ Other dependencies usages

- [LiteCommands (by Rollczi ❤️)](https://github.com/Rollczi/LiteCommands)
- [Adventure API & MiniMessage](https://docs.adventure.kyori.net/)
- [CDN Configs](https://github.com/dzikoysk/cdn)
- [PaperLib](https://github.com/PaperMC/PaperLib)
- [Lombok](https://projectlombok.org)
- [Spigot API](https://www.spigotmc.org/wiki/spigot-maven/#build-gradle)
- [expressible](https://github.com/dzikoysk/expressible)
- [panda-utilities](https://github.com/panda-lang/panda)
- [TriumphGUI](https://github.com/TriumphTeam/triumph-gui)
- [bStats](https://bstats.org/)
- [jackson-bukkit](https://github.com/eldoriarpg/jackson-bukkit)

## :heart: Special Thanks

[<img src="https://user-images.githubusercontent.com/65517973/210912946-447a6b9a-2685-4796-9482-a44bffc727ce.png" alt="JetBrains" width="150">](https://www.jetbrains.com)
![sentry.svg](assets/sentry.svg)

We express our gratitude to JetBrains for providing [Open Source Licenses](https://www.jetbrains.com/opensource/) for
their outstanding tools. We recommend using [IntelliJ IDEA](https://www.jetbrains.com/idea/) to work with our projects
and boost your productivity!

We also thank [Sentry](https://sentry.io) for providing us with
a [Open-Source Sponsorship plan](https://sentry.io/for/open-source/) for error tracking. We recommend using
[Sentry](https://sentry.io) to track
errors in
your projects and improve the quality of your software.


