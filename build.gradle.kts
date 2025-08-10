plugins {
    kotlin("jvm") version "2.2.0"
    application
}

group = "dev.toliner"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation("io.kotest:kotest-runner-junit5:5.9.1")
    testImplementation("io.kotest:kotest-assertions-core:5.9.1")
    testImplementation("io.kotest:kotest-property:5.9.1")
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("dev.toliner.survivorgame.MainKt")
}

tasks {
    test {
        useJUnitPlatform()
    }
    named<UpdateDaemonJvm>("updateDaemonJvm") {
        languageVersion = JavaLanguageVersion.of(21)
    }
}
