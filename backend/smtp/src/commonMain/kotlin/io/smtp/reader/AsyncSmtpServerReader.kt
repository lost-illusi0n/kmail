package dev.sitar.kmail.smtp.io.smtp.reader

import dev.sitar.kio.async.readers.AsyncReader
import dev.sitar.kio.buffers.DefaultBufferPool
import dev.sitar.kio.fullSlice
import dev.sitar.kio.use
import dev.sitar.kmail.message.Message
import dev.sitar.kmail.smtp.SmtpCommand
import dev.sitar.kmail.smtp.SmtpCommandTag
import dev.sitar.kmail.utils.io.readUtf8StringUntil
import kotlin.math.max

public class AsyncSmtpServerReader(reader: AsyncReader) : AsyncSmtpReader, AsyncReader by reader {
    private companion object {
        private val MESSAGE_TERMINATING_SEQUENCE = "\r\n.\r\n".toByteArray().fullSlice()
    }

    private suspend fun readSmtpCommandTag(): String {
        var lastChar = '\u0000'

        val tag = readUtf8StringUntil {
            val t = it == ' ' || (lastChar == '\r' && it == '\n')
            lastChar = it
            t
        }

        return if (lastChar == '\n') tag.dropLast(1) else tag
    }

    public suspend fun readSmtpCommand(): SmtpCommand {
        val rawTag = readSmtpCommandTag()
        val tag = SmtpCommandTag.fromTag(rawTag) ?: error("unknown command: $rawTag")

        return tag.serializer.deserialize(this)
    }

    public suspend fun readMailInput(): Message {
        val data: ByteArray = DefaultBufferPool.use(32) { resultBuffer ->
            for (byte in this) {
                resultBuffer.write(byte)

                val lastFive = resultBuffer[max(0, resultBuffer.writeIndex - 5)..resultBuffer.writeIndex]

                // check if terminating sequence is found at the end of the result buffer
                if (lastFive.contentEquals(MESSAGE_TERMINATING_SEQUENCE)) {
                    resultBuffer.writeIndex -= 3 // the first <CRLF> is part of the body. don't need to remove it
                    return@use resultBuffer.toByteArray()
                }
            }

            TODO("input stopped but terminating sequence not found after data")
        }

        return Message.fromText(data.decodeToString().removePrefix("\r\n"))
    }
}

public fun AsyncReader.asAsyncSmtpServerReader(): AsyncSmtpServerReader {
    return AsyncSmtpServerReader(this)
}