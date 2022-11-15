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
        implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.0")
        implementation("dev.sitar:kio")
    }
}