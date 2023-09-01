package dev.sitar.kmail.sasl

sealed class SaslMechanism(val mechanism: String) {
    object Plain: SaslMechanism("PLAIN") // https://www.rfc-editor.org/rfc/rfc4616.html
    class Other(mechanism: String): SaslMechanism(mechanism);

    companion object {
        public val MECHANISMS by lazy { arrayOf(Plain) }

        fun byMechanism(mechanism: String): SaslMechanism {
            return MECHANISMS.find { it.mechanism.contentEquals(mechanism) } ?: Other(mechanism)
        }
    }
}