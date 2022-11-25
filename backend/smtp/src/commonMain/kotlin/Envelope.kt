package dev.sitar.kmail.smtp


// TODO: support multiple recipients
public data class Envelope(
    val originatorAddress: Path,
    val recipientAddresses: List<Path>,
)