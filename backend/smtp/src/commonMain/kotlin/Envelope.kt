package dev.sitar.kmail.smtp


public data class Envelope(
    val originatorAddress: Path,
    val recipientAddresses: List<Path>,
)