package dev.sitar.kmail.smtp.frames.replies

import dev.sitar.kmail.smtp.io.smtp.writer.AsyncSmtpServerWriter
import dev.sitar.kmail.utils.io.writeLineEnd
import dev.sitar.kmail.utils.io.writeStringUtf8

public data class ReadyToStartTlsCompletion(val string: String) :
    SmtpReply.PositiveCompletion by SmtpReply.PositiveCompletion.Default(code = 220, lines = listOf(string)) {
    public companion object {
        public fun from(lines: List<String>): ReadyToStartTlsCompletion {
            return ReadyToStartTlsCompletion(lines.first())
        }

        public suspend fun serialize(output: AsyncSmtpServerWriter, tls: ReadyToStartTlsCompletion) {
            output.writeIsFinal(true)
            output.writeStringUtf8(tls.string)
            output.writeLineEnd()
        }
    }
}