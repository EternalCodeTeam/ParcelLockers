import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    `java-library`
    id("net.minecrell.plugin-yml.bukkit") version "0.5.2"
    id("xyz.jpenilla.run-paper") version "2.0.1"
    id("com.github.johnrengelman.shadow") version "7.1.2"
    id("net.ltgt.errorprone") version "3.0.1"
}

repositories {
    mavenLocal()
    mavenCentral()
    gradlePluginPortal()

    maven("https://jitpack.io")
    maven("https://repo.panda-lang.org/releases")
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://oss.sonatype.org/content/repositories/snapshots")
    maven("https://oss.sonatype.org/content/repositories/central")
    maven("https://papermc.io/repo/repository/maven-public/")
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:1.19.3-R0.1-SNAPSHOT")
    implementation("com.github.kangarko:Foundation:6.2.8") {
        exclude(group = "org.mineacademy.plugin", module = "*")
        exclude(group = "org.spigotmc", module = "spigot-api")
        exclude(group = "org.bukkit")
        exclude(group = "org.projectlombok")
    }
    implementation("org.panda-lang:expressible:1.3.0")
    compileOnly("org.projectlombok:lombok:1.18.24")
    annotationProcessor("org.projectlombok:lombok:1.18.24")
    implementation("io.papermc:paperlib:1.0.8")
    implementation("org.panda-lang:panda-utilities:0.5.2-alpha")
    errorprone("com.google.errorprone:error_prone_core:2.18.0")
}

apply(plugin = "com.github.johnrengelman.shadow")
apply(plugin = "net.minecrell.plugin-yml.bukkit")
apply(plugin = "xyz.jpenilla.run-paper")
apply(plugin = "net.ltgt.errorprone")

group = "xyz.jakubk15"
version = "1.0.0"
description = "ParcelLockers"

bukkit {
    main = "xyz.jakubk15.parcellockers.ParcelLockersPlugin"
    apiVersion = "1.13"
    prefix = "ParcelLockersPlugin"
    author = "Jakubk15"
    name = "ParcelLockers"
    description = "Plugin that provides functionality of parcel lockers in Minecraft, allowing players to send and receive parcels safely."
    website = "https://github.com/Jakubk15/ParcelLockers"
    version = "${project.version}"
    softDepend = listOf(
        "AuthMe",
        "BanManager",
        "BungeeChatAPI",
        "CMI",
        "DiscordSRV",
        "Factions",
        "Feudal",
        "ItemsAdder",
        "Essentials",
        "LegacyFactions",
        "Lands",
        "LuckPerms",
        "Multiverse-Core",
        "MVdWPlaceholderAPI",
        "MythicMobs",
        "mcMMO",
        "NashornPlus",
        "Nicky",
        "PlaceholderAPI",
        "ProtocolLib",
        "SimpleClans",
        "Towny",
        "TownyChat",
        "Vault",
        "WorldEdit")
    libraries = listOf(
        "org.openjdk.nashorn:nashorn-core:15.4"
    )
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))

}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.withType<ShadowJar> {
    archiveFileName.set("ParcelLockers v${project.version} (MC 1.8.8-1.19.x).jar")

    exclude(
        "org/intellij/lang/annotations/**",
        "org/jetbrains/annotations/**",
        "META-INF/**",
        "javax/**",
    )

    mergeServiceFiles()
    minimize()

    val prefix = "xyz.jakubk15.parcellockers.lib"
    listOf(
        "panda",
        "org.panda_lang",
        "net.dzikoysk",
        "io.papermc.lib",
        "org.mineacademy"
    ).forEach { pack ->
        relocate(pack, "$prefix.$pack")
    }
}

tasks {
    runServer {
        minecraftVersion("1.19.3")
    }
}
