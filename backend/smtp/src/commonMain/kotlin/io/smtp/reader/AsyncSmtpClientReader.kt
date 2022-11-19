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
    public suspend inline fun readSmtpReply(): SmtpReply<*> {
        val code = readSmtpCode()

        // we check the first digit of the code and deserialize as a category.
        return when (code / 100) {
            SmtpReply.PositiveCompletion.DIGIT -> SmtpReply.PositiveCompletion.deserialize(code, this)
            SmtpReply.PositiveIntermediate.DIGIT -> SmtpReply.PositiveIntermediate.deserialize(code, this)
            SmtpReply.PermanentNegative.DIGIT -> SmtpReply.PermanentNegative.deserialize(code, this)
            SmtpReply.TransientNegative.DIGIT -> SmtpReply.TransientNegative.deserialize(code, this)
            else -> error("unknown code: $code")
        }
    }
}

public fun AsyncReader.asAsyncSmtpClientReader(): AsyncSmtpClientReader {
    return AsyncSmtpClientReader(this)
}