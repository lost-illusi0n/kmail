
rootProject.name = "kmail-mailserver"

include("pop3")
include("imap")
include("smtp")

include("sasl")
include("message-format")

include("pop3-agent")
include("imap-agent")
include("smtp-agent")

include("runner")

include("utils")

pluginManagement {
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "kotlinx-atomicfu") useModule("org.jetbrains.kotlinx:atomicfu-gradle-plugin:0.18.5")
        }
    }
}

// TODO: publish latest kio
includeBuild("../../kio") {
    dependencySubstitution {
        substitute(module("dev.sitar:kio:1.1.2")).using(project(":"))
    }
}