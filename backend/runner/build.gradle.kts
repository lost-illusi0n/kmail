plugins {
    kotlin("multiplatform")
}

repositories {
    mavenCentral()
}

kotlin {
    jvm()

    sourceSets["commonMain"].dependencies {
        implementation("io.github.microutils:kotlin-logging:3.0.2")
        implementation("org.apache.logging.log4j:log4j-slf4j2-impl:2.19.0")
        implementation("org.apache.logging.log4j:log4j-core:2.19.0")
        implementation("org.apache.logging.log4j:log4j-api:2.19.0")

        implementation("io.ktor:ktor-network-tls-certificates:2.1.3")

        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
        implementation(project(":smtp-agent"))
    }
}