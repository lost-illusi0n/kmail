package dev.sitar.kmail.smtp.frames.replies

import dev.sitar.kmail.smtp.io.smtp.writer.AsyncSmtpServerWriter
import dev.sitar.kmail.smtp.io.writeStringUtf8

public data class ReadyToStartTlsCompletion(val string: String) :
    SmtpReply.PositiveCompletion by SmtpReply.PositiveCompletion.Default(code = 220, data = string) {
    public companion object {
        public fun from(data: String): ReadyToStartTlsCompletion {
            return ReadyToStartTlsCompletion(data)
        }

        public suspend fun serialize(output: AsyncSmtpServerWriter, tls: ReadyToStartTlsCompletion) {
            output.writeIsFinal(true)
            output.writeStringUtf8(tls.string)
            output.endLine()
        }
    }
}