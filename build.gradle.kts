plugins {
    `java-library`
    id("de.eldoria.plugin-yml.bukkit") version "0.7.1"
    id("xyz.jpenilla.run-paper") version "3.0.0-beta.1"
    id("com.gradleup.shadow") version "9.0.2"
}

group = "com.eternalcode"
version = "0.0.2-SNAPSHOT"
description = "Plugin that provides functionality of parcel lockers in Minecraft, allowing players to send and receive parcels safely."

repositories {
    gradlePluginPortal()
    mavenCentral()

    maven { url = uri("https://repo.panda-lang.org/releases") }
    maven { url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") }
    maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots") }
    maven { url = uri("https://oss.sonatype.org/content/repositories/central") }
    maven { url = uri("https://repo.papermc.io/repository/maven-public/") }
    maven { url = uri("https://repo.eternalcode.pl/releases") }
    maven { url  = uri("https://storehouse.okaeri.eu/repository/maven-public/") }
}

dependencies {
    // minecraft development api
    compileOnly("io.papermc.paper:paper-api:1.21.8-R0.1-SNAPSHOT")
    implementation("dev.rollczi:litecommands-bukkit:3.10.4")
    implementation("dev.rollczi:litecommands-adventure:3.10.4")

    // skull api
    implementation("dev.rollczi:liteskullapi:2.0.0")

    // gui
    implementation("dev.triumphteam:triumph-gui:3.1.12")
    implementation("de.rapha149.signgui:signgui:2.5.4")

    // configs
    implementation("eu.okaeri:okaeri-configs-serdes-commons:5.0.9")
    implementation("eu.okaeri:okaeri-configs-serdes-bukkit:5.0.9")
    implementation("eu.okaeri:okaeri-configs-yaml-bukkit:5.0.9")

    // expressible
    implementation("org.panda-lang:expressible:1.3.6")

    // gitcheck
    implementation("com.eternalcode:gitcheck:1.0.0")

    // metrics and sentry
    implementation("org.bstats:bstats-bukkit:3.1.0")

    // database
    implementation("com.zaxxer:HikariCP:7.0.1")
    implementation("com.j256.ormlite:ormlite-jdbc:6.1")
    implementation("com.h2database:h2:2.3.232")
    implementation("org.postgresql:postgresql:42.7.7")

    // lombok
    compileOnly("org.projectlombok:lombok:1.18.38")
    annotationProcessor("org.projectlombok:lombok:1.18.38")

    // jetbrains annotations
    api("org.jetbrains:annotations:26.0.2")

    // panda-utilities
    implementation("org.panda-lang:panda-utilities:0.5.2-alpha")

    // jackson-bukkit
    implementation("de.eldoria.jacksonbukkit:paper:1.2.0")

    // completable-futures
    implementation("com.spotify:completable-futures:0.3.6")

    // eternalcode commons
    implementation("com.eternalcode:eternalcode-commons-adventure:1.3.0")
    implementation("com.eternalcode:eternalcode-commons-bukkit:1.3.0")

    // multification
    implementation("com.eternalcode:multification-bukkit:1.2.2")
    implementation("com.eternalcode:multification-okaeri:1.2.2")

    // caffeine
    implementation("com.github.ben-manes.caffeine:caffeine:3.2.2")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.13.4")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.13.4")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.13.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    testImplementation("org.testcontainers:junit-jupiter:1.21.3")
    testImplementation("org.testcontainers:mysql:1.21.3")
    testImplementation("mysql:mysql-connector-java:8.0.33")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

bukkit {
    name = "ParcelLockers"
    main = "com.eternalcode.parcellockers.ParcelLockers"
    version = project.version.toString()
    apiVersion = "1.21"
    description = project.description
    author = "EternalCodeTeam"
    website = "https://github.com/EternalCodeTeam/ParcelLockers"

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
    }

    test {
        useJUnitPlatform()
    }

    shadowJar {
        archiveFileName.set("ParcelLockers v${project.version}.jar")

        exclude(
            "org/intellij/lang/annotations/**",
            "org/jetbrains/annotations/**",
            "META-INF/**",
            "javax/**",
            "javassist/**",
            "org/h2/util/**"
        )

        mergeServiceFiles()
        minimize {
            exclude(dependency("de\\.rapha149\\.signgui:signgui:.*")) // https://github.com/Rapha149/SignGUI/issues/15
        }

        val relocationPrefix = "com.eternalcode.parcellockers.libs"
        listOf(
            "panda",
            "org.bstats",
            "org.json",
            "org.postgresql",
            "eu.okaeri",
            "net.kyori",
            "io.papermc",
            "io.sentry",
            "dev.rollczi",
            "de.eldoria",
            "com.eternalcode",
            "com.fasterxml",
            "com.j256",
            "com.spotify",
            "com.zaxxer",
            "de.rapha149",
            "dev.triumphteam",
            "com.github.benmanes.caffeine"
        ).forEach { relocate(it, "$relocationPrefix.$it") }
    }
}
