package dev.sitar.kmail.smtp

import dev.sitar.dns.utils.NetworkAddress
import dev.sitar.kio.fullSlice

public data class Path(val atDomain: String?, val mailbox: Mailbox) {
    public fun asText(): String {
        return "<${atDomain?.plus(":") ?: ""}${mailbox.asText()}>"
    }

    public companion object {
        public fun fromText(text: String): Path? {
            if (!text.startsWith('<') || !text.endsWith('>')) return null

            val path = text.substring(1, text.length - 1)

            var atDomain: String? = null
            val mailboxText: String

            when (val separator = path.indexOf(':')) {
                -1 -> mailboxText = path
                else -> {
                    mailboxText = path.substring(separator + 1)
                    atDomain = path.substring(0, separator)
                }
            }

            val mailbox = Mailbox.fromText(mailboxText) ?: return null

            return Path(atDomain, mailbox)
        }
    }
}

public sealed interface Domain {
    @JvmInline
    public value class AddressLiteral(public val networkAddress: NetworkAddress): Domain {
        override fun asString(): String {
            return "[${networkAddress}]"
        }
    }

    @JvmInline
    public value class Actual(public val domain: String): Domain {
        override fun asString(): String {
            return domain
        }
    }

    public fun asString(): String

    public companion object {
        public fun fromText(text: String): Domain? {
            if (text.startsWith('[') && text.endsWith(']')) {
                val addressLiteral = text.substring(1, text.length - 1)

                // TODO: support ivp6
                val data = addressLiteral
                    .split('.')
                    .takeIf { it.size == 4 }
                    ?.map { it.toUByte() }
                    ?.toUByteArray() ?: return null

                return AddressLiteral(NetworkAddress.Ipv4Address(data.asByteArray().fullSlice()))
            }

            return Actual(text)
        }
    }
}

public data class Mailbox(
    val localPart: String,
    val domain: Domain,
) {
    public fun asText(): String {
        return "$localPart@${domain.asString()}"
    }

    public companion object {
        public fun fromText(text: String): Mailbox? {
            val parts = text.split('@')
            if (parts.size != 2) return null
            val localPart = parts[0]
            val domainText = parts[1]

            val domain = Domain.fromText(domainText) ?: return null
            return Mailbox(localPart, domain)
        }
    }
}