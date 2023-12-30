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

        api("dev.sitar:kio:1.1.3")
        api("io.ktor:ktor-io:2.2.4")
        api("io.ktor:ktor-network:2.1.3")
        api("io.ktor:ktor-network-tls:2.1.3")
    }

    sourceSets["jvmMain"].dependencies {
        implementation("io.netty:netty-all:4.1.101.Final")

    }
}