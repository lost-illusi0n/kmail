package dev.sitar.kmail.smtp.io.smtp.writer

import dev.sitar.kmail.smtp.frames.replies.*
import dev.sitar.kmail.smtp.io.AsyncWriterStream
import dev.sitar.kmail.smtp.io.writeStringUtf8

public class AsyncSmtpServerWriter(writer: AsyncWriterStream) : AsyncSmtpWriter, AsyncWriterStream by writer {
    public suspend fun writeStatusCode(status: Int) {
        writeStringUtf8(status.toString())
    }

    public suspend inline fun <reified T : SmtpReply<*>> writeReply(reply: T) {
        writeStatusCode(reply.code)

        when (T::class) {
            ReadyToStartTlsCompletion::class -> ReadyToStartTlsCompletion.serialize(
                this,
                reply as ReadyToStartTlsCompletion
            )

            GreetCompletion::class -> GreetCompletion.serialize(this, reply as GreetCompletion)
            OkCompletion::class -> OkCompletion.serialize(this, reply as OkCompletion)
            EhloCompletion::class -> EhloCompletion.serialize(this, reply as EhloCompletion)
            StartMailInputIntermediary::class -> StartMailInputIntermediary.serialize(this, reply as StartMailInputIntermediary)
        }

        flush()
    }
}

public fun AsyncWriterStream.asAsyncSmtpServerWriter(): AsyncSmtpServerWriter {
    return AsyncSmtpServerWriter(this)
}