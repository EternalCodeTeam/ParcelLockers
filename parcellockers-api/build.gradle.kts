plugins {
    id("parcellockers-java")
    `java-library`
    `maven-publish`
}

dependencies {
    compileOnlyApi("io.papermc.paper:paper-api:${Versions.PAPER_API}")
    api("org.jspecify:jspecify:${Versions.JSPECIFY}")

    testImplementation("org.junit.jupiter:junit-jupiter-api:${Versions.JUNIT}")
    testRuntimeOnly("io.papermc.paper:paper-api:${Versions.PAPER_API}")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${Versions.JUNIT}")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    withSourcesJar()
    withJavadocJar()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = "parcellockers-api"
            from(components["java"])
        }
    }

    repositories {
        mavenLocal()
        maven {
            val snapshot = version.toString().endsWith("-SNAPSHOT")
            name = if (snapshot) "eternalcodeSnapshots" else "eternalcodeReleases"
            url = uri(if (snapshot) "https://repo.eternalcode.pl/snapshots" else "https://repo.eternalcode.pl/releases")
            credentials {
                username = System.getenv("ETERNAL_CODE_MAVEN_USERNAME")
                password = System.getenv("ETERNAL_CODE_MAVEN_PASSWORD")
            }
        }
    }
}
