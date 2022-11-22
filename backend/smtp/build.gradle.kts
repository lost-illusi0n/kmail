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
        api(project(":message-format"))
        api("dev.sitar:kio:1.1.0")
    }
}