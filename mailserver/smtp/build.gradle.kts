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
        api(project(":utils"))
        api("dev.sitar:kio:1.1.3")
        api("dev.sitar:dnskotlin:0.2.1")
        implementation("io.ktor:ktor-utils:2.2.4")
    }
}