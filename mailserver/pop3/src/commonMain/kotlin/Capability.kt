package dev.sitar.kmail.pop3

sealed class Capability(val value: String) {
    object Top : Capability("TOP")

    object User : Capability("USER")

    data class Sasl(val mechanisms: List<String>) : Capability("SASL ${mechanisms.joinToString(" ")}")

    object ResponseCodes : Capability("RESP-CODES")

    class LoginDelay(val delay: Int) : Capability("LOGIN-DELAY $delay")

    object Pipelining : Capability("PIPELINING")

    data class Expire(val expire: Int) : Capability("EXPIRE $expire")

    object Uidl : Capability("UIDL")

    data class Implementation(val implementation: String) : Capability("IMPLEMENTATION $implementation")
}