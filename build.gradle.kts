plugins {
    kotlin("jvm") version "2.4.0"
    kotlin("plugin.serialization") version "2.4.0"
    id("com.gradleup.shadow") version "9.5.1"
    application
}

group = "org.fowo"
version = "1.0-SNAPSHOT"

kotlin {
    jvmToolchain(21)
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("com.github.ajalt.clikt:clikt:5.0.1")
    implementation("com.github.ajalt.clikt:clikt-markdown:5.0.1")
    
    // Phase 2 dependencies
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    implementation("io.github.z4kn4fein:semver:3.1.0")
    implementation("org.ow2.sat4j:org.ow2.sat4j.core:2.3.6")
}

application {
    mainClass.set("fowo.cli.MainKt")
}

tasks.shadowJar {
    archiveBaseName.set("fowo")
    archiveClassifier.set("all")
    archiveVersion.set("")
    manifest {
        attributes["Main-Class"] = "fowo.cli.MainKt"
    }
}
