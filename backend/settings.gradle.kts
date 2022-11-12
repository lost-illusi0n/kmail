
rootProject.name = "kmail"

include("imap")
include("smtp")

// TODO: publish latest kio
includeBuild("../../kio") {
    dependencySubstitution {
        substitute(module("dev.sitar:kio")).using(project(":"))
    }
}