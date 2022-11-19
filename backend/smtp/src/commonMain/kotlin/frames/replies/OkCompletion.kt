package dev.sitar.kmail.smtp.frames.replies

import dev.sitar.kmail.smtp.io.smtp.writer.AsyncSmtpServerWriter
import dev.sitar.kmail.smtp.io.writeStringUtf8

public data class OkCompletion(override val data: String) :
    SmtpReply.PositiveCompletion by SmtpReply.PositiveCompletion.Default(code = 250, data = data) {
    public companion object {
        public suspend fun serialize(output: AsyncSmtpServerWriter, ok: OkCompletion) {
            output.writeIsFinal(true)
            output.writeStringUtf8(ok.data)
            output.endLine()
        }

        public fun from(data: String): OkCompletion {
            return OkCompletion(data)
        }
    }
}