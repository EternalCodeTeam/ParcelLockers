plugins {
    `java-library`
    id("net.minecrell.plugin-yml.bukkit") version "0.6.0"
    id("xyz.jpenilla.run-paper") version "2.3.1"
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
}

dependencies {
    // minecraft development api
    compileOnly("org.spigotmc:spigot-api:1.21.7-R0.1-SNAPSHOT")
    implementation("net.kyori:adventure-platform-bukkit:4.4.1")
    implementation("net.kyori:adventure-text-minimessage:4.24.0")
    implementation("dev.rollczi:litecommands-bukkit:3.10.4")
    implementation("dev.rollczi:litecommands-adventure:3.10.4")

    // skull api
    implementation("dev.rollczi:liteskullapi:2.0.0")

    // gui
    implementation("dev.triumphteam:triumph-gui:3.1.11")
    implementation("de.rapha149.signgui:signgui:2.5.4")

    // CDN
    implementation("net.dzikoysk:cdn:1.14.9")

    // expressible
    implementation("org.panda-lang:expressible:1.3.6")

    // gitcheck
    implementation("com.eternalcode:gitcheck:1.0.0")

    // metrics and sentry
    implementation("org.bstats:bstats-bukkit:3.1.0")
    implementation("io.sentry:sentry:8.19.1")

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

    // paperlib
    implementation("io.papermc:paperlib:1.0.8")

    // panda-utilities
    implementation("org.panda-lang:panda-utilities:0.5.2-alpha")

    // jackson-bukkit
    implementation("de.eldoria.jacksonbukkit:paper:1.2.0")

    // completable-futures
    implementation("com.spotify:completable-futures:0.3.6")

    // eternalcode-commons
    implementation("com.eternalcode:eternalcode-commons-adventure:1.3.0")
    implementation("com.eternalcode:eternalcode-commons-bukkit:1.3.0")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.13.4")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.13.4")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.13.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.testcontainers:junit-jupiter:1.21.3")
    testImplementation("org.testcontainers:mysql:1.21.3")
    testImplementation("mysql:mysql-connector-java:8.0.33")

    testImplementation("org.spigotmc:spigot-api:1.21.5-R0.1-SNAPSHOT")
    testImplementation("net.kyori:adventure-platform-bukkit:4.4.1")
    testImplementation("net.kyori:adventure-text-minimessage:4.24.0")
    testImplementation("net.dzikoysk:cdn:1.14.9")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

bukkit {
    main = "com.eternalcode.parcellockers.ParcelLockers"
    apiVersion = "1.13"
    prefix = "ParcelLockers"
    author = "EternalCodeTeam"
    name = "ParcelLockers"
    description = project.description
    website = "https://github.com/EternalCodeTeam/ParcelLockers"
    version = project.version.toString()
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.setIncremental(true)
    options.compilerArgs.add("-parameters")
    options.release = 21
}

tasks {
    runServer {
        minecraftVersion("1.21.7")
    }

    clean {
        doLast {
            project.file("run/plugins").deleteRecursively()
            project.file("run/logs").deleteRecursively()
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
            "net.dzikoysk",
            "net.kyori",
            "io.papermc",
            "io.sentry",
            "dev.rollczi",
            "de.eldoria",
            "com.eternalcode.commons",
            "com.eternalcode.gitcheck",
            "com.fasterxml",
            "com.j256",
            "com.spotify",
            "com.zaxxer",
            "de.rapha149",
            "dev.triumphteam"
        ).forEach { relocate(it, "$relocationPrefix.$it") }
    }
}



