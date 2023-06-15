package dev.sitar.kmail.smtp.frames.reply

import dev.sitar.kmail.smtp.Domain

public data class EhloReply(
    val domain: Domain,
    val greet: String?,
    val capabilities: List<String>
): SmtpReply {
    override val code: SmtpReplyCode = SmtpReplyCode.PositiveCompletion(250)

    override suspend fun serialize(): SmtpReply.Raw {
        return SmtpReply.Raw(code, listOf("${domain.asString()}${greet.coerceAndSpace()}") + capabilities)
    }

    public companion object: SmtpReplyDeserializer<EhloReply> {
        override fun deserialize(raw: SmtpReply.Raw): EhloReply {
            val domain: String
            var greet: String? = null

            val first = raw.lines.first()
            when (val index = first.indexOf(' ')) {
                -1 -> domain = first
                else -> {
                    domain = first.substring(0..index)
                    greet = first.substring(index)
                }
            }

            val capabilities = raw.lines.drop(1)

            return EhloReply(Domain.fromText(domain)!!, greet, capabilities)
        }
    }
}