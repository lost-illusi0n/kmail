package dev.sitar.kmail.smtp

import io.ktor.util.*

public sealed interface SaslMechanism {
    public fun encode(): String
}

public data class PlainSaslMechanism(
    val authorizationIdentity: String,
    val authenticationIdentity: String,
    val password: String
) : SaslMechanism {
    public override fun encode(): String {
        return "$authorizationIdentity\u0000$authenticationIdentity\u0000$password".encodeBase64()
    }
}

public fun decodePlainSaslMechanism(encoded: String): PlainSaslMechanism {
    val parts = encoded.decodeBase64String().split('\u0000')
    require(parts.size == 3)

    return PlainSaslMechanism(parts[0], parts[1], parts[2])
}