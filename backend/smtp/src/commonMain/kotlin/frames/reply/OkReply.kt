package dev.sitar.kmail.smtp.frames.reply

public data class OkReply(val data: String): SmtpReply {
    override val code: SmtpReplyCode = SmtpReplyCode.PositiveCompletion(250)

    override suspend fun serialize(): SmtpReply.Raw {
        return SmtpReply.Raw(code, listOf(data))
    }

    public companion object: SmtpReplyDeserializer<OkReply> {
        override fun deserialize(raw: SmtpReply.Raw): OkReply {
            return OkReply(raw.lines.first())
        }
    }
}