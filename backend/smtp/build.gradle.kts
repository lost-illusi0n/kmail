plugins {
    kotlin("multiplatform")
}

repositories {
    mavenCentral()
}

kotlin {
    jvm()

    explicitApi()

    sourceSets["commonMain"].dependencies {
        implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.0")
        implementation("io.ktor:ktor-network:2.1.3")
        implementation("dev.sitar:kio:1.1.0")
    }
}