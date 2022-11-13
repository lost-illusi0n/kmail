package dev.sitar.kmail.smtp.io.smtp.reader

import dev.sitar.kio.async.readers.AsyncReader
import dev.sitar.kmail.smtp.io.readStringUtf8
import dev.sitar.kmail.smtp.io.readUtf8StringUntil

public interface AsyncSmtpReader : AsyncReader {
    public suspend fun readUtf8UntilSmtpEnding(): String {
        var lastChar = ' '

        return readUtf8StringUntil {
            val isEnding = lastChar == '\r' && it == '\n'
            lastChar = it
            isEnding
        }.dropLast(1)
    }

    public suspend fun readIsFinal(): Boolean {
        return read() == ' '.code.toByte()
    }
}