plugins {
    `java-library`
    checkstyle
    id("net.minecrell.plugin-yml.bukkit") version "0.6.0"
    id("xyz.jpenilla.run-paper") version "2.3.0"
    id("io.github.goooler.shadow") version "8.1.8"
}

group = "com.eternalcode"
version = "1.0.0-SNAPSHOT"
description =
    "Plugin that provides functionality of parcel lockers in Minecraft, allowing players to send and receive parcels safely."

repositories {
    gradlePluginPortal()
    mavenCentral()

    maven { url = uri("https://repo.panda-lang.org/releases") }
    maven { url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") }
    maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots") }
    maven { url = uri("https://oss.sonatype.org/content/repositories/central") }
    maven { url = uri("https://papermc.io/repo/repository/maven-public/") }
    maven { url = uri("https://repo.eternalcode.pl/releases") }
    maven { url = uri("https://repository.minecodes.pl/releases") }
}

dependencies {
    // minecraft development api
    compileOnly("org.spigotmc:spigot-api:1.21-R0.1-SNAPSHOT")
    implementation("net.kyori:adventure-platform-bukkit:4.3.3")
    implementation("net.kyori:adventure-text-minimessage:4.17.0")
    implementation("dev.rollczi:litecommands-bukkit:3.1.0")
    implementation("dev.rollczi:litecommands-adventure:3.1.0")

    // skull api
    implementation("dev.rollczi:liteskullapi:1.3.0")

    // gui library
    implementation("dev.triumphteam:triumph-gui:3.1.10")

    // economy
    compileOnly("com.github.MilkBowl:VaultAPI:1.7.1")

    // CDN
    implementation("net.dzikoysk:cdn:1.14.4")

    // expressible
    implementation("org.panda-lang:expressible:1.3.6")

    // gitcheck
    implementation("com.eternalcode:gitcheck:1.0.0")

    // metrics and sentry
    implementation("org.bstats:bstats-bukkit:3.0.2")
    implementation("io.sentry:sentry:7.12.0")

    // database
    implementation("com.zaxxer:HikariCP:5.1.0")

    // lombok
    compileOnly("org.projectlombok:lombok:1.18.34")
    annotationProcessor("org.projectlombok:lombok:1.18.34")

    // jetbrains annotations
    api("org.jetbrains:annotations:24.1.0")

    // paperlib
    implementation("io.papermc:paperlib:1.0.8")

    // signgui
    implementation("de.rapha149.signgui:signgui:2.3.5")

    // panda-utilities
    implementation("org.panda-lang:panda-utilities:0.5.2-alpha")

    // jackson-bukkit
    implementation("de.eldoria.jacksonbukkit:paper:1.2.0")

    // completable-futures
    implementation("com.spotify:completable-futures:0.3.6")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.2")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.10.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.2")

    testImplementation("org.testcontainers:junit-jupiter:1.20.0")
    testImplementation("org.testcontainers:mysql:1.20.0")
    testImplementation("mysql:mysql-connector-java:8.0.33")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

checkstyle {
    toolVersion = "10.17.0"

    configFile = file("${rootDir}/config/checkstyle/checkstyle.xml")

    maxErrors = 0
    maxWarnings = 0
}

bukkit {
    main = "com.eternalcode.parcellockers.ParcelLockers"
    apiVersion = "1.13"
    prefix = "ParcelLockers"
    author = "EternalCodeTeam"
    name = "ParcelLockers"
    description =
        "Plugin that provides functionality of parcel lockers in Minecraft, allowing players to send and receive parcels safely."
    website = "https://github.com/EternalCodeTeam/ParcelLockers"
    version = "1.0.0-SNAPSHOT"
    depend = listOf("Vault")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.setIncremental(true)
    options.compilerArgs.add("-parameters")
    options.release = 17
}

tasks {
    runServer {
        minecraftVersion("1.21")
        downloadPlugins.url("https://github.com/MilkBowl/Vault/releases/download/1.7.3/Vault.jar")
        downloadPlugins.url("https://github.com/EssentialsX/Essentials/releases/download/2.20.1/EssentialsX-2.20.1.jar")
    }

    test {
        useJUnitPlatform()
    }

    shadowJar {
        archiveFileName.set("ParcelLockers v${project.version} (MC 1.8.8-1.20.x).jar")

        exclude(
            "org/intellij/lang/annotations/**",
            "org/jetbrains/annotations/**",
            "META-INF/**",
            "javax/**",
        )

        mergeServiceFiles()
        minimize {
            exclude(dependency("de\\.rapha149\\.signgui:signgui:.*")) // https://github.com/Rapha149/SignGUI/issues/15
        }

        val prefix = "com.eternalcode.parcellockers.libs"
        listOf(
            "panda",
            "org.panda_lang",
            "net.dzikoysk",
            "io.papermc.lib",
            "org.bstats",
            "dev.rollczi",
            "net.kyori",
            "org.json",
            "com.fasterxml",
            "de.rapha149"
        ).forEach { relocate(it, prefix) }
    }
}
