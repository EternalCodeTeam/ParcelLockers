plugins {
    id("de.eldoria.plugin-yml.paper") version "0.9.0" apply false
    id("xyz.jpenilla.run-paper") version "3.1.0" apply false
    id("com.gradleup.shadow") version "9.6.1" apply false
    id("com.modrinth.minotaur") version "2.+" apply false
}

allprojects {
    group = "com.eternalcode"
    version = "0.5.1-BETA"

    repositories {
        maven("https://maven-central.storage-download.googleapis.com/maven2/")
        maven("https://repo.triumphteam.dev/snapshots/")
        maven("https://jitpack.io")
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://repo.eternalcode.pl/releases")
        maven("https://storehouse.okaeri.eu/repository/maven-public/")
        maven("https://nexus.scarsz.me/content/groups/public/")
    }
}

subprojects {
    apply(plugin = "java")

    extensions.configure<JavaPluginExtension> {
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
}
