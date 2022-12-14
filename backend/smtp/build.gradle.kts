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
        implementation(project(":utils"))
        api("dev.sitar:kio:1.1.0")
        api("dev.sitar:dnskotlin:0.0.1")
        api("io.ktor:ktor-io:2.1.3")
        implementation("io.ktor:ktor-utils:2.1.3")
    }
}