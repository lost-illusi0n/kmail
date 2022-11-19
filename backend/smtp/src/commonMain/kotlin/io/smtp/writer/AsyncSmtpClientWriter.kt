package dev.sitar.kmail.smtp.io.smtp.writer

import dev.sitar.kio.async.writers.AsyncWriter
import dev.sitar.kmail.smtp.*
import dev.sitar.kmail.smtp.frames.replies.*
import dev.sitar.kmail.smtp.io.AsyncByteWriteChannelWriter
import dev.sitar.kmail.smtp.io.writeStringUtf8
import io.ktor.utils.io.*

public class AsyncSmtpServerWriter(channelWriter: AsyncByteWriteChannelWriter) : AsyncSmtpWriter, AsyncWriter by channelWriter, ByteWriteChannel by channelWriter {
    public suspend fun writeStatusCode(status: Int) {
        writeStringUtf8(status.toString())
    }

    public suspend inline fun <reified T : SmtpReply<*>> writeReply(reply: T) {
        writeStatusCode(reply.code)

        when (T::class) {
            GreetCompletion::class -> GreetCompletion.serialize(this, reply as GreetCompletion)
            OkCompletion::class -> OkCompletion.serialize(this, reply as OkCompletion)
            EhloCompletion::class -> EhloCompletion.serialize(this, reply as EhloCompletion)
            StartMailInputIntermediary::class -> StartMailInputIntermediary.serialize(this, reply as StartMailInputIntermediary)
        }

        flush()
    }
}

public fun AsyncByteWriteChannelWriter.asAsyncSmtpServerWriter(): AsyncSmtpServerWriter {
    return AsyncSmtpServerWriter(this)
}