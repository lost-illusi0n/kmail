
rootProject.name = "kmail"

include("imap")
include("smtp")
include("message-format")

include("smtp-agent")

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