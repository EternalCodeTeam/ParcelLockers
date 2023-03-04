import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    `java-library`
    checkstyle
    id("net.minecrell.plugin-yml.bukkit") version "0.5.3"
    id("xyz.jpenilla.run-paper") version "2.0.1"
    id("com.github.johnrengelman.shadow") version "8.1.0"
    id("net.ltgt.errorprone") version "3.0.1"
}

group = "com.eternalcode"
version = "1.0.0-SNAPSHOT"
description =
    "Plugin that provides functionality of parcel lockers in Minecraft, allowing players to send and receive parcels safely."

repositories {
    gradlePluginPortal()
    mavenCentral()
    mavenLocal()

    maven { url = uri("https://repo.panda-lang.org/releases") }
    maven { url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") }
    maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots") }
    maven { url = uri("https://oss.sonatype.org/content/repositories/central") }
    maven { url = uri("https://papermc.io/repo/repository/maven-public/") }
    maven { url = uri("https://repo.eternalcode.pl/releases") }
    maven { url = uri("https://repository.minecodes.pl/releases") }
    maven { url = uri("https://storehouse.okaeri.eu/repository/maven-public/") }
}

dependencies {
    // minecraft development api
    compileOnly("org.spigotmc:spigot-api:1.19.3-R0.1-SNAPSHOT")
    implementation("net.kyori:adventure-platform-bukkit:4.2.0")
    implementation("net.kyori:adventure-text-minimessage:4.12.0")
    implementation("dev.rollczi.litecommands:bukkit-adventure:2.8.4")

    // database
    implementation("mysql:mysql-connector-java:8.0.32")

    // skull api
    implementation("dev.rollczi:liteskullapi:1.3.0")

    // gui library
    implementation("dev.triumphteam:triumph-gui:3.1.4")

    // CDN
    implementation("net.dzikoysk:cdn:1.14.4")

    // expressible
    implementation("org.panda-lang:expressible:1.3.1")

    // gitcheck
    implementation("com.eternalcode:gitcheck:1.0.0")

    // metrics
    implementation("org.bstats:bstats-bukkit:3.0.1")

    // lombok
    compileOnly("org.projectlombok:lombok:1.18.26")
    annotationProcessor("org.projectlombok:lombok:1.18.26")

    // jetbrains annotations
    api("org.jetbrains:annotations:24.0.1")

    // paperlib
    implementation("io.papermc:paperlib:1.0.8")

    // panda-utilities
    implementation("org.panda-lang:panda-utilities:0.5.2-alpha")

    // errorprone
    errorprone("com.google.errorprone:error_prone_core:2.18.0")

    // java discord api
    implementation("net.dv8tion:JDA:5.0.0-beta.5") {
        exclude(module = "opus-java")
    }
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

checkstyle {
    toolVersion = "10.8.0"

    configFile = file("${rootDir}/config/checkstyle/checkstyle.xml")

    maxErrors = 0
    maxWarnings = 0
}

bukkit {
    main = "com.eternalcode.parcellockers.ParcelLockers"
    apiVersion = "1.13"
    prefix = "ParcelLockers"
    author = "Jakubk15"
    name = "ParcelLockers"
    description =
        "Plugin that provides functionality of parcel lockers in Minecraft, allowing players to send and receive parcels safely."
    website = "https://github.com/EternalCodeTeam/ParcelLockers"
    version = "1.0.0-SNAPSHOT"
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

    val prefix = "com.eternalcode.parcellockers.libs"
    listOf(
        "panda",
        "org.panda_lang",
        "net.dzikoysk",
        "io.papermc.lib",
        "org.mineacademy",
        "eu.okaeri"
    ).forEach { pack ->
        relocate(pack, "$prefix.$pack")
    }
}

tasks {
    runServer {
        minecraftVersion("1.19.3")
    }
}

