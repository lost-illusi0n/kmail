package dev.sitar.kmail.smtp.frames.replies

import dev.sitar.kmail.smtp.io.smtp.reader.AsyncSmtpReader
import dev.sitar.kmail.smtp.io.smtp.writer.AsyncSmtpServerWriter
import dev.sitar.kmail.smtp.io.writeStringUtf8

public data class GreetCompletion(val greet: String) :
    SmtpReply.PositiveCompletion by SmtpReply.PositiveCompletion.Default(code = 220, data = "$greet") {
    public companion object {
        public fun from(data: String): GreetCompletion {
            return GreetCompletion(data)
        }

        public suspend fun serialize(output: AsyncSmtpServerWriter, greet: GreetCompletion) {
            output.writeIsFinal(true)
            output.writeStringUtf8(greet.greet)
            output.endLine()
        }
    }
}