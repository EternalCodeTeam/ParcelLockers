plugins {
    `java-library`
}

dependencies {
    compileOnlyApi("io.papermc.paper:paper-api:${Versions.PAPER_API}")
    api("org.jspecify:jspecify:${Versions.JSPECIFY}")

    testImplementation("org.junit.jupiter:junit-jupiter-api:${Versions.JUNIT}")
    testRuntimeOnly("io.papermc.paper:paper-api:${Versions.PAPER_API}")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${Versions.JUNIT}")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
