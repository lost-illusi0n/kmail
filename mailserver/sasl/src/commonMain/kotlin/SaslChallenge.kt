package dev.sitar.kmail.sasl

sealed interface SaslChallenge {
    data class Plain(
        val authorizationIdentity: String?,
        val authenticationIdentity: String,
        val password: String
    ) : SaslChallenge {
        companion object {
            fun fromString(challenge: String): Plain {
                val parts = challenge.split(Char(0))

                return when (parts.size) {
                    2 -> Plain(null, parts[0], parts[1])
                    3 -> Plain(parts[0], parts[1], parts[2])
                    else -> throw IllegalArgumentException("too many parts for plain challenge.")
                }
            }
        }
    }
}