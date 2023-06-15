package dev.sitar.kmail.smtp.io.smtp.reader

import dev.sitar.kio.async.readers.AsyncReader
import dev.sitar.kmail.smtp.frames.reply.SmtpReply
import dev.sitar.kmail.smtp.frames.reply.SmtpReplyCode
import dev.sitar.kmail.utils.io.readStringUtf8
import dev.sitar.kmail.utils.io.readUtf8UntilLineEnd

public class AsyncSmtpClientReader(reader: AsyncReader) : AsyncSmtpReader, AsyncReader by reader {
    public suspend fun readSmtpCode(): Int {
        return readStringUtf8(3).toInt()
    }

    public suspend fun readSmtpReply(): SmtpReply.Raw {
        val body = deserializeBody()
        val code = body.first().first
        val lines = body.map { it.second }

//        return type.deserialize(SmtpReply.Raw(SmtpReplyCode.from(code), lines)) ?: TODO()
        return SmtpReply.Raw(SmtpReplyCode.from(code), lines)
    }

    private suspend fun deserializeBody(): List<Pair<Int, String>> {
        var isFinal: Boolean

        return buildList {
            do {
                val code = readSmtpCode()
                isFinal = readIsFinal()
                add(code to readUtf8UntilLineEnd())
            } while (!isFinal)
        }
    }
}

public fun AsyncReader.asAsyncSmtpClientReader(): AsyncSmtpClientReader {
    return AsyncSmtpClientReader(this)
}