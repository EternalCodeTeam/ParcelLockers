plugins {
    `java-library`
    id("de.eldoria.plugin-yml.paper") version "0.7.1"
    id("xyz.jpenilla.run-paper") version "3.0.0"
    id("com.gradleup.shadow") version "9.1.0"
}

group = "com.eternalcode"
version = "0.0.2-SNAPSHOT"

repositories {
    gradlePluginPortal()
    maven { url = uri("https://maven-central.storage-download.googleapis.com/maven2/")}

    maven { url = uri("https://repo.triumphteam.dev/snapshots/")}
    maven { url = uri("https://repo.papermc.io/repository/maven-public/") }
    maven { url = uri("https://repo.eternalcode.pl/releases") }
    maven { url  = uri("https://storehouse.okaeri.eu/repository/maven-public/") }
}

dependencies {
    // minecraft development api
    compileOnly("io.papermc.paper:paper-api:1.21.8-R0.1-SNAPSHOT")
    paperLibrary("dev.rollczi:litecommands-bukkit:3.10.5")
    paperLibrary("dev.rollczi:litecommands-adventure:3.10.5")

    // skull api
    paperLibrary("dev.rollczi:liteskullapi:2.0.0")

    // gui
    paperLibrary("dev.triumphteam:triumph-gui-paper:3.1.13-SNAPSHOT")
    paperLibrary("de.rapha149.signgui:signgui:2.5.4")

    // configs
    paperLibrary("eu.okaeri:okaeri-configs-serdes-commons:5.0.9")
    paperLibrary("eu.okaeri:okaeri-configs-serdes-bukkit:5.0.9")
    paperLibrary("eu.okaeri:okaeri-configs-yaml-bukkit:5.0.9")

    // gitcheck
    paperLibrary("com.eternalcode:gitcheck:1.0.0")

    // metrics and sentry
    implementation("org.bstats:bstats-bukkit:3.1.0")

    // database
    paperLibrary("com.zaxxer:HikariCP:7.0.2")
    paperLibrary("com.j256.ormlite:ormlite-jdbc:6.1")
    paperLibrary("com.h2database:h2:2.3.232")
    paperLibrary("org.postgresql:postgresql:42.7.7")

    // lombok
    compileOnly("org.projectlombok:lombok:1.18.40")
    annotationProcessor("org.projectlombok:lombok:1.18.40")

    // jetbrains annotations
    compileOnly("org.jetbrains:annotations:26.0.2")

    // jackson-bukkit
    paperLibrary("de.eldoria.jacksonbukkit:paper:1.2.0")

    // completable-futures
    paperLibrary("com.spotify:completable-futures:0.3.6")

    // eternalcode commons
    paperLibrary("com.eternalcode:eternalcode-commons-adventure:1.3.1")
    paperLibrary("com.eternalcode:eternalcode-commons-bukkit:1.3.1")
    paperLibrary("com.eternalcode:eternalcode-commons-shared:1.3.1")

    // multification
    paperLibrary("com.eternalcode:multification-bukkit:1.2.2")
    paperLibrary("com.eternalcode:multification-okaeri:1.2.2")

    // caffeine
    paperLibrary("com.github.ben-manes.caffeine:caffeine:3.2.2")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.13.4")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.13.4")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.13.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    testImplementation("org.testcontainers:junit-jupiter:1.21.3")
    testImplementation("org.testcontainers:mysql:1.21.3")
    testImplementation("mysql:mysql-connector-java:8.0.33")
    testImplementation("com.eternalcode:eternalcode-commons-shared:1.3.1")
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
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.setIncremental(true)
    options.compilerArgs.add("-parameters")
    options.release = 21
}

tasks {
    runServer {
        minecraftVersion("1.21.8")
        downloadPlugins.modrinth("luckperms", "v5.5.0-bukkit")
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
