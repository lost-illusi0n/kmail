plugins {
    kotlin("jvm") version "1.7.20" apply false
    kotlin("multiplatform") version "1.7.20" apply false
    id("kotlinx-atomicfu") apply false
}

group = "dev.sitar"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}