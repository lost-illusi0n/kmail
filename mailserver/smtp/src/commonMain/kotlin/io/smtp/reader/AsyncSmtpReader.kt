package dev.sitar.kmail.smtp.io.smtp.reader

import dev.sitar.kio.async.readers.AsyncReader

public interface AsyncSmtpReader : AsyncReader {
    public suspend fun readIsFinal(): Boolean {
        return read() == ' '.code.toByte()
    }
}