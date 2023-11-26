plugins {
    kotlin("multiplatform")
    id("kotlinx-atomicfu")
}

repositories {
    mavenCentral()
    maven("https://s01.oss.sonatype.org/content/repositories/snapshots")
}

kotlin {
    jvm()

    sourceSets["commonMain"].dependencies {
        api(project(":pop3"))
        implementation(project(":utils"))
        implementation("io.github.microutils:kotlin-logging:3.0.2")

        implementation("dev.sitar:keystone:0.1-SNAPSHOT")
        implementation("dev.sitar:kio:1.1.2")
        implementation("io.ktor:ktor-network:2.1.3")
        implementation("io.ktor:ktor-network-tls:2.1.3")
        implementation("dev.sitar:dnskotlin:0.2.1")
    }
}