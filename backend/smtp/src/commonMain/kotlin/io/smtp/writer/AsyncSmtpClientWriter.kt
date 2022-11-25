package dev.sitar.kmail.smtp.io.smtp.writer

import dev.sitar.kmail.smtp.SMTP_LINE_ENDING
import dev.sitar.kmail.smtp.frames.replies.*
import dev.sitar.kmail.smtp.io.AsyncWriterStream
import dev.sitar.kmail.smtp.io.writeStringUtf8

public class AsyncSmtpServerWriter(writer: AsyncWriterStream) : AsyncSmtpWriter, AsyncWriterStream by writer {
    public suspend fun writeStatusCode(status: Int) {
        writeStringUtf8(status.toString())
    }

    public suspend fun writeReply(reply: SmtpReply<*>) {
        reply.lines.forEachIndexed { i, line ->
            writeStatusCode(reply.code)
            writeIsFinal(i == reply.lines.size - 1)
            writeStringUtf8(line)
            endLine()
        }

        flush()
    }
}

public fun AsyncWriterStream.asAsyncSmtpServerWriter(): AsyncSmtpServerWriter {
    return AsyncSmtpServerWriter(this)
}