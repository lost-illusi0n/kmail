package dev.sitar.kmail.smtp


// TODO: support multiple recipients
public data class Envelope(
    val originatorAddress: String,
    val recipientAddresses: List<String>,
)