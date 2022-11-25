plugins {
    kotlin("multiplatform")
    id("kotlinx-atomicfu")
}

repositories {
    mavenCentral()
}

kotlin {
    jvm()

    sourceSets["commonMain"].dependencies {
        api(project(":smtp"))
        implementation("io.github.microutils:kotlin-logging:3.0.2")

        implementation("dev.sitar:kio:1.1.0")
        implementation("io.ktor:ktor-network:2.1.3")
        implementation("io.ktor:ktor-network-tls:2.1.3")
        implementation("dev.sitar:dnskotlin:0.0.1")
    }
}