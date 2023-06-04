plugins {
    kotlin("multiplatform")
}

repositories {
    mavenCentral()
}

kotlin {
    jvm()

    sourceSets["commonMain"].dependencies {
        implementation(project(":utils"))
        implementation(project(":message-format"))
        implementation("io.github.microutils:kotlin-logging:3.0.2")
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
        implementation("dev.sitar:kio:1.1.1")
        implementation("io.ktor:ktor-network:2.1.3")
    }
}