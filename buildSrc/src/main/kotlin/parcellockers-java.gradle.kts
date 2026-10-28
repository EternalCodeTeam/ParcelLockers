plugins {
    java
}

group = "com.eternalcode"
version = "0.5.1-BETA"

repositories {
    maven("https://maven-central.storage-download.googleapis.com/maven2/") // maven central mirror
    maven("https://repo.triumphteam.dev/snapshots/")
    maven("https://jitpack.io")
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.eternalcode.pl/releases")
    maven("https://storehouse.okaeri.eu/repository/maven-public/")
    maven("https://nexus.scarsz.me/content/groups/public/") // DiscordSRV
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.isIncremental = true
    options.compilerArgs.add("-parameters")
    options.release = 21
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
