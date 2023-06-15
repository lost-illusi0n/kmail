package dev.sitar.kmail.smtp

import dev.sitar.kmail.message.Message

public data class InternetMessage(
    val envelope: Envelope,
    val message: Message
)