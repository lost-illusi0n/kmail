package dev.sitar.kmail.smtp.io.smtp.writer

import dev.sitar.kio.async.writers.AsyncWriter
import dev.sitar.kmail.smtp.*
import dev.sitar.kmail.smtp.io.AsyncWriterStream
import dev.sitar.kmail.smtp.io.writeStringUtf8
import io.ktor.utils.io.*

public class AsyncSmtpClientWriter(writer: AsyncWriterStream) : AsyncSmtpWriter, AsyncWriterStream by writer {
    public suspend fun <T: SmtpCommand> writeDiscriminator(command: T) {
        if (command.discriminator.isNotEmpty()) writeStringUtf8(command.discriminator)
    }

    public suspend inline fun <reified T : SmtpCommand> writeCommand(command: T) {
        writeDiscriminator(command)

        when (T::class) {
            EhloCommand::class -> EhloCommand.Serializer.serialize(command as EhloCommand, this)
            MailCommand::class -> MailCommand.Serializer.serialize(command as MailCommand, this)
            RecipientCommand::class -> RecipientCommand.Serializer.serialize(command as RecipientCommand, this)
            DataCommand::class -> DataCommand.Serializer.serialize(this)
            MailInputCommand::class -> MailInputCommand.Serializer.serialize(command as MailInputCommand, this)
            QuitCommand::class -> QuitCommand.Serializer.serialize(this)
            StartTlsCommand::class -> StartTlsCommand.Serializer.serialize(this)
            AuthenticationCommand::class -> AuthenticationCommand.Serializer.serialize(command as AuthenticationCommand, this)
            else -> error("command not serialized")
        }

        flush()
    }
}

public fun AsyncWriterStream.asAsyncSmtpClientWriter(): AsyncSmtpClientWriter {
    return AsyncSmtpClientWriter(this)
}