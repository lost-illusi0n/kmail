package dev.sitar.kmail.smtp.io.smtp.writer

import dev.sitar.kmail.smtp.frames.replies.SmtpReply
import dev.sitar.kmail.utils.io.AsyncWriterStream
import dev.sitar.kmail.utils.io.writeLineEnd
import dev.sitar.kmail.utils.io.writeStringUtf8

public class AsyncSmtpServerWriter(writer: AsyncWriterStream) : AsyncSmtpWriter, AsyncWriterStream by writer {
    public suspend fun writeStatusCode(status: Int) {
        writeStringUtf8(status.toString())
    }

    public suspend fun writeReply(reply: SmtpReply<*>) {
        reply.lines.forEachIndexed { i, line ->
            writeStatusCode(reply.code)
            writeIsFinal(i == reply.lines.size - 1)
            writeStringUtf8(line)
            writeLineEnd()
        }

        flush()
    }
}

public fun AsyncWriterStream.asAsyncSmtpServerWriter(): AsyncSmtpServerWriter {
    return AsyncSmtpServerWriter(this)
}