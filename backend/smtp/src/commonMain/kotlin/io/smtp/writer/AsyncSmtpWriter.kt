package dev.sitar.kmail.smtp.io.smtp.writer

import dev.sitar.kio.async.writers.AsyncWriter
import dev.sitar.kmail.smtp.*
import dev.sitar.kmail.smtp.io.AsyncByteWriteChannelWriter
import dev.sitar.kmail.smtp.io.writeStringUtf8
import io.ktor.utils.io.*

public interface AsyncSmtpWriter : AsyncWriter, ByteWriteChannel {
    public suspend fun writeIsFinal(isFinal: Boolean = true) {
        if (isFinal) write(' '.code.toByte()) else write('-'.code.toByte())
    }

    public suspend fun endLine() {
        writeStringUtf8(SMTP_LINE_ENDING)
    }
}