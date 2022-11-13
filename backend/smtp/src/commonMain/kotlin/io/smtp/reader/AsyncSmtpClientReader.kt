package dev.sitar.kmail.smtp.io.smtp.reader

import dev.sitar.kio.async.readers.AsyncReader
import dev.sitar.kmail.smtp.*
import dev.sitar.kmail.smtp.io.readStringUtf8
import dev.sitar.kmail.smtp.io.readUtf8StringUntil

public class AsyncSmtpClientReader(reader: AsyncReader) : AsyncSmtpReader, AsyncReader by reader {
    public suspend fun readSmtpCode(): Int {
        return readStringUtf8(3).toInt()
    }

    // TODO: instead of always expecting T (a good result), take a map (status code -> expected reply)
    public suspend inline fun <reified T : SmtpReply> readSmtpReply(): T {
        return when (val code = readSmtpCode()) {
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
            else -> error("missing reply $code: ${readUtf8UntilSmtpEnding()}")
        }
    }
}

public fun AsyncReader.asAsyncSmtpClientReader(): AsyncSmtpClientReader {
    return AsyncSmtpClientReader(this)
}