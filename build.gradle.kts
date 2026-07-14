import net.minecrell.pluginyml.paper.PaperPluginDescription
import xyz.jpenilla.runtask.task.AbstractRun

plugins {
    id("java")
    id("de.eldoria.plugin-yml.paper") version "0.9.0"
    id("xyz.jpenilla.run-paper") version "3.0.2"
    id("com.gradleup.shadow") version "9.5.1"
    id("com.modrinth.minotaur") version "2.+"
}

group = "com.eternalcode"
version = "0.4.0-ALPHA"

repositories {
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
    compileOnly("io.papermc.paper:paper-api:${Versions.PAPER_API}")
    paperLibrary("dev.rollczi:litecommands-bukkit:${Versions.LITECOMMANDS}")
    paperLibrary("dev.rollczi:litecommands-adventure:${Versions.LITECOMMANDS}")

    // gui
    paperLibrary("dev.triumphteam:triumph-gui-paper:${Versions.TRIUMPH_GUI}")

    // configs
    paperLibrary("eu.okaeri:okaeri-configs-serdes-commons:${Versions.OKAERI_CONFIGS}")
    paperLibrary("eu.okaeri:okaeri-configs-serdes-bukkit:${Versions.OKAERI_CONFIGS}")
    paperLibrary("eu.okaeri:okaeri-configs-yaml-bukkit:${Versions.OKAERI_CONFIGS}")

    // gitcheck
    paperLibrary("com.eternalcode:gitcheck:${Versions.GITCHECK}")

    // metrics
    implementation("org.bstats:bstats-bukkit:${Versions.BSTATS}")

    // database
    paperLibrary("com.zaxxer:HikariCP:${Versions.HIKARICP}")
    paperLibrary("com.j256.ormlite:ormlite-jdbc:${Versions.ORMLITE}")
    paperLibrary("com.h2database:h2:${Versions.H2}")
    paperLibrary("org.postgresql:postgresql:${Versions.POSTGRESQL}")

    // lombok
    compileOnly("org.projectlombok:lombok:${Versions.LOMBOK}")
    annotationProcessor("org.projectlombok:lombok:${Versions.LOMBOK}")

    // jetbrains annotations
    compileOnly("org.jetbrains:annotations:${Versions.JETBRAINS_ANNOTATIONS}")

    // jackson-bukkit
    paperLibrary("de.eldoria.jacksonbukkit:paper:${Versions.JACKSON_BUKKIT}")

    // completable-futures
    paperLibrary("com.spotify:completable-futures:${Versions.COMPLETABLE_FUTURES}")

    // eternalcode commons
    paperLibrary("com.eternalcode:eternalcode-commons-adventure:${Versions.ETERNALCODE_COMMONS}")
    paperLibrary("com.eternalcode:eternalcode-commons-bukkit:${Versions.ETERNALCODE_COMMONS}")
    paperLibrary("com.eternalcode:eternalcode-commons-shared:${Versions.ETERNALCODE_COMMONS}")

    // multification
    paperLibrary("com.eternalcode:multification-bukkit:${Versions.MULTIFICATION}")
    paperLibrary("com.eternalcode:multification-okaeri:${Versions.MULTIFICATION}")

    // caffeine
    paperLibrary("com.github.ben-manes.caffeine:caffeine:${Versions.CAFFEINE}")

    // vault
    compileOnly("com.github.MilkBowl:VaultAPI:${Versions.VAULT_API}")

    // discord integration library
    paperLibrary("com.discord4j:discord4j-core:${Versions.DISCORD4J}")

    // discordsrv (optional integration)
    compileOnly("com.discordsrv:discordsrv:${Versions.DISCORDSRV}")

    testImplementation("org.junit.jupiter:junit-jupiter-api:${Versions.JUNIT}")
    testImplementation("org.junit.jupiter:junit-jupiter-params:${Versions.JUNIT}")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${Versions.JUNIT}")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    testImplementation("org.mockito:mockito-core:${Versions.MOCKITO}")

    testImplementation("org.testcontainers:junit-jupiter:${Versions.TESTCONTAINERS}")
    testImplementation("org.testcontainers:mysql:${Versions.TESTCONTAINERS}")
    testImplementation("mysql:mysql-connector-java:${Versions.MYSQL_CONNECTOR}")
    testImplementation("com.eternalcode:eternalcode-commons-shared:${Versions.ETERNALCODE_COMMONS}")

    testImplementation("io.papermc.paper:paper-api:${Versions.PAPER_API}")
    testImplementation("eu.okaeri:okaeri-configs-yaml-bukkit:${Versions.OKAERI_CONFIGS}")
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

tasks.withType<AbstractRun> {
    javaLauncher = javaToolchains.launcherFor {
        vendor = JvmVendorSpec.JETBRAINS
        languageVersion = JavaLanguageVersion.of(25)
    }
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
        minecraftVersion("26.1.2")
        downloadPlugins {
            modrinth("luckperms", "v5.5.17-bukkit")
            modrinth("vaultunlocked", "2.17.0")
            modrinth("essentialsx", "2.22.0")
//            modrinth("discordsrv", "1.30.4") // uncomment to test with DiscordSRV integration
        }
        jvmArgs("-Dcom.mojang.eula.agree=true", "-XX:+AllowEnhancedClassRedefinition")
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
