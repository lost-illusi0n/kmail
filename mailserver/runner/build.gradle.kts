import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    application
    kotlin("jvm")
    kotlin("plugin.serialization")
}

repositories {
    mavenCentral()
    maven("https://s01.oss.sonatype.org/content/repositories/snapshots")
    maven("https://jitpack.io")
}

application {
    mainClass.set("dev.sitar.kmail.runner.LauncherJvmKt")
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation("io.github.microutils:kotlin-logging:3.0.2")
    implementation("org.apache.logging.log4j:log4j-slf4j2-impl:2.19.0")
    implementation("org.apache.logging.log4j:log4j-core:2.19.0")
    implementation("org.apache.logging.log4j:log4j-api:2.19.0")

    implementation("io.ktor:ktor-network-tls-certificates:2.1.3")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("aws.sdk.kotlin:s3:0.32.0-beta")

    implementation(project(":smtp-agent"))
    implementation(project(":smtp"))
    implementation(project(":imap-agent"))
    implementation(project(":pop3-agent"))

    // JAVA
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-toml:2.15.2")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.2")
    implementation("org.bouncycastle:bcprov-jdk18on:1.77")
}