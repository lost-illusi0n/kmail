
rootProject.name = "kmail"

include("imap")
include("smtp")
include("message-format")

include("smtp-agent")
include("imap-agent")

include("runner")

include("utils")

// TODO: publish dnskotlin
includeBuild("../../dnskotlin") {
    dependencySubstitution {
        substitute(module("dev.sitar:dnskotlin")).using(project(":"))
    }
}

// TODO: publish latest kio
includeBuild("../../kio") {
    dependencySubstitution {
        substitute(module("dev.sitar:kio")).using(project(":"))
    }
}

pluginManagement {
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "kotlinx-atomicfu") useModule("org.jetbrains.kotlinx:atomicfu-gradle-plugin:0.18.5")
        }
    }
}