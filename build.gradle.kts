plugins {
    kotlin("jvm") version "2.2.0"
    application
}

group = "dev.toliner"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val lwjglVersion = "3.3.4"
val lwjglNatives = "natives-windows"

dependencies {
    // Core
    implementation(platform("org.lwjgl:lwjgl-bom:$lwjglVersion"))
    implementation("org.lwjgl:lwjgl")
    implementation("org.lwjgl:lwjgl-opengl")
    implementation("org.lwjgl:lwjgl-glfw")
    implementation("org.lwjgl:lwjgl-stb")
    implementation("org.lwjgl:lwjgl-nanovg")

    runtimeOnly("org.lwjgl:lwjgl::$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-opengl::$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-glfw::$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-stb::$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-nanovg::$lwjglNatives")

    // SVG (Apache Batik)
    implementation("org.apache.xmlgraphics:batik-transcoder:1.17")
    implementation("org.apache.xmlgraphics:batik-codec:1.17")

    // Testing
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
