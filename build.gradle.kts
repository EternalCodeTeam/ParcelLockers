import net.minecrell.pluginyml.paper.PaperPluginDescription

plugins {
    id("java")
    id("de.eldoria.plugin-yml.paper") version "0.8.0"
    id("xyz.jpenilla.run-paper") version "3.0.2"
    id("com.gradleup.shadow") version "9.3.1"
    id("com.modrinth.minotaur") version "2.+"
}

group = "com.eternalcode"
version = "0.3.0-SNAPSHOT"

repositories {
    gradlePluginPortal()
    maven("https://maven-central.storage-download.googleapis.com/maven2/") // maven central mirror

    maven("https://repo.triumphteam.dev/snapshots/")
    maven("https://jitpack.io")
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.eternalcode.pl/releases")
    maven("https://storehouse.okaeri.eu/repository/maven-public/")
    maven("https://nexus.scarsz.me/content/groups/public/") // DiscordSRV
}

dependencies {
    // minecraft development api
    compileOnly("io.papermc.paper:paper-api:1.21.10-R0.1-SNAPSHOT")
    paperLibrary("dev.rollczi:litecommands-bukkit:3.10.9")
    paperLibrary("dev.rollczi:litecommands-adventure:3.10.9")

    // skull api
    paperLibrary("dev.rollczi:liteskullapi:2.0.0")

    // gui
    paperLibrary("dev.triumphteam:triumph-gui-paper:3.1.13")

    // configs
    paperLibrary("eu.okaeri:okaeri-configs-serdes-commons:5.0.13")
    paperLibrary("eu.okaeri:okaeri-configs-serdes-bukkit:5.0.13")
    paperLibrary("eu.okaeri:okaeri-configs-yaml-bukkit:5.0.13")

    // gitcheck
    paperLibrary("com.eternalcode:gitcheck:1.0.0")

    // metrics
    implementation("org.bstats:bstats-bukkit:3.1.0")

    // database
    paperLibrary("com.zaxxer:HikariCP:7.0.2")
    paperLibrary("com.j256.ormlite:ormlite-jdbc:6.1")
    paperLibrary("com.h2database:h2:2.4.240")
    paperLibrary("org.postgresql:postgresql:42.7.10")

    // lombok
    compileOnly("org.projectlombok:lombok:1.18.42")
    annotationProcessor("org.projectlombok:lombok:1.18.42")

    // jetbrains annotations
    compileOnly("org.jetbrains:annotations:26.0.2-1")

    // jackson-bukkit
    paperLibrary("de.eldoria.jacksonbukkit:paper:2.0.0")

    // completable-futures
    paperLibrary("com.spotify:completable-futures:0.3.6")

    // eternalcode commons
    paperLibrary("com.eternalcode:eternalcode-commons-adventure:1.3.2")
    paperLibrary("com.eternalcode:eternalcode-commons-bukkit:1.3.2")
    paperLibrary("com.eternalcode:eternalcode-commons-shared:1.3.2")

    // multification
    paperLibrary("com.eternalcode:multification-bukkit:1.2.4")
    paperLibrary("com.eternalcode:multification-okaeri:1.2.4")

    // caffeine
    paperLibrary("com.github.ben-manes.caffeine:caffeine:3.2.3")

    // vault
    compileOnly("com.github.MilkBowl:VaultAPI:1.7.1")

    // discord integration library
    paperLibrary("com.discord4j:discord4j-core:3.3.0")

    // discordsrv (optional integration)
    compileOnly("com.discordsrv:discordsrv:1.30.4")

    testImplementation("org.junit.jupiter:junit-jupiter-api:6.0.3")
    testImplementation("org.junit.jupiter:junit-jupiter-params:6.0.3")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:6.0.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    testImplementation("org.testcontainers:junit-jupiter:1.21.4")
    testImplementation("org.testcontainers:mysql:1.21.4")
    testImplementation("mysql:mysql-connector-java:8.0.33")
    testImplementation("com.eternalcode:eternalcode-commons-shared:1.3.2")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

paper {
    name = "ParcelLockers"
    main = "com.eternalcode.parcellockers.ParcelLockers"
    version = project.version.toString()
    apiVersion = "1.21"
    author = "EternalCodeTeam"
    website = "https://github.com/EternalCodeTeam/ParcelLockers"
    loader = "com.eternalcode.parcellockers.ParcelLockersLibraryLoader"
    generateLibrariesJson = true
    foliaSupported = false
    serverDependencies {
        register("Vault") {
            required = true
            load = PaperPluginDescription.RelativeLoadOrder.BEFORE
        }
        register("DiscordSRV") {
            required = false
            load = PaperPluginDescription.RelativeLoadOrder.BEFORE
        }
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.isIncremental = true
    options.compilerArgs.add("-parameters")
    options.release = 21
}

modrinth {
    token.set(providers.environmentVariable("MODRINTH_TOKEN"))
    projectId.set("parcellockers")
    versionNumber.set(project.version.toString())
    versionType.set(getVersionType(project.version.toString()))
    uploadFile.set(tasks.shadowJar)
    gameVersions.addAll("1.21.11")
    loaders.addAll("paper", "purpur")
    syncBodyFrom = rootProject.file("README.md").readText()
}

tasks {
    runServer {
        minecraftVersion("1.21.11")
        downloadPlugins {
            modrinth("luckperms", "v5.5.17-bukkit")
            modrinth("vaultunlocked", "2.17.0")
            modrinth("essentialsx", "2.21.2")
            modrinth("discordsrv", "1.30.4")
        }
    }

    test {
        useJUnitPlatform()
    }

    shadowJar {
        archiveFileName.set("ParcelLockers v${project.version}.jar")

        exclude(
            "org/intellij/lang/annotations/**",
            "org/jetbrains/annotations/**",
            "META-INF/**"
        )

        mergeServiceFiles()

        val relocationPrefix = "com.eternalcode.parcellockers.libs"

        listOf(
            "org.bstats"
        ).forEach { relocate(it, "$relocationPrefix.$it") }
    }
}

fun getVersionType(version: String): String {
    return when {
        version.contains("SNAPSHOT") -> "beta"
        version.contains("alpha", true) -> "alpha"
        version.contains("beta", true) -> "beta"
        else -> "release"
    }
}
