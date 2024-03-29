
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