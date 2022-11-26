plugins {
    kotlin("multiplatform")
}

repositories {
    mavenCentral()
}

kotlin {
    jvm()

    sourceSets["commonMain"].dependencies {
        api("dev.sitar:kio:1.1.0")
        api("io.ktor:ktor-io:2.1.3")
    }
}