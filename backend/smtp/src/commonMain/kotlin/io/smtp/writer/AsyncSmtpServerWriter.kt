package dev.sitar.kmail.smtp.io.smtp.writer

import dev.sitar.kmail.message.Message
import dev.sitar.kmail.smtp.*
import dev.sitar.kmail.utils.io.AsyncWriterStream
import dev.sitar.kmail.utils.io.writeLineEnd
import dev.sitar.kmail.utils.io.writeStringUtf8
import io.ktor.utils.io.*

public class AsyncSmtpClientWriter(writer: AsyncWriterStream) : AsyncSmtpWriter, AsyncWriterStream by writer {
    public suspend fun writeDiscriminator(command: SmtpCommand) {
        writeStringUtf8(command.tag.name.uppercase())
    }

    public suspend inline fun <reified T : SmtpCommand> writeCommand(command: T) {
        writeDiscriminator(command)
        writeIsFinal() // AFAIK all commands are one line

        @Suppress("UNCHECKED_CAST") // we own this enum. this should always be safe
        (command.tag.serializer as SmtpCommandSerializer<T>).serialize(command, this)

        flush()
    }

    public suspend fun writeMessageData(message: Message) {
        writeStringUtf8(message.asText())
        write('.'.code.toByte())
        writeLineEnd()

        flush()
    }
}

public fun AsyncWriterStream.asAsyncSmtpClientWriter(): AsyncSmtpClientWriter {
    return AsyncSmtpClientWriter(this)
}