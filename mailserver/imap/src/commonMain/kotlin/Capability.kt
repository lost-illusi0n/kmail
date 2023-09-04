package dev.sitar.kmail.imap

import dev.sitar.kmail.sasl.SaslMechanism

sealed class Capability(val value: String) {
    object Imap4Rev1: Capability("IMAP4rev1")
    object StartTls: Capability("STARTTLS")
    object LoginDisabled: Capability("LOGINDISABLED")
    object Notify: Capability("NOTIFY")
    object Idle: Capability("IDLE")
    class Auth(val mechanism: SaslMechanism): Capability("AUTH=${mechanism.mechanism}")
}