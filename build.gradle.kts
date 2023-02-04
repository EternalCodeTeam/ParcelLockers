import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    `java-library`
    application
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
    // spigot-api
    compileOnly("org.spigotmc:spigot-api:1.19.3-R0.1-SNAPSHOT")
    // foundation
    implementation("com.github.kangarko:Foundation:6.2.9") {
        exclude(group = "org.mineacademy.plugin", module = "*")
        exclude(group = "org.spigotmc", module = "spigot-api")
        exclude(group = "org.bukkit")
        exclude(group = "org.projectlombok")
    }
    // expressible
    implementation("org.panda-lang:expressible:1.3.1")
    // lombok
    compileOnly("org.projectlombok:lombok:1.18.26")
    annotationProcessor("org.projectlombok:lombok:1.18.26")
    // jetbrains annotations
    compileOnly("org.jetbrains:annotations:24.0.0")
    // paperlib
    implementation("io.papermc:paperlib:1.0.8")
    // panda-utilities
    implementation("org.panda-lang:panda-utilities:0.5.2-alpha")
    // errorprone
    errorprone("com.google.errorprone:error_prone_core:2.18.0")
    // java discord api
    implementation("net.dv8tion:JDA:5.0.0-beta.3") {
        exclude(module = "opus-java")
    }
}

group = "xyz.jakubk15"
version = "1.0.0"
description = "ParcelLockers"

bukkit {
    main = "xyz.jakubk15.parcellockers.ParcelLockersPlugin"
    apiVersion = "1.13"
    prefix = "ParcelLockers"
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
