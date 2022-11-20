package dev.sitar.kmail.smtp.io.smtp.writer

import dev.sitar.kmail.smtp.*
import dev.sitar.kmail.smtp.io.AsyncWriterStream
import dev.sitar.kmail.smtp.io.writeStringUtf8

public interface AsyncSmtpWriter : AsyncWriterStream {
    public suspend fun writeIsFinal(isFinal: Boolean = true) {
        if (isFinal) write(' '.code.toByte()) else write('-'.code.toByte())
    }

    public suspend fun endLine() {
        writeStringUtf8(SMTP_LINE_ENDING)
    }
}