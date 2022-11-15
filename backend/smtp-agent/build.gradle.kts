plugins {
    kotlin("multiplatform")
}

repositories {
    mavenCentral()
}

kotlin {
    jvm()

    sourceSets["commonMain"].dependencies {
        implementation(project(":smtp"))
        implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.0")
        implementation("io.ktor:ktor-network:2.1.3")
        implementation("io.ktor:ktor-network-tls:2.1.3")
        implementation("dev.sitar:kio:1.1.0")
        implementation("dev.sitar:dnskotlin:0.0.1")
    }
}