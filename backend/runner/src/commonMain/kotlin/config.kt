package dev.sitar.kmail.runner

data class KmailConfig(
    val domain: String,
    val keystorePassword: String?,
)

// TODO: read this from filesystem
val CONFIGURATION: KmailConfig = KmailConfig("0.0.0.0", "changeme")