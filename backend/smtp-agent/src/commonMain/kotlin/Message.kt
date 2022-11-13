package dev.sitar.kmail.smtp.agent

import dev.sitar.kmail.smtp.*

// TODO: create a module to parse and model email according to rfc 5322
data class Message(
    val ehlo: EhloCommand,
    val mail: MailCommand,
    val rcpt: RecipientCommand,
    val message: MessageCommand
)

val Message.queueId: String get() = hashCode().toString(16)