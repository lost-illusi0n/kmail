plugins {
    kotlin("multiplatform")
}

repositories {
    mavenCentral()
}

kotlin {
    jvm()
//    linuxX64()

    sourceSets["commonMain"].dependencies {
        api("org.jetbrains.kotlinx:kotlinx-datetime:0.4.0")
        implementation("dev.sitar:kio:1.1.1")
    }
}