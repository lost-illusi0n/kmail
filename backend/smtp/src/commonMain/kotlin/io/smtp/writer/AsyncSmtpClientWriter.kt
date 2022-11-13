package dev.sitar.kmail.smtp.io.smtp.writer

import dev.sitar.kio.async.writers.AsyncWriter
import dev.sitar.kmail.smtp.*
import dev.sitar.kmail.smtp.io.AsyncByteWriteChannelWriter
import dev.sitar.kmail.smtp.io.writeStringUtf8
import io.ktor.utils.io.*

public class AsyncSmtpServerWriter(channelWriter: AsyncByteWriteChannelWriter) : AsyncSmtpWriter, AsyncWriter by channelWriter, ByteWriteChannel by channelWriter {
    public suspend fun writeStatusCode(status: Int) {
        writeStringUtf8(status.toString())
    }

    public suspend inline fun <reified T : SmtpReply> writeReply(status: Int, reply: T) {
        writeStatusCode(status)

        when (T::class) {
            GreetReply::class -> GreetReply.Serializer.serialize(this, reply as GreetReply)
            OkReply::class -> OkReply.Serializer.serialize(this, reply as OkReply)
            EhloReply::class -> EhloReply.Serializer.serialize(this, reply as EhloReply)
            StartMailInputReply::class -> StartMailInputReply.Serializer.serialize(this)
        }

        flush()
    }
}

public fun AsyncByteWriteChannelWriter.asAsyncSmtpServerWriter(): AsyncSmtpServerWriter {
    return AsyncSmtpServerWriter(this)
}