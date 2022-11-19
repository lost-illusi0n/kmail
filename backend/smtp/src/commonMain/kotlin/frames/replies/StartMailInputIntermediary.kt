package dev.sitar.kmail.smtp.frames.replies

import dev.sitar.kmail.smtp.io.smtp.writer.AsyncSmtpServerWriter
import dev.sitar.kmail.smtp.io.writeStringUtf8

public data class StartMailInputIntermediary(val string: String) :
    SmtpReply.PositiveIntermediate by SmtpReply.PositiveIntermediate.Default(code = 354, string) {
    public companion object {
        public suspend fun serialize(output: AsyncSmtpServerWriter, startMailInput: StartMailInputIntermediary) {
            output.writeIsFinal(true)
            output.writeStringUtf8(startMailInput.string)
            output.endLine()
        }

        public fun from(data: String): StartMailInputIntermediary {
            return StartMailInputIntermediary(data)
        }
    }
}