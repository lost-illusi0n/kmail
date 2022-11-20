package dev.sitar.kmail.smtp.io.smtp.reader

import dev.sitar.kio.async.readers.AsyncReader
import dev.sitar.kmail.smtp.*
import dev.sitar.kmail.smtp.frames.replies.SmtpReply
import dev.sitar.kmail.smtp.frames.replies.tryAs
import dev.sitar.kmail.smtp.io.readStringUtf8
import dev.sitar.kmail.smtp.io.readUtf8StringUntil

public class AsyncSmtpClientReader(reader: AsyncReader) : AsyncSmtpReader, AsyncReader by reader {
    public suspend fun readSmtpCode(): Int {
        return readStringUtf8(3).toInt()
    }

    // TODO: instead of always expecting T (a good result), take a map (status code -> expected reply)
    public suspend fun readSmtpReply(): SmtpReply<*> {
        val body = deserializeBody()
        val code = body.first().first
        val lines = body.map { it.second }

        // we check the first digit of the code and deserialize as a category.
        return when (code / 100) {
            SmtpReply.PositiveCompletion.DIGIT -> SmtpReply.PositiveCompletion.Default(code, lines)
            SmtpReply.PositiveIntermediate.DIGIT -> SmtpReply.PositiveIntermediate.Default(code, lines)
            SmtpReply.PermanentNegative.DIGIT -> SmtpReply.PermanentNegative.Default(code, lines)
            SmtpReply.TransientNegative.DIGIT -> SmtpReply.TransientNegative.Default(code, lines)
            else -> error("unknown code: $code")
        }
    }

    private suspend fun deserializeBody(): List<Pair<Int, String>> {
        var isFinal: Boolean

        return buildList {
            do {
                val code = readSmtpCode()
                isFinal = readIsFinal()
                add(code to readUtf8UntilSmtpEnding())
            } while (!isFinal)
        }
    }
}

public fun AsyncReader.asAsyncSmtpClientReader(): AsyncSmtpClientReader {
    return AsyncSmtpClientReader(this)
}