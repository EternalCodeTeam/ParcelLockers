# ParcelLockers

### ParcelLockers is a user plugin, dedicated for Spigot-based servers.

<div align="center">

[![Supports Paper](https://raw.githubusercontent.com/intergrav/devins-badges/v3/assets/cozy/supported/paper_vector.svg)](https://papermc.io)
[![Supports Spigot](https://raw.githubusercontent.com/intergrav/devins-badges/v3/assets/cozy/supported/spigot_vector.svg)](https://spigotmc.org)

[![Patreon](https://raw.githubusercontent.com/intergrav/devins-badges/v3/assets/cozy/donate/patreon-plural_vector.svg)](https://www.patreon.com/eternalcode)
[![Website](https://raw.githubusercontent.com/intergrav/devins-badges/v3/assets/cozy/documentation/website_vector.svg)](https://eternalcode.pl/)
[![Discord](https://raw.githubusercontent.com/intergrav/devins-badges/v3/assets/cozy/social/discord-plural_vector.svg)](https://discord.gg/FQ7jmGBd6c)

[![Gradle](https://raw.githubusercontent.com/intergrav/devins-badges/v3/assets/cozy/built-with/gradle_vector.svg)](https://gradle.org/)
[![Java](https://raw.githubusercontent.com/intergrav/devins-badges/v3/assets/cozy/built-with/java17_vector.svg)](https://www.java.com/)

</div>

# Warning ❗

ParcelLockers is at an very early stage of development, and you use it at your own risk.

# Building

To build ParcelLockers execute this command using Gradle (Make sure you are using JDK 8 for maximum compatibility)

`./gradlew clean shadowJar`

When you build it for the first time, it may take a little longer.

# Development builds

You can gather the development builds from [GitHub Actions](https://github.com/EternalCodeTeam/ParcelLockers/actions)

# Contributing

Create a [public fork of the repository](https://github.com/EternalCodeTeam/ParcelLockers/fork), modify it with
appropriate
changes and then create a Pull Request.
See [CONTRIBUTING.md](https://github.com/EternalCodeTeam/ParcelLockers/blob/master/.github/CONTRIBUTING.md) to find out
more.

## TODO
- [ ] Add support for more databases (H2, PostgreSQL, MongoDB?)
- [ ] Add translation system, more languages
- [ ] Add delivery codes, so any person knowing the code can pick up the parcel
- [ ] Add delivery time, so the parcel can be picked up only in a specific time
- [ ] Rewrite database to ORMLite (far in future)
- [ ] Add "business" logic
- [ ] Rewrite Position class to use Integers instead of Doubles 
- [ ] Finish GUIs
- [ ] Add more GUIs (admin GUI, parcel create GUI, etc.)
- [ ] Add Discord integration (bot, webhooks, etc...)
- [ ] Add more commands
- [ ] Isolate cache into separate classes
- [ ] Extract database service methods into repositories (interfaces)
- [ ] Delete cyclic dependency (Parcel <-> ParcelLocker), switch to UUID instead of object references

# License

#### ParcelLockers is published under the GNU GPL-v3 license. In particular, the developer:

- Is freely allowed to modify and improve the code
- Can distribute production versions
- He must publish any changes made, ie. the public fork of this repository
- Cannot change the license or copyright

#### Additionally:

- It is forbidden to modify or remove the code responsible for the compliance of production versions with the license.
- The authors of this project are not responsible for using the application, modifying and distributing it.
- Trademarks appearing in this project and this document belong to their rightful owners and are used for informational
  purposes only.

# Other dependencies usages

- [LiteCommands (by Rollczi ❤️)](https://github.com/Rollczi/LiteCommands)
- [Adventure API & MiniMessage](https://docs.adventure.kyori.net/)
- [CDN Configs](https://github.com/dzikoysk/cdn)
- [PaperLib](https://github.com/PaperMC/PaperLib)
- [Lombok](https://projectlombok.org)
- [Spigot API](https://www.spigotmc.org/wiki/spigot-maven/#build-gradle)
- [expressible](https://github.com/dzikoysk/expressible)
- [panda-utilities](https://github.com/panda-lang/panda)
- [Errorprone](https://github.com/google/error-prone)
- [TriumphGUI](https://github.com/TriumphTeam/triumph-gui)
- [bStats](https://bstats.org/)

ParcelLockers is a advanced user delivery plugin, dedicated for Spigot-based servers.

🚫 Found an issue? Report it [here](https://github.com/EternalCodeTeam/ParcelLockers/issues).

🧩 Are you a developer and want to add something? Feel free
to [create a pull request](https://github.com/EternalCodeTeam/ParcelLockers/pulls).
