package dev.sitar.kmail.smtp.frames.replies

import dev.sitar.kmail.smtp.io.smtp.writer.AsyncSmtpServerWriter
import dev.sitar.kmail.utils.io.writeLineEnd
import dev.sitar.kmail.utils.io.writeStringUtf8

public data class OkCompletion(val data: String) :
    SmtpReply.PositiveCompletion by SmtpReply.PositiveCompletion.Default(code = 250, lines = listOf(data)) {
    public companion object {
        // TODO: we can probably get rid of this manual serialization as we can just serialize the lines property
        public suspend fun serialize(output: AsyncSmtpServerWriter, ok: OkCompletion) {
            output.writeIsFinal(true)
            output.writeStringUtf8(ok.data)
            output.writeLineEnd()
        }

        public fun from(lines: List<String>): OkCompletion {
            return OkCompletion(lines.first())
        }
    }
}