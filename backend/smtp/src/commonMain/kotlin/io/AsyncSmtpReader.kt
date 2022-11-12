package dev.sitar.kmail.smtp.io

import dev.sitar.kio.async.readers.AsyncReader
import dev.sitar.kmail.smtp.*

public class AsyncSmtpReader(public val reader: AsyncReader) : AsyncReader by reader {
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

    public suspend fun readSmtpCode(): Int {
        return readStringUtf8(3).toInt()
    }

    // TODO: instead of always expecting T (a good result), take a map (status code -> expected reply)
    public suspend inline fun <reified T : SmtpReply> readSmtpReply(): T {
        return when (readSmtpCode()) {
            220 -> GreetReply.Serializer.deserialize(this) as T
            221 -> OkReply.Serializer.deserialize(this) as T
            250 -> {
                when (T::class) {
                    EhloReply::class -> EhloReply.Serializer.deserialize(this) as T
                    OkReply::class -> OkReply.Serializer.deserialize(this) as T
                    else -> TODO()
                }
            }
            354 -> StartMailInputReply.Serializer.deserialize(this) as T
            else -> TODO()
        }
    }
}

public fun AsyncReader.asAsyncSmtpReader(): AsyncSmtpReader {
    return AsyncSmtpReader(this)
}