package dev.sitar.kmail.runner

import dev.sitar.kmail.smtp.Domain

data class KmailConfig(
    val domain: Domain,
    val keystorePassword: String?,
)

// TODO: read this from filesystem
val CONFIGURATION: KmailConfig = KmailConfig(Domain.fromText("[0.0.0.0]")!!, "changeme")