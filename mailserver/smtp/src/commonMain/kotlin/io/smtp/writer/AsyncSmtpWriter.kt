package dev.sitar.kmail.smtp.io.smtp.writer

import dev.sitar.kmail.utils.io.AsyncWriterStream

public interface AsyncSmtpWriter : AsyncWriterStream {
    public suspend fun writeIsFinal(isFinal: Boolean = true) {
        if (isFinal) write(' '.code.toByte()) else write('-'.code.toByte())
    }
}